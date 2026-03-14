package com.example.demo.google;

import com.example.demo.exception.GoogleBooksClientException;
import com.example.demo.exception.GoogleBooksAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoogleBooksClientImpl implements GoogleBooksClient {

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksClientImpl(
            @Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api-key:}") String apiKey,
            @Value("${google.books.timeout-ms:5000}") int timeoutMs
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(timeoutMs);
                    setReadTimeout(timeoutMs);
                }})
                .build();
    }

    @Override
    public GoogleBook search(String query, Integer maxResults, Integer startIndex) {
        try {
            log.info("Calling Google Books API for q='{}'", query);

            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes")
                            .queryParam("q", query)
                            .queryParam("maxResults", maxResults != null ? maxResults : 10)
                            .queryParam("startIndex", startIndex != null ? startIndex : 0)
                            .queryParam("key", apiKey) // required
                            .build())
                    .retrieve()
                    .body(GoogleBook.class);

        } catch (RestClientResponseException ex) {
            HttpStatusCode sc = HttpStatusCode.valueOf(ex.getRawStatusCode());

            if (sc.value() == 401 || sc.value() == 403) {
                throw new GoogleBooksAuthException("Invalid or missing Google Books API key", sc, ex);
            }

            // 404, 500, 502, 503, 504 etc
            throw new GoogleBooksClientException("Google Books upstream error: " + sc.value(), sc, ex);

        } catch (Exception ex) {
            // timeout/unreachable => 504
            throw new GoogleBooksClientException("Google Books timeout/unreachable", HttpStatus.GATEWAY_TIMEOUT, ex);
        }
    }
}