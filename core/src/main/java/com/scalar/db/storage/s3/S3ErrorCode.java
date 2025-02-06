package com.scalar.db.storage.s3;

public enum S3ErrorCode {
  NOT_FOUND(404),
  CONFLICT(409),
  PRECONDITION_FAILED(412);

  private final int code;

  S3ErrorCode(int code) {
    this.code = code;
  }

  public int get() {
    return code;
  }
}
