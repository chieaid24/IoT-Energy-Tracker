package com.chieaid24.alert_service.repository;

import com.chieaid24.alert_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {}
