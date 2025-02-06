package com.scalar.db.storage.s3;

public class S3ClientWrapperException extends Exception {
  private final StatusCode code;

  public S3ClientWrapperException(StatusCode code, String message) {
    super(message);
    this.code = code;
  }

  public S3ClientWrapperException(StatusCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public S3ClientWrapperException(StatusCode code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  public StatusCode getCode() {
    return code;
  }

  public enum StatusCode {
    NOT_FOUND,
    ALREADY_EXISTS,
    CONFLICT,
  }
}
