package com.curateme.clacobatchserver.config.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String downloadCsvFile(String folderPath, String fileName) {
        String s3Key = folderPath + "/" + fileName;
        String localFilePath = System.getProperty("java.io.tmpdir") + fileName;

        try {
            log.info("Downloading file from S3: {}/{}", bucketName, s3Key);
            amazonS3Client.getObject(new GetObjectRequest(bucketName, s3Key), new File(localFilePath));
            log.info("File downloaded successfully to: {}", localFilePath);
        } catch (Exception e) {
            log.error("Error downloading file from S3", e);
        }

        return localFilePath;
    }

    public void updateAndUploadCsvFile(String folderPath, String fileName, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            log.info("File updated successfully: {}", filePath);
        } catch (IOException e) {
            log.error("Error updating file", e);
            return;
        }

        // S3에 파일 업로드
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            String s3Key = folderPath + "/" + fileName;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(Files.size(Paths.get(filePath)));

            amazonS3Client.putObject(bucketName, s3Key, inputStream, metadata);
            log.info("File uploaded successfully to S3: {}/{}", bucketName, s3Key);
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
        }
    }
}
