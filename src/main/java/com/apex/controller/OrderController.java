package com.apex.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.apex.dto.request.OrderRequests.CreateOrderRequest;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.model.SalesOrder.Status;
import com.apex.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<Page<OrderResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID customerId,
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) String orderNumber,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(orderService.list(status, customerId, warehouseId, orderNumber, from, to, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(orderService.get(id));
  }
}
