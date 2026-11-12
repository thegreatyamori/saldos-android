package com.thegreatyamori.saldos.data.packages

import com.thegreatyamori.saldos.data.settings.SettingsStore
import com.thegreatyamori.saldos.data.sms.CntSmsParser
import com.thegreatyamori.saldos.data.sms.CntSmsReader
import com.thegreatyamori.saldos.data.sms.SmsScan
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.domain.TrackedPackage
import java.time.Instant

class PackageUpdater(
    private val reader: CntSmsReader,
    private val store: ActivationStore,
    private val settings: SettingsStore,
    private val parser: CntSmsParser = CntSmsParser(),
    private val tracker: PackageTracker = PackageTracker(),
    private val onClear: () -> Unit = {}
) {

    @Volatile
    private var cache: List<TrackedPackage> = store.read().packages

    fun onSnapshot(previous: Snapshot?, current: Snapshot): List<TrackedPackage> {
        val state = store.read()
        val scan = if (settings.smsTrackingEnabled) {
            reader.read(Instant.ofEpochMilli(state.lastSmsScanMs))
        } else {
            null
        }

        val activations = if (scan is SmsScan.Found) {
            scan.messages.mapNotNull(parser::parse)
        } else {
            emptyList()
        }

        val newestSms = if (scan is SmsScan.Found) {
            scan.messages.maxOfOrNull { it.dateMs } ?: state.lastSmsScanMs
        } else {
            state.lastSmsScanMs
        }

        val reconciled = tracker.reconcile(
            state = state,
            previous = previous,
            current = current,
            activations = activations,
            now = Instant.now()
        )
        store.write(reconciled.copy(lastSmsScanMs = newestSms))
        cache = reconciled.packages
        return cache
    }

    fun packages(): List<TrackedPackage> = cache

    fun clear() {
        cache = emptyList()
        store.clear()
        onClear()
    }
}
