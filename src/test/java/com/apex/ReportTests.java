package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class ReportTests extends IntegrationTest {

  @Test
  void stockValuationUsesCostAndRetailPrices() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "3.00", "5.00");
    addStock(admin, variant, wh, 10);

    getAs(admin, "/api/v1/reports/stock-valuation?warehouseId=" + wh)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUnits").value(10))
        .andExpect(jsonPath("$.totalCostValue").value(30.0))
        .andExpect(jsonPath("$.totalRetailValue").value(50.0))
        .andExpect(jsonPath("$.rows.length()").value(1));
  }

  @Test
  void lowStockReportListsItemsBelowThreshold() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), low = newVariant(admin), plenty = newVariant(admin);
    addStock(admin, low, wh, 2);
    addStock(admin, plenty, wh, 500);
    getAs(admin, "/api/v1/reports/low-stock?threshold=5")
        .andExpect(jsonPath("$[?(@.variantId=='" + low + "')]", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[?(@.variantId=='" + plenty + "')]", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void salesReportRecognisesRevenueOnShipmentAndSubtractsRefunds() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin, "4.00", "10.00");
    addStock(admin, variant, wh, 20);
    var order = postAs(admin, "/api/v1/orders",
        "{\"warehouseId\":\"%s\",\"lines\":[{\"variantId\":\"%s\",\"quantity\":5}]}".formatted(wh, variant));
    String id = read(order, "$.id"), lineId = read(order, "$.lines[0].id");

    String today = java.time.LocalDate.now().toString();
    // pending orders are not revenue
    getAs(admin, "/api/v1/reports/top-products?from=" + today + "&to=" + today + "&limit=100")
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')]", org.hamcrest.Matchers.hasSize(0)));

    postAs(admin, "/api/v1/orders/" + id + "/pick", "");
    postAs(admin, "/api/v1/orders/" + id + "/pack", "");
    postAs(admin, "/api/v1/orders/" + id + "/ship", "");
    postAs(admin, "/api/v1/orders/" + id + "/returns",
        "{\"lines\":[{\"orderLineId\":\"" + lineId + "\",\"quantity\":1}]}").andExpect(status().isCreated());

    var report = getAs(admin, "/api/v1/reports/sales?from=" + today + "&to=" + today).andExpect(status().isOk());
    report.andExpect(jsonPath("$.unitsSold").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
        .andExpect(jsonPath("$.days.length()").value(1));

    getAs(admin, "/api/v1/reports/top-products?from=" + today + "&to=" + today + "&limit=100")
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')].unitsSold", org.hamcrest.Matchers.contains(5)))
        .andExpect(jsonPath("$[?(@.variantId=='" + variant + "')].revenue", org.hamcrest.Matchers.contains(50.0)));
  }

  @Test
  void dashboardSummarisesTheBusiness() throws Exception {
    String admin = adminToken();
    String wh = newWarehouse(admin), variant = newVariant(admin);
    addStock(admin, variant, wh, 7);
    getAs(admin, "/api/v1/dashboard").andExpect(status().isOk())
        .andExpect(jsonPath("$.activeProducts").isNumber())
        .andExpect(jsonPath("$.totalUnitsOnHand").value(org.hamcrest.Matchers.greaterThanOrEqualTo(7)))
        .andExpect(jsonPath("$.generatedAt").exists());
  }

  @Test
  void auditTrailRecordsMutationsButNotReads() throws Exception {
    String admin = adminToken();
    String name = unique("Audited");
    postAs(admin, "/api/v1/categories", "{\"name\":\"" + name + "\"}").andExpect(status().isCreated());
    getAs(admin, "/api/v1/categories").andExpect(status().isOk());
    postAs(admin, "/api/v1/categories", "{}").andExpect(status().isBadRequest()); // failures are not audited

    getAs(admin, "/api/v1/audit-logs?resource=categories&method=POST&size=200")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].userEmail").value(ADMIN_EMAIL))
        .andExpect(jsonPath("$.content[0].statusCode").value(201))
        .andExpect(jsonPath("$.content[?(@.statusCode==400)]", org.hamcrest.Matchers.hasSize(0)));
    getAs(admin, "/api/v1/audit-logs?method=GET").andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void reportsAndAuditAreRestricted() throws Exception {
    getAs(tokenFor("CASHIER"), "/api/v1/dashboard").andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/reports/stock-valuation").andExpect(status().isForbidden());
    getAs(tokenFor("MANAGER"), "/api/v1/dashboard").andExpect(status().isOk());
    getAs(tokenFor("MANAGER"), "/api/v1/audit-logs").andExpect(status().isForbidden());
  }
}
