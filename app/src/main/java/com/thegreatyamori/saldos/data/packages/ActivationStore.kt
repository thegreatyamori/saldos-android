package com.thegreatyamori.saldos.data.packages

import android.content.Context
import com.thegreatyamori.saldos.domain.TrackedPackage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ActivationState(
    val packages: List<TrackedPackage> = emptyList(),
    val lastSmsScanMs: Long = 0L
)

class ActivationStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun read(): ActivationState {
        if (!file.exists()) return ActivationState()
        return try {
            json.decodeFromString(ActivationState.serializer(), file.readText())
        } catch (_: Exception) {
            ActivationState()
        }
    }

    @Synchronized
    fun write(state: ActivationState) {
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(ActivationState.serializer(), state))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        } catch (_: Exception) {
            // Tracking is best-effort; never break a refresh over it.
        }
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    companion object {
        fun create(context: Context): ActivationStore =
            ActivationStore(File(context.filesDir, "activations.json"))
    }
}
