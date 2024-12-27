package com.aws.cloud.service;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import com.amazonaws.AmazonClientException;

public class AwsServiceImplementation implements AwsService {

    @Override
    public void uploadFile(String bucketName, String keyName, Long contentLength, String contentType, InputStream value)
            throws AmazonClientException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uploadFile'");
    }

    @Override
    public ByteArrayOutputStream downloadFile(String bucketName, String keyName)
            throws IOException, AmazonClientException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'downloadFile'");
    }

    @Override
    public List<String> listFiles(String bucketName) throws AmazonClientException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listFiles'");
    }

    @Override
    public void deleteFile(String bucketName, String keyName) throws AmazonClientException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }
    
}
