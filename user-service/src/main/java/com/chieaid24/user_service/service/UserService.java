package com.chieaid24.user_service.service;

import com.chieaid24.user_service.dto.UserDto;
import com.chieaid24.user_service.entity.User;
import com.chieaid24.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserDto createUser(UserDto input) {
    final User createdUser =
        User.builder()
            .name(input.getName())
            .surname(input.getSurname())
            .email(input.getEmail())
            .address(input.getAddress())
            .alerting(input.isAlerting())
            .energyAlertingThreshold(input.getEnergyAlertingThreshold())
            .build();
    User saved = userRepository.save(createdUser);
    return toDto(saved);
  }
  
  public UserDto getUserById(Long id) {
    return userRepository.findById(id).map(this::toDto).orElse(null);
  }

  public void updateUser(Long id, UserDto userDto) {
    User existingUser =
        userRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

    existingUser.setName(userDto.getName());
    existingUser.setSurname(userDto.getSurname());
    existingUser.setEmail(userDto.getEmail());
    existingUser.setAddress(userDto.getAddress());
    existingUser.setAlerting(userDto.isAlerting());
    existingUser.setEnergyAlertingThreshold(userDto.getEnergyAlertingThreshold());

    userRepository.save(existingUser);
  }

  public void deleteUser(Long id) {
    User existingUser =
        userRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    userRepository.delete(existingUser);
  }

  private UserDto toDto(User user) {
    return UserDto.builder()
        .id(user.getId())
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .address(user.getAddress())
        .alerting(user.isAlerting())
        .energyAlertingThreshold(user.getEnergyAlertingThreshold())
        .build();
  }
}
