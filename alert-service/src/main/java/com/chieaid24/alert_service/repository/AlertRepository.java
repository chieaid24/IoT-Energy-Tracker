package com.chieaid24.alert_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chieaid24.alert_service.entity.Alert;


public interface AlertRepository extends JpaRepository<Alert, Long> {

}
