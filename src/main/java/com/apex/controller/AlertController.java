package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.AlertRequests.ReorderRuleRequest;
import com.apex.dto.response.AlertResponses.AlertResponse;
import com.apex.dto.response.AlertResponses.AlertSummary;
import com.apex.dto.response.AlertResponses.ReorderRuleResponse;
import com.apex.dto.response.AlertResponses.ReplenishmentSuggestion;
import com.apex.model.StockAlert.Status;
import com.apex.service.AlertService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlertController {

  private final AlertService alertService;

  @PostMapping("/reorder-rules")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<ReorderRuleResponse> createRule(@Valid @RequestBody ReorderRuleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createRule(request));
  }

  @GetMapping("/reorder-rules")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<List<ReorderRuleResponse>> listRules(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(alertService.listRules(warehouseId));
  }

  @PutMapping("/reorder-rules/{id}")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<ReorderRuleResponse> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody ReorderRuleRequest request) {
    return ResponseEntity.ok(alertService.updateRule(id, request));
  }

  @DeleteMapping("/reorder-rules/{id}")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
    alertService.deleteRule(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/reorder-rules/suggestions")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<List<ReplenishmentSuggestion>> suggestions(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(alertService.suggestions(warehouseId));
  }

  /** Without a status filter, returns alerts that still need attention (OPEN and ACKNOWLEDGED). */
  @GetMapping("/alerts")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<Page<AlertResponse>> listAlerts(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(alertService.listAlerts(status, warehouseId, pageable));
  }

  @GetMapping("/alerts/summary")
  @PreAuthorize("hasAuthority('alert:read')")
  public ResponseEntity<AlertSummary> summary() {
    return ResponseEntity.ok(alertService.summary());
  }

  @PostMapping("/alerts/{id}/acknowledge")
  @PreAuthorize("hasAuthority('alert:manage')")
  public ResponseEntity<AlertResponse> acknowledge(@PathVariable UUID id) {
    return ResponseEntity.ok(alertService.acknowledge(id));
  }
}
