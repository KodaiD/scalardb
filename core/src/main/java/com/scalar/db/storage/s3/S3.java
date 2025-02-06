package com.scalar.db.storage.s3;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.scalar.db.api.*;
import com.scalar.db.common.AbstractDistributedStorage;
import com.scalar.db.common.FilterableScanner;
import com.scalar.db.common.TableMetadataManager;
import com.scalar.db.common.checker.OperationChecker;
import com.scalar.db.common.error.CoreError;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class S3 extends AbstractDistributedStorage {
  private static final Logger logger = LoggerFactory.getLogger(S3.class);

  private final S3ClientWrapper wrapper;
  private final SelectStatementHandler selectStatementHandler;
  private final PutStatementHandler putStatementHandler;
  private final DeleteStatementHandler deleteStatementHandler;
  private final BatchHandler batchHandler;
  private final OperationChecker operationChecker;

  @Inject
  public S3(DatabaseConfig databaseConfig) {
    super(databaseConfig);
    if (databaseConfig.isCrossPartitionScanOrderingEnabled()) {
      throw new IllegalArgumentException(
          CoreError.S3_CROSS_PARTITION_SCAN_WITH_ORDERING_NOT_SUPPORTED.buildMessage());
    }
    S3Config config = new S3Config(databaseConfig);
    S3Client client = S3Utils.buildS3Client(config);
    wrapper = new S3ClientWrapper(config.getBucket(), client);
    TableMetadataManager metadataManager =
        new TableMetadataManager(
            new S3Admin(wrapper, config), databaseConfig.getMetadataCacheExpirationTimeSecs());
    operationChecker = new S3OperationChecker(databaseConfig, metadataManager);
    selectStatementHandler = new SelectStatementHandler(wrapper, metadataManager);
    putStatementHandler = new PutStatementHandler(wrapper, metadataManager);
    deleteStatementHandler = new DeleteStatementHandler(wrapper, metadataManager);
    batchHandler = new BatchHandler(wrapper, metadataManager);
    logger.info("S3 object is created properly");
  }

  public S3(S3Client client, DatabaseConfig databaseConfig) {
    super(databaseConfig);
    if (databaseConfig.isCrossPartitionScanOrderingEnabled()) {
      throw new IllegalArgumentException(
          CoreError.S3_CROSS_PARTITION_SCAN_WITH_ORDERING_NOT_SUPPORTED.buildMessage());
    }
    S3Config config = new S3Config(databaseConfig);
    this.wrapper = new S3ClientWrapper(config.getBucket(), client);
    TableMetadataManager metadataManager =
        new TableMetadataManager(
            new S3Admin(wrapper, config), databaseConfig.getMetadataCacheExpirationTimeSecs());
    operationChecker = new S3OperationChecker(databaseConfig, metadataManager);
    selectStatementHandler = new SelectStatementHandler(wrapper, metadataManager);
    putStatementHandler = new PutStatementHandler(wrapper, metadataManager);
    deleteStatementHandler = new DeleteStatementHandler(wrapper, metadataManager);
    batchHandler = new BatchHandler(wrapper, metadataManager);
    logger.info("S3 object is created properly with the client and the config");
  }

  @VisibleForTesting
  public S3(
      DatabaseConfig databaseConfig,
      S3ClientWrapper wrapper,
      SelectStatementHandler select,
      PutStatementHandler put,
      DeleteStatementHandler delete,
      BatchHandler batch,
      OperationChecker operationChecker) {
    super(databaseConfig);
    this.wrapper = wrapper;
    this.selectStatementHandler = select;
    this.putStatementHandler = put;
    this.deleteStatementHandler = delete;
    this.batchHandler = batch;
    this.operationChecker = operationChecker;
  }

  @Override
  public Optional<Result> get(Get get) throws ExecutionException {
    get = copyAndSetTargetToIfNot(get);
    operationChecker.check(get);
    Scanner scanner = null;
    try {
      if (get.getConjunctions().isEmpty()) {
        scanner = selectStatementHandler.handle(get);
      } else {
        scanner =
            new FilterableScanner(
                get, selectStatementHandler.handle(copyAndPrepareForDynamicFiltering(get)));
      }
      Optional<Result> ret = scanner.one();
      if (!scanner.one().isPresent()) {
        return ret;
      } else {
        throw new IllegalArgumentException(
            CoreError.GET_OPERATION_USED_FOR_NON_EXACT_MATCH_SELECTION.buildMessage(get));
      }
    } finally {
      if (scanner != null) {
        try {
          scanner.close();
        } catch (IOException e) {
          logger.warn("Failed to close the scanner", e);
        }
      }
    }
  }

  @Override
  public Scanner scan(Scan scan) throws ExecutionException {
    scan = copyAndSetTargetToIfNot(scan);
    operationChecker.check(scan);
    if (scan.getConjunctions().isEmpty()) {
      return selectStatementHandler.handle(scan);
    } else {
      return new FilterableScanner(
          scan, selectStatementHandler.handle(copyAndPrepareForDynamicFiltering(scan)));
    }
  }

  @Override
  public void put(Put put) throws ExecutionException {
    put = copyAndSetTargetToIfNot(put);
    operationChecker.check(put);
    putStatementHandler.handle(put);
  }

  @Override
  public void put(List<Put> puts) throws ExecutionException {
    mutate(puts);
  }

  @Override
  public void delete(Delete delete) throws ExecutionException {
    delete = copyAndSetTargetToIfNot(delete);
    operationChecker.check(delete);
    deleteStatementHandler.handle(delete);
  }

  @Override
  public void delete(List<Delete> deletes) throws ExecutionException {
    mutate(deletes);
  }

  @Override
  public void mutate(List<? extends Mutation> mutations) throws ExecutionException {
    if (mutations.size() == 1) {
      Mutation mutation = mutations.get(0);
      if (mutation instanceof Put) {
        put((Put) mutation);
        return;
      } else if (mutation instanceof Delete) {
        delete((Delete) mutation);
        return;
      }
    }
    mutations = copyAndSetTargetToIfNot(mutations);
    operationChecker.check(mutations);
    batchHandler.handle(mutations);
  }

  @Override
  public void close() {
    wrapper.close();
  }
}
