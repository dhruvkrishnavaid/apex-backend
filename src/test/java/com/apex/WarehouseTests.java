package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class WarehouseTests extends IntegrationTest {

  @Test
  void warehouseLifecycle() throws Exception {
    String admin = adminToken();
    String code = unique("wh");
    var created = postAs(admin, "/api/v1/warehouses",
        "{\"code\":\"" + code + "\",\"name\":\"Main DC\",\"city\":\"Sydney\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(code.toUpperCase()));
    String id = read(created, "$.id");

    postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + code.toLowerCase() + "\",\"name\":\"Dup\"}")
        .andExpect(status().isConflict());

    putAs(admin, "/api/v1/warehouses/" + id, "{\"name\":\"Main DC 2\",\"city\":\"Melbourne\",\"isActive\":true}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.city").value("Melbourne"));

    deleteAs(admin, "/api/v1/warehouses/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/warehouses?active=false").andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  void locationsAreUniquePerWarehouse() throws Exception {
    String admin = adminToken();
    String w1 = read(postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + unique("a") + "\",\"name\":\"A\"}"), "$.id");
    String w2 = read(postAs(admin, "/api/v1/warehouses", "{\"code\":\"" + unique("b") + "\",\"name\":\"B\"}"), "$.id");

    var loc = postAs(admin, "/api/v1/warehouses/" + w1 + "/locations", "{\"code\":\"a-01-01\"}")
        .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("A-01-01"));
    postAs(admin, "/api/v1/warehouses/" + w1 + "/locations", "{\"code\":\"A-01-01\"}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/warehouses/" + w2 + "/locations", "{\"code\":\"A-01-01\"}").andExpect(status().isCreated());

    getAs(admin, "/api/v1/warehouses/" + w1 + "/locations").andExpect(jsonPath("$.length()").value(1));
    putAs(admin, "/api/v1/warehouses/locations/" + read(loc, "$.id"), "{\"code\":\"A-01-02\",\"description\":\"top shelf\"}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("A-01-02"));
  }

  @Test
  void permissionsAreEnforced() throws Exception {
    String body = "{\"code\":\"" + unique("x") + "\",\"name\":\"X\"}";
    postAs(tokenFor("MANAGER"), "/api/v1/warehouses", body).andExpect(status().isForbidden());
    getAs(tokenFor("MANAGER"), "/api/v1/warehouses").andExpect(status().isOk());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/warehouses").andExpect(status().isOk());
    getAs(tokenFor("CASHIER"), "/api/v1/warehouses").andExpect(status().isForbidden());
  }
}
