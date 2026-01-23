package com.chieaid24.user_service.controller;

import com.chieaid24.user_service.dto.UserDto;
import com.chieaid24.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/user")
public class UserController {
  // init vars and such
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/create")
  public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
    UserDto created = userService.createUser(userDto);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }

  // ex) POST /api/v1/user/create/dummy?users=10
  @PostMapping("/create/dummy")
  public ResponseEntity<Integer> createDummyUsers(@RequestParam int users) {
    userService.createDummyUsers(users);
    return ResponseEntity.status(HttpStatus.CREATED).body(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
    UserDto userDto = userService.getUserById(id);
    if (userDto == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return ResponseEntity.ok(userDto);
  }

  @GetMapping("/total")
  public ResponseEntity<Long> getTotalUsers() {
    Long total = userService.getTotalUsers();
    return ResponseEntity.ok(total);
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {

    try {
      UserDto updated = userService.updateUser(id, userDto);
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteUser(@PathVariable Long id) {
    try {
      userService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }
}
