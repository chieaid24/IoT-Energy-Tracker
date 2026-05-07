package com.chieaid24.alert_service.repository;

import com.chieaid24.alert_service.entity.Alert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

  // Read paths exposed via AlertController. Explicitly readOnly so the routing
  // proxy sends them to the replica. The Kafka listener path writes via
  // saveAndFlush() and must stay outside any readOnly transaction.
  @Transactional(readOnly = true)
  List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

  @Transactional(readOnly = true)
  long countByUserId(Long userId);
}
