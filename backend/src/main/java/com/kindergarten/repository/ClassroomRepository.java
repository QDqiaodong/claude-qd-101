package com.kindergarten.repository;

import com.kindergarten.entity.Classroom;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByCode(String code);

    List<Classroom> findAllByOrderByIdAsc();

    /** 停用、新挂委托、执行委托等会互相影响状态的动作都先锁班级行。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classroom c where c.id = :id")
    Optional<Classroom> findForUpdateById(@Param("id") Long id);
}
