//package com.wood.app.controller;
//
//import com.amazonaws.auth.AWSCredentialsProvider;
//import com.amazonaws.auth.AWSStaticCredentialsProvider;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.client.builder.AwsClientBuilder;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.*;
//import com.amazonaws.util.IOUtils;
//import com.wood.app.api.ApiResult;
//import org.apache.commons.lang.text.StrBuilder;
//import org.apache.commons.lang.time.DateFormatUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.ByteArrayInputStream;
//import java.util.Date;
//import java.util.List;
//
//import static com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead;
//import static com.wood.app.config.Constant.*;
//
//@RestController
//public class S3Controller {
//    private final Logger logger = LoggerFactory.getLogger(getClass());
//
//    @GetMapping("/s3/test")
//    public ApiResult getToken() throws Exception {
//        // 创建 AmazonS3 实例。
//        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(S3_ENDPOINT, S3_REGION);
//        BasicAWSCredentials credentials = new BasicAWSCredentials(S3_ACCESS_ID, S3_SECRET_KEY);
//        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
//
//        AmazonS3 client = AmazonS3ClientBuilder.standard()
//                .withEndpointConfiguration(endpointConfiguration)
//                .withCredentials(credentialsProvider).build();
//
//        // 列举桶
//        List<Bucket> bucketList = client.listBuckets();
//        StrBuilder builder = new StrBuilder();
//        bucketList.stream().forEach((bucket) -> {
//            builder.append(bucket.getName());
//            builder.append(":");
//            builder.append(bucket.getCreationDate().toString());
//            builder.append("\n");
//        });
//        builder.append("\n");
//
//        // 上传文件
//        // 要上传的文件名。
//        String timestamp = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssssZZ");
//        String objectName = "ios/wood-" + timestamp + "-BingWallpaper.jpg";
//        ClassPathResource resource = new ClassPathResource("static/BingWallpaper.jpg");
//        ByteArrayInputStream inputStream = new ByteArrayInputStream(IOUtils.toByteArray(resource.getInputStream()));
//        PutObjectRequest putObjectRequest = new PutObjectRequest(S3_BUCKET_NAME, objectName, inputStream, null).withCannedAcl(PublicRead);
//        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
//        if (putObjectResult != null) {
//            builder.append("Content Md5: " + putObjectResult.getContentMd5());
//            builder.append("ETag: " + putObjectResult.getETag());
//            builder.append("\n");
//            builder.append("url:https://" + S3_ENDPOINT + "/" + S3_BUCKET_NAME + "/" + objectName);
//            builder.append("\n");
//        }
//        builder.append("\n");
//
//        // 列举文件
//        ObjectListing objectListing = client.listObjects("youwe-oss-bucket");
//        if (objectListing != null && objectListing.getObjectSummaries() != null) {
//            // objectListing.getObjectSummaries 包含列举到的文件的描述信息。
//            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//                builder.append("Name: " + objectSummary.getKey());
//                builder.append(", Size: " + objectSummary.getSize());
//                builder.append(", ETag: " + objectSummary.getETag());
//                builder.append(", Storage Class: " + objectSummary.getStorageClass());
//                builder.append(", Object Type: " + objectSummary.getObjectType());
//                builder.append(", Last Modified: " + objectSummary.getLastModified());
//                builder.append("\n");
//            }
//        }
//        // 关闭 AmazonS3 实例。
//        client.shutdown();
//
//        return ApiResult.ok(builder.toString());
//    }
//}
