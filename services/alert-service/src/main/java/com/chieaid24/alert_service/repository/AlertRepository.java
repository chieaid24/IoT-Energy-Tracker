package com.chieaid24.alert_service.repository;

import com.chieaid24.alert_service.entity.Alert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

  List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

  long countByUserId(Long userId);
}
