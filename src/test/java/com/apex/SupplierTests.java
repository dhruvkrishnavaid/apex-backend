package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class SupplierTests extends IntegrationTest {

  private String newSupplier(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/suppliers",
        "{\"code\":\"" + unique("s") + "\",\"name\":\"Acme Wholesale\",\"email\":\"sales@acme.test\",\"leadTimeDays\":7}")
        .andExpect(status().isCreated()), "$.id");
  }

  @Test
  void supplierCrudAndValidation() throws Exception {
    String admin = adminToken();
    String code = unique("sup");
    var created = postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + code + "\",\"name\":\"Globex\"}")
        .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(code.toUpperCase()));
    String id = read(created, "$.id");
    postAs(admin, "/api/v1/suppliers", "{\"code\":\"" + code + "\",\"name\":\"Again\"}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/suppliers", "{\"code\":\"x\",\"name\":\"Bad\",\"email\":\"nope\"}").andExpect(status().isBadRequest());

    putAs(admin, "/api/v1/suppliers/" + id, "{\"name\":\"Globex Intl\",\"leadTimeDays\":3,\"isActive\":true}")
        .andExpect(jsonPath("$.name").value("Globex Intl"));
    getAs(admin, "/api/v1/suppliers?keyword=globex").andExpect(jsonPath("$.content.length()").value(1));
    deleteAs(admin, "/api/v1/suppliers/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/suppliers/" + id).andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void supplierCatalogueTracksCostsAndPreferredSupplier() throws Exception {
    String admin = adminToken();
    String variant = newVariant(admin);
    String s1 = newSupplier(admin), s2 = newSupplier(admin);

    postAs(admin, "/api/v1/suppliers/" + s1 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.50,\"minOrderQuantity\":10}").andExpect(status().isCreated());
    postAs(admin, "/api/v1/suppliers/" + s1 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.50}").andExpect(status().isConflict());
    postAs(admin, "/api/v1/suppliers/" + s2 + "/products",
        "{\"variantId\":\"" + variant + "\",\"unitCost\":4.00,\"isPreferred\":true}").andExpect(status().isCreated());

    getAs(admin, "/api/v1/suppliers/by-variant/" + variant)
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].supplierId").value(s2));

    putAs(admin, "/api/v1/suppliers/" + s1 + "/products/" + variant, "{\"variantId\":\"" + variant + "\",\"unitCost\":3.00}")
        .andExpect(jsonPath("$.unitCost").value(3.0));
    deleteAs(admin, "/api/v1/suppliers/" + s1 + "/products/" + variant).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/suppliers/" + s1 + "/products").andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void warehouseStaffCannotManageSuppliers() throws Exception {
    postAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/suppliers", "{\"code\":\"zz\",\"name\":\"Z\"}").andExpect(status().isForbidden());
    getAs(tokenFor("WAREHOUSE_STAFF"), "/api/v1/suppliers").andExpect(status().isForbidden());
  }
}
