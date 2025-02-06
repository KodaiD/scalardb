package com.scalar.db.storage.s3;

import com.scalar.db.api.DistributedStorageAdmin;
import com.scalar.db.api.DistributedStorageProvider;
import com.scalar.db.common.CheckedDistributedStorageAdmin;
import com.scalar.db.config.DatabaseConfig;

public class S3Provider implements DistributedStorageProvider {
  @Override
  public String getName() {
    return S3Config.STORAGE_NAME;
  }

  @Override
  public S3 createDistributedStorage(DatabaseConfig config) {
    return new S3(config);
  }

  @Override
  public DistributedStorageAdmin createDistributedStorageAdmin(DatabaseConfig config) {
    return new CheckedDistributedStorageAdmin(new S3Admin(config), config);
  }
}
