package com.aws.cloud.service;

import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsServiceImplementation implements AwsService {
    private final AmazonS3 s3Client;

    @Override
    public void uploadFile(String bucketName, String keyName, Long contentLength, String contentType, InputStream value)
            throws AmazonClientException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);

        keyName = "spring-boot-aws-s3/" + keyName;

        s3Client.putObject(bucketName, keyName, value, metadata);
        log.info("File uploaded successfully to S3");

        // Store the URL of the uploaded file in the database
        log.info(s3Client.getUrl(bucketName, keyName).toString());
    }

    @Override
    public ByteArrayOutputStream downloadFile(String bucketName, String keyName)
            throws IOException, AmazonClientException {
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        InputStream inputStream = s3Object.getObjectContent();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int len;
        byte[] buffer = new byte[4096];

        while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        log.info("File downloaded successfully from S3", bucketName, keyName);
        return outputStream;

    }

    @Override
    public List<String> listFiles(String bucketName) throws AmazonClientException {
        List<String> keys = new ArrayList<>();
        ObjectListing objectListing = s3Client.listObjects(bucketName);

        while (true) {
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            if (objectSummaries.isEmpty()) {
                break;
            }

            objectSummaries.stream()
                    .filter(item -> !item.getKey().endsWith("/"))
                    .map(S3ObjectSummary::getKey)
                    .forEach(keys::add);

            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }

        log.info("Files found in bucket", bucketName, keys);
        return keys;
    }

    @Override
    public void deleteFile(String bucketName, String keyName) throws AmazonClientException {
        s3Client.deleteObject(bucketName, keyName);
        log.info("File deleted successfully from S3", bucketName, keyName);
    }

}
