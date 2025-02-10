package com.scalar.db.storage.s3;

import com.google.common.collect.ImmutableMap;
import com.scalar.db.config.DatabaseConfig;
import jdk.nashorn.internal.ir.annotations.Immutable;

import java.util.Map;
import java.util.Properties;

public class S3Env {
  private static final String PROP_S3_ENDPOINT_OVERRIDE = "scalardb.s3.endpoint_override";
  private static final String PROP_S3_REGION = "scalardb.s3.region";
  private static final String PROP_S3_ACCESS_KEY_ID = "scalardb.s3.access_key_id";
  private static final String PROP_S3_SECRET_ACCESS_KEY = "scalardb.s3.secret_access_key";
  private static final String PROP_S3_BUCKET = "scalardb.s3.bucket";

  private static final String DEFAULT_S3_ENDPOINT_OVERRIDE = "http://localhost:4566";
  private static final String DEFAULT_S3_REGION = "us-west-2";
  private static final String DEFAULT_S3_ACCESS_KEY_ID = "fakeMyKeyId";
  private static final String DEFAULT_S3_SECRET_ACCESS_KEY = "fakeSecretAccessKey";
    private static final String DEFAULT_S3_BUCKET = "fake-bucket";

  private S3Env() {}

  public static Properties getProperties(String testName) {
    String endpointOverride =
        System.getProperty(PROP_S3_ENDPOINT_OVERRIDE, DEFAULT_S3_ENDPOINT_OVERRIDE);
    String region = System.getProperty(PROP_S3_REGION, DEFAULT_S3_REGION);
    String accessKeyId = System.getProperty(PROP_S3_ACCESS_KEY_ID, DEFAULT_S3_ACCESS_KEY_ID);
    String secretAccessKey =
        System.getProperty(PROP_S3_SECRET_ACCESS_KEY, DEFAULT_S3_SECRET_ACCESS_KEY);
    String bucket = System.getProperty(PROP_S3_BUCKET, DEFAULT_S3_BUCKET);

    Properties properties = new Properties();
    if (endpointOverride != null) {
      properties.setProperty(S3Config.ENDPOINT_OVERRIDE, endpointOverride);
    }
    properties.setProperty(DatabaseConfig.CONTACT_POINTS, region);
    properties.setProperty(DatabaseConfig.USERNAME, accessKeyId);
    properties.setProperty(DatabaseConfig.PASSWORD, secretAccessKey);
    properties.setProperty(DatabaseConfig.STORAGE, "s3");
    properties.setProperty(DatabaseConfig.CROSS_PARTITION_SCAN, "true");
    properties.setProperty(DatabaseConfig.CROSS_PARTITION_SCAN_FILTERING, "true");
    properties.setProperty(DatabaseConfig.CROSS_PARTITION_SCAN_ORDERING, "false");
    properties.setProperty(S3Config.BUCKET, bucket);

    // Add testName as a metadata namespace suffix
    properties.setProperty(
        DatabaseConfig.SYSTEM_NAMESPACE_NAME,
        DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME + "_" + testName);

    return properties;
  }
}
