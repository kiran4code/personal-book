# Personal Book List with Google Books Integration

## Context
You have a Spring Boot project with a REST endpoint `/books` that returns
all books from an in-memory H2 database. The code includes a `Book` entity,
`BookRepository`, `BookController`, and a Google Books integration (`/books/google`)
exposing a search that returns the upstream Google schema via `GoogleBookService`.

## Using the Google Books API
* Base URL: `https://www.googleapis.com/books/v1`
* Search endpoint: `GET /volumes?q={query}` (e.g., `q=effective+java`, optional `maxResults`, `startIndex`).
* Volume details endpoint: `GET /volumes/{id}` to fetch a single book by Google volume ID.
* This project uses `GoogleBookService` with a configurable base URL. Set
  `google.books.base*url` in `application.properties` (or override in tests)
  to point to the real API or a mock server. The search route is exposed as
  `GET /google?q={query}` returning the upstream Google schema.

## Task
Implement the following, with accompanying tests for each change (tests are
mandatory):

1. Add a new REST endpoint that takes a Google Books volume ID as a parameter
   and adds the book to your personal list.

   * Endpoint: `POST /books/{googleId}` (path variable `googleId`).
   * Behavior:
       * Fetch the book details from the Google Books API (via `GoogleBookService`).
       * Map appropriate fields from `GoogleBook` to your `Book` entity
       (e.g., id, title, first author, pageCount).
       * Persist the mapped `Book` using `BookRepository`.
       * Return `201 Created` with the persisted `Book` in the response body.
   * Tests (Spring Boot tests):
       * Happy path: valid `googleId` returns 201 and persists the book with
       correct fields.
       * Error path: invalid or missing upstream data returns an appropriate
       error (e.g., `400 Bad Request`), and nothing is persisted.
       * Prefer mocking the downstream (e.g., MockWebServer/WireMock) to avoid
       flakiness; a smoke test hitting the real API is optional.

2. Keep existing functionality intact and verify:

   * The existing `GET /books` endpoint still returns all persisted books.
   * The Google search endpoint `/google` continues to return the
     Google schema payload as-is.
   * Tests should seed data where needed and assert on JSON responses and
     status codes.

You may refactor or add code as needed, but keep the existing structure.
Aim to complete within 30 minutes.

## Getting Started

### Prerequisites
- Java 17
- Maven 3.x
- Internet access (required when calling the real Google Books API)

### Clone and Build

## Open bash/cmd
git clone https://github.com/kiran4code/personal-book.git
cd personal-book
mvn clean test

## Configure Google Books API Key

This application integrates with the **Google Books API**. To run the Google search endpoint and integration tests, you must create and configure a Google API key.

### Step 1: Create a Google Books API Key (Google Cloud Console)
1. Open Google Cloud Console: https://console.cloud.google.com/
2. Create a new project (or select an existing one).
3. Enable the **Google Books API**:
   - Go to **APIs & Services → Library**
   - Search for **Google Books API** (Books API)
   - Click **Enable**
4. Create an API key:
   - Go to **APIs & Services → Credentials**
   - Click **Create Credentials → API key**
5. (Recommended) Restrict the key:
   - Open the created key in **Credentials**
   - Under **API restrictions**, select **Restrict key**
   - Choose **Google Books API** and save

### Step 2: Configure the API key as an Environment Variable
Set the API key in your environment:

**Mac/Linux**
## Open bash/cmd, run below commands to set env variable
export GOOGLE_BOOKS_API_KEY=AIzaSyAnfLc_RBt9395nwSDOq4lfYvHpvNU6XPs

### Win
## Open bash/cmd and run below command
set GOOGLE_BOOKS_API_KEY=AIzaSyAnfLc_RBt9395nwSDOq4lfYvHpvNU6XPs

### Call google end points
## Open bash/cmd and run below command
curl "http://localhost:8080/google?q=spring%20boot&maxResults=3"

### Verify Saved in DB
curl "http://localhost:8080/books"

