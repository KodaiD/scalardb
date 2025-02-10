package com.scalar.db.storage.s3;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.TableMetadata;
import com.scalar.db.common.error.CoreError;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.io.DataType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class S3Admin implements DistributedStorageAdmin {
  private static final String NAMESPACE_TABLE = "namespaces";
  private static final String METADATA_TABLE = "metadata";

  private final S3ClientWrapper wrapper;
  private final String metadataNamespace;

  @Inject
  public S3Admin(DatabaseConfig databaseConfig) {
    S3Config config = new S3Config(databaseConfig);
    wrapper = new S3ClientWrapper(config.getBucket(), S3Utils.buildS3Client(config));
    metadataNamespace =
        config.getMetadataNamespace().orElse(DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME);
  }

  public S3Admin(S3ClientWrapper wrapper, S3Config config) {
    this.wrapper = wrapper;
    metadataNamespace =
        config.getMetadataNamespace().orElse(DatabaseConfig.DEFAULT_SYSTEM_NAMESPACE_NAME);
  }

  @Override
  public TableMetadata getImportTableMetadata(
      String namespace, String table, Map<String, DataType> overrideColumnsType)
      throws ExecutionException {
    throw new UnsupportedOperationException(CoreError.S3_IMPORT_NOT_SUPPORTED.buildMessage());
  }

  @Override
  public void addRawColumnToTable(
      String namespace, String table, String columnName, DataType columnType)
      throws ExecutionException {
    throw new UnsupportedOperationException(CoreError.S3_IMPORT_NOT_SUPPORTED.buildMessage());
  }

  @Override
  public void close() {
    wrapper.close();
  }

  @Override
  public void createNamespace(String namespace, Map<String, String> options)
      throws ExecutionException {
    try {
      insertNamespace(namespace);
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to create the namespace %s.", namespace), e);
    }
  }

  @Override
  public void createTable(
      String namespace, String table, TableMetadata metadata, Map<String, String> options)
      throws ExecutionException {
    try {
      insertTableMetadata(namespace, table, metadata);
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to create the table %s.%s.", table, namespace), e);
    }
  }

  @Override
  public void dropTable(String namespace, String table) throws ExecutionException {
    try {
      deleteTableMetadata(namespace, table);
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to drop the table %s.%s.", table, namespace), e);
    }
  }

  @Override
  public void dropNamespace(String namespace) throws ExecutionException {
    try {
      deleteNamespace(namespace);
    } catch (Exception e) {
      throw new ExecutionException(String.format("Failed to drop the namespace %s.", namespace), e);
    }
  }

  @Override
  public void truncateTable(String namespace, String table) throws ExecutionException {
    try {
      Set<String> keys = wrapper.listKeys(S3Utils.getObjectKey(namespace, table, null, null));
      for (String key : keys) {
        wrapper.deleteIfExists(key);
      }
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to truncate the table %s.%s.", table, namespace), e);
    }
  }

  @Override
  public void createIndex(
      String namespace, String table, String columnName, Map<String, String> options)
      throws ExecutionException {
    throw new UnsupportedOperationException(CoreError.S3_INDEX_NOT_SUPPORTED.buildMessage());
  }

  @Override
  public void dropIndex(String namespace, String table, String columnName)
      throws ExecutionException {
    throw new UnsupportedOperationException(CoreError.S3_INDEX_NOT_SUPPORTED.buildMessage());
  }

  @Nullable
  @Override
  public TableMetadata getTableMetadata(String namespace, String table) throws ExecutionException {
    try {
      S3ClientWrapperResponse response =
          wrapper.get(S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, table));
      return JsonConvertor.deserialize(response.getValue(), S3TableMetadata.class)
          .toTableMetadata();
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to get the metadata of the table %s.%s.", table, namespace), e);
    }
  }

  @Override
  public Set<String> getNamespaceTableNames(String namespace) throws ExecutionException {
    try {
      if (!namespaceExists(namespace)) {
        return Collections.emptySet();
      }
      return wrapper
          .listKeys(S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, null))
          .stream()
          .map(
              key -> {
                List<String> parts = Splitter.on(S3Utils.OBJECT_KEY_DELIMITER).splitToList(key);
                return !parts.isEmpty() ? parts.get(parts.size() - 1) : "";
              })
          .filter(lastPart -> !lastPart.isEmpty())
          .collect(Collectors.toSet());
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to get the table names of the namespace %s.", namespace), e);
    }
  }

  @Override
  public boolean namespaceExists(String namespace) throws ExecutionException {
    if (metadataNamespace.equals(namespace)) {
      return true;
    }
    try {
      wrapper.get(S3Utils.getObjectKey(metadataNamespace, NAMESPACE_TABLE, namespace, namespace));
      return true;
    } catch (S3ClientWrapperException e) {
      if (e.getCode() == S3ClientWrapperException.StatusCode.NOT_FOUND) {
        return false;
      }
      throw new ExecutionException(
          String.format("Failed to check the existence of the namespace %s.", namespace), e);
    } catch (Exception e) {
      throw new ExecutionException(
          String.format("Failed to check the existence of the namespace %s.", namespace), e);
    }
  }

  @Override
  public void repairNamespace(String namespace, Map<String, String> options)
      throws ExecutionException {
    // TODO: Implement this method
  }

  @Override
  public void repairTable(
      String namespace, String table, TableMetadata metadata, Map<String, String> options)
      throws ExecutionException {
    // TODO: Implement this method
  }

  @Override
  public void addNewColumnToTable(
      String namespace, String table, String columnName, DataType columnType)
      throws ExecutionException {
    try {
      S3ClientWrapperResponse response =
          wrapper.get(S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, table));
      TableMetadata currentMetadata =
          JsonConvertor.deserialize(response.getValue(), S3TableMetadata.class).toTableMetadata();
      TableMetadata newMetadata =
          TableMetadata.newBuilder(currentMetadata).addColumn(columnName, columnType).build();
      if (!wrapper.compareAndSwap(
          S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, table),
          JsonConvertor.serialize(new S3TableMetadata(newMetadata)),
          response.getETag())) {
        throw new ExecutionException(
            String.format(
                "Failed to add the column %s to the table %s.%s due to a conflict.",
                columnName, table, namespace));
      }
    } catch (Exception e) {
      throw new ExecutionException(
          String.format(
              "Failed to add the column %s to the table %s.%s.", columnName, table, namespace),
          e);
    }
  }

  @Override
  public void importTable(
      String namespace,
      String table,
      Map<String, String> options,
      Map<String, DataType> overrideColumnsType)
      throws ExecutionException {
    throw new UnsupportedOperationException(CoreError.S3_IMPORT_NOT_SUPPORTED.buildMessage());
  }

  @Override
  public Set<String> getNamespaceNames() throws ExecutionException {
    try {
      return wrapper
          .listKeys(S3Utils.getObjectKey(metadataNamespace, NAMESPACE_TABLE, null, null))
          .stream()
          .map(
              key -> {
                List<String> parts = Splitter.on(S3Utils.OBJECT_KEY_DELIMITER).splitToList(key);
                return !parts.isEmpty() ? parts.get(parts.size() - 1) : "";
              })
          .filter(lastPart -> !lastPart.isEmpty())
          .collect(Collectors.toSet());
    } catch (Exception e) {
      throw new ExecutionException("Failed to get the namespace names.", e);
    }
  }

  @Override
  public void upgrade(Map<String, String> options) throws ExecutionException {
    // TODO: Implement this method
  }

  private void insertTableMetadata(String namespace, String table, TableMetadata metadata)
      throws ExecutionException {
    try {
      wrapper.insert(
          S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, table),
          JsonConvertor.serialize(new S3TableMetadata(metadata)));
    } catch (Exception e) {
      throw new ExecutionException("Failed to insert the table metadata.", e);
    }
  }

  private void deleteTableMetadata(String namespace, String table) throws ExecutionException {
    try {
      wrapper.deleteIfExists(
          S3Utils.getObjectKey(metadataNamespace, METADATA_TABLE, namespace, table));
    } catch (Exception e) {
      throw new ExecutionException("Failed to delete the table metadata.", e);
    }
  }

  private void insertNamespace(String namespace) throws ExecutionException {
    try {
      wrapper.insert(
          S3Utils.getObjectKey(metadataNamespace, NAMESPACE_TABLE, namespace, namespace),
          JsonConvertor.serialize(new S3Namespace(namespace)));
    } catch (Exception e) {
      throw new ExecutionException("Failed to insert the namespace.", e);
    }
  }

  private void deleteNamespace(String namespace) throws ExecutionException {
    try {
      wrapper.deleteIfExists(
          S3Utils.getObjectKey(metadataNamespace, NAMESPACE_TABLE, namespace, namespace));
    } catch (Exception e) {
      throw new ExecutionException("Failed to delete the namespace.", e);
    }
  }
}
