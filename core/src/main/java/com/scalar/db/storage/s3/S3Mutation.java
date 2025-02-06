package com.scalar.db.storage.s3;

import com.scalar.db.api.Delete;
import com.scalar.db.api.Mutation;
import com.scalar.db.api.Put;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.Column;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class S3Mutation extends S3Operation {
  S3Mutation(Mutation mutation, TableMetadata metadata) {
    super(mutation, metadata);
  }

  @Nonnull
  public S3Record makeRecord() {
    Mutation mutation = (Mutation) getOperation();

    if (mutation instanceof Delete) {
      return new S3Record();
    }
    Put put = (Put) mutation;

    return new S3Record(
        getConcatenatedKey(),
        toMap(put.getPartitionKey().getColumns()),
        put.getClusteringKey().map(k -> toMap(k.getColumns())).orElse(Collections.emptyMap()),
        toMapForPut(put));
  }

  private Map<String, Object> toMap(Collection<Column<?>> columns) {
    MapVisitor visitor = new MapVisitor();
    columns.forEach(c -> c.accept(visitor));
    return visitor.get();
  }

  private Map<String, Object> toMapForPut(Put put) {
    MapVisitor visitor = new MapVisitor();
    put.getColumns().values().forEach(c -> c.accept(visitor));
    return visitor.get();
  }
}
