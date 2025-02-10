package com.scalar.db.storage.s3;

import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitIntegrationTestBase;
import java.util.Properties;

public class ConsensusCommitIntegrationTestWithS3 extends ConsensusCommitIntegrationTestBase {
  @Override
  protected Properties getProps(String testName) {
    return ConsensusCommitS3Env.getProperties(testName);
  }

  @Override
  protected boolean isTimestampTypeSupported() {
    return false;
  }

  @Override
  public void get_GetGivenForIndexColumn_ShouldReturnRecords() throws TransactionException {
    // This test is not supported for S3
  }

  @Override
  public void scan_ScanGivenForIndexColumn_ShouldReturnRecords() throws TransactionException {
    // This test is not supported for S3
  }

  @Override
  public void scan_ScanGivenForIndexColumnWithConjunctions_ShouldReturnRecords()
      throws TransactionException {
    // This test is not supported for S3
  }
}
