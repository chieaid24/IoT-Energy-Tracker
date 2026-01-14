package com.chieaid24.user_service.service;
import com.chieaid24.user_service.dto.UserDto;
import org.springframework.stereotype.Servive;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class UserService {

    public UserDto createUser(UserDto userDTO) {
        // simulate user creation logic
        log.info("Creating user: {}", userDto);
        return userDto;
    }
}
