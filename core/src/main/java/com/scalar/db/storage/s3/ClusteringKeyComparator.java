package com.scalar.db.storage.s3;

import com.scalar.db.api.Scan;
import java.util.Comparator;
import java.util.Map;

public class ClusteringKeyComparator implements Comparator<Map<String, Object>> {
  private final Map<String, Scan.Ordering.Order> orderings;

  public ClusteringKeyComparator(Map<String, Scan.Ordering.Order> orderings) {
    this.orderings = orderings;
  }

  @Override
  public int compare(Map<String, Object> key1, Map<String, Object> key2) {
    for (Map.Entry<String, Scan.Ordering.Order> ordering : orderings.entrySet()) {
      String key = ordering.getKey();
      Scan.Ordering.Order order = ordering.getValue();
      String value1 = key1.get(key).toString();
      String value2 = key2.get(key).toString();
      int cmp = value1.compareTo(value2);
      if (cmp != 0) {
        return order == Scan.Ordering.Order.ASC ? cmp : -cmp;
      }
    }
    return 0;
  }
}
