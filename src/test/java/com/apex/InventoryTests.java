package com.apex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.apex.model.StockMovement.MovementType;
import com.apex.service.InventoryService;
import com.apex.support.IntegrationTest;

class InventoryTests extends IntegrationTest {

  @Autowired InventoryService inventoryService;
  @Autowired TransactionTemplate tx;

  @Test
  void adjustmentsUpdateStockAndWriteLedger() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);

    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 50, "opening stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantityOnHand").value(50))
        .andExpect(jsonPath("$.quantityAvailable").value(50));
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -8, "broken in store"))
        .andExpect(jsonPath("$.quantityOnHand").value(42));

    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant)
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].quantityAfter").value(42))
        .andExpect(jsonPath("$.content[0].quantityChange").value(-8));

    getAs(admin, "/api/v1/inventory/variants/" + variant)
        .andExpect(jsonPath("$.totalOnHand").value(42))
        .andExpect(jsonPath("$.warehouses.length()").value(1));
  }

  @Test
  void stockCannotGoNegativeAndReasonIsMandatory() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 5);

    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -6, "too many"))
        .andExpect(status().isUnprocessableEntity());
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 0, "nothing"))
        .andExpect(status().isBadRequest());
    postAs(admin, "/api/v1/inventory/adjustments",
        "{\"variantId\":\"" + variant + "\",\"warehouseId\":\"" + wh + "\",\"quantityChange\":1}")
        .andExpect(status().isBadRequest());
    assertEquals(5, onHand(admin, variant, wh));
  }

  @Test
  void stockCountSetsQuantityAndRecordsDifference() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);

    postAs(admin, "/api/v1/inventory/stock-counts",
        "{\"variantId\":\"" + variant + "\",\"warehouseId\":\"" + wh + "\",\"countedQuantity\":7}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.quantityOnHand").value(7));
    getAs(admin, "/api/v1/inventory/movements?variantId=" + variant + "&type=STOCK_COUNT")
        .andExpect(jsonPath("$.content[0].quantityChange").value(-3));
  }

  @Test
  void reservationsReduceAvailabilityButNotOnHand() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);
    java.util.UUID v = java.util.UUID.fromString(variant);
    java.util.UUID w = java.util.UUID.fromString(wh);

    tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 6));
    getAs(admin, "/api/v1/inventory?variantId=" + variant)
        .andExpect(jsonPath("$.content[0].quantityOnHand").value(10))
        .andExpect(jsonPath("$.content[0].quantityReserved").value(6))
        .andExpect(jsonPath("$.content[0].quantityAvailable").value(4));

    // reserved units are protected from manual removal and further reservation
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, -5, "oops"))
        .andExpect(status().isUnprocessableEntity());
    org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
        () -> tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 5)));

    tx.executeWithoutResult(s -> inventoryService.consumeReserved(v, w, 6, MovementType.SALE, "ORDER", null, "shipped"));
    assertEquals(4, onHand(admin, variant, wh));
    assertEquals(0, reserved(admin, variant, wh));
  }

  @Test
  void concurrentReservationsNeverOversell() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 10);
    java.util.UUID v = java.util.UUID.fromString(variant);
    java.util.UUID w = java.util.UUID.fromString(wh);

    ExecutorService pool = Executors.newFixedThreadPool(8);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      Callable<Boolean> task = () -> {
        try {
          tx.executeWithoutResult(s -> inventoryService.reserve(v, w, 1));
          return true;
        } catch (Exception e) {
          return false;
        }
      };
      results.add(pool.submit(task));
    }
    long ok = 0;
    for (Future<Boolean> f : results) {
      if (f.get()) {
        ok++;
      }
    }
    pool.shutdown();
    assertEquals(10, ok);
    assertEquals(10, reserved(admin, variant, wh));
  }

  @Test
  void warehouseWithStockCannotBeDeactivated() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String wh = newWarehouse(admin);
    addStock(admin, variant, wh, 3);
    deleteAs(admin, "/api/v1/warehouses/" + wh).andExpect(status().isConflict());

    addStock(admin, variant, wh, -3);
    deleteAs(admin, "/api/v1/warehouses/" + wh).andExpect(status().isNoContent());
    // stock cannot be added to an inactive warehouse
    postAs(admin, "/api/v1/inventory/adjustments", adj(variant, wh, 1, "late"))
        .andExpect(status().isConflict());
  }

  @Test
  void inventoryPermissionsAreEnforced() throws Exception {
    String variant = newVariant(adminToken());
    String wh = newWarehouse(adminToken());
    postAs(tokenFor("CASHIER"), "/api/v1/inventory/adjustments", adj(variant, wh, 1, "x")).andExpect(status().isForbidden());
    getAs(tokenFor("CASHIER"), "/api/v1/inventory").andExpect(status().isOk());
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/inventory/adjustments", adj(variant, wh, 1, "x")).andExpect(status().isOk());
  }

  private String adj(String variant, String wh, int delta, String reason) {
    return "{\"variantId\":\"%s\",\"warehouseId\":\"%s\",\"quantityChange\":%d,\"reason\":\"%s\"}"
        .formatted(variant, wh, delta, reason);
  }
}
