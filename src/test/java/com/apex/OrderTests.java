package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class OrderTests extends IntegrationTest {

  private String order(String wh, String variant, int qty) {
    return "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}".formatted(wh, variant, qty);
  }

  @Test
  void customerCrud() throws Exception {
    String admin = adminToken();
    String email = unique("c") + "@shop.test";
    var created = postAs(admin, "/api/v1/customers", "{\"name\":\"Jane Doe\",\"email\":\"" + email + "\",\"phone\":\"0400\"}")
        .andExpect(status().isCreated());
    String id = read(created, "$.id");
    postAs(admin, "/api/v1/customers", "{\"name\":\"Other\",\"email\":\"" + email.toUpperCase() + "\"}")
        .andExpect(status().isConflict());
    putAs(admin, "/api/v1/customers/" + id, "{\"name\":\"Jane Smith\",\"email\":\"" + email + "\"}")
        .andExpect(jsonPath("$.name").value("Jane Smith"));
    getAs(admin, "/api/v1/customers?keyword=smith").andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/customers/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/customers/" + id).andExpect(status().isNotFound());
  }

  @Test
  void orderReservesStockAndComputesTotals() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4.00", "10.00");
    addStock(admin, variant, wh, 10);

    postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":3,"discountPercent":10}]}""".formatted(wh, variant))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.subtotal").value(27.0))
        .andExpect(jsonPath("$.total").value(27.0))
        .andExpect(jsonPath("$.lines[0].unitPrice").value(10.0));
    assertEquals(10, onHand(admin, variant, wh));
    assertEquals(3, reserved(admin, variant, wh));
  }

  @Test
  void orderFailsAtomicallyWhenAnyLineLacksStock() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), v1 = newVariant(admin), v2 = newVariant(admin);
    addStock(admin, v1, wh, 5);
    addStock(admin, v2, wh, 1);

    postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":2},{"variantId":"%s","quantity":2}]}"""
        .formatted(wh, v1, v2)).andExpect(status().isUnprocessableEntity());
    assertEquals(0, reserved(admin, v1, wh)); // first line's reservation rolled back
  }

  @Test
  void cashierCanSellButNotSeeSuppliers() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 4);
    String cashier = tokenFor("CASHIER");
    postAs(cashier, "/api/v1/orders", order(wh, variant, 1)).andExpect(status().isCreated());
    getAs(cashier, "/api/v1/orders").andExpect(status().isOk());
    getAs(cashier, "/api/v1/suppliers").andExpect(status().isForbidden());
  }

  @Test
  void filtersAndValidation() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 4);
    String id = read(postAs(admin, "/api/v1/orders", order(wh, variant, 1)), "$.id");
    getAs(admin, "/api/v1/orders?status=PENDING&warehouseId=" + wh).andExpect(jsonPath("$.content[0].id").value(id));
    getAs(admin, "/api/v1/orders?status=SHIPPED&warehouseId=" + wh).andExpect(jsonPath("$.content.length()").value(0));
    postAs(admin, "/api/v1/orders", "{\"warehouseId\":\"" + wh + "\",\"lines\":[]}").andExpect(status().isBadRequest());
    postAs(admin, "/api/v1/orders", order(wh, variant, 0)).andExpect(status().isBadRequest());
  }
}
