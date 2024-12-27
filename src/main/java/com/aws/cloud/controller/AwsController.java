package com.aws.cloud.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.aws.cloud.config.FileType;
import com.aws.cloud.service.AwsService;

import lombok.val;
import lombok.SneakyThrows;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/aws")
@RequiredArgsConstructor
public class AwsController {
    private AwsService awsService;

    @GetMapping("/{bucketName}")
    public ResponseEntity<?> listFiles(@PathVariable("bucketName") String bucketName) {
        var body = awsService.listFiles(bucketName);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{bucketName}/upload")
    @SneakyThrows(IOException.class)
    public ResponseEntity<?> uploadFile(
            @PathVariable("bucketName") String bucketName,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String contentType = file.getContentType();
        long fileSize = file.getSize();
        InputStream inputStream = file.getInputStream();

        awsService.uploadFile(bucketName, fileName, fileSize, contentType, inputStream);

        return ResponseEntity.ok().body("File uploaded successfully");
    }

    @SneakyThrows
    @GetMapping("/{bucketName}/download/{fileName}")
    public ResponseEntity<?> downloadFile(
            @PathVariable("bucketName") String bucketName,
            @PathVariable("fileName") String fileName) {
        val body = awsService.downloadFile(bucketName, fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(FileType.getMediaTypeFromFilename(fileName))
                .body(body.toByteArray());
    }

    @DeleteMapping("/{bucketName}/{fileName}")
    public ResponseEntity<?> deleteFile(
            @PathVariable("bucketName") String bucketName,
            @PathVariable("fileName") String fileName) {
        awsService.deleteFile(bucketName, fileName);
        return ResponseEntity.ok().build();
    }
}
