package com.scalar.db.storage.s3;

import com.scalar.db.common.ConsensusCommitTestUtils;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitIntegrationTestUtils;
import java.util.Properties;

public class ConsensusCommitS3Env {
  private ConsensusCommitS3Env() {}

  public static Properties getProperties(String testName) {
    Properties properties = S3Env.getProperties(testName);

    // Add testName as a coordinator schema suffix
    ConsensusCommitIntegrationTestUtils.addSuffixToCoordinatorNamespace(properties, testName);

    return ConsensusCommitTestUtils.loadConsensusCommitProperties(properties);
  }
}
