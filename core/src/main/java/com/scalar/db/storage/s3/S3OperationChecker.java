package com.scalar.db.storage.s3;

import com.scalar.db.api.*;
import com.scalar.db.common.TableMetadataManager;
import com.scalar.db.common.checker.OperationChecker;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;

public class S3OperationChecker extends OperationChecker {
  private static final char[] ILLEGAL_CHARACTERS_IN_PRIMARY_KEY = {
    // Not allowed due to the concatenated key limitation
    '*',

    // Provided by S3
    '/',
    // TODO: Add more illegal characters
  };

  public S3OperationChecker(DatabaseConfig databaseConfig, TableMetadataManager metadataManager) {
    super(databaseConfig, metadataManager);
  }

  @Override
  public void check(Put put) throws ExecutionException {
    super.check(put);
    TableMetadata metadata = getTableMetadata(put);
    checkCondition(put, metadata);
  }

  @Override
  public void check(Delete delete) throws ExecutionException {
    super.check(delete);
    TableMetadata metadata = getTableMetadata(delete);
    checkCondition(delete, metadata);
  }

  private void checkCondition(Mutation mutation, TableMetadata metadata) {
    if (!mutation.getCondition().isPresent()) {}
    // TODO: Implement the condition check
  }
}
