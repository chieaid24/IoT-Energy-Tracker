package com.chieaid24.user_service.repository;

import com.chieaid24.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  @Modifying
  @Transactional
  @Query("DELETE FROM User u")
  void deleteAllUsers();

  @Modifying
  @Transactional
  @Query(value = "ALTER TABLE `user` AUTO_INCREMENT = 1", nativeQuery = true)
  void resetAutoIncrement();
}
