package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class CatalogCompletionTests extends IntegrationTest {

  private String category(String admin) throws Exception {
    return read(postAs(admin, "/api/v1/categories", "{\"name\":\"" + unique("Cat") + "\"}"), "$.id");
  }

  @Test
  void productAndVariantCanBeUpdatedAndVariantDeactivated() throws Exception {
    String admin = adminToken();
    String cat = category(admin);
    String cat2 = category(admin);
    var created = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Shirt","unitOfMeasure":"piece",
         "variants":[{"sku":"%s","costPrice":5,"retailPrice":10}]}""".formatted(cat, unique("SH")));
    String productId = read(created, "$.id");
    String variantId = read(created, "$.variants[0].id");

    putAs(admin, "/api/v1/products/" + productId, """
        {"categoryId":"%s","name":"Polo Shirt","unitOfMeasure":"piece","brand":"Apex","isActive":true}""".formatted(cat2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Polo Shirt"))
        .andExpect(jsonPath("$.categoryId").value(cat2));

    putAs(admin, "/api/v1/products/variants/" + variantId, """
        {"costPrice":6,"retailPrice":12.5,"weight":{"value":0.2,"unit":"kg"},"variantAttributes":{"size":"M"}}""")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retailPrice").value(12.5))
        .andExpect(jsonPath("$.weight.unit").value("kg"));

    deleteAs(admin, "/api/v1/products/variants/" + variantId).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  void mediaCanBeAttachedAndOnlyOnePrimaryRemains() throws Exception {
    String admin = adminToken();
    String productId = read(postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Lamp","unitOfMeasure":"piece"}""".formatted(category(admin))), "$.id");

    String m1 = read(postAs(admin, "/api/v1/products/" + productId + "/media",
        "{\"url\":\"https://cdn/a.jpg\",\"fileName\":\"a.jpg\",\"mimeType\":\"image/jpeg\",\"isPrimary\":true}")
        .andExpect(status().isCreated()), "$.id");
    postAs(admin, "/api/v1/products/" + productId + "/media",
        "{\"url\":\"https://cdn/b.jpg\",\"fileName\":\"b.jpg\",\"mimeType\":\"image/jpeg\",\"isPrimary\":true,\"sortOrder\":1}")
        .andExpect(status().isCreated());

    getAs(admin, "/api/v1/products/" + productId + "/media")
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].isPrimary").value(false))
        .andExpect(jsonPath("$[1].isPrimary").value(true));
    getAs(admin, "/api/v1/products/" + productId).andExpect(jsonPath("$.mediaUrls.length()").value(2));

    deleteAs(admin, "/api/v1/products/media/" + m1).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/" + productId + "/media").andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void barcodesKeepASinglePrimaryAndCanBeRemoved() throws Exception {
    String admin = adminToken();
    var created = postAs(admin, "/api/v1/products", """
        {"categoryId":"%s","name":"Gum","unitOfMeasure":"pack",
         "variants":[{"sku":"%s","costPrice":1,"retailPrice":2,"barcodes":[{"barcode":"%s","type":"EAN_13"}]}]}"""
        .formatted(category(admin), unique("GM"), unique("111")));
    String variantId = read(created, "$.variants[0].id");

    var second = postAs(admin, "/api/v1/products/variants/" + variantId + "/barcodes",
        "{\"barcode\":\"" + unique("222") + "\",\"type\":\"UPC_A\",\"isPrimary\":true}").andExpect(status().isCreated());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.barcodes[?(@.isPrimary==true)]", org.hamcrest.Matchers.hasSize(1)));

    deleteAs(admin, "/api/v1/products/barcodes/id/" + read(second, "$.id")).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/products/variants/sku/" + read(created, "$.variants[0].sku"))
        .andExpect(jsonPath("$.barcodes.length()").value(1))
        .andExpect(jsonPath("$.barcodes[0].isPrimary").value(true));
  }

  @Test
  void categoriesCanBeMovedButNotIntoTheirOwnSubtree() throws Exception {
    String admin = adminToken();
    String root = category(admin);
    String child = read(postAs(admin, "/api/v1/categories",
        "{\"name\":\"" + unique("Child") + "\",\"parentId\":\"" + root + "\"}"), "$.id");
    String other = category(admin);

    patchAs(admin, "/api/v1/categories/" + root + "/parent", "{\"parentId\":\"" + child + "\"}")
        .andExpect(status().isBadRequest());
    patchAs(admin, "/api/v1/categories/" + child + "/parent", "{\"parentId\":\"" + other + "\"}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.parentId").value(other));
    patchAs(admin, "/api/v1/categories/" + child + "/parent", "{}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.parentId").doesNotExist());
  }
}
