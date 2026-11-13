package com.scasp.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Stage 3 requirement 7: retrieves an authorised HTTP resource with java.net.http.HttpClient.
 * The bearer token is supplied by the caller (e.g. from the SCASP_API_TOKEN environment variable) and is
 * never logged or committed. Every failure becomes a {@link Result} with a user-safe message instead of
 * an exception escaping to the user.
 */
public final class HttpResourceFetcher {
    public record Result(boolean ok, int status, String body, String message) {}

    private final HttpClient client;
    private final Duration timeout;

    public HttpResourceFetcher(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public Result fetch(String url, String bearerToken) {
        URI uri;
        try {
            uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                return new Result(false, 0, "", "Only http/https URLs are allowed");
        } catch (IllegalArgumentException e) {
            return new Result(false, 0, "", "Malformed URL");
        }
        HttpRequest.Builder b = HttpRequest.newBuilder(uri).timeout(timeout).header("Accept", "text/plain, application/json").GET();
        if (bearerToken != null && !bearerToken.isBlank()) b.header("Authorization", "Bearer " + bearerToken);
        try {
            HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return switch (r.statusCode() / 100) {
                case 2 -> new Result(true, r.statusCode(), r.body(), "OK");
                case 4 -> new Result(false, r.statusCode(), "", r.statusCode() == 401 || r.statusCode() == 403
                        ? "Not authorised to read this resource" : "Resource request rejected (" + r.statusCode() + ")");
                default -> new Result(false, r.statusCode(), "", "Remote service error (" + r.statusCode() + ")");
            };
        } catch (HttpTimeoutException e) {
            return new Result(false, 0, "", "Remote service timed out");
        } catch (IOException e) {
            return new Result(false, 0, "", "Remote service unavailable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, 0, "", "Request interrupted");
        }
    }

    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "http://localhost:8080/scasp/api/announcements";
        Result r = new HttpResourceFetcher(Duration.ofSeconds(5)).fetch(url, System.getenv("SCASP_API_TOKEN"));
        System.out.println(r.ok() ? "HTTP " + r.status() + "\n" + r.body() : "Failed safely: " + r.message() + " (status " + r.status() + ")");
    }
}
