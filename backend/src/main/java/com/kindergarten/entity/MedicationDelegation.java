package com.kindergarten.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 午间服药委托：家长交给班级中午执行的一次服药安排。 */
@Entity
@Table(name = "medication_delegation")
public class MedicationDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 委托挂在哪个班级；委托关闭后也保留原班级，不随班级停用被清掉 */
    @Column(name = "classroom_id", nullable = false)
    public Long classroomId;

    /** 孩子怎么称呼，如朵朵、李明（小一班） */
    @Column(name = "child_name", nullable = false, length = 32)
    public String childName;

    @Column(name = "medicine_name", nullable = false, length = 64)
    public String medicineName;

    /** 这一次剂量，如 5ml、半片 */
    @Column(nullable = false, length = 32)
    public String dose;

    /** 家长签字日期 */
    @Column(name = "parent_sign_date", nullable = false)
    public LocalDate parentSignDate;

    /** 未执行 / 已执行 / 家长退回 / 未服关闭 */
    @Column(nullable = false, length = 16)
    public String status;

    /** 当班老师实际喂药时刻；只有已执行记录这个时间 */
    @Column(name = "executed_at")
    public LocalDateTime executedAt;

    /** 家长退回或未服关闭的处理时刻 */
    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    /** 孩子没来、拒服等未服原因；未服关闭时必须填写 */
    @Column(name = "close_reason", length = 255)
    public String closeReason;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;
}
