package com.example.demo.google;

import java.util.List;

public record GoogleVolume(
        String id,
        VolumeInfo volumeInfo
) {
    public record VolumeInfo(
            String title,
            List<String> authors,
            Integer pageCount
    ) {}
}