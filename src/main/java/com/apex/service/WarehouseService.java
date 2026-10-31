package com.apex.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.WarehouseRequests.CreateWarehouseRequest;
import com.apex.dto.request.WarehouseRequests.LocationRequest;
import com.apex.dto.request.WarehouseRequests.UpdateWarehouseRequest;
import com.apex.dto.response.WarehouseResponses.LocationResponse;
import com.apex.dto.response.WarehouseResponses.WarehouseResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.StorageLocation;
import com.apex.model.Warehouse;
import com.apex.repository.StorageLocationRepository;
import com.apex.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseService {

  private final WarehouseRepository warehouseRepository;
  private final StorageLocationRepository locationRepository;

  @Transactional
  public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
    String code = normalizeCode(request.code());
    if (warehouseRepository.existsByCode(code)) {
      throw new DuplicateResourceException("Warehouse code already exists: " + code);
    }
    Warehouse warehouse = Warehouse.builder()
        .code(code)
        .name(request.name())
        .addressLine(request.addressLine())
        .city(request.city())
        .state(request.state())
        .postalCode(request.postalCode())
        .country(request.country())
        .contactPhone(request.contactPhone())
        .build();
    return toResponse(warehouseRepository.save(warehouse));
  }

  @Transactional(readOnly = true)
  public Page<WarehouseResponse> listWarehouses(Boolean active, Pageable pageable) {
    Page<Warehouse> page = active == null
        ? warehouseRepository.findAll(pageable)
        : warehouseRepository.findByIsActive(active, pageable);
    return page.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public WarehouseResponse getWarehouse(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public WarehouseResponse updateWarehouse(UUID id, UpdateWarehouseRequest request) {
    Warehouse warehouse = find(id);
    warehouse.setName(request.name());
    warehouse.setAddressLine(request.addressLine());
    warehouse.setCity(request.city());
    warehouse.setState(request.state());
    warehouse.setPostalCode(request.postalCode());
    warehouse.setCountry(request.country());
    warehouse.setContactPhone(request.contactPhone());
    if (request.isActive() != null) {
      warehouse.setIsActive(request.isActive());
    }
    return toResponse(warehouseRepository.save(warehouse));
  }

  /** Warehouses are deactivated, never removed: stock history references them. */
  @Transactional
  public void deactivateWarehouse(UUID id) {
    Warehouse warehouse = find(id);
    warehouse.setIsActive(false);
    warehouseRepository.save(warehouse);
  }

  // ---------- Locations ----------

  @Transactional
  public LocationResponse addLocation(UUID warehouseId, LocationRequest request) {
    Warehouse warehouse = find(warehouseId);
    String code = normalizeCode(request.code());
    if (locationRepository.existsByWarehouseIdAndCode(warehouseId, code)) {
      throw new DuplicateResourceException("Location already exists in this warehouse: " + code);
    }
    StorageLocation location = StorageLocation.builder()
        .warehouse(warehouse)
        .code(code)
        .description(request.description())
        .build();
    return toResponse(locationRepository.save(location));
  }

  @Transactional(readOnly = true)
  public List<LocationResponse> listLocations(UUID warehouseId) {
    find(warehouseId);
    return locationRepository.findByWarehouseIdOrderByCodeAsc(warehouseId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public LocationResponse updateLocation(UUID locationId, LocationRequest request) {
    StorageLocation location = findLocation(locationId);
    String code = normalizeCode(request.code());
    if (!code.equals(location.getCode())
        && locationRepository.existsByWarehouseIdAndCode(location.getWarehouse().getId(), code)) {
      throw new DuplicateResourceException("Location already exists in this warehouse: " + code);
    }
    location.setCode(code);
    location.setDescription(request.description());
    if (request.isActive() != null) {
      location.setIsActive(request.isActive());
    }
    return toResponse(locationRepository.save(location));
  }

  @Transactional
  public void deactivateLocation(UUID locationId) {
    StorageLocation location = findLocation(locationId);
    location.setIsActive(false);
    locationRepository.save(location);
  }

  // ---------- Helpers used by other services ----------

  /** Returns an active warehouse or fails; used when stock is moved into or out of it. */
  @Transactional(readOnly = true)
  public Warehouse requireActive(UUID id) {
    Warehouse warehouse = find(id);
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }
    return warehouse;
  }

  private Warehouse find(UUID id) {
    return warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
  }

  private StorageLocation findLocation(UUID id) {
    return locationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private WarehouseResponse toResponse(Warehouse w) {
    return new WarehouseResponse(w.getId(), w.getCode(), w.getName(), w.getAddressLine(), w.getCity(),
        w.getState(), w.getPostalCode(), w.getCountry(), w.getContactPhone(), w.getIsActive(), w.getCreatedAt());
  }

  private LocationResponse toResponse(StorageLocation l) {
    return new LocationResponse(l.getId(), l.getWarehouse().getId(), l.getCode(), l.getDescription(), l.getIsActive());
  }
}
