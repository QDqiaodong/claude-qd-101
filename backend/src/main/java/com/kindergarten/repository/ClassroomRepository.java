package com.kindergarten.repository;

import com.kindergarten.entity.Classroom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    boolean existsByCode(String code);

    List<Classroom> findAllByOrderByIdAsc();
}
