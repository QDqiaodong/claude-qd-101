package com.kindergarten.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 消毒记录：一件教具做一次消毒。 */
@Entity
@Table(name = "disinfection")
public class Disinfection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "aid_id", nullable = false)
    public Long aidId;

    @Column(name = "disinfect_date", nullable = false)
    public LocalDate disinfectDate;

    /** 擦拭 / 浸泡 / 紫外线 */
    @Column(nullable = false, length = 16)
    public String method;

    /** 合格 / 不合格 */
    @Column(nullable = false, length = 16)
    public String result;

    @Column(nullable = false, length = 32)
    public String operator;
}
