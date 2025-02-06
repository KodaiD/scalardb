package com.scalar.db.storage.s3;

import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class S3ClientWrapper {
  private final String bucket;
  private final S3Client client;

  public S3ClientWrapper(String bucket, S3Client client) {
    this.bucket = bucket;
    this.client = client;
  }

  public S3ClientWrapperResponse get(String key) throws S3ClientWrapperException {
    try {
      ResponseBytes<GetObjectResponse> response =
          client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
      return new S3ClientWrapperResponse(response.asUtf8String(), response.response().eTag());
    } catch (S3Exception e) {
      if (e.statusCode() == S3ErrorCode.NOT_FOUND.get()) {
        throw new S3ClientWrapperException(S3ClientWrapperException.StatusCode.NOT_FOUND, e);
      }
      throw e;
    }
  }

  public void insert(String key, String value) throws S3ClientWrapperException {
    try {
      client.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).ifNoneMatch("*").build(),
          RequestBody.fromString(value));
    } catch (S3Exception e) {
      if (e.statusCode() == S3ErrorCode.PRECONDITION_FAILED.get()) {
        throw new S3ClientWrapperException(S3ClientWrapperException.StatusCode.ALREADY_EXISTS, e);
      } else if (e.statusCode() == S3ErrorCode.CONFLICT.get()) {
        throw new S3ClientWrapperException(S3ClientWrapperException.StatusCode.CONFLICT, e);
      }
      throw e;
    }
  }

  public boolean compareAndSwap(String key, String value, String eTag)
      throws S3ClientWrapperException {
    try {
      client.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).ifMatch(eTag).build(),
          RequestBody.fromString(value));
      return true;
    } catch (S3Exception e) {
      if (e.statusCode() == S3ErrorCode.PRECONDITION_FAILED.get()
          || e.statusCode() == S3ErrorCode.NOT_FOUND.get()
          || e.statusCode() == S3ErrorCode.CONFLICT.get()) {
        return false;
      }
      throw e;
    }
  }

  public void deleteIfExists(String key) {
    client.deleteObject(b -> b.bucket(bucket).key(key));
  }

  public boolean compareAndDelete(String key, String eTag) throws S3ClientWrapperException {
    try {
      client.deleteObject(b -> b.bucket(bucket).key(key).ifMatch(eTag));
      return true;
    } catch (S3Exception e) {
      if (e.statusCode() == S3ErrorCode.PRECONDITION_FAILED.get()
          || e.statusCode() == S3ErrorCode.CONFLICT.get()) {
        return false;
      }
      throw e;
    }
  }

  public Set<String> listKeys(String prefix) {
    return client
        .listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
        .contents()
        .stream()
        .map(S3Object::key)
        .collect(Collectors.toSet());
  }

  public void close() {
    client.close();
  }
}
