package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PaydayFrequency
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.CategorySearchPickerDialog
import com.example.ui.components.CategorySelectField
import com.example.ui.components.PaydayScheduleSelector
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.pocketCategoryFor
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.theme.bentoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PocketEditorSheet(
    existingPocket: PocketEntity?,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        name: String,
        mode: PocketMode,
        targetAmount: Long,
        currentAmount: Long,
        iconKey: String,
        colorSlot: Int,
        featured: Boolean,
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val bento = MaterialTheme.bentoColors

    var name by rememberSaveable(existingPocket?.id) { mutableStateOf(existingPocket?.name ?: "") }
    var mode by rememberSaveable(existingPocket?.id) { mutableStateOf(existingPocket?.pocketMode ?: PocketMode.BUDGET_LIMIT) }
    var targetText by rememberSaveable(existingPocket?.id, currencyConfig.code) {
        mutableStateOf(idrToEditableInput(existingPocket?.targetAmount ?: 1_500_000L, currencyConfig))
    }
    var currentText by rememberSaveable(existingPocket?.id, currencyConfig.code) {
        mutableStateOf(idrToEditableInput(existingPocket?.currentAmount ?: 0L, currencyConfig))
    }
    var iconKey by rememberSaveable(existingPocket?.id) { mutableStateOf(existingPocket?.iconKey ?: "FOOD") }
    var colorSlot by rememberSaveable(existingPocket?.id) { mutableIntStateOf(existingPocket?.colorSlot ?: 0) }
    var featured by rememberSaveable(existingPocket?.id) { mutableStateOf(existingPocket?.featured ?: false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)
    val amountKeyboardType = if (!currencyConfig.isBaseIdr && currencyConfig.decimalDigits > 0) {
        KeyboardType.Decimal
    } else {
        KeyboardType.Number
    }

    BentoModalSheetDialog(
        title = if (existingPocket == null) "Buat Target Kantong" else "Atur Kantong",
        subtitle = "Sesuaikan nama, batas target, kategori, & warna kartu",
        headerIcon = Icons.Rounded.AccountBalanceWallet,
        closeButtonTestTag = "pocket_sheet_close_button",
        onDismiss = onDismiss,
        footer = {
            if (!errorMsg.isNullOrBlank()) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (existingPocket != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(existingPocket.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("pocket_delete_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Hapus Kantong",
                            tint = bento.expenseColor,
                        )
                    }
                }

                Button(
                    onClick = {
                        val target = editableInputToIdr(targetText, currencyConfig)
                        val current = editableInputToIdr(currentText, currencyConfig)
                        when {
                            name.isBlank() -> errorMsg = "Nama kantong wajib diisi."
                            target <= 0L -> errorMsg = "Nominal target harus lebih dari ${currencyConfig.symbol}0."
                            else -> {
                                onSave(
                                    existingPocket?.id,
                                    name.trim(),
                                    mode,
                                    target,
                                    current,
                                    iconKey,
                                    colorSlot,
                                    featured,
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
                        .weight(1f)
                        .height(52.dp)
                        .testTag("pocket_save_button"),
                ) {
                    Text(
                        text = "Simpan Kantong",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) {

        // Mode selector: Budget Limit (Regress) vs Savings Goal (Progress)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val isBudget = mode == PocketMode.BUDGET_LIMIT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pocket_mode_budget")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isBudget) bento.accentTile else bento.nestedCardSurface)
                        .clickable { mode = PocketMode.BUDGET_LIMIT }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Batas Pengeluaran",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isBudget) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pocket_mode_savings")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isBudget) bento.accentTile else bento.nestedCardSurface)
                        .clickable { mode = PocketMode.SAVINGS_GOAL }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Target Tabungan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!isBudget) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Text(
            text = if (mode == PocketMode.BUDGET_LIMIT) {
                "Mode Regress: Sisa anggaran berkurang setiap kali pengeluaran dicatat ke kantong ini."
            } else {
                "Mode Progress: Progres tabungan bertambah setiap kali pemasukan/alokasi dicatat ke kantong ini."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = bento.mutedText,
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(50)
                errorMsg = null
            },
            label = { Text("Nama Target / Kantong") },
            placeholder = { Text("Contoh: Makan/Minum, Dana Darurat") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pocket_name_input"),
        )

        OutlinedTextField(
            value = targetText,
            onValueChange = {
                targetText = sanitizeCurrencyInput(it, currencyConfig)
                errorMsg = null
            },
            label = {
                Text(if (mode == PocketMode.BUDGET_LIMIT) "Batas Maksimal (${currencyConfig.code})" else "Target Tabungan (${currencyConfig.code})")
            },
            prefix = { Text("${currencyConfig.symbol} ") },
            visualTransformation = currencyTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = amountKeyboardType),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pocket_target_input"),
        )

        OutlinedTextField(
            value = currentText,
            onValueChange = {
                currentText = sanitizeCurrencyInput(it, currencyConfig)
                errorMsg = null
            },
            label = {
                Text(if (mode == PocketMode.BUDGET_LIMIT) "Sudah Terpakai (${currencyConfig.code})" else "Sudah Terkumpul (${currencyConfig.code})")
            },
            prefix = { Text("${currencyConfig.symbol} ") },
            visualTransformation = currencyTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = amountKeyboardType),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pocket_current_input"),
        )

        CategorySelectField(
            selectedKey = iconKey,
            onClick = { showCategoryPicker = true },
        )

        Text(
            text = "Warna Kartu",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            bento.pocketColors.forEachIndexed { idx, color ->
                val selected = colorSlot == idx
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { colorSlot = idx }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Jadikan Kantong Unggulan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tampil paling atas dalam ukuran besar di Beranda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.mutedText,
                )
            }
            Switch(
                checked = featured,
                onCheckedChange = { featured = it },
            )
        }
    }

    if (showCategoryPicker) {
        CategorySearchPickerDialog(
            selectedKey = iconKey,
            onDismiss = { showCategoryPicker = false },
            onSelect = { selected ->
                val prevAutoLabel = pocketCategoryFor(iconKey).label
                if (existingPocket == null && (name.isBlank() || name == prevAutoLabel)) {
                    name = selected.label
                }
                iconKey = selected.key
                showCategoryPicker = false
            },
        )
    }
}

@Composable
fun PaydaySettingsSheet(
    currentName: String,
    currentPaydayFrequency: PaydayFrequency,
    currentPaydayDay: Int,
    currentPaydayWeekday: Int,
    currentPaydayHour: Int,
    currentPaydayMinute: Int,
    currentMonthlyBudget: Long,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        paydayFrequency: PaydayFrequency,
        paydayDay: Int,
        paydayWeekday: Int,
        paydayHour: Int,
        paydayMinute: Int,
        monthlyBudget: Long,
    ) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    var frequency by rememberSaveable { mutableStateOf(currentPaydayFrequency) }
    var paydayDay by rememberSaveable { mutableIntStateOf(currentPaydayDay.coerceIn(1, 31)) }
    var paydayWeekday by rememberSaveable { mutableIntStateOf(currentPaydayWeekday.coerceIn(1, 7)) }
    var paydayHour by rememberSaveable { mutableIntStateOf(currentPaydayHour.coerceIn(0, 23)) }
    var paydayMinute by rememberSaveable { mutableIntStateOf(currentPaydayMinute.coerceIn(0, 59)) }
    var budgetText by rememberSaveable(currencyConfig.code) {
        mutableStateOf(idrToEditableInput(currentMonthlyBudget, currencyConfig))
    }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)

    BentoModalSheetDialog(
        title = "Atur Siklus Gajian & Anggaran",
        subtitle = "Pilih jadwal gajian Harian, Mingguan, atau Bulanan",
        headerIcon = Icons.Rounded.CalendarMonth,
        closeButtonTestTag = "payday_sheet_close_button",
        onDismiss = onDismiss,
        footer = {
            Button(
                onClick = {
                    val budget = editableInputToIdr(budgetText, currencyConfig)
                    onSave(
                        currentName,
                        frequency,
                        paydayDay,
                        paydayWeekday,
                        paydayHour,
                        paydayMinute,
                        budget,
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = bento.accentTile,
                    contentColor = bento.onAccentTile,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("payday_save_button"),
            ) {
                Text(
                    text = "Simpan Pengaturan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    ) {
        PaydayScheduleSelector(
            frequency = frequency,
            onFrequencyChange = { frequency = it },
            paydayDay = paydayDay,
            onPaydayDayChange = { paydayDay = it },
            paydayWeekday = paydayWeekday,
            onPaydayWeekdayChange = { paydayWeekday = it },
            paydayHour = paydayHour,
            paydayMinute = paydayMinute,
            onPaydayTimeChange = { h, m ->
                paydayHour = h
                paydayMinute = m
            },
        )

        OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = sanitizeCurrencyInput(it, currencyConfig) },
            label = { Text("Target Anggaran Bulanan (${currencyConfig.code})") },
            prefix = { Text("${currencyConfig.symbol} ") },
            visualTransformation = currencyTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (!currencyConfig.isBaseIdr && currencyConfig.decimalDigits > 0) {
                    KeyboardType.Decimal
                } else {
                    KeyboardType.Number
                }
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_budget_input"),
        )
    }
}
