package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.FinanceDashboardStats
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.TransactionItemRow
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import java.util.Calendar

private enum class HistoryFilter {
    ALL,
    EXPENSE,
    INCOME,
}

private enum class DateRangeFilter(val label: String) {
    ALL("Semua Tanggal"),
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
}

@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    stats: FinanceDashboardStats,
    currencyConfig: CurrencyDisplayConfig,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    wallets: List<WalletEntity> = emptyList(),
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var dateRange by remember { mutableStateOf(DateRangeFilter.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val availableCategories = remember(transactions) {
        transactions
            .map { it.pocketName.ifBlank { it.categoryLabel.ifBlank { "Umum" } } }
            .distinct()
            .sorted()
    }

    val filtered = remember(transactions, query, filter, dateRange, selectedCategory) {
        val tokens = query.trim().lowercase().split(" ").filter { it.isNotBlank() }
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sevenDaysAgo = startOfToday - 6L * 86_400_000L
        val monthStart = FormatUtils.startOfCurrentMonthMillis(now)

        transactions.filter { tx ->
            val typeOk = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.EXPENSE -> tx.txType == TransactionType.EXPENSE
                HistoryFilter.INCOME -> tx.txType == TransactionType.INCOME
            }

            val dateOk = when (dateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> tx.occurredAtMillis >= startOfToday
                DateRangeFilter.LAST_7_DAYS -> tx.occurredAtMillis >= sevenDaysAgo
                DateRangeFilter.THIS_MONTH -> tx.occurredAtMillis >= monthStart
            }

            val categoryName = tx.pocketName.ifBlank { tx.categoryLabel.ifBlank { "Umum" } }
            val categoryOk = selectedCategory == null || categoryName.equals(selectedCategory, ignoreCase = true)

            val queryOk = if (tokens.isEmpty()) {
                true
            } else {
                val dateText = FormatUtils.searchableDateTokens(tx.occurredAtMillis)
                val haystack = buildString {
                    append(tx.title.lowercase())
                    append(' ')
                    append(tx.note.lowercase())
                    append(' ')
                    append(tx.locationName.lowercase())
                    append(' ')
                    append(categoryName.lowercase())
                    append(' ')
                    append(tx.categoryLabel.lowercase())
                    append(' ')
                    append(tx.walletName.lowercase())
                    append(' ')
                    append(tx.txType.label.lowercase())
                    append(' ')
                    append(dateText)
                }
                tokens.all { token -> haystack.contains(token) }
            }

            typeOk && dateOk && categoryOk && queryOk
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { FormatUtils.formatDateGroup(it.occurredAtMillis) }
    }

    val hasActiveFilters = query.isNotBlank() ||
        filter != HistoryFilter.ALL ||
        dateRange != DateRangeFilter.ALL ||
        selectedCategory != null

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = strings.historyHeaderTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = strings.historySearchPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.mutedText,
                )
            }

            // Monthly income & expense summary Bento Row
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
                            text = strings.heroIncomeMonth,
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${FormatUtils.formatMoney(stats.monthIncome, currencyConfig)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = bento.incomeColor,
                        )
                    }
                    BentoSurfaceCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            text = strings.heroExpenseMonth,
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${FormatUtils.formatMoney(stats.monthExpense, currencyConfig)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = bento.expenseColor,
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.historySearchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = strings.historyHeaderTitle) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.testTag("history_search_clear"),
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                )
            }

            // Type & Date filter chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryFilter.entries.forEach { item ->
                            val filterLabel = when (item) {
                                HistoryFilter.ALL -> strings.historyFilterAll
                                HistoryFilter.EXPENSE -> strings.historyFilterExpense
                                HistoryFilter.INCOME -> strings.historyFilterIncome
                            }
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(filterLabel) },
                                modifier = Modifier.testTag("history_filter_${item.name.lowercase()}"),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DateRangeFilter.entries.forEach { range ->
                            FilterChip(
                                selected = dateRange == range,
                                onClick = { dateRange = range },
                                leadingIcon = if (range == DateRangeFilter.ALL) null else {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                },
                                label = { Text(range.label) },
                                modifier = Modifier.testTag("history_date_${range.name.lowercase()}"),
                            )
                        }
                    }

                    if (availableCategories.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                },
                                label = { Text("Semua Kategori") },
                                modifier = Modifier.testTag("history_category_all"),
                            )
                            availableCategories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                    label = { Text(category) },
                                    modifier = Modifier.testTag("history_category_${category.lowercase().replace(" ", "_")}"),
                                )
                            }
                        }
                    }
                }
            }

            if (hasActiveFilters) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Menampilkan ${filtered.size} dari ${transactions.size} transaksi",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Text(
                            text = "Reset Filter",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = bento.highlightLabel,
                            modifier = Modifier
                                .testTag("history_reset_filters")
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    query = ""
                                    filter = HistoryFilter.ALL
                                    dateRange = DateRangeFilter.ALL
                                    selectedCategory = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    BentoSurfaceCard(contentPadding = PaddingValues(28.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = bento.mutedText,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (transactions.isEmpty()) "Belum ada catatan transaksi" else "Tidak ditemukan transaksi yang cocok",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                grouped.forEach { (dateLabel, itemsInGroup) ->
                    item(key = "header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = bento.mutedText,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    items(itemsInGroup, key = { it.id }) { tx ->
                        TransactionItemRow(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) },
                            currencyConfig = currencyConfig,
                            wallet = wallets.firstOrNull { it.id == tx.walletId },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
