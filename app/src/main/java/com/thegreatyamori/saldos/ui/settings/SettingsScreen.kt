package com.thegreatyamori.saldos.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thegreatyamori.saldos.data.settings.RefreshPolicy
import com.thegreatyamori.saldos.ui.SaldosUiState

@Composable
fun SettingsScreen(
    state: SaldosUiState,
    versionName: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onPolicyChange: (RefreshPolicy) -> Unit,
    onSelectLine: (String) -> Unit,
    onAddWidget: () -> Unit,
    onAddSquareWidget: () -> Unit
) {
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                SectionTitle("CUENTA")
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.accountIdentifier ?: "Sin cuenta",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onLogout) {
                            Text("Salir", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                SectionTitle("ACTUALIZACIÓN")
                SettingsCard {
                    Column {
                        PolicyRow(
                            label = "Solo datos móviles",
                            description = "Cada 15 min, solo con datos",
                            selected = state.refreshPolicy == RefreshPolicy.DATA_ONLY,
                            onSelect = { onPolicyChange(RefreshPolicy.DATA_ONLY) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        PolicyRow(
                            label = "Siempre",
                            description = "Cada 15 min, también en WiFi",
                            selected = state.refreshPolicy == RefreshPolicy.ALWAYS,
                            onSelect = { onPolicyChange(RefreshPolicy.ALWAYS) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        PolicyRow(
                            label = "Manual",
                            description = "Solo al tocar actualizar",
                            selected = state.refreshPolicy == RefreshPolicy.MANUAL,
                            onSelect = { onPolicyChange(RefreshPolicy.MANUAL) }
                        )
                    }
                }
            }

            item {
                SectionTitle("WIDGET")
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Línea del widget",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        val lines = state.snapshot?.lines.orEmpty()
                        if (lines.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(lines, key = { it.phoneValue }) { line ->
                                    FilterChip(
                                        selected = line.phoneValue == state.snapshot?.line?.phoneValue,
                                        onClick = { onSelectLine(line.phoneValue) },
                                        label = { Text(line.phone) }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = state.snapshot?.line?.phone ?: "Sin línea",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onAddWidget,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Widget 2x1")
                            }
                            OutlinedButton(
                                onClick = onAddSquareWidget,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Widget 1x1")
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("ACERCA DE")
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Versión", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = versionName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { showPrivacy = true },
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            Text("Privacidad")
                        }
                        Text(
                            text = "Cliente no oficial de CNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacidad") },
            text = {
                Text(
                    "Tus credenciales se guardan cifradas en este dispositivo con " +
                        "Android Keystore. No se envían a servidores propios: la app " +
                        "se conecta directamente a CNT. El widget solo lee los datos " +
                        "cacheados en el teléfono."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun PolicyRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
