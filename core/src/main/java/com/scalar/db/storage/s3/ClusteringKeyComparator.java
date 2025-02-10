package com.scalar.db.storage.s3;

import com.scalar.db.api.Scan;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.io.*;
import java.util.Comparator;
import java.util.Map;

public class ClusteringKeyComparator implements Comparator<Map<String, Object>> {
  private final Map<String, Scan.Ordering.Order> orderings;
  private final TableMetadata metadata;

  public ClusteringKeyComparator(
      Map<String, Scan.Ordering.Order> orderings, TableMetadata metadata) {
    this.orderings = orderings;
    this.metadata = metadata;
  }

  @Override
  public int compare(Map<String, Object> key1, Map<String, Object> key2) {
    for (Map.Entry<String, Scan.Ordering.Order> ordering : orderings.entrySet()) {
      String columnName = ordering.getKey();
      Scan.Ordering.Order order = ordering.getValue();

      Column<?> column1 =
          ColumnValueMapper.convert(
              key1.get(columnName), columnName, metadata.getColumnDataType(columnName));
      Column<?> column2 =
          ColumnValueMapper.convert(
              key2.get(columnName), columnName, metadata.getColumnDataType(columnName));

      int cmp;
      switch (metadata.getColumnDataType(columnName)) {
        case BOOLEAN:
          cmp = ((BooleanColumn) column1).compareTo((BooleanColumn) column2);
          break;
        case INT:
          cmp = ((IntColumn) column1).compareTo((IntColumn) column2);
          break;
        case BIGINT:
          cmp = ((BigIntColumn) column1).compareTo((BigIntColumn) column2);
          break;
        case FLOAT:
          cmp = ((FloatColumn) column1).compareTo((FloatColumn) column2);
          break;
        case DOUBLE:
          cmp = ((DoubleColumn) column1).compareTo((DoubleColumn) column2);
          break;
        case TEXT:
          cmp = ((TextColumn) column1).compareTo((TextColumn) column2);
          break;
        case BLOB:
          cmp = ((BlobColumn) column1).compareTo((BlobColumn) column2);
          break;
        case DATE:
          cmp = ((DateColumn) column1).compareTo((DateColumn) column2);
          break;
        case TIME:
          cmp = ((TimeColumn) column1).compareTo((TimeColumn) column2);
          break;
        case TIMESTAMP:
          cmp = ((TimestampColumn) column1).compareTo((TimestampColumn) column2);
          break;
        case TIMESTAMPTZ:
          cmp = ((TimestampTZColumn) column1).compareTo((TimestampTZColumn) column2);
          break;
        default:
          throw new AssertionError();
      }
      if (cmp != 0) {
        return order == Scan.Ordering.Order.ASC ? cmp : -cmp;
      }
    }
    return 0;
  }
}
