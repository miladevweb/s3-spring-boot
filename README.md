<br>

# <div align="center">**AWS S3 + Spring Boot 🧠**</div>

This is a simple Spring Boot application that demonstrates how to use AWS S3 to store and retrieve files.

### **_📌 Dependencies:_**

- AWS SDK for Java

```xml
<dependency>
  	<groupId>com.amazonaws</groupId>
  	<artifactId>aws-java-sdk</artifactId>
  	<version>1.12.780</version>
</dependency>
```

### **_📌 Create and S3 Bucket and Give Permissions to the IAM User:_**

1. Go to the AWS Management Console and navigate to the S3 service.
2. Click on the "Create Bucket" button.
3. Enter a unique name for the bucket and select the region where you want to create the bucket.
4. Click on the "Create Bucket" button.
5. Once the bucket is created, click on the "Permissions" tab.
6. Click on the "Add bucket policy" button.
7. In the "Bucket Policy" section, enter the following policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowPublicRead",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/FOLDER_NAME/*",
      // Optional
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": "YOUR_IP_ADDRESS"
        }
      }
    }
  ]
}
```

8. If you want to block public access to the bucket, keep the checkbox checked.
9. Create a new IAM user and give it the necessary permissions to access the bucket **_\<S3 Full Access\>_**.
10. Copy the access key and secret key and add them to the **_application.properties_** file.
11. Add the region to the **_application.properties_** file.
12. Add the bucket name to the **_application.properties_** file.

### **_📌 Configuration_**

Add your environment variables to the **_application.properties_** file.

```properties
cloud.aws.credentials.accessKey=AWS_ACCESS_KEY
cloud.aws.credentials.secretKey=AWS_SECRET_KEY
cloud.aws.region.static=AWS_REGION
cloud.aws.s3.bucket=AWS_S3_BUCKET_NAME

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

Create a bean for the AmazonS3 client and inject it into our [**_service_**](#-service).

```java
@Configuration
public class AwsConfig {
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AmazonS3 s3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }
}
```

### **_📌 Service_**

Create a service that uses the AmazonS3 client to store and retrieve files.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsServiceImplementation implements AwsService {
    private final AmazonS3 s3Client;

    // UPLOAD a file to the S3 bucket
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

    // DOWNLOAD a file from the S3 bucket
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

    // LIST all files in the S3 bucket
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

    // DELETE a file from the S3 bucket
    @Override
    public void deleteFile(String bucketName, String keyName) throws AmazonClientException {
        s3Client.deleteObject(bucketName, keyName);
        log.info("File deleted successfully from S3", bucketName, keyName);
    }

}
```

### **_📌 Controller_**

Create a controller that handles the requests to the [**_service_**](#-service).

```java
@Controller
@RequestMapping("/aws")
@RequiredArgsConstructor
public class AwsController {
    private final AwsService awsService;

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
        var body = awsService.downloadFile(bucketName, fileName);

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
```

With the above [**_controller_**](#-controller), you can upload, download, list and delete fiels from the S3 bucket.
