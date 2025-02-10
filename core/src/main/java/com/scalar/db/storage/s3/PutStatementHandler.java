package com.scalar.db.storage.s3;

import com.scalar.db.api.*;
import com.scalar.db.common.TableMetadataManager;
import com.scalar.db.common.error.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.storage.NoMutationException;
import com.scalar.db.exception.storage.RetriableExecutionException;
import java.util.List;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class PutStatementHandler extends StatementHandler {
  public PutStatementHandler(S3ClientWrapper wrapper, TableMetadataManager metadataManager) {
    super(wrapper, metadataManager);
  }

  public void handle(Put put) throws ExecutionException {
    TableMetadata tableMetadata = metadataManager.getTableMetadata(put);
    S3Mutation mutation = new S3Mutation(put, tableMetadata);

    if (!put.getCondition().isPresent()) {
      upsertRecord(
          getNamespace(put),
          getTable(put),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey(),
          mutation);
    } else if (put.getCondition().get() instanceof PutIfNotExists) {
      insertRecord(
          getNamespace(put),
          getTable(put),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey(),
          mutation);
    } else if (put.getCondition().get() instanceof PutIfExists) {
      updateRecord(
          getNamespace(put),
          getTable(put),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey(),
          mutation);
    } else {
      assert put.getCondition().get() instanceof PutIf;
      conditionalUpdateRecord(
          getNamespace(put),
          getTable(put),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey(),
          mutation,
          put.getCondition().get().getExpressions());
    }
  }

  private void upsertRecord(
      String namespace, String table, String partition, String concatenatedKey, S3Mutation mutation)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      S3ClientWrapperResponse response = wrapper.get(objectKey);
      S3Record currentRecord = JsonConvertor.deserialize(response.getValue(), S3Record.class);
      S3Record record = mutation.makeRecord(currentRecord);
      if (!wrapper.compareAndSwap(objectKey, JsonConvertor.serialize(record), response.getETag())) {
        throw new RetriableExecutionException(
            CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage());
      }
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        try {
          S3Record record = mutation.makeRecord();
          wrapper.insert(objectKey, JsonConvertor.serialize(record));
        } catch (S3ClientWrapperException e2) {
          if (e2.getCode() == S3ClientWrapperException.StatusCode.ALREADY_EXISTS
              || e2.getCode() == S3ClientWrapperException.StatusCode.CONFLICT) {
            throw new RetriableExecutionException(
                CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage(), e2);
          } else {
            throw new ExecutionException(
                CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e2);
          }
        } catch (Exception e2) {
          throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e2);
        }
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
      }
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
    }
  }

  private void insertRecord(
      String namespace, String table, String partition, String concatenatedKey, S3Mutation mutation)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      S3Record record = mutation.makeRecord();
      wrapper.insert(objectKey, JsonConvertor.serialize(record));
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.CONFLICT) {
        throw new RetriableExecutionException(
            CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage(), e);
      } else if (e.getCode() == S3ClientWrapperException.StatusCode.ALREADY_EXISTS) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage(), e);
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
      }
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
    }
  }

  private void updateRecord(
      String namespace, String table, String partition, String concatenatedKey, S3Mutation mutation)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      S3ClientWrapperResponse response = wrapper.get(objectKey);
      S3Record currentRecord = JsonConvertor.deserialize(response.getValue(), S3Record.class);
      S3Record record = mutation.makeRecord(currentRecord);
      if (!wrapper.compareAndSwap(objectKey, JsonConvertor.serialize(record), response.getETag())) {
        throw new RetriableExecutionException(
            CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage());
      }
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage(), e);
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
      }
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
    }
  }

  private void conditionalUpdateRecord(
      String namespace,
      String table,
      String partition,
      String concatenatedKey,
      S3Mutation mutation,
      List<ConditionalExpression> expressions)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      S3ClientWrapperResponse response = wrapper.get(objectKey);
      S3Record currentRecord = JsonConvertor.deserialize(response.getValue(), S3Record.class);

      if (!areConditionsMet(currentRecord, expressions)) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage());
      }
      S3Record record = mutation.makeRecord(currentRecord);
      if (!wrapper.compareAndSwap(objectKey, JsonConvertor.serialize(record), response.getETag())) {
        throw new RetriableExecutionException(
            CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage());
      }
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage(), e);
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
      }
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(), e);
    }
  }
}
