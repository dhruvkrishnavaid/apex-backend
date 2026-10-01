package com.apex.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/api/v1/health")
  public ResponseEntity<Map<String, String>> getHealth() {
    Map<String, String> healthStatus = new HashMap<>();
    healthStatus.put("status", "200 OK");
    healthStatus.put("message", "Application is running smoothly.");
    return new ResponseEntity<>(healthStatus, HttpStatus.OK);
  }
}
