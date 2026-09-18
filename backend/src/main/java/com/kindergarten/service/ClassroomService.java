package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.MedicationDelegation;
import com.kindergarten.entity.TeachingAid;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.MedicationDelegationRepository;
import com.kindergarten.repository.TeachingAidRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomService {

    private final ClassroomRepository classrooms;
    private final TeachingAidRepository aids;
    private final MedicationDelegationRepository delegations;

    public ClassroomService(ClassroomRepository classrooms, TeachingAidRepository aids,
                            MedicationDelegationRepository delegations) {
        this.classrooms = classrooms;
        this.aids = aids;
        this.delegations = delegations;
    }

    public List<Classroom> list(String status, String keyword) {
        return classrooms.findAllByOrderByIdAsc().stream()
                .filter(c -> status == null || status.isEmpty() || status.equals(c.status))
                .filter(c -> keyword == null || keyword.isEmpty()
                        || c.name.contains(keyword) || c.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Classroom create(Classroom input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("班级编号不能为空");
        }
        if (classrooms.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的班级用掉了");
        }
        if (input.capacity != null && input.capacity <= 0) {
            throw new BizException("可容纳人数要大于 0");
        }
        Classroom saved = new Classroom();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.capacity = input.capacity == null ? 20 : input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "使用中" : input.status;
        return classrooms.save(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Classroom update(Long id, Classroom input) {
        Classroom c = classrooms.findById(id).orElseThrow(() -> new BizException("班级不存在"));
        if (input.name != null) {
            c.name = input.name;
        }
        if (input.capacity != null && !input.capacity.equals(c.capacity)) {
            if (input.capacity <= 0) {
                throw new BizException("可容纳人数要大于 0");
            }
            c.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(c.status)) {
            if ("停用".equals(input.status)) {
                // 停用和新挂委托、执行委托抢同一把班级行锁，避免一边停用、一边落执行。
                classrooms.findForUpdateById(c.id).orElseThrow(() -> new BizException("班级不存在"));
                List<String> blockers = new ArrayList<>();

                List<TeachingAid> own = aids.findByClassroomId(c.id);
                if (!own.isEmpty()) {
                    blockers.add("这个班名下还有 " + own.size()
                            + " 件教具，先都挪走或者处理掉才能停用");
                }

                List<MedicationDelegation> pending = delegations
                        .findByClassroomIdAndStatusOrderByIdAsc(c.id, "未执行");
                if (!pending.isEmpty()) {
                    String detail = pending.stream()
                            .limit(5)
                            .map(d -> d.childName + "·" + d.medicineName)
                            .collect(Collectors.joining("、"));
                    String more = pending.size() > 5 ? " 等" : "";
                    blockers.add("这个班还有 " + pending.size()
                            + " 张未执行服药委托（" + detail + more
                            + "）；当班老师先记下实际喂药时刻并点已执行，或由家长把委托退回后才能停用。"
                            + "本次停用已顶回：班级仍是使用中，委托仍是未执行，不会自动作废");
                }

                if (!blockers.isEmpty()) {
                    throw new BizException(String.join("；", blockers));
                }
            }
            c.status = input.status;
        }
        return classrooms.save(c);
    }
}
