package com.thegreatyamori.saldos.data.settings

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var preferredPhoneValue: String?
        get() = prefs.getString(KEY_PREFERRED_LINE, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_PREFERRED_LINE) else putString(KEY_PREFERRED_LINE, value)
            }.apply()
        }

    var refreshPolicy: RefreshPolicy
        get() = prefs.getString(KEY_REFRESH_POLICY, null)
            ?.let { stored -> RefreshPolicy.entries.firstOrNull { it.name == stored } }
            ?: RefreshPolicy.DATA_ONLY
        set(value) {
            prefs.edit().putString(KEY_REFRESH_POLICY, value.name).apply()
        }

    private companion object {
        const val PREFS = "saldos.settings"
        const val KEY_PREFERRED_LINE = "preferred_phone_value"
        const val KEY_REFRESH_POLICY = "refresh_policy"
    }
}
