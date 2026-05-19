package com.chieaid24.user_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chieaid24.user_service.entity.User;
import com.chieaid24.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class UserRepositoryTests {

  @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  // DataSourceConfig reads spring.datasource.* and spring.datasource.replica.* directly via
  // @ConfigurationProperties, so @ServiceConnection won't reach the routing datasource. Wire
  // both pools to the same container instead.
  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.replica.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.replica.username", mysql::getUsername);
    registry.add("spring.datasource.replica.password", mysql::getPassword);
    registry.add("jwt.secret", () -> "test-secret-32-bytes-long-for-hmac-sha256-padding!!");
  }

  @Autowired private UserRepository userRepository;

  @Test
  void savesAndLoadsUserThroughFlywayManagedSchema() {
    User saved =
        userRepository.save(
            User.builder()
                .name("Test")
                .surname("User")
                .email("test@example.com")
                .address("123 Main St")
                .alerting(true)
                .energyAlertingThreshold(500.0)
                .password("hashed-not-real")
                .authProvider("LOCAL")
                .build());

    assertThat(saved.getId()).isNotNull();
    assertThat(userRepository.findByEmail("test@example.com"))
        .isPresent()
        .hasValueSatisfying(u -> assertThat(u.getName()).isEqualTo("Test"));
  }
}
