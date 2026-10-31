package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class AlertTests extends IntegrationTest {

  private String rule(String variant, String wh, int point, int qty) {
    return "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"reorderPoint\":%d,\"reorderQuantity\":%d}"
        .formatted(variant, wh, point, qty);
  }

  private String alertsFor(String admin, String wh) {
    return "/api/v1/alerts?warehouseId=" + wh;
  }

  @Test
  void alertRaisedWhenStockFallsToReorderPointAndResolvedOnRestock() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    addStock(admin, variant, wh, 20);
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 50)).andExpect(status().isCreated());
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 50)).andExpect(status().isConflict());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));

    // selling stock through an order (reservation) already counts against availability
    postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":16}]}".formatted(wh, variant))
        .andExpect(status().isCreated());
    var alerts = getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("LOW_STOCK"))
        .andExpect(jsonPath("$.content[0].availableQuantity").value(4));
    String alertId = read(alerts, "$.content[0].id");

    // dropping to zero escalates the same alert instead of opening a second one
    addStockDelta(admin, variant, wh, -4);
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("OUT_OF_STOCK"));

    postAs(admin, "/api/v1/alerts/" + alertId + "/acknowledge", "")
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    getAs(admin, "/api/v1/alerts/summary").andExpect(status().isOk());

    // restocking above the reorder point resolves it
    addStock(admin, variant, wh, 30);
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
    getAs(admin, "/api/v1/alerts?status=RESOLVED&warehouseId=" + wh).andExpect(jsonPath("$.content.length()").value(1));
    postAs(admin, "/api/v1/alerts/" + alertId + "/acknowledge", "").andExpect(status().isConflict());
  }

  private void addStockDelta(String admin, String variant, String wh, int delta) throws Exception {
    postAs(admin, "/api/v1/inventory/adjustments",
        "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"quantityChange\":%d,\"reason\":\"t\"}".formatted(variant, wh, delta))
        .andExpect(status().isOk());
  }

  @Test
  void creatingRuleOnAlreadyLowStockRaisesAlertImmediately() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    addStock(admin, variant, wh, 2);
    String ruleId = read(postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 5, 10)), "$.id");
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1));

    // raising or lowering the threshold re-evaluates; deleting the rule clears the alert
    putAs(admin, "/api/v1/reorder-rules/" + ruleId, rule(variant, wh, 1, 10)).andExpect(status().isOk());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
    putAs(admin, "/api/v1/reorder-rules/" + ruleId, rule(variant, wh, 5, 10));
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/reorder-rules/" + ruleId).andExpect(status().isNoContent());
    getAs(admin, alertsFor(admin, wh)).andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void suggestionsListLowItemsWithPreferredSupplier() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    String supplier = read(postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + unique("s") + "\",\"name\":\"Prime Supply\"}"), "$.id");
    postAs(admin, "/api/v1/suppliers/" + supplier + "/products", "{\"variantId\":\"" + variant + "\",\"unitCost\":1.75}");
    addStock(admin, variant, wh, 1);
    postAs(admin, "/api/v1/reorder-rules", rule(variant, wh, 3, 40));

    getAs(admin, "/api/v1/reorder-rules/suggestions?warehouseId=" + wh)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].suggestedQuantity").value(40))
        .andExpect(jsonPath("$[0].preferredSupplierName").value("Prime Supply"))
        .andExpect(jsonPath("$[0].estimatedUnitCost").value(1.75));
  }

  @Test
  void permissions() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin), wh = newWarehouse(admin);
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/reorder-rules", rule(variant, wh, 1, 1)).andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/alerts").andExpect(status().isOk());
    getAs(tokenFor("CASHIER"), "/api/v1/alerts").andExpect(status().isForbidden());
  }
}
