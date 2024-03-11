package org.eu.polarexpress.conductor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Component
public class HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(HttpHandler.class);
    private final HttpClient client;

    @Value("${util.user-agent}")
    private String userAgent;

    public HttpHandler() {
        CookieHandler.setDefault(new CookieManager());
        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(CookieHandler.getDefault())
                .build();
    }

    public void addCookie(String name, String value, String path, String domain, int version, String uri) {
        try {
            HttpCookie sessionCookie = new HttpCookie(name, value);
            sessionCookie.setPath(path);
            sessionCookie.setVersion(version);
            sessionCookie.setDomain(domain);
            sessionCookie.setSecure(false);
            sessionCookie.setHttpOnly(false);
            ((CookieManager) CookieHandler.getDefault()).getCookieStore().add(new URI(uri),
                    sessionCookie);
        } catch (URISyntaxException exception) {
            logger.error(exception.getMessage());
        }
    }

    public CompletableFuture<HttpResponse<Void>> head(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .HEAD()
                .uri(URI.create(uri))
                .header("User-Agent", userAgent)
                .header("Referer", "https://pixiv.net/")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    public CompletableFuture<HttpResponse<String>> get(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .header("User-Agent", userAgent)
                .header("Referer", "https://pixiv.net/")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<String>> getJson(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", "https://pixiv.net/")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public CompletableFuture<HttpResponse<InputStream>> stream(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(uri))
                .header("User-Agent", userAgent)
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .header("Referer", "https://pixiv.net/")
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
    }

}
