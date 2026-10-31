package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class PurchaseOrderTests extends IntegrationTest {

  private String newSupplier(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + unique("s") + "\",\"name\":\"Acme\"}"), "$.id");
  }

  private String po(String supplier, String wh, String variant, int qty, String cost) {
    return "{\"supplierId\":\"%s\",\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d%s}]}"
        .formatted(supplier, wh, variant, qty, cost == null ? "" : ",\"unitCost\":" + cost);
  }

  @Test
  void purchaseOrderFlowFromDraftToFullyReceived() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin, "3.00", "6.00");
    postAs(admin, "/api/v1/suppliers/" + supplier + "/products", "{\"variantId\":\"" + variant + "\",\"unitCost\":2.50}");

    var created = postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 100, null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.lines[0].unitCost").value(2.5))   // supplier catalogue price
        .andExpect(jsonPath("$.totalAmount").value(250.0));
    String id = read(created, "$.id");
    String lineId = read(created, "$.lines[0].id");

    // draft is editable, submitted is not
    putAs(admin, "/api/v1/purchase-orders/" + id,
        "{\"warehouseId\":\"" + wh + "\",\"lines\":[{\"variantId\":\"" + variant + "\",\"quantity\":60,\"unitCost\":2.00}]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(120.0));
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(lineId, 1)).andExpect(status().isConflict());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/submit", "").andExpect(jsonPath("$.status").value("ORDERED"));
    putAs(admin, "/api/v1/purchase-orders/" + id, "{\"warehouseId\":\"" + wh + "\",\"lines\":[{\"variantId\":\"" + variant + "\",\"quantity\":1}]}")
        .andExpect(status().isConflict());

    String newLineId = read(getAs(admin, "/api/v1/purchase-orders/" + id), "$.lines[0].id");
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 25))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.receiptNumber").exists());
    assertEquals(25, onHand(admin, variant, wh));
    getAs(admin, "/api/v1/purchase-orders/" + id)
        .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
        .andExpect(jsonPath("$.lines[0].quantityOutstanding").value(35));

    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 36)).andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/cancel", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/purchase-orders/" + id + "/receive", receive(newLineId, 35));
    getAs(admin, "/api/v1/purchase-orders/" + id).andExpect(jsonPath("$.status").value("RECEIVED"));
    assertEquals(60, onHand(admin, variant, wh));
    getAs(admin, "/api/v1/purchase-orders/" + id + "/receipts").andExpect(jsonPath("$.length()").value(2));
    getAs(admin, "/api/v1/inventory/movements?type=PURCHASE_RECEIPT&variantId=" + variant)
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void cancelAndCloseRules() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin);

    String a = read(postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")), "$.id");
    postAs(admin, "/api/v1/purchase-orders/" + a + "/cancel", "").andExpect(jsonPath("$.status").value("CANCELLED"));
    postAs(admin, "/api/v1/purchase-orders/" + a + "/submit", "").andExpect(status().isConflict());

    var b = postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00"));
    String bId = read(b, "$.id");
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/submit", "");
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/receive", receive(read(b, "$.lines[0].id"), 2));
    postAs(admin, "/api/v1/purchase-orders/" + bId + "/close", "").andExpect(jsonPath("$.status").value("CLOSED"));
    getAs(admin, "/api/v1/purchase-orders?status=CLOSED&supplierId=" + supplier).andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void roleChecks() throws Exception {
    String admin = adminToken();
    String supplier = newSupplier(admin), wh = newWarehouse(admin), variant = newVariant(admin);
    String id = read(postAs(admin, "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")), "$.id");
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders", po(supplier, wh, variant, 5, "1.00")).andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders/" + id).andExpect(status().isOk());
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/purchase-orders/" + id + "/cancel", "").andExpect(status().isForbidden());
  }

  private String receive(String lineId, int qty) {
    return "{\"lines\":[{\"lineId\":\"%s\",\"quantity\":%d}]}".formatted(lineId, qty);
  }
}
