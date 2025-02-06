package com.scalar.db.storage.s3;

import com.scalar.db.api.ConditionalExpression;
import com.scalar.db.api.Operation;
import com.scalar.db.common.TableMetadataManager;
import com.scalar.db.io.Column;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public abstract class StatementHandler {
  protected final S3ClientWrapper wrapper;
  protected final TableMetadataManager metadataManager;

  protected StatementHandler(S3ClientWrapper wrapper, TableMetadataManager metadataManager) {
    this.wrapper = wrapper;
    this.metadataManager = metadataManager;
  }

  @Nonnull
  protected String getNamespace(Operation operation) {
    assert operation.forNamespace().isPresent();
    return operation.forNamespace().get();
  }

  @Nonnull
  protected String getTable(Operation operation) {
    assert operation.forTable().isPresent();
    return operation.forTable().get();
  }

  protected boolean areConditionsMet(S3Record record, List<ConditionalExpression> expressions) {
    for (ConditionalExpression expression : expressions) {
      Column<?> column = expression.getColumn();
      switch (expression.getOperator()) {
        case EQ:
          if (column.getValue().isPresent()) {
            return record.getValues().get(column.getName()).equals(column.getValue().get());
          } else {
            return false;
          }
        case NE:
          if (column.getValue().isPresent()) {
            return !record.getValues().get(column.getName()).equals(column.getValue().get());
          } else {
            return false;
          }
        default:
          throw new AssertionError(); // TODO: Implement other operators
      }
    }
    return true;
  }
}
