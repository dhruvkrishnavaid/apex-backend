package com.apex.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.TransferRequests.CreateTransferRequest;
import com.apex.dto.request.TransferRequests.ReceiveTransferRequest;
import com.apex.dto.response.TransferResponses.TransferResponse;
import com.apex.model.StockTransfer.Status;
import com.apex.service.TransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  @PostMapping
  @PreAuthorize("hasAuthority('transfer:create')")
  public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('transfer:read')")
  public ResponseEntity<Page<TransferResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(transferService.list(status, warehouseId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('transfer:read')")
  public ResponseEntity<TransferResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.get(id));
  }

  @PostMapping("/{id}/dispatch")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> dispatch(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.dispatch(id));
  }

  @PostMapping("/{id}/receive")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> receive(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ReceiveTransferRequest request) {
    return ResponseEntity.ok(transferService.receive(id, request));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('transfer:update')")
  public ResponseEntity<TransferResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(transferService.cancel(id));
  }
}
