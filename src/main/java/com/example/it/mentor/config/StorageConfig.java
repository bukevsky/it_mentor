package com.example.it.mentor.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner ensureBucket(MinioClient client) {
        return args -> {
            try {
                boolean exists = client.bucketExists(
                        BucketExistsArgs.builder().bucket(props.getBucketName()).build());
                if (!exists) {
                    client.makeBucket(
                            MakeBucketArgs.builder().bucket(props.getBucketName()).build());
                    log.info("MinIO bucket '{}' создан", props.getBucketName());
                }
            } catch (Exception e) {
                log.error("Не удалось создать MinIO bucket '{}': {}", props.getBucketName(), e.getMessage(), e);
            }
        };
    }
}
