package com.kindergarten.repository;

import com.kindergarten.entity.TeachingAid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeachingAidRepository extends JpaRepository<TeachingAid, Long> {

    boolean existsByCode(String code);

    List<TeachingAid> findByClassroomId(Long classroomId);

    List<TeachingAid> findAllByOrderByIdAsc();
}
