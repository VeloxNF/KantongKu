package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.PocketBentoCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

private enum class PocketFilter(val label: String) {
    ALL("Semua"),
    BUDGET("Batas Pengeluaran"),
    SAVINGS("Target Tabungan"),
}

@Composable
fun PocketsScreen(
    pockets: List<PocketEntity>,
    currencyConfig: CurrencyDisplayConfig,
    onAddPocketClick: () -> Unit,
    onSmartPocketClick: () -> Unit,
    onPocketClick: (PocketEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    var filter by remember { mutableStateOf(PocketFilter.ALL) }

    val budgetPockets = pockets.filter { it.pocketMode == PocketMode.BUDGET_LIMIT }
    val savingsPockets = pockets.filter { it.pocketMode == PocketMode.SAVINGS_GOAL }

    val totalBudgetLimit = budgetPockets.sumOf { it.targetAmount }
    val totalBudgetSpent = budgetPockets.sumOf { it.currentAmount }
    val totalSavingsTarget = savingsPockets.sumOf { it.targetAmount }
    val totalSavingsCurrent = savingsPockets.sumOf { it.currentAmount }

    val filteredPockets = when (filter) {
        PocketFilter.ALL -> pockets
        PocketFilter.BUDGET -> budgetPockets
        PocketFilter.SAVINGS -> savingsPockets
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val columns = if (maxWidth >= 680.dp) 2 else 1

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = strings.pocketsHeaderTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Untuk alokasi anggaran & target. Saldo asli tetap tersimpan di Dompet Utama.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }

                    Button(
                        onClick = onAddPocketClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bento.accentTile,
                            contentColor = bento.onAccentTile,
                        ),
                        modifier = Modifier.testTag("pockets_add_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" ${strings.pocketsNewButton}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Dompet Pintar CTA Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_pocket_button")
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(bento.heroStart, bento.heroEnd)))
                        .clickable(onClick = onSmartPocketClick)
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bento.accentTile),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = strings.pocketsSmartButton,
                                tint = bento.onAccentTile,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.pocketsSmartButton,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bento.onHero,
                            )
                            Text(
                                text = "Pilih kategori & ikon sesuai kebutuhan Anda, lalu buat rekomendasi alokasi otomatis",
                                style = MaterialTheme.typography.labelMedium,
                                color = bento.onHero.copy(alpha = 0.85f),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = strings.pocketsSmartButton,
                            tint = bento.onHero,
                        )
                    }
                }
            }

            // Summary Bento Pair
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoSurfaceCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            text = "Batas Anggaran",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCompactMoney((totalBudgetLimit - totalBudgetSpent).coerceAtLeast(0L), currencyConfig),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Sisa dari ${FormatUtils.formatCompactMoney(totalBudgetLimit, currencyConfig)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }

                    BentoSurfaceCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            text = "Target Tabungan",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCompactMoney(totalSavingsCurrent, currencyConfig),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = bento.incomeColor,
                        )
                        Text(
                            text = "Dari ${FormatUtils.formatCompactMoney(totalSavingsTarget, currencyConfig)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                }
            }

            // Filter Pills
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PocketFilter.entries.forEach { item ->
                        FilterChip(
                            selected = filter == item,
                            onClick = { filter = item },
                            label = { Text(item.label) },
                        )
                    }
                }
            }

            if (filteredPockets.isEmpty()) {
                item {
                    BentoSurfaceCard(
                        onClick = onAddPocketClick,
                        contentPadding = PaddingValues(28.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = bento.highlightLabel,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada kantong di kategori ini",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk untuk membuat target batas pengeluaran atau tabungan baru.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.mutedText,
                            )
                        }
                    }
                }
            } else {
                val rows = filteredPockets.chunked(columns)
                items(rows.size) { rowIdx ->
                    val rowItems = rows[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { pocket ->
                            PocketBentoCard(
                                pocket = pocket,
                                isFeaturedLayout = true,
                                onClick = { onPocketClick(pocket) },
                                modifier = Modifier.weight(1f),
                                currencyConfig = currencyConfig,
                            )
                        }
                        if (columns == 2 && rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
