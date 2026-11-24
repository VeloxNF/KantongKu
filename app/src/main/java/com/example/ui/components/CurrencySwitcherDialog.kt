package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.currency.CurrencyCatalog
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.currency.CurrencyInfo
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

@Composable
fun CurrencySwitcherDialog(
    currentConfig: CurrencyDisplayConfig,
    exchangeRates: Map<String, Double>,
    isRefreshingRates: Boolean,
    onRefreshRates: () -> Unit,
    onSelectCurrency: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("SEMUA") }

    val allCurrencies = remember { CurrencyCatalog.allCurrencies }
    val popularSet = remember { CurrencyCatalog.popularCodes.toSet() }

    val filtered = remember(query, filterMode, allCurrencies) {
        val q = query.trim().lowercase()
        allCurrencies.filter { item ->
            val groupOk = filterMode == "SEMUA" || item.code in popularSet
            val queryOk = q.isBlank() ||
                item.code.lowercase().contains(q) ||
                item.nameId.lowercase().contains(q) ||
                item.symbol.lowercase().contains(q) ||
                item.regionLabel.lowercase().contains(q)
            groupOk && queryOk
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 580.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(26.dp),
            color = bento.cardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        CurrencyFlagBadge(
                            flagEmoji = currentConfig.flagEmoji,
                            code = currentConfig.code,
                            size = 42.dp,
                        )
                        Column {
                            Text(
                                text = "Pilih Mata Uang Dunia",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${currentConfig.code} • ${allCurrencies.size} negara & kurs live",
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                // Live Exchange Rate Status & Refresh
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bento.nestedCardSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "${currentConfig.flagEmoji} ${FormatUtils.formatExchangeRateLine(currentConfig)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = bento.highlightLabel,
                            )
                            Text(
                                text = if (currentConfig.updatedAtMillis > 0L) {
                                    "Kurs live: ${FormatUtils.formatDateTimeShort(currentConfig.updatedAtMillis)}"
                                } else {
                                    "Menggunakan kurs referensi tersimpan"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.mutedText,
                            )
                        }

                        OutlinedButton(
                            onClick = onRefreshRates,
                            enabled = !isRefreshingRates,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("currency_refresh_rates_button"),
                        ) {
                            if (isRefreshingRates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" Perbarui", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari negara, kode, atau mata uang (USD, Jepang, Euro...)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_search_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = filterMode == "SEMUA",
                        onClick = { filterMode = "SEMUA" },
                        label = { Text("Semua (${allCurrencies.size})") },
                    )
                    FilterChip(
                        selected = filterMode == "POPULER",
                        onClick = { filterMode = "POPULER" },
                        label = { Text("Populer") },
                    )
                }

                if (isRefreshingRates) {
                    CurrencyRatesListSkeleton(
                        rowCount = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    )
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Mata uang \"$query\" tidak ditemukan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        items(filtered, key = { it.code }) { item ->
                            val isSelected = item.code == currentConfig.code
                            val rate = if (item.code == "IDR") 1.0
                            else exchangeRates[item.code] ?: CurrencyCatalog.seedRatesPerIdr[item.code]
                            CurrencyRowItem(
                                info = item,
                                rateFromIdr = rate,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectCurrency(item.code)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRowItem(
    info: CurrencyInfo,
    rateFromIdr: Double?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val sampleFormat = remember(info.code) {
        val sampleConfig = CurrencyDisplayConfig(
            code = info.code,
            nameId = info.nameId,
            symbol = info.symbol,
            rateFromIdr = 1.0,
            groupingSeparator = info.groupingSeparator,
            decimalSeparator = info.decimalSeparator,
            decimalDigits = info.decimalDigits,
            countryCode = info.countryCode,
            flagEmoji = info.flagEmoji,
        )
        FormatUtils.formatRawNumberWithConfig(
            if (info.decimalDigits > 0) 1250.50 else 1000000.0,
            sampleConfig,
        )
    }

    val ratePreview = remember(info.code, rateFromIdr) {
        when {
            info.code == "IDR" -> "Mata uang dasar"
            rateFromIdr != null && rateFromIdr > 0.0 -> {
                val cfg = CurrencyCatalog.buildDisplayConfig(info.code, mapOf(info.code to rateFromIdr), 0L)
                FormatUtils.formatExchangeRateLine(cfg)
            }
            else -> "Kurs 1:1"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("currency_option_${info.code.lowercase()}")
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) bento.accentTile.copy(alpha = 0.16f) else bento.nestedCardSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) bento.highlightLabel else bento.borderSubtle.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CurrencyFlagBadge(
                flagEmoji = info.flagEmoji,
                code = info.code,
                size = 38.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = info.code,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) bento.highlightLabel else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "• ${info.regionLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${info.nameId} (${info.symbol})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$ratePreview • ${info.symbol}$sampleFormat",
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Aktif",
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
