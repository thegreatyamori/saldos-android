package com.thegreatyamori.saldos.widget

import androidx.compose.ui.graphics.Color
import com.thegreatyamori.saldos.domain.BalanceItem

object WidgetColors {
    val Background = Color(0xFF131C24)
    val Track = Color(0xFF1C2833)
    val Text = Color(0xFFEAF2F8)
    val Dim = Color(0xFF9FB3C8)
    val Accent = Color(0xFF01A0E4)

    fun accentFor(hero: BalanceItem?): Color {
        val hex = hero?.color ?: return Accent
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: IllegalArgumentException) {
            Accent
        }
    }
}
