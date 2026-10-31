package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class FulfilmentTests extends IntegrationTest {

  private String newOrder(String admin, String wh, String variant, int qty) throws Exception {
    return read(postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":%d}]}".formatted(wh, variant, qty))
        .andExpect(status().isCreated()), "$.id");
  }

  @Test
  void happyPathPickPackShipDeliver() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4", "10");
    addStock(admin, variant, wh, 10);
    String id = newOrder(admin, wh, variant, 4);

    // cannot pack or ship before picking
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/orders/" + id + "/ship", "").andExpect(status().isConflict());

    postAs(admin, "/api/v1/orders/" + id + "/pick", "").andExpect(jsonPath("$.status").value("PICKING"))
        .andExpect(jsonPath("$.lines[0].quantityPicked").value(4));
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(jsonPath("$.status").value("PACKED"));
    assertEquals(10, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/ship", "{\"carrier\":\"DHL\",\"trackingNumber\":\"TRK1\"}")
        .andExpect(jsonPath("$.status").value("SHIPPED")).andExpect(jsonPath("$.trackingNumber").value("TRK1"));
    assertEquals(6, onHand(admin, variant, wh));
    assertEquals(0, reserved(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/cancel", "").andExpect(status().isConflict());
    postAs(admin, "/api/v1/orders/" + id + "/deliver", "").andExpect(jsonPath("$.status").value("DELIVERED"));
    getAs(admin, "/api/v1/inventory/movements?type=SALE&variantId=" + variant)
        .andExpect(jsonPath("$.content[0].quantityChange").value(-4));
  }

  @Test
  void partialPickBlocksPacking() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 10);
    var created = postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":5}]}".formatted(wh, variant));
    String id = read(created, "$.id"), lineId = read(created, "$.lines[0].id");

    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":3}]}");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":6}]}")
        .andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/orders/" + id + "/pick", "{\"lines\":[{\"lineId\":\"" + lineId + "\",\"quantity\":5}]}");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "").andExpect(status().isOk());
  }

  @Test
  void cancellingReleasesReservedStock() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 5);
    String id = newOrder(admin, wh, variant, 5);
    assertEquals(5, reserved(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/cancel", "{\"reason\":\"customer changed mind\"}")
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("customer changed mind")));
    assertEquals(0, reserved(admin, variant, wh));
    assertEquals(5, onHand(admin, variant, wh));
    newOrder(admin, wh, variant, 5); // stock is sellable again
    postAs(admin, "/api/v1/orders/" + id + "/pick", "").andExpect(status().isConflict());
  }

  @Test
  void returnsRestockOrWriteOffAndRefundIncludesDiscount() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4", "10");
    addStock(admin, variant, wh, 10);
    var created = postAs(admin, "/api/v1/orders", """
        {"warehouseId":"%s","lines":[{"variantId":"%s","quantity":4,"discountPercent":50}]}""".formatted(wh, variant));
    String id = read(created, "$.id"), lineId = read(created, "$.lines[0].id");

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}").andExpect(status().isConflict()); // not shipped

    postAs(admin, "/api/v1/orders/" + id + "/pick", "");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "");
    postAs(admin, "/api/v1/orders/" + id + "/ship", "");
    assertEquals(6, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"reason\":\"faulty\",\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.refundAmount").value(5.0))   // 10 * 50% off
        .andExpect(jsonPath("$.restocked").value(true));
    assertEquals(7, onHand(admin, variant, wh));

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"restock\":false,\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":2}]}")
        .andExpect(jsonPath("$.refundAmount").value(10.0));
    assertEquals(7, onHand(admin, variant, wh)); // written off, not restocked

    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":2}]}").andExpect(status().isUnprocessableEntity());
    getAs(admin, "/api/v1/orders/" + id + "/returns").andExpect(jsonPath("$.length()").value(2));
    getAs(admin, "/api/v1/orders/" + id).andExpect(jsonPath("$.lines[0].quantityReturned").value(3));
  }

  @Test
  void warehouseStaffCanFulfilButNotCancel() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 2);
    String id = newOrder(admin, wh, variant, 1);
    String staff = tokenFor("WAREHOUSE_STAFF");
    postAs(staff, "/api/v1/orders/" + id + "/pick", "").andExpect(status().isOk());
    postAs(staff, "/api/v1/orders/" + id + "/cancel", "").andExpect(status().isForbidden());
  }
}
