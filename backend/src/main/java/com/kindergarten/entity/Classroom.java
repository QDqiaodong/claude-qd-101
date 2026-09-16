package com.kindergarten.entity;

import jakarta.persistence.*;

/** 班级（活动室）：小朋友日常活动与教具存放的屋子。 */
@Entity
@Table(name = "classroom")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 可容纳小朋友人数 */
    @Column(nullable = false)
    public Integer capacity;

    /** 使用中 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
