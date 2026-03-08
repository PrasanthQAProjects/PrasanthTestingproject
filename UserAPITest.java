package com.qaportfolio.tests;

import com.qaportfolio.base.BaseAPI;
import com.qaportfolio.utils.TokenManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserAPITest extends BaseAPI {

    private String adminToken;
    private String userToken;
    private String tenantAToken;
    private String tenantBToken;
    private int createdUserId;

    @BeforeClass
    public void getTokens() {
        adminToken   = TokenManager.getToken("admin@example.com",  "Admin@123");
        userToken    = TokenManager.getToken("user@example.com",   "User@123");
        tenantAToken = TokenManager.getToken("tenantA@example.com","TenantA@123");
        tenantBToken = TokenManager.getToken("tenantB@example.com","TenantB@123");
    }

    // ── TC01: GET All Users ────────────────────────────────
    @Test(priority = 1, description = "GET /users returns 200 with list of users")
    public void testGetAllUsers() {
        withAuth(adminToken)
            .when()
                .get("/api/users")
            .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("size()", greaterThan(0))
                .time(lessThan(2000L));  // Response under 2 seconds
    }

    // ── TC02: GET Single User ──────────────────────────────
    @Test(priority = 2, description = "GET /users/{id} returns correct user")
    public void testGetUserById() {
        withAuth(adminToken)
            .when()
                .get("/api/users/1")
            .then()
                .statusCode(200)
                .body("id",    equalTo(1))
                .body("email", notNullValue())
                .body("name",  notNullValue());
    }

    // ── TC03: POST Create User ─────────────────────────────
    @Test(priority = 3, description = "POST /users creates new user successfully")
    public void testCreateUser() {
        String requestBody = """
            {
                "name":  "Test User",
                "email": "newuser@example.com",
                "role":  "viewer",
                "plan":  "free"
            }
            """;

        Response response = withAuth(adminToken)
            .body(requestBody)
            .when()
                .post("/api/users")
            .then()
                .statusCode(201)
                .body("name",  equalTo("Test User"))
                .body("email", equalTo("newuser@example.com"))
                .body("id",    notNullValue())
                .extract().response();

        // Save created user ID for later tests
        createdUserId = response.jsonPath().getInt("id");
    }

    // ── TC04: PUT Update User ──────────────────────────────
    @Test(priority = 4, description = "PUT /users/{id} updates user successfully",
          dependsOnMethods = "testCreateUser")
    public void testUpdateUser() {
        String requestBody = """
            {
                "name": "Updated User Name"
            }
            """;

        withAuth(adminToken)
            .body(requestBody)
            .when()
                .put("/api/users/" + createdUserId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Updated User Name"));
    }

    // ── TC05: DELETE User ──────────────────────────────────
    @Test(priority = 5, description = "DELETE /users/{id} removes user",
          dependsOnMethods = "testUpdateUser")
    public void testDeleteUser() {
        withAuth(adminToken)
            .when()
                .delete("/api/users/" + createdUserId)
            .then()
                .statusCode(204);

        // Verify user no longer exists
        withAuth(adminToken)
            .when()
                .get("/api/users/" + createdUserId)
            .then()
                .statusCode(404);
    }

    // ── TC06: Unauthorized Access (No Token) ──────────────
    @Test(priority = 6, description = "Request without token returns 401")
    public void testUnauthorizedAccess() {
        withoutAuth()
            .when()
                .get("/api/users")
            .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized"));
    }

    // ── TC07: Expired Token Returns 401 ───────────────────
    @Test(priority = 7, description = "Expired JWT token returns 401")
    public void testExpiredToken() {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired";

        withAuth(expiredToken)
            .when()
                .get("/api/users")
            .then()
                .statusCode(401);
    }

    // ── TC08: Role-Based Access — Viewer Cannot Delete ────
    @Test(priority = 8, description = "Viewer role cannot delete users (403)")
    public void testViewerCannotDeleteUser() {
        withAuth(userToken)
            .when()
                .delete("/api/users/1")
            .then()
                .statusCode(403)
                .body("error", containsString("Forbidden"));
    }

    // ── TC09: Multi-Tenant Isolation ──────────────────────
    @Test(priority = 9, description = "Tenant A cannot access Tenant B data")
    public void testTenantDataIsolation() {
        // Tenant B's resource ID
        int tenantBResourceId = 999;

        withAuth(tenantAToken)
            .when()
                .get("/api/tenantB/users/" + tenantBResourceId)
            .then()
                .statusCode(403);  // Must be forbidden!
    }

    // ── TC10: Rate Limiting ────────────────────────────────
    @Test(priority = 10, description = "API returns 429 when rate limit exceeded")
    public void testRateLimiting() {
        // Send 101 requests to trigger rate limit
        for (int i = 0; i < 100; i++) {
            withAuth(userToken).get("/api/users");
        }

        // 101st request should be rate limited
        withAuth(userToken)
            .when()
                .get("/api/users")
            .then()
                .statusCode(429)
                .body("error", containsString("Rate limit exceeded"));
    }

    // ── TC11: Invalid User ID (404) ────────────────────────
    @Test(priority = 11, description = "GET non-existent user returns 404")
    public void testGetNonExistentUser() {
        withAuth(adminToken)
            .when()
                .get("/api/users/99999")
            .then()
                .statusCode(404)
                .body("error", equalTo("User not found"));
    }

    // ── TC12: Invalid Request Body (400) ──────────────────
    @Test(priority = 12, description = "POST with missing required fields returns 400")
    public void testCreateUserWithMissingFields() {
        String invalidBody = """
            {
                "name": "No Email User"
            }
            """;

        withAuth(adminToken)
            .body(invalidBody)
            .when()
                .post("/api/users")
            .then()
                .statusCode(400)
                .body("error", containsString("email is required"));
    }
}
