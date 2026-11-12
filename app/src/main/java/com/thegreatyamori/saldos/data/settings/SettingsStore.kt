package com.thegreatyamori.saldos.data.settings

import android.content.Context
import androidx.core.content.edit

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var preferredPhoneValue: String?
        get() = prefs.getString(KEY_PREFERRED_LINE, null)
        set(value) {
            prefs.edit {
                if (value == null) remove(KEY_PREFERRED_LINE) else putString(KEY_PREFERRED_LINE, value)
            }
        }

    var refreshPolicy: RefreshPolicy
        get() = prefs.getString(KEY_REFRESH_POLICY, null)
            ?.let { stored -> RefreshPolicy.entries.firstOrNull { it.name == stored } }
            ?: RefreshPolicy.DATA_ONLY
        set(value) {
            prefs.edit { putString(KEY_REFRESH_POLICY, value.name) }
        }

    var smsTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_TRACKING, false)
        set(value) {
            prefs.edit { putBoolean(KEY_SMS_TRACKING, value) }
        }

    var smsOptInDismissed: Boolean
        get() = prefs.getBoolean(KEY_SMS_OPT_IN_DISMISSED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_SMS_OPT_IN_DISMISSED, value) }
        }

    var expiryRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXPIRY_REMINDERS, true)
        set(value) {
            prefs.edit { putBoolean(KEY_EXPIRY_REMINDERS, value) }
        }

    var notificationsPermissionAsked: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_PERMISSION_ASKED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_NOTIFICATIONS_PERMISSION_ASKED, value) }
        }

    private companion object {
        const val PREFS = "saldos.settings"
        const val KEY_PREFERRED_LINE = "preferred_phone_value"
        const val KEY_REFRESH_POLICY = "refresh_policy"
        const val KEY_SMS_TRACKING = "sms_tracking_enabled"
        const val KEY_SMS_OPT_IN_DISMISSED = "sms_opt_in_dismissed"
        const val KEY_EXPIRY_REMINDERS = "expiry_reminders_enabled"
        const val KEY_NOTIFICATIONS_PERMISSION_ASKED = "notifications_permission_asked"
    }
}
