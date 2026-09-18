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

    /**
     * SELECT ... FOR UPDATE：停用班级、挂服药委托、执行/关单/退回委托都先拿这把锁，
     * 同一个班的这些操作因此排成一队，谁提交在前，另一边看到的就是最新结果。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classroom c where c.id = :id")
    Optional<Classroom> findByIdForUpdate(@Param("id") Long id);
}
