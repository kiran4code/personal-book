package com.example.demo.integration;

import com.example.demo.db.BookRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoogleBooksHappyPathIT {

    @Autowired TestRestTemplate restTemplate;
    @Autowired BookRepository bookRepository;

    @Test
    void validApiKey_realGoogleCall_savesToDb() {
        String key = System.getenv("GOOGLE_BOOKS_API_KEY");
        System.out.println("Checking GOOGLE_BOOKS_API_KEY");
        System.out.println("GOOGLE_BOOKS_API_KEY=" + System.getenv("GOOGLE_BOOKS_API_KEY"));
        Assumptions.assumeTrue(key != null && !key.isBlank(),
                "Set GOOGLE_BOOKS_API_KEY env var to run this test");
        System.out.println("GOOGLE_BOOKS_API_KEY=" + System.getenv("GOOGLE_BOOKS_API_KEY"));
        long before = bookRepository.count();

        ResponseEntity<String> resp =
                restTemplate.getForEntity("/google?q=spring%20boot&maxResults=3&startIndex=0", String.class);

        assertEquals(200, resp.getStatusCode().value());
        assertTrue(bookRepository.count() > before, "Expected DB count to increase after saving");
    }
}