package com.scalar.db.storage.s3;

import com.scalar.db.api.DistributedStorageJapaneseIntegrationTestBase;
import java.util.Properties;

public class S3JapaneseIntegrationTest extends DistributedStorageJapaneseIntegrationTestBase {

    @Override
    protected Properties getProperties(String testName) {
        return S3Env.getProperties(testName);
    }
}

