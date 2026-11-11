package com.example.ecommerce.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 clients for product images. The same code talks to AWS S3 in production and MinIO locally; only
 * {@code app.storage.*} changes. Clients upload directly to storage with pre-signed URLs (Phase 6), so
 * image bytes never pass through the API.
 */
@Slf4j
@Configuration
public class StorageConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.storage();
        var builder = S3Client.builder()
                .region(Region.of(storage.region()))
                .credentialsProvider(credentials(storage))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(storage.pathStyle()).build());
        if (StringUtils.hasText(storage.endpoint())) {
            builder.endpointOverride(URI.create(storage.endpoint()));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.storage();
        var builder = S3Presigner.builder()
                .region(Region.of(storage.region()))
                .credentialsProvider(credentials(storage))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(storage.pathStyle()).build());
        if (StringUtils.hasText(storage.endpoint())) {
            builder.endpointOverride(URI.create(storage.endpoint()));
        }
        return builder.build();
    }

    /**
     * Local convenience: makes sure the bucket exists. Never fatal, so the API still starts (and reports
     * the problem) when storage is down; image uploads are the only feature that needs it.
     */
    @Bean
    public ApplicationRunner storageBucketInitializer(AppProperties appProperties, S3Client s3Client) {
        return args -> {
            AppProperties.Storage storage = appProperties.storage();
            if (!storage.createBucket()) {
                return;
            }
            try {
                s3Client.headBucket(b -> b.bucket(storage.bucket()));
                log.info("Storage bucket '{}' is available", storage.bucket());
            } catch (NoSuchBucketException missing) {
                s3Client.createBucket(b -> b.bucket(storage.bucket()));
                log.info("Created storage bucket '{}'", storage.bucket());
            } catch (SdkException ex) {
                log.warn("Object storage is not reachable ({}); image uploads will fail until it is", ex.getMessage());
            }
        };
    }

    /** Explicit keys when configured (local S3 server); otherwise the standard AWS chain (env vars, IAM role...). */
    private static AwsCredentialsProvider credentials(AppProperties.Storage storage) {
        if (StringUtils.hasText(storage.accessKey()) && StringUtils.hasText(storage.secretKey())) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(storage.accessKey(), storage.secretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
