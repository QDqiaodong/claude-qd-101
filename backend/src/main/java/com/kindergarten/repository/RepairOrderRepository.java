package com.kindergarten.repository;

import com.kindergarten.entity.RepairOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    List<RepairOrder> findByAidIdAndStatusNot(Long aidId, String status);

    List<RepairOrder> findAllByOrderByUpdatedAtDesc();
}
