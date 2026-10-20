package com.thegreatyamori.saldos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SaldosColors = darkColorScheme(
    primary = Color(0xFF01A0E4),
    onPrimary = Color(0xFF00202E),
    secondary = Color(0xFF25D366),
    background = Color(0xFF0B1218),
    surface = Color(0xFF131C24),
    surfaceVariant = Color(0xFF1C2833),
    onSurface = Color(0xFFEAF2F8),
    onSurfaceVariant = Color(0xFF9FB3C8),
    error = Color(0xFFFF6B6B)
)

@Composable
fun SaldosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SaldosColors, content = content)
}
