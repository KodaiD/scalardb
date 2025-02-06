package com.scalar.db.storage.s3;

public class S3ClientWrapperResponse {
  private final String value;
  private final String eTag;

  public S3ClientWrapperResponse(String value, String eTag) {
    this.value = value;
    this.eTag = eTag;
  }

  public String getValue() {
    return value;
  }

  public String getETag() {
    return eTag;
  }
}
