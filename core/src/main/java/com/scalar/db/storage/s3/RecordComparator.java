package com.scalar.db.storage.s3;

import com.scalar.db.api.Scan;
import java.util.Comparator;
import java.util.Map;

public class RecordComparator implements Comparator<S3Record> {
  private final Map<String, Scan.Ordering.Order> orderings;

  public RecordComparator(Map<String, Scan.Ordering.Order> orderings) {
    this.orderings = orderings;
  }

  @Override
  public int compare(S3Record record1, S3Record record2) {
    ClusteringKeyComparator comparator = new ClusteringKeyComparator(orderings);
    return comparator.compare(record1.getClusteringKey(), record2.getClusteringKey());
  }
}
