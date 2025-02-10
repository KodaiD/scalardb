package com.scalar.db.storage.s3;

import com.scalar.db.api.DistributedStorageIntegrationTestBase;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class S3IntegrationTest extends DistributedStorageIntegrationTestBase {
  @Override
  protected Properties getProperties(String testName) {
    return S3Env.getProperties(testName);
  }

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void get_GetGivenForIndexedColumn_ShouldGet() {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void get_GetGivenForIndexedColumnWithMatchedConjunctions_ShouldGet() {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void get_GetGivenForIndexedColumnWithUnmatchedConjunctions_ShouldReturnEmpty() {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void
      get_GetGivenForIndexedColumnMatchingMultipleRecords_ShouldThrowIllegalArgumentException() {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void scan_ScanGivenForIndexedColumn_ShouldScan() {}

  @Test
  @Override
  @Disabled("Index-related operations are not supported for S3")
  public void scan_ScanGivenForNonIndexedColumn_ShouldThrowIllegalArgumentException() {}

  @Test
  @Override
  @Disabled("DeleteIfExists is not supported for S3")
  public void delete_DeleteWithIfExistsGivenWhenNoSuchRecord_ShouldThrowNoMutationException() {}

  @Test
  @Override
  @Disabled("DeleteIfExists is not supported for S3")
  public void delete_DeleteWithIfExistsGivenWhenSuchRecordExists_ShouldDeleteProperly() {}

  @Test
  @Override
  @Disabled("DeleteIfExists is not supported for S3")
  public void delete_MultipleDeleteWithDifferentConditionsGiven_ShouldDeleteProperly() {}

  @Test
  @Override
  @Disabled("Delete with DeleteIf when no such record exists does not throw NoMutationException")
  public void delete_DeleteWithIfGivenWhenNoSuchRecord_ShouldThrowNoMutationException() {}
}
