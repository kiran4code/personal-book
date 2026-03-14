package com.example.demo.google;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
public class GoogleBookService {
    private final RestClient restClient;
    private final BookRepository bookRepository;  // ADDED: used to save books
    private final String apiKey;                  // ADDED: API key from properties
    private final BookMapper bookMapper;

    /*public GoogleBookService(@Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }*/
    // CHANGED: constructor now injects apiKey + BookRepository
    public GoogleBookService(
            @Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api-key:}") String apiKey,     // ADDED
            BookRepository bookRepository,                          // ADDED
            BookMapper bookMapper
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;                    // ADDED
        this.bookRepository = bookRepository;    // ADDED
        this.bookMapper = bookMapper;
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
}

