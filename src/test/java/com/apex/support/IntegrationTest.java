package com.apex.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Shared helpers for MockMvc based integration tests (H2 in-memory database). */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

  public static final String ADMIN_EMAIL = "admin@apex.test";
  public static final String ADMIN_PASSWORD = "Admin#12345";

  @Autowired protected MockMvc mvc;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected UserRepository userRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  protected String adminToken() throws Exception {
    return login(ADMIN_EMAIL, ADMIN_PASSWORD);
  }

  /** Creates (or reuses) a user with a built-in role and returns an access token for it. */
  protected String tokenFor(String roleName) throws Exception {
    String email = roleName.toLowerCase() + "@apex.test";
    if (!userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      Role role = roleRepository.findByName(roleName).orElseThrow();
      userRepository.save(User.builder().firstName(roleName).lastName("User").email(email)
          .passwordHash(passwordEncoder.encode("Passw0rd!x")).role(role).build());
    }
    return login(email, "Passw0rd!x");
  }

  protected String login(String email, String password) throws Exception {
    String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  protected ResultActions getAs(String token, String url) throws Exception {
    return mvc.perform(auth(get(url), token));
  }

  protected ResultActions postAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(post(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions putAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(put(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions patchAs(String token, String url, String json) throws Exception {
    return mvc.perform(auth(patch(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions deleteAs(String token, String url) throws Exception {
    return mvc.perform(auth(delete(url), token));
  }

  private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
    return token == null ? b : b.header("Authorization", "Bearer " + token);
  }

  protected static String read(ResultActions result, String path) throws Exception {
    Object v = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    return String.valueOf(v);
  }

  protected static String unique(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  // ---------- fixtures ----------

  protected String newCategory(String token) throws Exception {
    return read(postAs(token, "/api/v1/categories", "{\"name\":\"" + unique("Cat") + "\"}"), "$.id");
  }

  /** Creates a product with one variant and returns the variant id. */
  protected String newVariant(String token) throws Exception {
    return newVariant(token, "1.00", "2.00");
  }

  protected String newVariant(String token, String cost, String retail) throws Exception {
    var created = postAs(token, "/api/v1/products", """
        {"categoryId":"%s","name":"Item %s","unitOfMeasure":"piece",
         "variants":[{"sku":"%s","costPrice":%s,"retailPrice":%s}]}"""
        .formatted(newCategory(token), unique("p"), unique("SKU"), cost, retail));
    return read(created, "$.variants[0].id");
  }

  protected String newWarehouse(String token) throws Exception {
    return read(postAs(token, "/api/v1/warehouses", "{\"code\":\"" + unique("wh") + "\",\"name\":\"WH\"}"), "$.id");
  }

  protected void addStock(String token, String variantId, String warehouseId, int qty) throws Exception {
    postAs(token, "/api/v1/inventory/adjustments", """
        {"variantId":"%s","warehouseId":"%s","quantityChange":%d,"reason":"test stock"}"""
        .formatted(variantId, warehouseId, qty));
  }

  protected int onHand(String token, String variantId, String warehouseId) throws Exception {
    var r = getAs(token, "/api/v1/inventory?variantId=" + variantId + "&warehouseId=" + warehouseId);
    String body = r.andReturn().getResponse().getContentAsString();
    java.util.List<Integer> v = JsonPath.read(body, "$.content[*].quantityOnHand");
    return v.isEmpty() ? 0 : v.get(0);
  }

  protected int reserved(String token, String variantId, String warehouseId) throws Exception {
    var r = getAs(token, "/api/v1/inventory?variantId=" + variantId + "&warehouseId=" + warehouseId);
    java.util.List<Integer> v = JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.content[*].quantityReserved");
    return v.isEmpty() ? 0 : v.get(0);
  }
}
