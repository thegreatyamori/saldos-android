package com.thegreatyamori.saldos.util

import android.util.Log

/**
 * Diagnostics for the CNT flow.
 *
 * Every message uses the single logcat tag [TAG] so the whole refresh can be
 * followed with:
 *
 * ```
 * adb logcat -s Saldos
 * ```
 *
 * Failed responses are truncated and never include credentials, tokens or the
 * encrypted payloads.
 */
object Diag {
    const val TAG = "Saldos"
    private const val MAX_BODY = 300

    fun step(message: String) {
        Log.d(TAG, message)
    }

    fun warn(message: String, error: Throwable? = null) {
        Log.w(TAG, message, error)
    }

    fun request(
        method: String,
        path: String,
        hasCsrf: Boolean,
        cookieCount: Int
    ) {
        Log.d(TAG, "→ $method $path csrf=${if (hasCsrf) "yes" else "no"} cookies=$cookieCount")
    }

    fun response(path: String, status: Int, body: String) {
        if (status in 200..299) {
            Log.d(TAG, "← $path $status (${body.length} chars)")
        } else {
            Log.w(TAG, "← $path $status body=${body.take(MAX_BODY)}")
        }
    }

    /** Logs an accepted payload that still needs inspection (e.g. empty data). */
    fun body(path: String, body: String, limit: Int = 1200) {
        Log.d(TAG, "body $path=${body.take(limit)}")
    }

    /** Keeps the last [visible] characters so logs stay useful but not identifying. */
    fun mask(value: String, visible: Int = 3): String {
        if (value.length <= visible) return "*".repeat(value.length)
        return "*".repeat(value.length - visible) + value.takeLast(visible)
    }
}
