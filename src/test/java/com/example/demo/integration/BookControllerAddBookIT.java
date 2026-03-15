package com.example.demo.integration;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerAddBookIT {

    static MockWebServer mockWebServer;

    @Autowired MockMvc mockMvc;
    @Autowired BookRepository bookRepository;

    @BeforeAll
    static void startServer() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("google.books.base-url", () -> mockWebServer.url("/").toString());
        registry.add("google.books.api-key", () -> "TEST_KEY");
    }

    @BeforeEach
    void cleanDb() {
        bookRepository.deleteAll();
    }

    @Test
    void postBooksByGoogleId_happyPath_returns201_andPersists() throws Exception {
        String googleId = "lRtdEAAAQBAJ";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "id": "%s",
                      "volumeInfo": {
                        "title": "Spring in Action",
                        "authors": ["Craig Walls", "Other"],
                        "pageCount": 520
                      }
                    }
                """.formatted(googleId)));

        mockMvc.perform(post("/books/{googleId}", googleId))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(googleId))
                .andExpect(jsonPath("$.title").value("Spring in Action"))
                .andExpect(jsonPath("$.author").value("Craig Walls"))
                .andExpect(jsonPath("$.pageCount").value(520));

        // verify DB persisted
        assertThat(bookRepository.findById(googleId)).isPresent();

        // verify API key was used
        var recorded = mockWebServer.takeRequest();
        assertThat(recorded.getPath()).contains("key=TEST_KEY");
        assertThat(recorded.getPath()).contains("/volumes/" + googleId);
    }

    @Test
    void postBooksByGoogleId_missingUpstreamTitle_returns400_andDoesNotPersist() throws Exception {
        String googleId = "bad-id";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    { "id": "%s", "volumeInfo": { "authors": ["A"], "pageCount": 10 } }
                """.formatted(googleId)));

        mockMvc.perform(post("/books/{googleId}", googleId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(bookRepository.count()).isEqualTo(0);
    }

    @Test
    void getBooks_stillWorks_returnsAllPersisted() throws Exception {
        bookRepository.save(new Book("id1", "T1", "A1"));
        bookRepository.save(new Book("id2", "T2", "A2"));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void googleSearch_endpointStillReturnsGoogleSchemaPayloadAsIs() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "kind": "books#volumes",
                      "totalItems": 1,
                      "items": [
                        {
                          "id": "x1",
                          "selfLink": "s",
                          "volumeInfo": {
                            "title": "Some Title",
                            "authors": ["Auth"],
                            "pageCount": 111,
                            "publishedDate": "2020",
                            "publisher": "P",
                            "printType": "BOOK",
                            "maturityRating": "NOT_MATURE",
                            "categories": ["C"],
                            "language": "en",
                            "previewLink": "p",
                            "infoLink": "i"
                          },
                          "searchInfo": { "textSnippet": "snip" }
                        }
                      ]
                    }
                """));

        mockMvc.perform(get("/google").param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("books#volumes"))
                .andExpect(jsonPath("$.items[0].id").value("x1"));
    }
}
