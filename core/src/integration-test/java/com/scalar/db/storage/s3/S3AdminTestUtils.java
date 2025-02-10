package com.scalar.db.storage.s3;

import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.util.AdminTestUtils;
import java.net.URI;
import java.util.Properties;
import java.util.stream.Collectors;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3AdminTestUtils extends AdminTestUtils {
  private final S3Client client;
  private final String metadataNamespace;
  private final String bucket;

  public S3AdminTestUtils(Properties properties) {
    super(properties);
    S3Config config = new S3Config(new DatabaseConfig(properties));
    StaticCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey()));

    S3ClientBuilder builder = S3Client.builder();
    config.getEndpointOverride().ifPresent(e -> builder.endpointOverride(URI.create(e)));
    client =
        builder
            .credentialsProvider(credentialsProvider)
            .region(Region.of(config.getRegion()))
            .forcePathStyle(true)
            .build();
    metadataNamespace = config.getMetadataNamespace();
    bucket = config.getBucket();
  }

  @Override
  public void dropNamespacesTable() throws Exception {
    // Do nothing
    // S3 does not have a concept of table
  }

  @Override
  public void dropMetadataTable() throws Exception {
    // Do nothing
    // S3 does not have a concept of table
  }

  @Override
  public void truncateNamespacesTable() throws Exception {
    client
        .listObjectsV2Paginator(
            b ->
                b.bucket(bucket)
                    .prefix(
                        S3Utils.getObjectKey(
                            metadataNamespace, S3Admin.NAMESPACE_TABLE, null, null)))
        .contents()
        .stream()
        .map(S3Object::key)
        .collect(Collectors.toList())
        .forEach(
            key -> {
              client.deleteObject(b -> b.bucket(bucket).key(key));
            });
  }

  @Override
  public void truncateMetadataTable() throws Exception {
    client
        .listObjectsV2Paginator(
            b ->
                b.bucket(bucket)
                    .prefix(
                        S3Utils.getObjectKey(
                            metadataNamespace, S3Admin.METADATA_TABLE, null, null)))
        .contents()
        .stream()
        .map(S3Object::key)
        .collect(Collectors.toList())
        .forEach(
            key -> {
              client.deleteObject(b -> b.bucket(bucket).key(key));
            });
  }

  @Override
  public void corruptMetadata(String namespace, String table) throws Exception {
    client.putObject(
        b ->
            b.bucket(bucket)
                .key(
                    S3Utils.getObjectKey(
                        metadataNamespace, S3Admin.METADATA_TABLE, namespace, table))
                .build(),
        RequestBody.fromString("corrupted"));
  }

  @Override
  public void dropNamespace(String namespace) throws Exception {
    // Do nothing
    // S3 does not have a concept of namespace
  }

  @Override
  public boolean namespaceExists(String namespace) throws Exception {
    // S3 does not have a concept of namespace
    return true;
  }

  @Override
  public boolean tableExists(String namespace, String table) throws Exception {
    // S3 does not have a concept of table
    return true;
  }

  @Override
  public void dropTable(String namespace, String table) throws Exception {
    // Do nothing
    // S3 does not have a concept of table
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
