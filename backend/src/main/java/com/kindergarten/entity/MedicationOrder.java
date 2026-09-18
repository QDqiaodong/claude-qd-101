package com.kindergarten.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 午间服药委托：家长签字委托班级中午给孩子喂一次药。单子只流转状态，永不删除。 */
@Entity
@Table(name = "medication_order")
public class MedicationOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 挂在哪个班级（只能挂「使用中」的班级） */
    @Column(name = "classroom_id", nullable = false)
    public Long classroomId;

    /** 孩子怎么称呼 */
    @Column(name = "child_name", nullable = false, length = 32)
    public String childName;

    /** 药品名称 */
    @Column(name = "medicine_name", nullable = false, length = 64)
    public String medicineName;

    /** 这一次剂量 */
    @Column(nullable = false, length = 64)
    public String dose;

    /** 家长签字日 */
    @Column(name = "parent_sign_date", nullable = false)
    public LocalDate parentSignDate;

    /** 未执行 / 已执行 / 已关闭 / 已退回 */
    @Column(nullable = false, length = 16)
    public String status;

    /** 实际喂药时刻（点「已执行」时记下） */
    @Column(name = "actual_time")
    public LocalDateTime actualTime;

    /** 关单原因（孩子没来 / 拒服等，关单必填） */
    @Column(name = "close_reason", length = 255)
    public String closeReason;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
