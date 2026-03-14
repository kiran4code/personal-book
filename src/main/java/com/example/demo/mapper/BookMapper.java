package com.example.demo.mapper;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleBook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "volumeInfo.title")
    @Mapping(target = "author", expression = "java(joinAuthors(item.volumeInfo().authors()))")
    @Mapping(target = "pageCount", source = "volumeInfo.pageCount")
    Book toEntity(GoogleBook.Item item);

    default String joinAuthors(List<String> authors) {
        if (authors == null || authors.isEmpty()) return null;
        return String.join(", ", authors);
    }
}