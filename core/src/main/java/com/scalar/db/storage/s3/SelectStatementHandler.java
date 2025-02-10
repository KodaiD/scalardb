package com.scalar.db.storage.s3;

import com.scalar.db.api.*;
import com.scalar.db.api.Scanner;
import com.scalar.db.common.EmptyScanner;
import com.scalar.db.common.TableMetadataManager;
import com.scalar.db.common.error.CoreError;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.util.ScalarDbUtils;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SelectStatementHandler extends StatementHandler {
  public SelectStatementHandler(S3ClientWrapper wrapper, TableMetadataManager metadataManager) {
    super(wrapper, metadataManager);
  }

  @Nonnull
  public Scanner handle(Selection selection) throws ExecutionException {
    TableMetadata tableMetadata = metadataManager.getTableMetadata(selection);
    if (selection instanceof Get) {
      if (ScalarDbUtils.isSecondaryIndexSpecified(selection, tableMetadata)) {
        throw new ExecutionException(
            CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(
                "Get with secondary index is not supported"));
      } else {
        return executeGet((Get) selection, tableMetadata);
      }
    } else {
      if (selection instanceof ScanAll) {
        return executeScanAll((ScanAll) selection, tableMetadata);
      } else if (ScalarDbUtils.isSecondaryIndexSpecified(selection, tableMetadata)) {
        throw new ExecutionException(
            CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(
                "Scan with secondary index is not supported"));
      } else {
        return executeScan((Scan) selection, tableMetadata);
      }
    }
  }

  private Scanner executeGet(Get get, TableMetadata metadata) throws ExecutionException {
    S3Operation operation = new S3Operation(get, metadata);
    operation.checkArgument(Get.class);
    Optional<S3Record> record =
        getRecord(
            getNamespace(get),
            getTable(get),
            operation.getConcatenatedPartitionKey(),
            operation.getConcatenatedKey());
    if (!record.isPresent()) {
      return new EmptyScanner();
    }
    return new PointQueryScanner(
        record.get(), new ResultInterpreter(get.getProjections(), metadata));
  }

  private Scanner executeScan(Scan scan, TableMetadata metadata) throws ExecutionException {
    S3Operation operation = new S3Operation(scan, metadata);
    operation.checkArgument(Scan.class);
    List<S3Record> records =
        new ArrayList<>(
            getRecordsInPartition(
                getNamespace(scan), getTable(scan), operation.getConcatenatedPartitionKey()));

    Map<String, Scan.Ordering.Order> orders = new HashMap<>(metadata.getClusteringOrders());

    if (scan.getStartClusteringKey().isPresent()) {
      Map<String, Object> startClusteringKey =
          scan.getStartClusteringKey()
              .map(
                  k -> {
                    MapVisitor visitor = new MapVisitor();
                    k.getColumns().forEach(c -> c.accept(visitor));
                    return visitor.get();
                  })
              .orElse(Collections.emptyMap());
      records =
          records.stream()
              .filter(
                  r -> {
                    if (scan.getStartInclusive()) {
                      return new ClusteringKeyComparator(orders)
                              .compare(r.getClusteringKey(), startClusteringKey)
                          >= 0;
                    } else {
                      return new ClusteringKeyComparator(orders)
                              .compare(r.getClusteringKey(), startClusteringKey)
                          > 0;
                    }
                  })
              .collect(Collectors.toList());
    }

    if (scan.getEndClusteringKey().isPresent()) {
      Map<String, Object> endClusteringKey =
          scan.getEndClusteringKey()
              .map(
                  k -> {
                    MapVisitor visitor = new MapVisitor();
                    k.getColumns().forEach(c -> c.accept(visitor));
                    return visitor.get();
                  })
              .orElse(Collections.emptyMap());
      records =
          records.stream()
              .filter(
                  r -> {
                    if (scan.getEndInclusive()) {
                      return new ClusteringKeyComparator(orders)
                              .compare(r.getClusteringKey(), endClusteringKey)
                          <= 0;
                    } else {
                      return new ClusteringKeyComparator(orders)
                              .compare(r.getClusteringKey(), endClusteringKey)
                          < 0;
                    }
                  })
              .collect(Collectors.toList());
    }

    scan.getOrderings()
        .forEach(ordering -> orders.put(ordering.getColumnName(), ordering.getOrder()));
    records.sort(
        (r1, r2) ->
            new ClusteringKeyComparator(orders)
                .compare(r1.getClusteringKey(), r2.getClusteringKey()));

    if (scan.getLimit() > 0) {
      records = records.subList(0, Math.min(scan.getLimit(), records.size()));
    }

    return new RangeQueryScanner(
        records.iterator(),
        new ResultInterpreter(scan.getProjections(), metadata),
        scan.getLimit());
  }

  private Scanner executeScanAll(ScanAll scan, TableMetadata metadata) throws ExecutionException {
    S3Operation operation = new S3Operation(scan, metadata);
    operation.checkArgument(ScanAll.class);
    Set<S3Record> records = getRecordsInTable(getNamespace(scan), getTable(scan));
    if (scan.getLimit() > 0) {
      records = records.stream().limit(scan.getLimit()).collect(Collectors.toSet());
    }
    return new RangeQueryScanner(
        records.iterator(),
        new ResultInterpreter(scan.getProjections(), metadata),
        scan.getLimit());
  }

  private Optional<S3Record> getRecord(
      String namespace, String table, String partition, String concatenatedKey)
      throws ExecutionException {
    try {
      S3ClientWrapperResponse response =
          wrapper.get(S3Utils.getObjectKey(namespace, table, partition, concatenatedKey));
      return Optional.of(JsonConvertor.deserialize(response.getValue(), S3Record.class));
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        return Optional.empty();
      } else {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
      }
    } catch (Exception e) {
      throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
    }
  }

  private Set<S3Record> getRecordsInPartition(String namespace, String table, String partition)
      throws ExecutionException {
    Set<String> keys = wrapper.listKeys(S3Utils.getObjectKey(namespace, table, partition, null));
    Set<S3Record> records = new HashSet<>();
    for (String key : keys) {
      try {
        S3ClientWrapperResponse response = wrapper.get(key);
        records.add(JsonConvertor.deserialize(response.getValue(), S3Record.class));
      } catch (S3ClientWrapperException e) {
        if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
          continue;
        } else {
          throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
        }
      } catch (Exception e) {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
      }
    }
    return records;
  }

  private Set<S3Record> getRecordsInTable(String namespace, String table)
      throws ExecutionException {
    Set<String> keys = wrapper.listKeys(S3Utils.getObjectKey(namespace, table, null, null));
    Set<S3Record> records = new HashSet<>();
    for (String key : keys) {
      try {
        S3ClientWrapperResponse response = wrapper.get(key);
        records.add(JsonConvertor.deserialize(response.getValue(), S3Record.class));
      } catch (S3ClientWrapperException e) {
        if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
          // Do nothing
        } else {
          throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
        }
      } catch (Exception e) {
        throw new ExecutionException(CoreError.S3_ERROR_OCCURRED_IN_SELECTION.buildMessage(), e);
      }
    }
    return records;
  }
}
