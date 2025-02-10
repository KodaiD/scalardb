package com.scalar.db.storage.s3;

import static com.scalar.db.config.ConfigUtils.getString;

import com.scalar.db.common.error.CoreError;
import com.scalar.db.config.DatabaseConfig;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Immutable
public class S3Config {
  public static final String STORAGE_NAME = "s3";
  public static final String PREFIX = DatabaseConfig.PREFIX + STORAGE_NAME + ".";
  public static final String ENDPOINT_OVERRIDE = PREFIX + "endpoint_override";
  public static final String BUCKET = PREFIX + "bucket";

  /**
   * @deprecated As of 5.0, will be removed.
   */
  @Deprecated
  public static final String TABLE_METADATA_NAMESPACE = PREFIX + "table_metadata.namespace";

  private static final Logger logger = LoggerFactory.getLogger(S3Config.class);
  private final String region;
  private final String accessKeyId;
  private final String secretAccessKey;
  @Nullable private final String endpointOverride;
  private final String bucket;
  private final String metadataNamespace;

  public S3Config(DatabaseConfig databaseConfig) {
    String storage = databaseConfig.getStorage();
    if (!storage.equals(STORAGE_NAME)) {
      throw new IllegalArgumentException(
          DatabaseConfig.STORAGE + " should be '" + STORAGE_NAME + "'");
    }

    if (databaseConfig.getContactPoints().isEmpty()) {
      throw new IllegalArgumentException(CoreError.INVALID_CONTACT_POINTS.buildMessage());
    }
    region = databaseConfig.getContactPoints().get(0);
    accessKeyId = databaseConfig.getUsername().orElse(null);
    secretAccessKey = databaseConfig.getPassword().orElse(null);
    if (databaseConfig.getProperties().containsKey("scalar.db.s3.endpoint-override")) {
      logger.warn(
          "The property \"scalar.db.s3.endpoint-override\" is deprecated and will be removed in 5.0.0. "
              + "Please use \""
              + ENDPOINT_OVERRIDE
              + "\" instead");
    }
    endpointOverride =
        getString(
            databaseConfig.getProperties(),
            ENDPOINT_OVERRIDE,
            getString(
                databaseConfig.getProperties(),
                "scalar.db.s3.endpoint-override", // for backward compatibility
                null));
    if (!databaseConfig.getProperties().containsKey(BUCKET)) {
      throw new IllegalArgumentException("Bucket name is not specified.");
    }
    bucket = getString(databaseConfig.getProperties(), BUCKET, null);

    if (databaseConfig.getProperties().containsKey(TABLE_METADATA_NAMESPACE)) {
      logger.warn(
          "The configuration property \""
              + TABLE_METADATA_NAMESPACE
              + "\" is deprecated and will be removed in 5.0.0.");

      metadataNamespace =
          getString(
              databaseConfig.getProperties(),
              TABLE_METADATA_NAMESPACE,
              DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME);
    } else {
      metadataNamespace = databaseConfig.getSystemNamespaceName();
    }
  }

  public String getRegion() {
    return region;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public Optional<String> getEndpointOverride() {
    return Optional.ofNullable(endpointOverride);
  }

  public String getBucket() {
    return bucket;
  }

  public String getMetadataNamespace() {
    return metadataNamespace;
  }
}
