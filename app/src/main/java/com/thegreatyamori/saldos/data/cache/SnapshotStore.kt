package com.thegreatyamori.saldos.data.cache

import android.content.Context
import com.thegreatyamori.saldos.domain.Snapshot
import kotlinx.serialization.json.Json
import java.io.File
import java.util.logging.Logger

class SnapshotStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val logger = Logger.getLogger(SnapshotStore::class.java.name)

    fun read(): Snapshot? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString(Snapshot.serializer(), file.readText())
        } catch (error: Exception) {
            logger.warning("snapshot cache unreadable: ${error.message}")
            null
        }
    }

    fun write(snapshot: Snapshot) {
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(Snapshot.serializer(), snapshot))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        } catch (error: Exception) {
            logger.warning("snapshot cache write failed: ${error.message}")
        }
    }

    fun clear() {
        file.delete()
    }

    companion object {
        fun create(context: Context): SnapshotStore =
            SnapshotStore(File(context.filesDir, "snapshot.json"))
    }
}
