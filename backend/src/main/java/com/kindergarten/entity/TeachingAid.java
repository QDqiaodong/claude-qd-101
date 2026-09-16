package com.kindergarten.entity;

import jakarta.persistence.*;

/** 玩具教具台账：一件教具要么挂在某个班级，要么放公共区。 */
@Entity
@Table(name = "teaching_aid")
public class TeachingAid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 积木 / 绘本 / 拼图 / 乐器 / 运动 / 手工 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 归属班级；空表示放在公共区 */
    @Column(name = "classroom_id")
    public Long classroomId;

    /** 可用 / 破损 / 维修中 */
    @Column(nullable = false, length = 16)
    public String status;
}
