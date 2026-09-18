package com.kindergarten.repository;

import com.kindergarten.entity.MedicationOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, Long> {

    List<MedicationOrder> findAllByOrderByIdDesc();

    long countByClassroomIdAndStatus(Long classroomId, String status);

    /** 只取班级 id 的标量查询：不把实体带进持久化上下文，随后的锁读才能拿到最新行。 */
    @Query("select m.classroomId from MedicationOrder m where m.id = :id")
    Optional<Long> findClassroomIdById(@Param("id") Long id);

    /** SELECT ... FOR UPDATE：和「停用班级」抢同一把班级行锁之后再读，看到的必是已提交的最新状态。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MedicationOrder m where m.id = :id")
    Optional<MedicationOrder> findByIdForUpdate(@Param("id") Long id);
}
