package com.scalar.db.storage.s3;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class S3Namespace extends S3DatabaseObject {
  private final String name;

  public S3Namespace() {
    this(null);
  }

  public S3Namespace(@Nullable String name) {
    this.name = name != null ? name : "";
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof S3Namespace)) {
      return false;
    }
    S3Namespace that = (S3Namespace) o;

    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
