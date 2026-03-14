package com.example.demo.mapper;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleVolume;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GoogleVolumeToBookMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "volumeInfo.title")
    @Mapping(target = "author", expression = "java(firstAuthor(volume.volumeInfo() != null ? volume.volumeInfo().authors() : null))")
    @Mapping(target = "pageCount", source = "volumeInfo.pageCount")
    Book toBook(GoogleVolume volume);

    default String firstAuthor(List<String> authors) {
        return (authors == null || authors.isEmpty()) ? null : authors.get(0);
    }
}