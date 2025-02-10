package com.scalar.db.storage.s3;

import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.transaction.consensuscommit.ConsensusCommitIntegrationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void get_GetGivenForIndexColumn_ShouldReturnRecords() throws TransactionException {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void scan_ScanGivenForIndexColumn_ShouldReturnRecords() throws TransactionException {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void scan_ScanGivenForIndexColumnWithConjunctions_ShouldReturnRecords()
      throws TransactionException {}
}
