package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.apex.dto.request.WarehouseRequests.CreateWarehouseRequest;
import com.apex.dto.request.WarehouseRequests.LocationRequest;
import com.apex.dto.request.WarehouseRequests.UpdateWarehouseRequest;
import com.apex.dto.response.WarehouseResponses.LocationResponse;
import com.apex.dto.response.WarehouseResponses.WarehouseResponse;
import com.apex.service.WarehouseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

  private final WarehouseService warehouseService;

  @PostMapping
  @PreAuthorize("hasAuthority('warehouse:create')")
  public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.createWarehouse(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<Page<WarehouseResponse>> list(
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "code") Pageable pageable) {
    return ResponseEntity.ok(warehouseService.listWarehouses(active, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<WarehouseResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(warehouseService.getWarehouse(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<WarehouseResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateWarehouseRequest request) {
    return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('warehouse:delete')")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    warehouseService.deactivateWarehouse(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Locations ----------

  @PostMapping("/{id}/locations")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<LocationResponse> addLocation(
      @PathVariable UUID id,
      @Valid @RequestBody LocationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.addLocation(id, request));
  }

  @GetMapping("/{id}/locations")
  @PreAuthorize("hasAuthority('warehouse:read')")
  public ResponseEntity<List<LocationResponse>> listLocations(@PathVariable UUID id) {
    return ResponseEntity.ok(warehouseService.listLocations(id));
  }

  @PutMapping("/locations/{locationId}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<LocationResponse> updateLocation(
      @PathVariable UUID locationId,
      @Valid @RequestBody LocationRequest request) {
    return ResponseEntity.ok(warehouseService.updateLocation(locationId, request));
  }

  @DeleteMapping("/locations/{locationId}")
  @PreAuthorize("hasAuthority('warehouse:update')")
  public ResponseEntity<Void> deactivateLocation(@PathVariable UUID locationId) {
    warehouseService.deactivateLocation(locationId);
    return ResponseEntity.noContent().build();
  }
}
