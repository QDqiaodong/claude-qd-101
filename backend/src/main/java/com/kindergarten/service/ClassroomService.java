package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.TeachingAid;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.MedicationOrderRepository;
import com.kindergarten.repository.TeachingAidRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomService {

    private final ClassroomRepository classrooms;
    private final TeachingAidRepository aids;
    private final MedicationOrderRepository medicationOrders;

    public ClassroomService(ClassroomRepository classrooms, TeachingAidRepository aids,
                            MedicationOrderRepository medicationOrders) {
        this.classrooms = classrooms;
        this.aids = aids;
        this.medicationOrders = medicationOrders;
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

    @Transactional
    public Classroom update(Long id, Classroom input) {
        // 停用、挂委托、执行委托都抢这同一把班级行锁：谁的事务先提交，另一边看到的就是最新结果
        Classroom c = classrooms.findByIdForUpdate(id).orElseThrow(() -> new BizException("班级不存在"));
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
                List<TeachingAid> own = aids.findByClassroomId(c.id);
                if (!own.isEmpty()) {
                    throw new BizException("这个班名下还有 " + own.size()
                            + " 件教具，先都挪走或者处理掉才能停用");
                }
                // 第二道关：还有未执行的服药委托就顶回去。委托一张不动，班级仍是使用中
                long pending = medicationOrders.countByClassroomIdAndStatus(c.id, "未执行");
                if (pending > 0) {
                    throw new BizException("停用没成功：这个班还有 " + pending
                            + " 张未执行的服药委托。班级仍是「使用中」，委托原样保留——"
                            + "请老师逐张记下喂药时刻点已执行、写清原因关单，或由家长退回，再来停用");
                }
            }
            c.status = input.status;
        }
        return classrooms.save(c);
    }
}
