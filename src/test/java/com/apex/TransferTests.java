package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class TransferTests extends IntegrationTest {

  private String body(String from, String to, String variant, int qty) {
    return "{\"fromWarehouseId\":\"%s\",\"toWarehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}"
        .formatted(from, to, variant, qty);
  }

  @Test
  void fullTransferMovesStockBetweenWarehouses() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 20);

    var created = postAs(admin, "/api/v1/transfers", body(a, b, variant, 8))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED"));
    String id = read(created, "$.id");
    assertEquals(20, onHand(admin, variant, a));
    assertEquals(8, reserved(admin, variant, a));

    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "").andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    assertEquals(12, onHand(admin, variant, a));
    assertEquals(0, reserved(admin, variant, a));
    assertEquals(0, onHand(admin, variant, b));

    postAs(admin, "/api/v1/transfers/" + id + "/receive", "").andExpect(jsonPath("$.status").value("RECEIVED"));
    assertEquals(8, onHand(admin, variant, b));

    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant + "&type=TRANSFER_IN")
        .andExpect(jsonPath("$.content.length()").value(1));
    postAs(admin, "/api/v1/transfers/" + id + "/receive", "").andExpect(status().isConflict());
  }

  @Test
  void partialReceiptOnlyAddsReceivedQuantity() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 10);
    var created = postAs(admin, "/api/v1/transfers", body(a, b, variant, 10));
    String id = read(created, "$.id");
    String lineId = read(created, "$.lines[0].id");
    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "");

    postAs(admin, "/api/v1/transfers/" + id + "/receive",
        "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"receivedQuantity\":11}]}").andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers/" + id + "/receive",
        "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"receivedQuantity\":9}]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.lines[0].receivedQuantity").value(9));
    assertEquals(9, onHand(admin, variant, b));
  }

  @Test
  void cancelReleasesReservationAndInsufficientStockIsRejected() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), a = newWarehouse(admin), b = newWarehouse(admin);
    addStock(admin, variant, a, 5);

    postAs(admin, "/api/v1/transfers", body(a, b, variant, 6)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers", body(a, a, variant, 1)).andExpect(status().isBadRequest());

    String id = read(postAs(admin, "/api/v1/transfers", body(a, b, variant, 5)), "$.id");
    postAs(admin, "/api/v1/transfers", body(a, b, variant, 1)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/transfers/" + id + "/cancel", "").andExpect(jsonPath("$.status").value("CANCELLED"));
    assertEquals(0, reserved(admin, variant, a));
    postAs(admin, "/api/v1/transfers/" + id + "/dispatch", "").andExpect(status().isConflict());
    getAs(admin, "/api/v1/transfers?status=CANCELLED").andExpect(status().isOk());
  }

  @Test
  void cashierCannotTransfer() throws Exception {
    String id = java.util.UUID.randomUUID().toString();
    postAs(tokenFor("CASHIER"), "/api/v1/transfers", body(id, java.util.UUID.randomUUID().toString(), id, 1)).andExpect(status().isForbidden());
  }
}
