package com.chieaid24.user_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.chieaid24.user_service.entity.User;
import com.chieaid24.user_service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
class UserServiceApplicationTests {

  private static final int NUM_USERS = 10;
  @Autowired
  private UserRepository userRepository;

  @Test
  void contextLoads() {}

  @Disabled
  @Test
  void createUsers() {
    for (int i = 1; i <= NUM_USERS; i++) {
      userRepository.save(
          User.builder()
              .name("User " + i)
              .surname("Surname" + i)
              .email("user" + i + "@example.com")
              .address(i + "Example St, City")
              .alerting(i % 2 == 0)
              .energyAlertingThreshold(1000.0 + i)
              .build()
      );
    }
    log.info("User Repository created with {} users", userRepository.count());
  }
}
