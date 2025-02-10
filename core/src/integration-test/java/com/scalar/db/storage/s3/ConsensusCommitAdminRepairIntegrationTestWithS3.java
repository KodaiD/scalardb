package com.scalar.db.storage.s3;

import com.scalar.db.transaction.consensuscommit.ConsensusCommitAdminRepairIntegrationTestBase;
import java.util.Properties;

public class ConsensusCommitAdminRepairIntegrationTestWithS3
    extends ConsensusCommitAdminRepairIntegrationTestBase {

  @Override
  protected Properties getProps(String testName) {
    return S3Env.getProperties(testName);
  }

  @Override
  protected void initialize(String testName) throws Exception {
    super.initialize(testName);
    adminTestUtils = new S3AdminTestUtils(getProperties(testName));
  }
}
