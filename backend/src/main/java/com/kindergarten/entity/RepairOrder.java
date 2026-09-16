package com.kindergarten.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 教具报修单：破损教具走一张单子，结案后才能回到可用。 */
@Entity
@Table(name = "repair_order")
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "aid_id", nullable = false)
    public Long aidId;

    /** 点检 / 报修 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(name = "fault_desc", length = 255)
    public String faultDesc;

    @Column(nullable = false, length = 32)
    public String reporter;

    /** 待处理 / 维修中 / 已结案 */
    @Column(nullable = false, length = 16)
    public String status;

    /** 合格 / 退回维修 */
    @Column(length = 16)
    public String conclusion;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
