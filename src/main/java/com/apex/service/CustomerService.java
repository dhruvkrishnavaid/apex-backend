package com.apex.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.CustomerRequests.CustomerRequest;
import com.apex.dto.response.OrderResponses.CustomerResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Customer;
import com.apex.repository.CustomerRepository;
import com.apex.repository.SalesOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final SalesOrderRepository orderRepository;

  @Transactional
  public CustomerResponse create(CustomerRequest r) {
    if (r.email() != null && !r.email().isBlank() && customerRepository.existsByEmailIgnoreCase(r.email())) {
      throw new DuplicateResourceException("A customer with this email already exists");
    }
    Customer customer = Customer.builder().build();
    apply(customer, r);
    return toResponse(customerRepository.save(customer));
  }

  @Transactional(readOnly = true)
  public Page<CustomerResponse> list(String keyword, Pageable pageable) {
    Specification<Customer> spec = (root, query, cb) -> cb.conjunction();
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("email")), pattern),
          cb.like(cb.lower(root.get("phone")), pattern)));
    }
    return customerRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public CustomerResponse update(UUID id, CustomerRequest r) {
    Customer customer = find(id);
    if (r.email() != null && !r.email().isBlank() && !r.email().equalsIgnoreCase(customer.getEmail())
        && customerRepository.existsByEmailIgnoreCase(r.email())) {
      throw new DuplicateResourceException("A customer with this email already exists");
    }
    apply(customer, r);
    return toResponse(customerRepository.save(customer));
  }

  /** Customers with order history are deactivated; the rest are removed. */
  @Transactional
  public void delete(UUID id) {
    Customer customer = find(id);
    if (orderRepository.countByCustomerId(id) > 0) {
      customer.setIsActive(false);
      customerRepository.save(customer);
    } else {
      customerRepository.delete(customer);
    }
  }

  private void apply(Customer c, CustomerRequest r) {
    c.setName(r.name());
    c.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
    c.setPhone(r.phone());
    c.setAddressLine(r.addressLine());
    c.setCity(r.city());
    c.setState(r.state());
    c.setPostalCode(r.postalCode());
    c.setCountry(r.country());
    c.setNotes(r.notes());
    if (r.isActive() != null) {
      c.setIsActive(r.isActive());
    }
  }

  private Customer find(UUID id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
  }

  private CustomerResponse toResponse(Customer c) {
    return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddressLine(),
        c.getCity(), c.getState(), c.getPostalCode(), c.getCountry(), c.getNotes(), c.getIsActive(),
        c.getCreatedAt());
  }
}
