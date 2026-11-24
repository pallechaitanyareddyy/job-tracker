package com.chaitu.jobtracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end test: starts the real application on a random port, with the real security rules and the
 * jobtracker_test database, and calls it over HTTP exactly like a browser or PowerShell would.
 * Every test registers its own unique email, so test runs never collide with each other.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Passw0rd!123";
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Test
    void registerCreatesAnAccountAndNeverReturnsThePassword() throws Exception {
        String email = newEmail();

        HttpResponse<String> response = register(email, PASSWORD);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains(email).doesNotContain("password");
    }

    @Test
    void registeringTheSameEmailTwiceReturnsConflict() throws Exception {
        String email = newEmail();
        register(email, PASSWORD);

        HttpResponse<String> second = register(email.toUpperCase(), PASSWORD);

        assertThat(second.statusCode()).isEqualTo(409);
    }

    @Test
    void aTooShortPasswordIsRejectedWithAFieldError() throws Exception {
        HttpResponse<String> response = register(newEmail(), "short");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("password");
    }

    @Test
    void loginWithAWrongPasswordReturnsUnauthorized() throws Exception {
        String email = newEmail();
        register(email, PASSWORD);

        assertThat(login(email, "not-the-password").statusCode()).isEqualTo(401);
        assertThat(login(newEmail(), PASSWORD).statusCode()).isEqualTo(401);
    }

    @Test
    void loginReturnsAnAccessTokenAndAnHttpOnlyRefreshCookie() throws Exception {
        String email = newEmail();
        register(email, PASSWORD);

        HttpResponse<String> response = login(email, PASSWORD);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("accessToken").contains("Bearer");
        assertThat(response.body()).doesNotContain("refresh");

        String cookie = response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No refresh_token cookie was set"));
        assertThat(cookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/v1/auth");
    }

    @Test
    void profileWithoutATokenReturnsUnauthorized() throws Exception {
        assertThat(get("/api/v1/users/profile", null).statusCode()).isEqualTo(401);
    }

    @Test
    void profileWithAValidTokenReturnsTheOwnersProfile() throws Exception {
        String email = newEmail();
        register(email, PASSWORD);
        String accessToken = extractAccessToken(login(email, PASSWORD).body());

        HttpResponse<String> response = get("/api/v1/users/profile", accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(email);
    }

    private static String newEmail() {
        return "it-" + UUID.randomUUID() + "@example.com";
    }

    private static String extractAccessToken(String loginBody) {
        Matcher matcher = ACCESS_TOKEN.matcher(loginBody);
        assertThat(matcher.find()).as("login response contains an accessToken").isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> register(String email, String password) throws Exception {
        String json = """
                {"email":"%s","password":"%s","fullName":"Test User"}
                """.formatted(email, password);
        return postJson("/api/v1/auth/register", json);
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        String json = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        return postJson("/api/v1/auth/login", json);
    }

    private HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
