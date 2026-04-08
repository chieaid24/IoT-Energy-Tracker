package com.chieaid24.user_service.service;

import com.chieaid24.user_service.dto.AuthResponse;
import com.chieaid24.user_service.dto.GoogleLoginRequest;
import com.chieaid24.user_service.dto.LoginRequest;
import com.chieaid24.user_service.dto.RegisterRequest;
import com.chieaid24.user_service.entity.User;
import com.chieaid24.user_service.repository.UserRepository;
import com.chieaid24.user_service.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${google.client.id:}")
  private String googleClientId;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  public AuthResponse register(RegisterRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new IllegalArgumentException("Email already registered");
    }

    User user =
        User.builder()
            .name(request.getName())
            .surname(request.getSurname())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .authProvider("LOCAL")
            .build();

    User saved = userRepository.save(user);
    String token = jwtUtil.generateToken(saved);

    return AuthResponse.builder()
        .token(token)
        .userId(saved.getId())
        .email(saved.getEmail())
        .name(saved.getName())
        .build();
  }

  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    String token = jwtUtil.generateToken(user);

    return AuthResponse.builder()
        .token(token)
        .userId(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .build();
  }

  public AuthResponse googleLogin(GoogleLoginRequest request) {
    JsonNode tokenInfo = validateGoogleToken(request.getIdToken());

    String email = tokenInfo.get("email").asText();
    String name = tokenInfo.has("name") ? tokenInfo.get("name").asText() : email;

    User user =
        userRepository
            .findByEmail(email)
            .orElseGet(
                () -> {
                  User newUser =
                      User.builder().name(name).email(email).authProvider("GOOGLE").build();
                  return userRepository.save(newUser);
                });

    String token = jwtUtil.generateToken(user);

    return AuthResponse.builder()
        .token(token)
        .userId(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .build();
  }

  private JsonNode validateGoogleToken(String idToken) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
              .GET()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IllegalArgumentException("Invalid Google token");
      }

      JsonNode tokenInfo = objectMapper.readTree(response.body());

      if (!googleClientId.isEmpty() && !googleClientId.equals(tokenInfo.get("aud").asText())) {
        throw new IllegalArgumentException("Google token audience mismatch");
      }

      return tokenInfo;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to validate Google token", e);
      throw new IllegalArgumentException("Failed to validate Google token");
    }
  }
}
