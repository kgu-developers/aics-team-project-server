package kgu.developers.domain.fileobject.infrastructure;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileUploadFailedException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file) {
        // 원본 파일명이 같아도 서로 덮어쓰지 않도록 저장 키는 UUID로 새로 만든다.
        String storageKey = "submissions/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | S3Exception e) {
            throw new FileUploadFailedException(e);
        }
        return storageKey;
    }

    @Override
    public String presignedUrl(String storageKey) {
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(DOWNLOAD_URL_TTL)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(storageKey)
                                .build())
                        .build());
        return presigned.url().toString();
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
