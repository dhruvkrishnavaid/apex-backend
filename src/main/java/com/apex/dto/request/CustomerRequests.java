package com.apex.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerRequests {

  public record CustomerRequest(
      @NotBlank(message = "Customer name is required") @Size(max = 200) String name,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      @Size(max = 1000) String notes,
      Boolean isActive) {
  }
}
