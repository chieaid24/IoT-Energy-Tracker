package com.chieaid24.device_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.chieaid24.device_service.entity.Device;
import com.chieaid24.device_service.model.DeviceType;
import com.chieaid24.device_service.repository.DeviceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
class DeviceServiceApplicationTests {

	private static final int NUM_USERS = 10;
	private static final int NUM_DEVICES = 200;
	@Autowired
	private DeviceRepository deviceRepository;

	@Test
	void contextLoads() {
	}

	// create 200 mock devices for testing over 10 users
	// @Disabled
	@Test
	void createDevices() {
		for (int i = 1; i <= NUM_DEVICES; i++) {
			deviceRepository.save(
					Device.builder()
							.name("Device " + i)
							.type(DeviceType.values()[(i % DeviceType.values().length)])
							.location("Location " + ((i % 5) + 1))
							.userId((long) ((i % NUM_USERS) + 1))
							.build()
			);
		}
		log.info("Device Repository created with {} devices", deviceRepository.count());
	}

}
