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

import com.apex.dto.request.OrderRequests.CancelOrderRequest;
import com.apex.dto.request.OrderRequests.CreateOrderRequest;
import com.apex.dto.request.OrderRequests.CreateReturnRequest;
import com.apex.dto.request.OrderRequests.PickRequest;
import com.apex.dto.request.OrderRequests.ShipOrderRequest;
import com.apex.dto.response.OrderResponses.ReturnResponse;
import com.apex.service.FulfilmentService;
import java.util.List;
import java.util.stream.Collectors;
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
  private final FulfilmentService fulfilmentService;

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

  // ---------- Fulfilment ----------

  @PostMapping("/{id}/pick")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> pick(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) PickRequest request) {
    java.util.Map<UUID, Integer> picks = request == null || request.lines() == null ? null
        : request.lines().stream().collect(Collectors.toMap(
            PickRequest.PickLine::lineId, PickRequest.PickLine::quantity, Integer::sum));
    return ResponseEntity.ok(fulfilmentService.pick(id, picks));
  }

  @PostMapping("/{id}/pack")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> pack(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.pack(id));
  }

  @PostMapping("/{id}/ship")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> ship(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ShipOrderRequest request) {
    return ResponseEntity.ok(fulfilmentService.ship(id, request));
  }

  @PostMapping("/{id}/deliver")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<OrderResponse> deliver(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.deliver(id));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('order:cancel')")
  public ResponseEntity<OrderResponse> cancel(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) CancelOrderRequest request) {
    return ResponseEntity.ok(fulfilmentService.cancel(id, request == null ? null : request.reason()));
  }

  // ---------- Returns ----------

  @PostMapping("/{id}/returns")
  @PreAuthorize("hasAuthority('order:update')")
  public ResponseEntity<ReturnResponse> createReturn(
      @PathVariable UUID id,
      @Valid @RequestBody CreateReturnRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(fulfilmentService.createReturn(id, request));
  }

  @GetMapping("/{id}/returns")
  @PreAuthorize("hasAuthority('order:read')")
  public ResponseEntity<List<ReturnResponse>> listReturns(@PathVariable UUID id) {
    return ResponseEntity.ok(fulfilmentService.listReturns(id));
  }
}
