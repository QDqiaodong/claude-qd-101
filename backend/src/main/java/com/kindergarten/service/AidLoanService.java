package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.AidLoan;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.TeachingAid;
import com.kindergarten.repository.AidLoanRepository;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.TeachingAidRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AidLoanService {

    private final AidLoanRepository loans;
    private final TeachingAidRepository aids;
    private final ClassroomRepository classrooms;

    public AidLoanService(AidLoanRepository loans, TeachingAidRepository aids,
                          ClassroomRepository classrooms) {
        this.loans = loans;
        this.aids = aids;
        this.classrooms = classrooms;
    }

    public List<AidLoan> list(Long classroomId, String status) {
        return loans.findAllByOrderByIdDesc().stream()
                .filter(l -> classroomId == null || classroomId.equals(l.classroomId))
                .filter(l -> status == null || status.isEmpty() || status.equals(l.status))
                .toList();
    }

    @Transactional
    public AidLoan create(AidLoan input) {
        if (input.aidId == null) {
            throw new BizException("请选择要借的教具");
        }
        if (input.classroomId == null) {
            throw new BizException("请选择借用的班级");
        }
        if (input.loanDate == null || input.dueDate == null) {
            throw new BizException("借出日期和应还日期都要填");
        }
        if (!input.dueDate.isAfter(input.loanDate)) {
            throw new BizException("应还日期要晚于借出日期");
        }
        TeachingAid aid = aids.findById(input.aidId)
                .orElseThrow(() -> new BizException("教具不存在"));
        Classroom room = classrooms.findById(input.classroomId)
                .orElseThrow(() -> new BizException("班级不存在"));
        if ("停用".equals(room.status)) {
            throw new BizException("班级 " + room.name + " 已经停用了，不能借教具");
        }
        if (!"可用".equals(aid.status)) {
            throw new BizException("教具 " + aid.name + " 现在是「" + aid.status + "」，借不了");
        }
        for (AidLoan busy : loans.findByAidIdAndStatus(aid.id, "在借")) {
            if (overlap(busy, input)) {
                throw new BizException("这件教具在 " + busy.loanDate + " 到 " + busy.dueDate
                        + " 之间已经被借走了");
            }
        }
        AidLoan saved = new AidLoan();
        saved.aidId = aid.id;
        saved.classroomId = room.id;
        saved.loanDate = input.loanDate;
        saved.dueDate = input.dueDate;
        saved.status = "在借";
        return loans.save(saved);
    }

    private boolean overlap(AidLoan busy, AidLoan input) {
        return !busy.loanDate.isAfter(input.dueDate) && !input.loanDate.isAfter(busy.dueDate);
    }

    @Transactional
    public AidLoan giveBack(Long id, LocalDate returnDate) {
        AidLoan loan = loans.findById(id).orElseThrow(() -> new BizException("借用记录不存在"));
        if (!"在借".equals(loan.status)) {
            throw new BizException("这条借用已经还过库里了");
        }
        loan.returnDate = returnDate == null ? LocalDate.now() : returnDate;
        loan.status = "已归还";
        return loans.save(loan);
    }
}
