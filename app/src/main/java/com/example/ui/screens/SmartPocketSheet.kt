package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.repository.SmartPocketAllocationItem
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.CategorySearchPickerDialog
import com.example.ui.components.POCKET_CATEGORY_CATALOG
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.pocketCategoryFor
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import kotlin.math.max
import kotlin.math.roundToLong

enum class SmartPeriodUnit(val label: String, val daysMultiplier: Int) {
    DAY("Hari", 1),
    WEEK("Minggu", 7),
    MONTH("Bulan", 30),
}

private data class SmartDraftPocket(
    val uid: String,
    val existingPocketId: String?,
    val name: String,
    val iconKey: String,
    val colorSlot: Int,
    val weight: Int,
    val amountText: String,
    val isManualOverride: Boolean = false,
) {
    fun parsedAmountIdr(config: CurrencyDisplayConfig): Long =
        editableInputToIdr(amountText, config)
}

private val SmartDraftListSaver = listSaver<List<SmartDraftPocket>, String>(
    save = { list ->
        list.flatMap { item ->
            listOf(
                item.uid,
                item.existingPocketId.orEmpty(),
                item.name,
                item.iconKey,
                item.colorSlot.toString(),
                item.weight.toString(),
                item.amountText,
                item.isManualOverride.toString(),
            )
        }
    },
    restore = { flat ->
        flat.chunked(8).mapNotNull { chunk ->
            if (chunk.size < 8) null
            else SmartDraftPocket(
                uid = chunk[0],
                existingPocketId = chunk[1].takeIf { it.isNotEmpty() },
                name = chunk[2],
                iconKey = chunk[3],
                colorSlot = chunk[4].toIntOrNull() ?: 0,
                weight = chunk[5].toIntOrNull() ?: 1,
                amountText = chunk[6],
                isManualOverride = chunk[7].toBooleanStrictOrNull() ?: false,
            )
        }
    },
)

private val QUICK_SELECT_KEYS = listOf(
    "FOOD",
    "TRANSPORT",
    "COFFEE",
    "GROCERIES",
    "FUEL",
    "SPORT",
    "UTILITIES",
    "HEALTH",
    "SHOPPING",
    "STREAMING",
    "HOME",
    "SAVINGS",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartPocketSheet(
    existingPockets: List<PocketEntity>,
    currentMonthlyBudget: Long,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onApply: (items: List<SmartPocketAllocationItem>, updatedMonthlyBudget: Long?) -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    val initialMaxBudgetIdr = currentMonthlyBudget.takeIf { it > 0L } ?: 3_000_000L
    var maxBudgetText by rememberSaveable(currencyConfig.code) {
        mutableStateOf(idrToEditableInput(initialMaxBudgetIdr, currencyConfig))
    }
    var durationText by rememberSaveable { mutableStateOf("1") }
    var periodUnit by rememberSaveable { mutableStateOf(SmartPeriodUnit.MONTH) }
    var updateProfileBudget by rememberSaveable { mutableStateOf(true) }

    // User picks the categories & icons themselves before generating recommendation preview
    var selectedCategoryKeys by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var draftItems by rememberSaveable(currencyConfig.code, stateSaver = SmartDraftListSaver) {
        mutableStateOf(emptyList())
    }
    var hasPreview by rememberSaveable { mutableStateOf(false) }
    var showCategoryCatalogDialog by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)
    val amountKeyboardType = if (!currencyConfig.isBaseIdr && currencyConfig.decimalDigits > 0) {
        KeyboardType.Decimal
    } else {
        KeyboardType.Number
    }

    val maxBudget = editableInputToIdr(maxBudgetText, currencyConfig)
    val durationValue = (durationText.toIntOrNull() ?: 0).coerceAtLeast(0)
    val totalDays = max(1, durationValue * periodUnit.daysMultiplier)
    val totalAllocated = draftItems.sumOf { it.parsedAmountIdr(currencyConfig) }
    val remaining = maxBudget - totalAllocated

    fun buildAndGenerateRecommendation() {
        when {
            maxBudget <= 0L -> {
                errorMsg = "Masukkan nominal budget yang dialokasikan terlebih dahulu."
            }
            durationValue <= 0 -> {
                errorMsg = "Masukkan rentang waktu minimal 1."
            }
            selectedCategoryKeys.isEmpty() -> {
                errorMsg = "Pilih minimal 1 kategori di atas sebelum membuat rekomendasi."
            }
            else -> {
                val selectedDrafts = selectedCategoryKeys.mapIndexed { idx, key ->
                    val cat = pocketCategoryFor(key)
                    val matchedExisting = existingPockets.firstOrNull {
                        it.pocketMode == PocketMode.BUDGET_LIMIT && it.iconKey.equals(key, ignoreCase = true)
                    }
                    SmartDraftPocket(
                        uid = matchedExisting?.id ?: "smart_${key}_$idx",
                        existingPocketId = matchedExisting?.id,
                        name = matchedExisting?.name ?: cat.label,
                        iconKey = cat.key,
                        colorSlot = idx % 6,
                        weight = cat.defaultWeight,
                        amountText = "",
                        isManualOverride = false,
                    )
                }
                draftItems = distributeAutomatically(selectedDrafts, maxBudget, currencyConfig)
                hasPreview = true
                errorMsg = null
            }
        }
    }

    fun toggleCategorySelection(key: String) {
        val nextKeys = if (key in selectedCategoryKeys) {
            selectedCategoryKeys - key
        } else {
            selectedCategoryKeys + key
        }
        selectedCategoryKeys = nextKeys
        errorMsg = null
        if (hasPreview) {
            if (nextKeys.isEmpty()) {
                draftItems = emptyList()
                hasPreview = false
            } else {
                val refreshed = nextKeys.mapIndexed { idx, k ->
                    val existingItem = draftItems.firstOrNull { it.iconKey == k }
                    if (existingItem != null) {
                        existingItem
                    } else {
                        val cat = pocketCategoryFor(k)
                        val matchedExisting = existingPockets.firstOrNull {
                            it.pocketMode == PocketMode.BUDGET_LIMIT && it.iconKey.equals(k, ignoreCase = true)
                        }
                        SmartDraftPocket(
                            uid = matchedExisting?.id ?: "smart_${k}_$idx",
                            existingPocketId = matchedExisting?.id,
                            name = matchedExisting?.name ?: cat.label,
                            iconKey = cat.key,
                            colorSlot = idx % 6,
                            weight = cat.defaultWeight,
                            amountText = "",
                            isManualOverride = false,
                        )
                    }
                }
                draftItems = if (maxBudget > 0L) {
                    rebalanceRemaining(refreshed, maxBudget, currencyConfig)
                        ?: distributeAutomatically(refreshed, maxBudget, currencyConfig)
                } else {
                    refreshed
                }
            }
        }
    }

    BentoModalSheetDialog(
        title = "Dompet Pintar",
        subtitle = "Tentukan budget, pilih kategori yang kamu butuhkan, lalu buat rekomendasi otomatis",
        headerIcon = Icons.Rounded.AutoFixHigh,
        closeButtonTestTag = "smart_pocket_close_button",
        onDismiss = onDismiss,
        footer = {
            if (!errorMsg.isNullOrBlank()) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }

            if (hasPreview && draftItems.isNotEmpty()) {
                Button(
                    onClick = {
                        val invalidItem = draftItems.firstOrNull { it.parsedAmountIdr(currencyConfig) <= 0L }
                        when {
                            maxBudget <= 0L -> errorMsg = "Masukkan maksimal budget lebih dari ${currencyConfig.symbol}0."
                            durationValue <= 0 -> errorMsg = "Masukkan rentang waktu minimal 1."
                            invalidItem != null -> errorMsg = "Nominal untuk '${invalidItem.name}' harus lebih dari ${currencyConfig.symbol}0."
                            totalAllocated > maxBudget + 100L -> errorMsg = "Total alokasi melebihi maksimal budget. Klik 'Sesuaikan Sisa' atau ubah nominal."
                            else -> {
                                val estimatedMonthly = if (updateProfileBudget) {
                                    ((maxBudget.toDouble() / totalDays.toDouble()) * 30.0).roundToLong()
                                } else {
                                    null
                                }
                                onApply(
                                    draftItems.map { item ->
                                        SmartPocketAllocationItem(
                                            existingPocketId = item.existingPocketId,
                                            name = item.name,
                                            iconKey = item.iconKey,
                                            colorSlot = item.colorSlot,
                                            targetAmount = item.parsedAmountIdr(currencyConfig),
                                        )
                                    },
                                    estimatedMonthly,
                                )
                                onDismiss()
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("smart_apply_button"),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = " Terapkan ke Kantong",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) {
        // Step 1: Budget to allocate
        OutlinedTextField(
            value = maxBudgetText,
            onValueChange = { input ->
                maxBudgetText = sanitizeCurrencyInput(input, currencyConfig)
                errorMsg = null
            },
            label = { Text("Budget yang Dialokasi (${currencyConfig.code})") },
            placeholder = { Text(if (currencyConfig.isBaseIdr) "Contoh: 3.000.000" else "Contoh: 200") },
            prefix = { Text("${currencyConfig.symbol} ") },
            visualTransformation = currencyTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = amountKeyboardType),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_max_budget_input"),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(1_000_000L, 2_000_000L, 3_000_000L, 5_000_000L).forEach { presetIdr ->
                FilterChip(
                    selected = maxBudget == presetIdr,
                    onClick = {
                        maxBudgetText = idrToEditableInput(presetIdr, currencyConfig)
                        errorMsg = null
                    },
                    label = { Text(FormatUtils.formatCompactMoney(presetIdr, currencyConfig)) },
                )
            }
        }

        // Time Horizon Section
        Text(
            text = "Rentang Waktu Pengeluaran",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = durationText,
                onValueChange = { input ->
                    durationText = input.filter { it.isDigit() }.take(3)
                    errorMsg = null
                },
                label = { Text("Jumlah") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(0.36f)
                    .testTag("smart_duration_value_input"),
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
                modifier = Modifier.weight(0.64f),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SmartPeriodUnit.entries.forEach { unit ->
                        val selected = periodUnit == unit
                        val tag = when (unit) {
                            SmartPeriodUnit.DAY -> "smart_unit_day"
                            SmartPeriodUnit.WEEK -> "smart_unit_week"
                            SmartPeriodUnit.MONTH -> "smart_unit_month"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(tag)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) bento.accentTile else bento.nestedCardSurface)
                                .clickable {
                                    periodUnit = unit
                                    errorMsg = null
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val dailyAvg = if (maxBudget > 0L && durationValue > 0) maxBudget / totalDays else 0L
            Text(
                text = if (durationValue > 0) {
                    "Periode: $durationValue ${periodUnit.label} ($totalDays hari) • Rata-rata ${FormatUtils.formatMoney(dailyAvg, currencyConfig)}/hari"
                } else {
                    "Masukkan angka durasi minimal 1 ${periodUnit.label}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = bento.highlightLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        HorizontalDivider(color = bento.borderSubtle)

        // Step 2: User-selected categories & icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pilih Kategori & Ikon (${selectedCategoryKeys.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ketuk lebih dari satu kategori sesuai kebutuhanmu",
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                )
            }
            OutlinedButton(
                onClick = { showCategoryCatalogDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("smart_add_category_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Lainnya (${POCKET_CATEGORY_CATALOG.size})", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Selected category tags preview with small rounded-square X remove buttons
        if (selectedCategoryKeys.isNotEmpty()) {
            BentoSurfaceCard(
                containerColor = bento.nestedCardSurface,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_selected_tags_preview"),
            ) {
                Text(
                    text = "Kategori Terpilih:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = bento.highlightLabel,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedCategoryKeys.forEach { key ->
                        val cat = pocketCategoryFor(key)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bento.accentTile.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, bento.highlightLabel.copy(alpha = 0.55f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = bento.highlightLabel,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Small rounded-square X button
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bento.cardSurface)
                                        .border(1.dp, bento.borderSubtle, RoundedCornerShape(6.dp))
                                        .clickable { toggleCategorySelection(key) }
                                        .testTag("smart_remove_tag_${key.lowercase()}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus ${cat.label}",
                                        tint = bento.expenseColor,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick-select clickable category & icon grid
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK_SELECT_KEYS.forEach { key ->
                val cat = pocketCategoryFor(key)
                val isSelected = key in selectedCategoryKeys
                FilterChip(
                    selected = isSelected,
                    onClick = { toggleCategorySelection(key) },
                    leadingIcon = {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text(cat.label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = bento.accentTile,
                        selectedLabelColor = bento.onAccentTile,
                        selectedLeadingIconColor = bento.onAccentTile,
                    ),
                    modifier = Modifier.testTag("smart_pick_cat_${key.lowercase()}"),
                )
            }
        }

        // Step 3: Buatkan Rekomendasi button
        Button(
            onClick = { buildAndGenerateRecommendation() },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = bento.heroStart,
                contentColor = bento.onHero,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("smart_generate_preview_button"),
        ) {
            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (hasPreview) " Perbarui Rekomendasi Alokasi" else " Buatkan Rekomendasi",
                fontWeight = FontWeight.Bold,
            )
        }

        // Step 4: Allocation Preview (Shown only after clicking "Buatkan Rekomendasi")
        if (hasPreview && draftItems.isNotEmpty()) {
            HorizontalDivider(color = bento.borderSubtle)

            BentoSurfaceCard(
                containerColor = bento.nestedCardSurface,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = "Preview Rekomendasi Alokasi Dana",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Edit nominal tiap kategori langsung atau kunci sebelum menyesuaikan sisa",
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Total Dialokasikan", style = MaterialTheme.typography.labelSmall, color = bento.mutedText)
                            Text(
                                text = FormatUtils.formatMoney(totalAllocated, currencyConfig),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalAllocated > maxBudget + 100L) bento.expenseColor else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (remaining >= 0L) "Sisa Budget" else "Kelebihan Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.mutedText,
                            )
                            Text(
                                text = FormatUtils.formatMoney(kotlin.math.abs(remaining), currencyConfig),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    kotlin.math.abs(remaining) <= 100L -> bento.incomeColor
                                    remaining > 0L -> bento.warningColor
                                    else -> bento.expenseColor
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (maxBudget <= 0L) {
                            errorMsg = "Masukkan maksimal budget lebih dari ${currencyConfig.symbol}0."
                        } else {
                            val rebalanced = rebalanceRemaining(draftItems, maxBudget, currencyConfig)
                            if (rebalanced == null) {
                                errorMsg = "Nominal yang dikunci melebihi maksimal budget. Turunkan nominal manual atau klik Reset."
                            } else {
                                draftItems = rebalanced
                                errorMsg = null
                            }
                        }
                    },
                    enabled = kotlin.math.abs(remaining) > 100L && draftItems.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("smart_rebalance_button"),
                ) {
                    Icon(Icons.Rounded.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Sesuaikan Sisa", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { buildAndGenerateRecommendation() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("smart_reset_button"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Reset", maxLines = 1)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                draftItems.forEachIndexed { index, item ->
                    val cat = pocketCategoryFor(item.iconKey)
                    val parsedIdr = item.parsedAmountIdr(currencyConfig)
                    val pct = if (maxBudget > 0L) {
                        ((parsedIdr.toDouble() / maxBudget.toDouble()) * 100).toInt().coerceIn(0, 999)
                    } else 0
                    val dailyAmt = if (totalDays > 0) parsedIdr / totalDays else 0L

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = bento.nestedCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (item.isManualOverride) bento.highlightLabel.copy(alpha = 0.65f) else bento.borderSubtle.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bento.cardSurface),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = item.name,
                                        tint = bento.highlightLabel,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "$pct% • ~${FormatUtils.formatCompactMoney(dailyAmt, currencyConfig)}/hr • " +
                                            if (item.existingPocketId != null) "Kantong Ada" else "Kantong Baru",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bento.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        draftItems = draftItems.mapIndexed { idx, d ->
                                            if (idx == index) d.copy(isManualOverride = !d.isManualOverride) else d
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (item.isManualOverride) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                        contentDescription = if (item.isManualOverride) "Buka kunci nominal" else "Kunci nominal",
                                        tint = if (item.isManualOverride) bento.highlightLabel else bento.mutedText,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                IconButton(
                                    onClick = { toggleCategorySelection(item.iconKey) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus dari alokasi",
                                        tint = bento.mutedText,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = item.amountText,
                                onValueChange = { raw ->
                                    val clean = sanitizeCurrencyInput(raw, currencyConfig)
                                    draftItems = draftItems.mapIndexed { idx, d ->
                                        if (idx == index) {
                                            d.copy(amountText = clean, isManualOverride = true)
                                        } else {
                                            d
                                        }
                                    }
                                    errorMsg = null
                                },
                                label = { Text("Alokasi ${item.name} (${currencyConfig.code})") },
                                prefix = { Text("${currencyConfig.symbol} ") },
                                visualTransformation = currencyTransformation,
                                keyboardOptions = KeyboardOptions(keyboardType = amountKeyboardType),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("smart_item_amount_${index}"),
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val estimatedMonthly = if (maxBudget > 0L && durationValue > 0) {
                    ((maxBudget.toDouble() / totalDays.toDouble()) * 30.0).roundToLong()
                } else 0L

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Perbarui Anggaran Bulanan",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Set target bulanan menjadi ${FormatUtils.formatMoney(estimatedMonthly, currencyConfig)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                    Switch(
                        checked = updateProfileBudget,
                        onCheckedChange = { updateProfileBudget = it },
                    )
                }
            }
        }
    }

    if (showCategoryCatalogDialog) {
        val currentKeys = selectedCategoryKeys.toSet()
        CategorySearchPickerDialog(
            selectedKey = null,
            excludedKeys = currentKeys,
            title = "Tambah Kategori ke Dompet Pintar",
            onDismiss = { showCategoryCatalogDialog = false },
            onSelect = { selected ->
                toggleCategorySelection(selected.key)
                showCategoryCatalogDialog = false
            },
        )
    }
}

private fun distributeAutomatically(
    items: List<SmartDraftPocket>,
    totalBudgetIdr: Long,
    currencyConfig: CurrencyDisplayConfig,
): List<SmartDraftPocket> {
    if (items.isEmpty() || totalBudgetIdr <= 0L) return items
    val weights = items.map { max(1, it.weight) }
    val weightSum = weights.sum().toDouble()
    val roundingStep = if (totalBudgetIdr >= 50_000L) 1_000L else 100L

    var allocatedSoFar = 0L
    return items.mapIndexed { index, item ->
        val amountIdr = if (index == items.lastIndex) {
            (totalBudgetIdr - allocatedSoFar).coerceAtLeast(0L)
        } else {
            val raw = (totalBudgetIdr.toDouble() * (weights[index] / weightSum))
            val rounded = ((raw / roundingStep).roundToLong() * roundingStep)
                .coerceIn(0L, (totalBudgetIdr - allocatedSoFar).coerceAtLeast(0L))
            allocatedSoFar += rounded
            rounded
        }
        item.copy(
            amountText = idrToEditableInput(amountIdr, currencyConfig),
            isManualOverride = false,
        )
    }
}

private fun rebalanceRemaining(
    items: List<SmartDraftPocket>,
    totalBudgetIdr: Long,
    currencyConfig: CurrencyDisplayConfig,
): List<SmartDraftPocket>? {
    if (items.isEmpty() || totalBudgetIdr <= 0L) return items

    val lockedItems = items.filter { it.isManualOverride }
    val unlockedIndices = items.mapIndexedNotNull { index, item ->
        if (!item.isManualOverride) index else null
    }

    if (unlockedIndices.isEmpty()) {
        val currentSum = items.sumOf { it.parsedAmountIdr(currencyConfig) }.coerceAtLeast(1L)
        var running = 0L
        val step = if (totalBudgetIdr >= 50_000L) 1_000L else 100L
        return items.mapIndexed { idx, item ->
            val nextAmountIdr = if (idx == items.lastIndex) {
                (totalBudgetIdr - running).coerceAtLeast(0L)
            } else {
                val parsed = item.parsedAmountIdr(currencyConfig)
                val scaled = ((totalBudgetIdr.toDouble() * (parsed.toDouble() / currentSum.toDouble())) / step).roundToLong() * step
                val clamped = scaled.coerceIn(0L, (totalBudgetIdr - running).coerceAtLeast(0L))
                running += clamped
                clamped
            }
            item.copy(
                amountText = idrToEditableInput(nextAmountIdr, currencyConfig),
                isManualOverride = false,
            )
        }
    }

    val lockedSum = lockedItems.sumOf { it.parsedAmountIdr(currencyConfig) }
    val remaining = totalBudgetIdr - lockedSum
    if (remaining < -100L) return null
    val safeRemaining = remaining.coerceAtLeast(0L)

    val unlockedWeightSum = unlockedIndices.sumOf { max(1, items[it].weight) }.toDouble()
    val roundingStep = if (safeRemaining >= 50_000L) 1_000L else 100L
    var distributed = 0L

    return items.mapIndexed { index, item ->
        if (item.isManualOverride) {
            item
        } else if (index == unlockedIndices.last()) {
            val finalShare = (safeRemaining - distributed).coerceAtLeast(0L)
            item.copy(amountText = idrToEditableInput(finalShare, currencyConfig))
        } else {
            val raw = safeRemaining.toDouble() * (max(1, item.weight) / unlockedWeightSum)
            val rounded = ((raw / roundingStep).roundToLong() * roundingStep)
                .coerceIn(0L, (safeRemaining - distributed).coerceAtLeast(0L))
            distributed += rounded
            item.copy(amountText = idrToEditableInput(rounded, currencyConfig))
        }
    }
}
