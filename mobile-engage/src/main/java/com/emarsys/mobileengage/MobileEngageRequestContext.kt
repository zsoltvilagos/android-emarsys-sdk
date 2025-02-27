package com.emarsys.mobileengage

import com.emarsys.core.Mockable
import com.emarsys.core.device.DeviceInfo
import com.emarsys.core.provider.timestamp.TimestampProvider
import com.emarsys.core.provider.uuid.UUIDProvider
import com.emarsys.core.storage.Storage

@Mockable
data class MobileEngageRequestContext(
        var applicationCode: String?,
        val contactFieldId: Int,
        val deviceInfo: DeviceInfo,
        val timestampProvider: TimestampProvider,
        val uuidProvider: UUIDProvider,
        val clientStateStorage: Storage<String>,
        val contactTokenStorage: Storage<String>,
        val refreshTokenStorage: Storage<String>,
        val contactFieldValueStorage: Storage<String>)