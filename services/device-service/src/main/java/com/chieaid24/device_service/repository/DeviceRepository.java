package com.chieaid24.device_service.repository;

import com.chieaid24.device_service.entity.Device;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

  List<Device> findAllByUserId(Long userId);

  @Modifying
  @Transactional
  @Query(value = "TRUNCATE TABLE device", nativeQuery = true)
  void truncate();
}
