package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.MedicationOrder;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.MedicationOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationService {

    private final MedicationOrderRepository orders;
    private final ClassroomRepository classrooms;

    public MedicationService(MedicationOrderRepository orders, ClassroomRepository classrooms) {
        this.orders = orders;
        this.classrooms = classrooms;
    }

    public List<MedicationOrder> list(Long classroomId, String status) {
        return orders.findAllByOrderByIdDesc().stream()
                .filter(o -> classroomId == null || classroomId.equals(o.classroomId))
                .filter(o -> status == null || status.isEmpty() || status.equals(o.status))
                .toList();
    }

    @Transactional
    public MedicationOrder create(MedicationOrder input) {
        if (input.classroomId == null) {
            throw new BizException("请选择要挂的班级");
        }
        if (input.childName == null || input.childName.isBlank()) {
            throw new BizException("孩子怎么称呼要写清");
        }
        if (input.medicineName == null || input.medicineName.isBlank()) {
            throw new BizException("药品名称要写清");
        }
        if (input.dose == null || input.dose.isBlank()) {
            throw new BizException("这一次剂量要写清");
        }
        if (input.parentSignDate == null) {
            throw new BizException("家长签字日要填");
        }
        // 先锁班级行再判断状态：停用的事务若已提交，这里看到的就是「停用」
        Classroom c = classrooms.findByIdForUpdate(input.classroomId)
                .orElseThrow(() -> new BizException("班级不存在"));
        if (!"使用中".equals(c.status)) {
            throw new BizException("「" + c.name + "」已经停用了，新的服药委托挂不上去");
        }
        MedicationOrder o = new MedicationOrder();
        o.classroomId = c.id;
        o.childName = input.childName.trim();
        o.medicineName = input.medicineName.trim();
        o.dose = input.dose.trim();
        o.parentSignDate = input.parentSignDate;
        o.status = "未执行";
        o.createdAt = LocalDateTime.now();
        o.updatedAt = o.createdAt;
        return orders.save(o);
    }

    @Transactional
    public MedicationOrder execute(Long id, LocalDateTime actualTime) {
        MedicationOrder o = lockOrder(id);
        if (!"未执行".equals(o.status)) {
            throw new BizException("这张单子已经是「" + o.status + "」了，别再重复操作");
        }
        o.status = "已执行";
        o.actualTime = actualTime != null ? actualTime : LocalDateTime.now();
        o.updatedAt = LocalDateTime.now();
        return orders.save(o);
    }

    @Transactional
    public MedicationOrder close(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException("孩子没来或拒服都要写下原因才能关单，空着不许关");
        }
        MedicationOrder o = lockOrder(id);
        if (!"未执行".equals(o.status)) {
            throw new BizException("这张单子已经是「" + o.status + "」了，别再重复操作");
        }
        o.status = "已关闭";
        o.closeReason = reason.trim();
        o.updatedAt = LocalDateTime.now();
        return orders.save(o);
    }

    @Transactional
    public MedicationOrder withdraw(Long id) {
        MedicationOrder o = lockOrder(id);
        if (!"未执行".equals(o.status)) {
            throw new BizException("这张单子已经是「" + o.status + "」了，别再重复操作");
        }
        o.status = "已退回";
        o.updatedAt = LocalDateTime.now();
        return orders.save(o);
    }

    /**
     * 动委托之前先锁它所属班级那一行：和「停用班级」排进同一支队列。
     * 停用先提交，这里读到的就是最新委托状态；这里先提交，停用那边数到的未执行就是 0。
     * 班级 id 用标量查询探一下（不进持久化上下文），随后锁读拿到的实体必是库里最新状态。
     */
    private MedicationOrder lockOrder(Long id) {
        Long classroomId = orders.findClassroomIdById(id)
                .orElseThrow(() -> new BizException("服药委托不存在"));
        classrooms.findByIdForUpdate(classroomId)
                .orElseThrow(() -> new BizException("班级不存在"));
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("服药委托不存在"));
    }
}
