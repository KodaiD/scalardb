package com.scalar.db.storage.s3;

import com.scalar.db.api.DistributedStorageAdminRepairIntegrationTestBase;
import java.util.Properties;

public class S3AdminRepairIntegrationTest extends DistributedStorageAdminRepairIntegrationTestBase {
  @Override
  protected Properties getProperties(String testName) {
    return S3Env.getProperties(testName);
  }

  @Override
  protected void initialize(String testName) throws Exception {
    super.initialize(testName);
    adminTestUtils = new S3AdminTestUtils(getProperties(testName));
  }
}
