package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.dto.MedicationActionRequest;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.MedicationDelegation;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.MedicationDelegationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationDelegationService {

    public static final String PENDING = "未执行";
    public static final String EXECUTED = "已执行";
    public static final String RETURNED = "家长退回";
    public static final String CLOSED = "未服关闭";

    private final MedicationDelegationRepository delegations;
    private final ClassroomRepository classrooms;

    public MedicationDelegationService(MedicationDelegationRepository delegations,
                                       ClassroomRepository classrooms) {
        this.delegations = delegations;
        this.classrooms = classrooms;
    }

    public List<MedicationDelegation> list(Long classroomId, String status, String keyword) {
        return delegations.findAllByOrderByIdDesc().stream()
                .filter(d -> classroomId == null || classroomId.equals(d.classroomId))
                .filter(d -> status == null || status.isEmpty() || status.equals(d.status))
                .filter(d -> keyword == null || keyword.isEmpty()
                        || d.childName.contains(keyword) || d.medicineName.contains(keyword))
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MedicationDelegation create(MedicationDelegation input) {
        if (input.classroomId == null) {
            throw new BizException("请选择服药委托要挂的班级");
        }
        String childName = trim(input.childName);
        if (childName.isEmpty()) {
            throw new BizException("请写清孩子怎么称呼");
        }
        String medicineName = trim(input.medicineName);
        if (medicineName.isEmpty()) {
            throw new BizException("请写清药品名称");
        }
        String dose = trim(input.dose);
        if (dose.isEmpty()) {
            throw new BizException("请写清这一次剂量");
        }
        if (input.parentSignDate == null) {
            throw new BizException("请填写家长签字日期");
        }

        // 新委托和停用抢同一个班级行：停用先拿锁时，这里会等，随后看到“停用”并拒绝。
        Classroom classroom = classrooms.findForUpdateById(input.classroomId)
                .orElseThrow(() -> new BizException("要挂委托的班级不存在"));
        if (!"使用中".equals(classroom.status)) {
            throw new BizException("班级 " + classroom.name + " 已停用，不能再挂新的服药委托");
        }

        LocalDateTime now = LocalDateTime.now();
        MedicationDelegation saved = new MedicationDelegation();
        saved.classroomId = classroom.id;
        saved.childName = childName;
        saved.medicineName = medicineName;
        saved.dose = dose;
        saved.parentSignDate = input.parentSignDate;
        saved.status = PENDING;
        saved.createdAt = now;
        saved.updatedAt = now;
        return delegations.save(saved);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MedicationDelegation execute(Long id, MedicationActionRequest action) {
        LocalDateTime executedAt = action == null ? null : action.executedAt();
        if (executedAt == null) {
            throw new BizException("请先记下实际喂药时刻，再点已执行");
        }

        MedicationDelegation delegation = lockPendingDelegation(id);
        delegation.status = EXECUTED;
        delegation.executedAt = executedAt;
        delegation.closeReason = null;
        delegation.closedAt = null;
        delegation.updatedAt = LocalDateTime.now();
        return delegations.save(delegation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MedicationDelegation returnByParent(Long id, MedicationActionRequest action) {
        MedicationDelegation delegation = lockPendingDelegation(id);
        delegation.status = RETURNED;
        delegation.closedAt = LocalDateTime.now();
        delegation.closeReason = normalizeReason(action == null ? null : action.reason());
        delegation.updatedAt = LocalDateTime.now();
        return delegations.save(delegation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MedicationDelegation closeWithoutMedicine(Long id, MedicationActionRequest action) {
        String reason = normalizeReason(action == null ? null : action.reason());
        if (reason == null) {
            throw new BizException("孩子没来或者拒服，必须写下原因才能关上这张单子");
        }

        MedicationDelegation delegation = lockPendingDelegation(id);
        delegation.status = CLOSED;
        delegation.closedAt = LocalDateTime.now();
        delegation.closeReason = reason;
        delegation.updatedAt = LocalDateTime.now();
        return delegations.save(delegation);
    }

    private MedicationDelegation lockPendingDelegation(Long id) {
        MedicationDelegation delegation = delegations.findById(id)
                .orElseThrow(() -> new BizException("服药委托不存在"));

        // 先锁班级、再锁委托，和停用班级保持一致的加锁顺序。
        Classroom classroom = classrooms.findForUpdateById(delegation.classroomId)
                .orElseThrow(() -> new BizException("委托所属班级不存在"));
        if (!"使用中".equals(classroom.status)) {
            throw new BizException("班级 " + classroom.name + " 已停用，不能处理服药委托");
        }
        delegation = delegations.findForUpdateById(id)
                .orElseThrow(() -> new BizException("服药委托不存在"));
        if (!PENDING.equals(delegation.status)) {
            throw new BizException("这张委托现在是「" + delegation.status + "」，不能重复处理");
        }
        if (!"使用中".equals(classroom.status)) {
            throw new BizException("班级 " + classroom.name
                    + " 已停用，不能处理服药委托；请刷新后核对班级状态和委托原文");
        }
        return delegation;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeReason(String reason) {
        String value = trim(reason);
        return value.isEmpty() ? null : value;
    }
}
