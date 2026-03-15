package com.example.demo.google;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.exception.ValidationException;
import com.example.demo.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientResponseException;
import com.example.demo.mapper.GoogleVolumeToBookMapper;


import java.util.List;

@Slf4j
@Service
public class GoogleBookService {
    private final RestClient restClient;
    private final BookRepository bookRepository;  // ADDED: used to save books
    private final String apiKey;                  // ADDED: API key from properties
    private final BookMapper bookMapper;
    private final GoogleVolumeToBookMapper googleVolumeToBookMapper;

    /*public GoogleBookService(@Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }*/
    // CHANGED: constructor now injects apiKey + BookRepository
    public GoogleBookService(
            @Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api-key:}") String apiKey,     // ADDED
            BookRepository bookRepository,                          // ADDED
            BookMapper bookMapper, GoogleVolumeToBookMapper googleVolumeToBookMapper
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;                    // ADDED
        this.bookRepository = bookRepository;    // ADDED
        this.bookMapper = bookMapper;
        this.googleVolumeToBookMapper = googleVolumeToBookMapper;
    }

    @Transactional
    public GoogleBook searchBooks(String query, Integer maxResults, Integer startIndex) {
        try {
            GoogleBook googleBook = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes")
                            .queryParam("q", query)
                            .queryParam("maxResults", maxResults != null ? maxResults : 10)
                            .queryParam("startIndex", startIndex != null ? startIndex : 0)
                            .build())
                    .retrieve()
                    .body(GoogleBook.class);
            // ADDED: persist response into DB using BookRepository
            if (googleBook != null && googleBook.items() != null) {
                int saved = 0;                                  // ADDED: track saves

                for (GoogleBook.Item item : googleBook.items()) {
                    // ADDED: slightly stronger null checks
                    if (item == null || item.id() == null || item.volumeInfo() == null) continue;
                    if (item.volumeInfo().title() == null) continue;

                    // ADDED: prevent duplicate insert (id is primary key)
                    if (!bookRepository.existsById(item.id())) {
                        Book book = bookMapper.toEntity(item);  // CHANGED: mapping moved to mapper
                        bookRepository.save(book);
                        saved++;                                // ADDED
                    }
                }
                log.info("Google search completed for q='{}', saved={}", query, saved); // ADDED
            }
            return googleBook;
        }catch (Exception e) {
            log.error("Error while calling Google or saving books", e);
            throw e;
        }
    }
    public GoogleVolume fetchVolumeById(String googleId) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/volumes/{id}")
                            .queryParam("key", apiKey)
                            .build(googleId))
                    .retrieve()
                    .body(GoogleVolume.class);
        } catch (RestClientResponseException ex) {
            HttpStatusCode sc = HttpStatusCode.valueOf(ex.getRawStatusCode());
            // If the volume doesn't exist, treat it as bad request for this API (as requested)
            if (sc.value() == 404) {
                throw new ValidationException("Invalid googleId: volume not found");
            }
            // Bubble up other codes (401/403/5xx etc) via your existing exceptions if you already added them
            if (sc.value() == 401 || sc.value() == 403) {
                throw new com.example.demo.exception.GoogleBooksAuthException(
                        "Invalid or missing Google Books API key", sc, ex);
            }
            throw new com.example.demo.exception.GoogleBooksClientException(
                    "Google Books upstream error: " + sc.value(), sc, ex);
        } catch (Exception ex) {
            throw new com.example.demo.exception.GoogleBooksClientException(
                    "Google Books timeout/unreachable", HttpStatus.GATEWAY_TIMEOUT, ex);
        }
    }
    @Transactional
    public Book addBookToPersonalList(String googleId) {
        if (googleId == null || googleId.isBlank()) {
            throw new ValidationException("googleId must not be blank");
        }

        GoogleVolume volume = fetchVolumeById(googleId);

        // Validate upstream data correctness before persisting
        if (volume == null || volume.id() == null || volume.id().isBlank()) {
            throw new ValidationException("Invalid upstream response: missing id");
        }
        if (volume.volumeInfo() == null || volume.volumeInfo().title() == null || volume.volumeInfo().title().isBlank()) {
            throw new ValidationException("Invalid upstream response: missing title");
        }

        Book book = googleVolumeToBookMapper.toBook(volume);
        return bookRepository.save(book);
    }
}

