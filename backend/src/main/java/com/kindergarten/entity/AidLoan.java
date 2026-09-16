package com.kindergarten.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 借用记录：某个班级把一件教具借走去用一段时间。 */
@Entity
@Table(name = "aid_loan")
public class AidLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "aid_id", nullable = false)
    public Long aidId;

    @Column(name = "classroom_id", nullable = false)
    public Long classroomId;

    @Column(name = "loan_date", nullable = false)
    public LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    public LocalDate dueDate;

    @Column(name = "return_date")
    public LocalDate returnDate;

    /** 在借 / 已归还 */
    @Column(nullable = false, length = 16)
    public String status;
}
