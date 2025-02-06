package com.scalar.db.storage.s3;

import java.net.URI;
import javax.annotation.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

public class S3Utils {
  public static final String OBJECT_KEY_DELIMITER = "/";
  public static final String PARTITION_KEY_DELIMITER = "*";

  public static S3Client buildS3Client(S3Config config) {
    AwsCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey()));
    S3ClientBuilder builder = S3Client.builder();
    config.getEndpointOverride().ifPresent(e -> builder.endpointOverride(URI.create(e)));
    return builder
        .credentialsProvider(credentialsProvider)
        .region(Region.of(config.getRegion()))
        .forcePathStyle(true)
        .build();
  }

  public static String getObjectKey(
      String namespace,
      String table,
      @Nullable String partition,
      @Nullable String concatenatedKey) {
    if (partition == null) {
      if (concatenatedKey == null) {
        return String.join(OBJECT_KEY_DELIMITER, namespace, table, "");
      } else {
        return String.join(OBJECT_KEY_DELIMITER, namespace, table, concatenatedKey);
      }
    } else {
      if (concatenatedKey == null) {
        return String.join(OBJECT_KEY_DELIMITER, namespace, table, partition, "");
      } else {
        return String.join(OBJECT_KEY_DELIMITER, namespace, table, partition, concatenatedKey);
      }
    }
  }
}
