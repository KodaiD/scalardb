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
public class DeleteStatementHandler extends StatementHandler {
  public DeleteStatementHandler(S3ClientWrapper wrapper, TableMetadataManager metadataManager) {
    super(wrapper, metadataManager);
  }

  public void handle(Delete delete) throws ExecutionException {
    TableMetadata metadata = metadataManager.getTableMetadata(delete);
    S3Mutation mutation = new S3Mutation(delete, metadata);

    if (!delete.getCondition().isPresent()) {
      deleteRecordIfExists(
          getNamespace(delete),
          getTable(delete),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey());
    } else if (delete.getCondition().get() instanceof DeleteIfExists) {
      throw new ExecutionException(
          CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage(
              "DeleteIfExists condition is not supported in S3"));
    } else {
      assert delete.getCondition().get() instanceof DeleteIf;
      conditionalDeleteRecord(
          getNamespace(delete),
          getTable(delete),
          mutation.getConcatenatedPartitionKey(),
          mutation.getConcatenatedKey(),
          delete.getCondition().get().getExpressions());
    }
  }

  private void deleteRecordIfExists(
      String namespace, String table, String partition, String concatenatedKey)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      wrapper.deleteIfExists(objectKey);
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage());
    }
  }

  private void conditionalDeleteRecord(
      String namespace,
      String table,
      String partition,
      String concatenatedKey,
      List<ConditionalExpression> expressions)
      throws ExecutionException {
    String objectKey = S3Utils.getObjectKey(namespace, table, partition, concatenatedKey);
    try {
      S3ClientWrapperResponse response = wrapper.get(objectKey);
      S3Record currentRecord = JsonConvertor.deserialize(response.getValue(), S3Record.class);

      if (!areConditionsMet(currentRecord, expressions)) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage());
      }

      if (!wrapper.compareAndDelete(objectKey, response.getETag())) {
        throw new RetriableExecutionException(
            CoreError.S3_TRANSACTION_CONFLICT_OCCURRED_IN_MUTATION.buildMessage());
      }
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        throw new NoMutationException(CoreError.NO_MUTATION_APPLIED.buildMessage());
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage());
      }
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_MUTATION.buildMessage());
    }
  }
}
