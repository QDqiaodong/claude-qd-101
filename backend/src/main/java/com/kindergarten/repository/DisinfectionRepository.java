package com.kindergarten.repository;

import com.kindergarten.entity.Disinfection;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisinfectionRepository extends JpaRepository<Disinfection, Long> {

    List<Disinfection> findByAidIdAndDisinfectDate(Long aidId, LocalDate disinfectDate);

    List<Disinfection> findAllByOrderByIdDesc();
}
