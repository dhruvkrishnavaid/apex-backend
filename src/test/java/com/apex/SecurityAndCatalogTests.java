package com.apex;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.apex.support.IntegrationTest;

class SecurityAndCatalogTests extends IntegrationTest {

  @Test
  void healthIsPublicButEverythingElseNeedsToken() throws Exception {
    mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void loginRejectsBadCredentials() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerIsNotOpenToAnonymousOrUnprivilegedUsers() throws Exception {
    String body = "{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"x@y.com\",\"password\":\"longenough1\",\"roleId\":\""
        + roleRepository.findByName("ADMIN").orElseThrow().getId() + "\"}";
    postAs(null, "/api/v1/auth/register", body).andExpect(status().isUnauthorized());
    postAs(tokenFor("CASHIER"), "/api/v1/auth/register", body).andExpect(status().isForbidden());
  }

  @Test
  void adminCanRegisterUsersAndEmailsAreCaseInsensitive() throws Exception {
    String email = unique("Staff") + "@Apex.Test";
    String roleId = roleRepository.findByName("CASHIER").orElseThrow().getId().toString();
    postAs(adminToken(), "/api/v1/auth/register",
        "{\"firstName\":\"S\",\"lastName\":\"T\",\"email\":\"" + email + "\",\"password\":\"longenough1\",\"roleId\":\""
            + roleId + "\"}")
        .andExpect(status().isCreated());
    login(email.toLowerCase(), "longenough1");
    postAs(adminToken(), "/api/v1/auth/register",
        "{\"firstName\":\"S\",\"lastName\":\"T\",\"email\":\"" + email.toUpperCase() + "\",\"password\":\"longenough1\",\"roleId\":\""
            + roleId + "\"}")
        .andExpect(status().isConflict());
  }

  @Test
  void cashierCanReadCatalogButNotModifyIt() throws Exception {
    String cashier = tokenFor("CASHIER");
    getAs(cashier, "/api/v1/categories").andExpect(status().isOk());
    postAs(cashier, "/api/v1/categories", "{\"name\":\"Nope\"}").andExpect(status().isForbidden());
  }

  @Test
  void productLifecycleHonoursSoftDeleteAndCategoryRules() throws Exception {
    String admin = adminToken();
    var category = postAs(admin, "/api/v1/categories", "{\"name\":\"" + unique("Drinks") + "\"}")
        .andExpect(status().isCreated());
    String categoryId = read(category, "$.id");

    String sku = unique("SKU");
    var product = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Cola","unitOfMeasure":"bottle",
         "variants":[{"sku":"%s","costPrice":1.00,"retailPrice":2.50,
                      "barcodes":[{"barcode":"%s","type":"EAN_13"}]}]}
        """.formatted(categoryId, sku, unique("890")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(""));
    String productId = read(product, "$.id");

    // duplicate SKU -> 409
    postAs(admin, "/api/v1/products/" + productId + "/variants",
        "{\"sku\":\"" + sku + "\",\"costPrice\":1,\"retailPrice\":2}").andExpect(status().isConflict());

    // category in use cannot be removed
    deleteAs(admin, "/api/v1/categories/" + categoryId).andExpect(status().isConflict());

    deleteAs(admin, "/api/v1/products/" + productId).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/" + productId).andExpect(status().isNotFound());
    getAs(admin, "/api/v1/products?categoryId=" + categoryId).andExpect(jsonPath("$.content.length()").value(0));
    // archived products still reference the category, so it can only be deactivated
    deleteAs(admin, "/api/v1/categories/" + categoryId).andExpect(status().isConflict());
    putAs(admin, "/api/v1/categories/" + categoryId, "{\"name\":\"Retired\",\"isActive\":false}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void errorsAreReportedWithProperStatusCodes() throws Exception {
    String admin = adminToken();
    getAs(admin, "/api/v1/does-not-exist").andExpect(status().isNotFound());
    postAs(admin, "/api/v1/categories", "{not json").andExpect(status().isBadRequest());
    getAs(admin, "/api/v1/products?sort=nonsense,asc").andExpect(status().isBadRequest());
    getAs(admin, "/api/v1/products/not-a-uuid").andExpect(status().isBadRequest());
  }
}
