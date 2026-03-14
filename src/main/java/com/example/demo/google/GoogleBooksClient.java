package com.example.demo.google;

public interface GoogleBooksClient {
    GoogleBook search(String query, Integer maxResults, Integer startIndex);
}
