package com.kindergarten.repository;

import com.kindergarten.entity.AidLoan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AidLoanRepository extends JpaRepository<AidLoan, Long> {

    List<AidLoan> findByAidIdAndStatus(Long aidId, String status);

    List<AidLoan> findByClassroomIdAndStatus(Long classroomId, String status);

    List<AidLoan> findAllByOrderByIdDesc();
}
