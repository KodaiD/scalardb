package com.scalar.db.storage.s3;

import com.scalar.db.api.DistributedStorageColumnValueIntegrationTestBase;
import java.util.Properties;

public class S3ColumnValueIntegrationTest extends DistributedStorageColumnValueIntegrationTestBase {
  @Override
  protected Properties getProperties(String testName) {
    return S3Env.getProperties(testName);
  }
}
