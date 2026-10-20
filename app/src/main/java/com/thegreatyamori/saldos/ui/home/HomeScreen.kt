package com.thegreatyamori.saldos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thegreatyamori.saldos.ui.SaldosUiState
import com.thegreatyamori.saldos.ui.components.HeroCard
import com.thegreatyamori.saldos.ui.components.ItemRow
import com.thegreatyamori.saldos.ui.components.LinesRow
import com.thegreatyamori.saldos.ui.components.SnapshotMeta
import com.thegreatyamori.saldos.ui.components.StatusBanner

@Composable
fun HomeScreen(
    state: SaldosUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectLine: (String) -> Unit
) {
    val snapshot = state.snapshot

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            HomeHeader(
                refreshing = state.refreshing,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { StatusBanner(error = state.error, hasCachedData = snapshot != null) }
                item { HeroCard(hero = snapshot?.hero) }

                items(snapshot?.items.orEmpty(), key = { it.label }) { item ->
                    ItemRow(item)
                }

                if (snapshot != null) {
                    item {
                        LinesRow(
                            lines = snapshot.lines,
                            selectedPhoneValue = snapshot.line.phoneValue,
                            onSelect = onSelectLine
                        )
                    }
                    item { SnapshotMeta(snapshot) }
                } else {
                    item {
                        Text(
                            text = "Sin datos todavía. Tocá Actualizar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Saldos",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.weight(1f))
        OutlinedIconButton(
            onClick = onRefresh,
            enabled = !refreshing,
            modifier = Modifier.size(48.dp)
        ) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Actualizar",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Ajustes",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
