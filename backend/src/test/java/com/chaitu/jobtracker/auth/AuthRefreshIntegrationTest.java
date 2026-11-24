package com.chaitu.jobtracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests refresh-token rotation, reuse detection and logout over real HTTP.
 * The cookie is passed by hand (as a "Cookie" header) the way a browser would send it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthRefreshIntegrationTest {

    private static final String PASSWORD = "Passw0rd!123";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Test
    void refreshGivesANewCookieAndANewAccessToken() throws Exception {
        String refreshToken = registerAndLoginForRefreshToken();

        HttpResponse<String> response = post("/api/v1/auth/refresh", refreshToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("accessToken");
        String newToken = cookieValue(response);
        assertThat(newToken).isNotBlank().isNotEqualTo(refreshToken);
    }

    @Test
    void refreshWithoutACookieReturnsUnauthorized() throws Exception {
        assertThat(post("/api/v1/auth/refresh", null).statusCode()).isEqualTo(401);
    }

    @Test
    void reusingAnOldRefreshTokenIsRejectedAndKillsTheNewOneToo() throws Exception {
        String first = registerAndLoginForRefreshToken();
        String second = cookieValue(post("/api/v1/auth/refresh", first));

        // The first token was already used: this looks like theft.
        HttpResponse<String> replay = post("/api/v1/auth/refresh", first);
        assertThat(replay.statusCode()).isEqualTo(401);
        assertThat(replay.headers().allValues("Set-Cookie")).anyMatch(c -> c.contains("Max-Age=0"));

        // As a safety measure, the newest token of that user no longer works either.
        assertThat(post("/api/v1/auth/refresh", second).statusCode()).isEqualTo(401);
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        String refreshToken = registerAndLoginForRefreshToken();

        HttpResponse<String> logout = post("/api/v1/auth/logout", refreshToken);

        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().allValues("Set-Cookie")).anyMatch(c -> c.contains("Max-Age=0"));
        assertThat(post("/api/v1/auth/refresh", refreshToken).statusCode()).isEqualTo(401);
    }

    @Test
    void logoutWithoutACookieStillSucceeds() throws Exception {
        assertThat(post("/api/v1/auth/logout", null).statusCode()).isEqualTo(204);
    }

    private String registerAndLoginForRefreshToken() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", """
                {"email":"%s","password":"%s","fullName":"Test User"}
                """.formatted(email, PASSWORD), null);
        HttpResponse<String> login = post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD), null);
        return cookieValue(login);
    }

    private static String cookieValue(HttpResponse<String> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("refresh_token="))
                .map(value -> value.substring("refresh_token=".length(), value.indexOf(';')))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No refresh_token cookie was set"));
    }

    /** POST with an empty JSON body and an optional refresh cookie. */
    private HttpResponse<String> post(String path, String refreshCookie) throws Exception {
        return post(path, "", refreshCookie);
    }

    private HttpResponse<String> post(String path, String json, String refreshCookie) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (refreshCookie != null) {
            builder.header("Cookie", "refresh_token=" + refreshCookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
