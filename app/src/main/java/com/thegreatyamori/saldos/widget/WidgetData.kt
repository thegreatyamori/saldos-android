package com.thegreatyamori.saldos.widget

import android.content.Context
import com.thegreatyamori.saldos.data.cache.SnapshotStore
import com.thegreatyamori.saldos.domain.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadSnapshot(context: Context): Snapshot? = withContext(Dispatchers.IO) {
    SnapshotStore.create(context).read()
}
