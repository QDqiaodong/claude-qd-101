package com.kindergarten.repository;

import com.kindergarten.entity.MedicationDelegation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationDelegationRepository extends JpaRepository<MedicationDelegation, Long> {

    long countByClassroomIdAndStatus(Long classroomId, String status);

    List<MedicationDelegation> findByClassroomIdAndStatusOrderByIdAsc(Long classroomId, String status);

    List<MedicationDelegation> findAllByOrderByIdDesc();

    /**
     * 执行、退回等动作先拿班级锁，再锁委托，保证和“停用班级”在同一条排队顺序里。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from MedicationDelegation d where d.id = :id")
    Optional<MedicationDelegation> findForUpdateById(@Param("id") Long id);
}
