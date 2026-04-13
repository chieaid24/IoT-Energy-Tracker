package com.chieaid24.user_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String rootMessage = ex.getMostSpecificCause().getMessage();
    if (rootMessage != null && rootMessage.contains("foreign key constraint")) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              "Cannot delete users while devices still exist. Delete all devices first via DELETE /api/v1/device/all");
    }
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("Data integrity violation: " + rootMessage);
  }
}
