package com.example.ecommerce.controller;

import com.example.ecommerce.support.CapturingPasswordResetNotifier;
import com.example.ecommerce.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: the /api/v1/auth endpoints end to end (HTTP, security filters, services, PostgreSQL, Redis).
 */
@IntegrationTest
class AuthApiIntegrationTest {

    private static final String PASSWORD = "Secret@123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CapturingPasswordResetNotifier resetNotifier;

    // ------------------------------------------------------------------ sign-up and sign-in

    @Test
    void registerSignsTheCustomerIn() throws Exception {
        String username = unique("reg");
        register(username)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auth/me"))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.username", is(username)))
                .andExpect(jsonPath("$.data.user.roles", containsInAnyOrder("ROLE_CUSTOMER")));

        register(username)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    void registrationInputIsValidatedFieldByField() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"not-an-email","password":"weak"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("email")))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("password")))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("username")));
    }

    @Test
    void loginWorksWithUsernameOrEmailAndRejectsWrongPasswords() throws Exception {
        String username = unique("login");
        register(username).andExpect(status().isCreated());

        login(username, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.data.user.username", is(username)));
        login(username + "@mail.test", PASSWORD).andExpect(status().isOk());

        login(username, "Wrong@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_CREDENTIALS")));
        login("nobody-" + username, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    void bootstrapAdminIsSuperAdmin() throws Exception {
        String token = accessTokenOf(login("admin", "Admin@123"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_ADMIN")));
    }

    // ------------------------------------------------------------------ access tokens

    @Test
    void protectedEndpointsNeedAValidAccessToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")));

        String username = unique("me");
        String token = accessTokenOf(register(username));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is(username)));
    }

    @Test
    void customersCannotReachAdminEndpoints() throws Exception {
        String token = accessTokenOf(register(unique("cust")));
        mvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    // ------------------------------------------------------------------ refresh rotation

    @Test
    void refreshRotatesTokensAndDetectsReuse() throws Exception {
        String first = refreshTokenOf(register(unique("rot")));

        String secondResponse = refresh(first).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = JsonPath.read(secondResponse, "$.data.refreshToken");
        String accessFromSecond = JsonPath.read(secondResponse, "$.data.accessToken");
        assertThat(second).isNotEqualTo(first);

        // the old token is presented again: theft is assumed and the whole sign-in ends
        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("REFRESH_TOKEN_REUSED")));
        refresh(second)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")));

        // access tokens already issued stay usable until they expire (15 minutes)
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessFromSecond))
                .andExpect(status().isOk());

        refresh("made-up-token").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")));
    }

    @Test
    void refreshIgnoresAnExpiredAccessTokenSentAlong() throws Exception {
        String refresh = refreshTokenOf(register(unique("hdr")));
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer expired.or.broken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ ending sessions

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refresh = refreshTokenOf(register(unique("out")));
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk());
        refresh(refresh).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEverywhereEndsAccessAndRefreshTokensOfAllDevices() throws Exception {
        String username = unique("all");
        String phone = register(username).andReturn().getResponse().getContentAsString();
        String laptop = login(username, PASSWORD).andReturn().getResponse().getContentAsString();

        mvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + JsonPath.read(phone, "$.data.accessToken")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(laptop, "$.data.accessToken")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("INVALID_TOKEN")));
        refresh(JsonPath.read(laptop, "$.data.refreshToken")).andExpect(status().isUnauthorized());
        login(username, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void changingThePasswordEndsOtherSessionsAndKeepsThisOne() throws Exception {
        String username = unique("pwd");
        String other = register(username).andReturn().getResponse().getContentAsString();
        String token = accessTokenOf(login(username, PASSWORD));

        changePassword(token, "Wrong@123", "Changed@456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")));
        changePassword(token, PASSWORD, "Changed@456", "Mismatch@456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));

        String changed = changePassword(token, PASSWORD, "Changed@456")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(changed, "$.data.accessToken")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(other, "$.data.accessToken")))
                .andExpect(status().isUnauthorized());
        login(username, PASSWORD).andExpect(status().isUnauthorized());
        login(username, "Changed@456").andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ password recovery

    @Test
    void passwordCanBeResetOnceWithTheEmailedToken() throws Exception {
        String username = unique("reset");
        String email = username + "@mail.test";
        String session = register(username).andReturn().getResponse().getContentAsString();

        forgotPassword(email, "10.1.0.1").andExpect(status().isAccepted());
        forgotPassword("nobody-" + email, "10.1.0.1").andExpect(status().isAccepted()); // same answer
        String token = resetNotifier.lastTokenFor(email);
        assertThat(token).isNotBlank();

        resetPassword(token, "Reset@789").andExpect(status().isOk());
        resetPassword(token, "Again@789")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + JsonPath.read(session, "$.data.accessToken")))
                .andExpect(status().isUnauthorized());
        login(username, "Reset@789").andExpect(status().isOk());
    }

    @Test
    void forgotPasswordIsRateLimitedPerClientAddress() throws Exception {
        String address = "10.77." + (int) (Math.random() * 250) + ".9";
        for (int i = 0; i < 3; i++) {
            forgotPassword("someone@mail.test", address).andExpect(status().isAccepted());
        }
        forgotPassword("someone@mail.test", address)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")));
        // another client is not affected
        forgotPassword("someone@mail.test", "10.78.0.1").andExpect(status().isAccepted());
    }

    // ------------------------------------------------------------------ helpers

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String username) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s@mail.test","password":"%s","fullName":"Test User"}"""
                        .formatted(username, username, PASSWORD)));
    }

    private ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"%s\"}".formatted(login, password)));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private ResultActions changePassword(String accessToken, String current, String next) throws Exception {
        return changePassword(accessToken, current, next, next);
    }

    private ResultActions changePassword(String accessToken, String current, String next, String confirm) throws Exception {
        return mvc.perform(put("/api/v1/auth/password").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}"
                        .formatted(current, next, confirm)));
    }

    private ResultActions forgotPassword(String email, String clientAddress) throws Exception {
        return mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}")
                .with(request -> {
                    request.setRemoteAddr(clientAddress);
                    return request;
                }));
    }

    private ResultActions resetPassword(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}"
                        .formatted(token, newPassword, newPassword)));
    }

    private static String accessTokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.accessToken");
    }

    private static String refreshTokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.refreshToken");
    }
}
