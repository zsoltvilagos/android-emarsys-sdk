package com.emarsys.core.di;

import android.os.Handler;

import com.emarsys.core.DeviceInfo;
import com.emarsys.core.activity.ActivityLifecycleWatchdog;
import com.emarsys.core.activity.CurrentActivityWatchdog;
import com.emarsys.core.database.CoreSQLiteDatabase;
import com.emarsys.core.database.repository.Repository;
import com.emarsys.core.database.repository.SqlSpecification;
import com.emarsys.core.provider.timestamp.TimestampProvider;
import com.emarsys.core.provider.uuid.UUIDProvider;
import com.emarsys.core.shard.ShardModel;

public interface DependencyContainer {

    Handler getCoreSdkHandler();

    ActivityLifecycleWatchdog getActivityLifecycleWatchdog();

    CurrentActivityWatchdog getCurrentActivityWatchdog();

    CoreSQLiteDatabase getCoreSQLiteDatabase();

    DeviceInfo getDeviceInfo();

    Repository<ShardModel, SqlSpecification> getShardRepository();

    TimestampProvider getTimestampProvider();

    UUIDProvider getUuidProvider();

}
