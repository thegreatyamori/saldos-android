package com.thegreatyamori.saldos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thegreatyamori.saldos.domain.BalanceItem
import com.thegreatyamori.saldos.domain.LineInfo
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.ui.UiError
import com.thegreatyamori.saldos.ui.message
import com.thegreatyamori.saldos.util.timeAgo
import kotlin.math.roundToInt

private val FallbackAccent = Color(0xFF01A0E4)

@Composable
fun HeroCard(hero: BalanceItem?, modifier: Modifier = Modifier) {
    val accent = parseHexColor(hero?.color)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                Ring(
                    ratio = hero?.ratio ?: 0f,
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 9.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hero?.value ?: "--",
                        style = MaterialTheme.typography.headlineSmall,
                        color = accent
                    )
                    Text(
                        text = hero?.unit.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hero?.label ?: "Sin datos",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "de ${hero?.total ?: "--"} ${hero?.unit.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${((hero?.ratio ?: 0f) * 100).roundToInt()}% usado",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
    }
}

@Composable
fun ItemRow(item: BalanceItem) {
    val accent = parseHexColor(item.color)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.value == "∞") "∞" else "${item.value} ${item.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = accent
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { item.ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun StatusBanner(error: UiError?, hasCachedData: Boolean) {
    if (error == null) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
    ) {
        Text(
            text = error.message() + if (hasCachedData) " · mostrando últimos datos" else "",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun LinesRow(
    lines: List<LineInfo>,
    selectedPhoneValue: String?,
    onSelect: (String) -> Unit
) {
    if (lines.size <= 1) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lines, key = { it.phoneValue }) { line ->
            FilterChip(
                selected = line.phoneValue == selectedPhoneValue,
                onClick = { onSelect(line.phoneValue) },
                label = { Text(line.phone) }
            )
        }
    }
}

@Composable
fun SnapshotMeta(snapshot: Snapshot) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${snapshot.header.phone} · ${snapshot.header.planTitle}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Actualizado ${timeAgo(snapshot.fetchedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseHexColor(value: String?): Color = try {
    if (value.isNullOrBlank()) FallbackAccent else Color(android.graphics.Color.parseColor(value))
} catch (_: IllegalArgumentException) {
    FallbackAccent
}
