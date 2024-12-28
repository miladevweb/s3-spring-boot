package com.aws.cloud.config;

import java.util.Arrays;

import org.springframework.http.MediaType;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter // getter for extension and mediaType
@AllArgsConstructor(access = lombok.AccessLevel.PACKAGE)
public enum FileType {
    /* values() */
    PNG("png", MediaType.IMAGE_PNG),
    JPG("jpg", MediaType.IMAGE_JPEG),
    JPEG("jpeg", MediaType.IMAGE_JPEG),
    TXT("txt", MediaType.TEXT_PLAIN),
    PDF("pdf", MediaType.APPLICATION_PDF);

    private final String extension;
    private final MediaType mediaType;

    public static MediaType getMediaTypeFromFilename(String filename) {
        var dotIndex = filename.lastIndexOf(".");
        var fileExtension = (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);

        return Arrays.stream(values())
                .filter(e -> e.getExtension().equals(fileExtension))
                .findFirst()
                .map(FileType::getMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}