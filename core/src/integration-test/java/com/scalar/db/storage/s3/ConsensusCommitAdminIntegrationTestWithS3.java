package com.scalar.db.storage.s3;

import com.scalar.db.transaction.consensuscommit.ConsensusCommitAdminIntegrationTestBase;
import com.scalar.db.util.AdminTestUtils;
import java.util.Properties;

public class ConsensusCommitAdminIntegrationTestWithS3
    extends ConsensusCommitAdminIntegrationTestBase {
  @Override
  protected Properties getProps(String testName) {
    return S3Env.getProperties(testName);
  }

  @Override
  protected AdminTestUtils getAdminTestUtils(String testName) {
    return new S3AdminTestUtils(getProperties(testName));
  }

  @Override
  public void createIndex_ForAllDataTypesWithExistingData_ShouldCreateIndexesCorrectly() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void createIndex_ForNonExistingTable_ShouldThrowIllegalArgumentException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void createIndex_ForNonExistingColumn_ShouldThrowIllegalArgumentException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void createIndex_ForAlreadyExistingIndex_ShouldThrowIllegalArgumentException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void createIndex_IfNotExists_ForAlreadyExistingIndex_ShouldNotThrowAnyException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void dropIndex_ForAllDataTypesWithExistingData_ShouldDropIndexCorrectly() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void dropIndex_ForNonExistingTable_ShouldThrowIllegalArgumentException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void dropIndex_ForNonExistingIndex_ShouldThrowIllegalArgumentException() {
    // Index-related operations are not supported for S3
  }

  @Override
  public void dropIndex_IfExists_ForNonExistingIndex_ShouldNotThrowAnyException() {
    // Index-related operations are not supported for S3
  }
}
