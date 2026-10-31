package com.apex;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.apex.support.IntegrationTest;

class IdentityTests extends IntegrationTest {

  private String roleId(String name) {
    return roleRepository.findByName(name).orElseThrow().getId().toString();
  }

  private String newUser(String admin, String email, String role) throws Exception {
    return read(postAs(admin, "/api/v1/auth/register", """
        {"firstName":"U","lastName":"X","email":"%s","password":"longenough1","roleId":"%s"}"""
        .formatted(email, roleId(role))).andExpect(status().isCreated()), "$.user.id");
  }

  @Test
  void adminManagesUsers() throws Exception {
    String admin = adminToken();
    String email = unique("emp") + "@apex.test";
    String id = newUser(admin, email, "CASHIER");

    getAs(admin, "/api/v1/users").andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    putAs(admin, "/api/v1/users/" + id, """
        {"firstName":"New","lastName":"Name","roleId":"%s","isActive":true}""".formatted(roleId("MANAGER")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.roleName").value("MANAGER"));

    // deactivated users cannot log in
    putAs(admin, "/api/v1/users/" + id, """
        {"firstName":"New","lastName":"Name","roleId":"%s","isActive":false}""".formatted(roleId("MANAGER")))
        .andExpect(status().isOk());
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
        .contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"longenough1\"}"))
        .andExpect(status().isUnauthorized());

    deleteAs(admin, "/api/v1/users/" + id).andExpect(status().isNoContent());
    getAs(admin, "/api/v1/users/" + id).andExpect(status().isNotFound());
    newUser(admin, email, "CASHIER"); // e-mail is free again
  }

  @Test
  void adminCannotLockThemselvesOut() throws Exception {
    String admin = adminToken();
    String selfId = userRepository.findByEmailAndDeletedAtIsNull(ADMIN_EMAIL).orElseThrow().getId().toString();
    putAs(admin, "/api/v1/users/" + selfId, """
        {"firstName":"S","lastName":"A","roleId":"%s","isActive":false}""".formatted(roleId("ADMIN")))
        .andExpect(status().isUnprocessableEntity());
    deleteAs(admin, "/api/v1/users/" + selfId).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void passwordsCanBeChangedAndReset() throws Exception {
    String admin = adminToken();
    String email = unique("pw") + "@apex.test";
    String id = newUser(admin, email, "CASHIER");
    String token = login(email, "longenough1");

    patchAs(token, "/api/v1/auth/password", "{\"currentPassword\":\"nope\",\"newPassword\":\"brandnew123\"}")
        .andExpect(status().isUnauthorized());
    patchAs(token, "/api/v1/auth/password", "{\"currentPassword\":\"longenough1\",\"newPassword\":\"brandnew123\"}")
        .andExpect(status().isNoContent());
    login(email, "brandnew123");

    patchAs(token, "/api/v1/users/" + id + "/password", "{\"newPassword\":\"sneaky12345\"}")
        .andExpect(status().isForbidden());
    patchAs(admin, "/api/v1/users/" + id + "/password", "{\"newPassword\":\"resetbyadmin1\"}")
        .andExpect(status().isNoContent());
    login(email, "resetbyadmin1");
  }

  @Test
  void rolesCanBeManagedButBuiltInsAreProtected() throws Exception {
    String admin = adminToken();
    getAs(admin, "/api/v1/roles/permissions").andExpect(status().isOk());

    String name = unique("auditor").replace("-", "_").toUpperCase();
    var created = postAs(admin, "/api/v1/roles",
        "{\"name\":\"" + name + "\",\"permissions\":[\"product:read\",\"report:read\"]}")
        .andExpect(status().isCreated());
    String id = read(created, "$.id");

    postAs(admin, "/api/v1/roles", "{\"name\":\"bad\",\"permissions\":[\"made:up\"]}")
        .andExpect(status().isBadRequest());
    putAs(admin, "/api/v1/roles/" + id, "{\"name\":\"" + name + "\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isOk()).andExpect(jsonPath("$.permissions.length()").value(1));

    putAs(admin, "/api/v1/roles/" + roleId("CASHIER"), "{\"name\":\"RENAMED\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isUnprocessableEntity());
    putAs(admin, "/api/v1/roles/" + roleId("ADMIN"), "{\"name\":\"ADMIN\",\"permissions\":[\"product:read\"]}")
        .andExpect(status().isUnprocessableEntity());
    deleteAs(admin, "/api/v1/roles/" + roleId("MANAGER")).andExpect(status().isUnprocessableEntity());

    newUser(admin, unique("aud") + "@apex.test", name);
    deleteAs(admin, "/api/v1/roles/" + id).andExpect(status().isConflict());
    getAs(tokenFor("CASHIER"), "/api/v1/roles").andExpect(status().isForbidden());
  }
}
