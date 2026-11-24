package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.CategorySearchPickerDialog
import com.example.ui.components.CategorySelectField
import com.example.ui.components.WalletLogoBadge
import com.example.ui.components.addDeltaInActiveCurrency
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.pocketCategoryFor
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
    initialType: TransactionType,
    existingTransaction: TransactionEntity?,
    pockets: List<PocketEntity>,
    wallets: List<WalletEntity>,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onOpenReceiptScanner: (() -> Unit)? = null,
    onSave: (
        existingId: String?,
        title: String,
        note: String,
        amount: Long,
        type: TransactionType,
        pocketId: String?,
        walletId: String?,
        categoryKey: String,
        categoryLabel: String,
        locationName: String,
        occurredAtMillis: Long,
    ) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current

    var txType by rememberSaveable(existingTransaction?.id, initialType) {
        mutableStateOf(existingTransaction?.txType ?: initialType)
    }
    var amountText by rememberSaveable(existingTransaction?.id, currencyConfig.code) {
        mutableStateOf(
            if (existingTransaction != null && existingTransaction.amount > 0L) {
                idrToEditableInput(existingTransaction.amount, currencyConfig)
            } else {
                ""
            }
        )
    }
    var title by rememberSaveable(existingTransaction?.id) { mutableStateOf(existingTransaction?.title ?: "") }
    var locationName by rememberSaveable(existingTransaction?.id) {
        mutableStateOf(existingTransaction?.locationName ?: "")
    }
    var occurredAtMillis by rememberSaveable(existingTransaction?.id) {
        mutableLongStateOf(existingTransaction?.occurredAtMillis ?: System.currentTimeMillis())
    }
    var note by rememberSaveable(existingTransaction?.id) { mutableStateOf(existingTransaction?.note ?: "") }

    val initialCategoryKey = existingTransaction?.categoryKey?.takeIf { it.isNotBlank() }
        ?: if (existingTransaction != null && existingTransaction.pocketId.isNotBlank()) {
            pockets.firstOrNull { it.id == existingTransaction.pocketId }?.iconKey ?: "OTHER"
        } else if ((existingTransaction?.txType ?: initialType) == TransactionType.INCOME) {
            "SALARY"
        } else {
            "FOOD"
        }

    var selectedCategoryKey by rememberSaveable(existingTransaction?.id) {
        mutableStateOf(initialCategoryKey)
    }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }

    val initialPocketId = existingTransaction?.pocketId
        ?: pockets.firstOrNull { it.iconKey.equals(initialCategoryKey, ignoreCase = true) }?.id
        ?: ""
    var selectedPocketId by rememberSaveable(existingTransaction?.id) {
        mutableStateOf(initialPocketId)
    }
    val defaultWalletId = (wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull())?.id.orEmpty()
    var selectedWalletId by rememberSaveable(existingTransaction?.id) {
        val candidate = existingTransaction?.walletId?.takeIf { id -> wallets.any { it.id == id } }
        mutableStateOf(candidate ?: defaultWalletId)
    }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)

    BentoModalSheetDialog(
        title = if (existingTransaction == null) {
            if (txType == TransactionType.INCOME) strings.txIncomeTitle else strings.txExpenseTitle
        } else {
            "Ubah Transaksi"
        },
        subtitle = "Pilih satu dompet sumber/tujuan & alokasikan kantong bila perlu",
        headerIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
        closeButtonTestTag = "tx_sheet_close_button",
        onDismiss = onDismiss,
        footer = {
            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }

            Button(
                onClick = {
                    val amountIdr = editableInputToIdr(amountText, currencyConfig)
                    val categoryOption = pocketCategoryFor(selectedCategoryKey)
                    val resolvedWalletId = selectedWalletId.takeIf { it.isNotBlank() } ?: defaultWalletId
                    when {
                        amountIdr <= 0L -> validationError = "Masukkan nominal lebih dari ${currencyConfig.symbol}0."
                        title.isBlank() -> validationError = "Judul transaksi tidak boleh kosong."
                        wallets.isNotEmpty() && resolvedWalletId.isBlank() ->
                            validationError = "Pilih salah satu dompet."
                        else -> {
                            onSave(
                                existingTransaction?.id,
                                title.trim(),
                                note.trim(),
                                amountIdr,
                                txType,
                                selectedPocketId.takeIf { it.isNotBlank() },
                                resolvedWalletId.takeIf { it.isNotBlank() },
                                categoryOption.key,
                                categoryOption.label,
                                if (txType == TransactionType.EXPENSE) locationName.trim() else "",
                                occurredAtMillis,
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
                    .testTag("tx_save_button"),
            ) {
                Text(
                    text = "Simpan Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    ) {

        if (existingTransaction == null && onOpenReceiptScanner != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_open_receipt_scanner")
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismiss()
                        onOpenReceiptScanner()
                    },
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
                border = BorderStroke(1.dp, bento.highlightLabel.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bento.accentTile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = "Scan Struk",
                            tint = bento.onAccentTile,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Punya struk belanja / bukti transfer?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Scan otomatis pakai kamera atau upload dari galeri",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                }
            }
        }

        // Type toggle
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val isExpense = txType == TransactionType.EXPENSE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_type_expense")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isExpense) bento.accentTile else bento.nestedCardSurface)
                        .clickable {
                            txType = TransactionType.EXPENSE
                            if (selectedCategoryKey in setOf("SALARY", "FREELANCE", "BUSINESS")) {
                                selectedCategoryKey = "FOOD"
                                val matching = pockets.firstOrNull { it.iconKey == "FOOD" }
                                selectedPocketId = matching?.id ?: ""
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Pengeluaran",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tx_type_income")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isExpense) bento.incomeColor else bento.nestedCardSurface)
                        .clickable {
                            txType = TransactionType.INCOME
                            if (selectedCategoryKey == "FOOD" && existingTransaction == null) {
                                selectedCategoryKey = "SALARY"
                                val matching = pockets.firstOrNull { it.iconKey == "SALARY" }
                                selectedPocketId = matching?.id ?: ""
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Pemasukan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExpense) bento.onHero else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                amountText = sanitizeCurrencyInput(input, currencyConfig)
                validationError = null
            },
            label = { Text("Nominal (${currencyConfig.code})") },
            placeholder = { Text(if (currencyConfig.isBaseIdr) "Contoh: 35.000" else "Contoh: 25") },
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
                .testTag("tx_amount_input"),
        )

        // Quick amount add chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(10_000L, 25_000L, 50_000L, 100_000L).forEach { deltaIdr ->
                FilterChip(
                    selected = false,
                    onClick = {
                        amountText = addDeltaInActiveCurrency(amountText, deltaIdr, currencyConfig)
                        validationError = null
                    },
                    label = { Text("+${FormatUtils.formatCompactMoney(deltaIdr, currencyConfig)}") },
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it.take(80)
                validationError = null
            },
            label = { Text("Judul Transaksi") },
            placeholder = {
                Text(if (txType == TransactionType.EXPENSE) "Contoh: Makan Siang / Belanja" else "Contoh: Gaji Bulanan / Bonus")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_title_input"),
        )

        // Full Category Selector (same complete catalog as Tambah Kantong + Search)
        CategorySelectField(
            selectedKey = selectedCategoryKey,
            onClick = { showCategoryPicker = true },
            label = if (txType == TransactionType.EXPENSE) "Kategori Pengeluaran" else "Kategori Pemasukan",
            helperText = "Semua kategori kantong tersedia • Ketuk untuk cari",
            testTag = "tx_category_select_field",
        )

        // Editable Date & Time (for both Pengeluaran & Pemasukan)
        DateTimePickerRow(
            occurredAtMillis = occurredAtMillis,
            onDateTimeChange = { occurredAtMillis = it },
            dateLabel = strings.txDateLabel,
            timeLabel = strings.txTimeLabel,
        )

        // Place Name input ONLY for Pengeluaran (not Pemasukan)
        if (txType == TransactionType.EXPENSE) {
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it.take(100) },
                label = { Text(strings.txPlaceNameLabel) },
                placeholder = { Text(strings.txPlaceNamePlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_location_input"),
            )
        }

        // Detailed Note
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(500) },
            label = { Text("Catatan Detail (Beli apa saja / sumber dana)") },
            placeholder = {
                Text("Contoh: Nasi padang rendang, es teh, parkir")
            },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_note_input"),
        )

        if (wallets.isNotEmpty()) {
            Text(
                text = strings.txWalletLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                wallets.forEach { wallet ->
                    val defaultTag = if (wallet.isDefault) " • Utama" else ""
                    FilterChip(
                        selected = selectedWalletId == wallet.id,
                        onClick = {
                            selectedWalletId = wallet.id
                            validationError = null
                        },
                        leadingIcon = {
                            WalletLogoBadge(
                                providerCode = wallet.providerCode,
                                walletType = wallet.type,
                                shortName = wallet.providerShortName,
                                size = 18.dp,
                            )
                        },
                        label = {
                            Text("${wallet.name}$defaultTag (${FormatUtils.formatCompactMoney(wallet.balance, currencyConfig)})")
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (txType == TransactionType.EXPENSE) {
                    "Diambil dari Kantong Mana?"
                } else {
                    "Ditambahkan ke Kantong Mana?"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedPocketId.isBlank(),
                    onClick = { selectedPocketId = "" },
                    label = { Text("Tidak Ada di Kantong (Riwayat Saja)") },
                )
                pockets.forEach { pocket ->
                    val hint = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                        "Sisa ${FormatUtils.formatCompactMoney(pocket.remainingAmount, currencyConfig)}"
                    } else {
                        "${FormatUtils.formatCompactMoney(pocket.currentAmount, currencyConfig)}/${FormatUtils.formatCompactMoney(pocket.targetAmount, currencyConfig)}"
                    }
                    FilterChip(
                        selected = selectedPocketId == pocket.id,
                        onClick = { selectedPocketId = pocket.id },
                        label = { Text("${pocket.name} ($hint)") },
                    )
                }
            }
            Text(
                text = if (selectedPocketId.isNotBlank()) {
                    "Alokasi kantong terpilih akan ikut diperbarui bersama saldo Dompet Utama."
                } else {
                    "Menu Kantong hanya untuk alokasi. Jika tidak ada kantong yang sesuai, transaksi tetap dicatat ke riwayat & Dompet Utama."
                },
                style = MaterialTheme.typography.labelSmall,
                color = bento.mutedText,
            )
        }
    }

    if (showCategoryPicker) {
        CategorySearchPickerDialog(
            selectedKey = selectedCategoryKey,
            title = if (txType == TransactionType.EXPENSE) "Pilih Kategori Pengeluaran" else "Pilih Kategori Pemasukan",
            onDismiss = { showCategoryPicker = false },
            onSelect = { chosen ->
                selectedCategoryKey = chosen.key
                val matchingPocket = pockets.firstOrNull { it.iconKey.equals(chosen.key, ignoreCase = true) }
                if (matchingPocket != null) {
                    selectedPocketId = matchingPocket.id
                }
                showCategoryPicker = false
            },
        )
    }
}

@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    wallet: WalletEntity? = null,
) {
    val bento = MaterialTheme.bentoColors
    val isIncome = transaction.txType == TransactionType.INCOME
    val amountColor = if (isIncome) bento.incomeColor else bento.expenseColor
    val categoryInfo = pocketCategoryFor(transaction.categoryKey)

    BentoModalSheetDialog(
        title = "Detail Transaksi",
        subtitle = FormatUtils.formatDateTimeShort(transaction.occurredAtMillis),
        headerIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
        closeButtonTestTag = "tx_detail_drag_handle",
        onDismiss = onDismiss,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tx_detail_delete"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Hapus",
                        tint = bento.expenseColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" Hapus", color = bento.expenseColor)
                }
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tx_detail_edit"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Ubah",
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" Ubah", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(amountColor.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = transaction.txType.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = amountColor,
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bento.nestedCardSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = null,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = transaction.categoryLabel.ifBlank { categoryInfo.label },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Text(
            text = transaction.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "${if (isIncome) "+" else "-"}${FormatUtils.formatMoney(transaction.amount, currencyConfig, withSpace = true)}",
            style = MaterialTheme.typography.displayMedium,
            color = amountColor,
        )

        if (!isIncome && transaction.locationName.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bento.nestedCardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Tempat: ${transaction.locationName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (transaction.walletName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WalletLogoBadge(
                    providerCode = wallet?.providerCode ?: "",
                    walletType = wallet?.type ?: if (transaction.walletName.contains("tunai", ignoreCase = true)) WalletType.CASH else WalletType.BANK,
                    shortName = wallet?.providerShortName ?: transaction.walletName,
                    size = 24.dp,
                )
                Text(
                    text = "Dompet: ${transaction.walletName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = bento.incomeColor,
                )
            }
        }

        Text(
            text = if (transaction.pocketName.isNotBlank()) {
                "Alokasi Kantong: ${transaction.pocketName}"
            } else {
                "Alokasi Kantong: Tidak terhubung (Riwayat saja)"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (transaction.pocketName.isNotBlank()) bento.highlightLabel else bento.mutedText,
        )

        HorizontalDivider(color = bento.borderSubtle)

        Text(
            text = "Catatan Detail Pembelian / Sumber",
            style = MaterialTheme.typography.labelLarge,
            color = bento.mutedText,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = transaction.note.ifBlank { "Tidak ada catatan tambahan." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerRow(
    occurredAtMillis: Long,
    onDateTimeChange: (Long) -> Unit,
    dateLabel: String = "Tanggal",
    timeLabel: String = "Jam",
) {
    val bento = MaterialTheme.bentoColors
    val locale = remember { Locale.Builder().setLanguage("id").setRegion("ID").build() }
    val dateFormatted = remember(occurredAtMillis) {
        SimpleDateFormat("EEE, d MMM yyyy", locale).format(Date(occurredAtMillis))
    }
    val timeFormatted = remember(occurredAtMillis) {
        SimpleDateFormat("HH:mm", locale).format(Date(occurredAtMillis))
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1.3f)
                .testTag("tx_date_picker_button")
                .clip(RoundedCornerShape(15.dp))
                .clickable { showDatePicker = true },
            shape = RoundedCornerShape(15.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = dateLabel,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(0.9f)
                .testTag("tx_time_picker_button")
                .clip(RoundedCornerShape(15.dp))
                .clickable { showTimePicker = true },
            shape = RoundedCornerShape(15.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = timeLabel,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val localCal = remember(occurredAtMillis) {
            Calendar.getInstance().apply { timeInMillis = occurredAtMillis }
        }
        val initialUtcMillis = remember(occurredAtMillis) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0,
                )
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtc = datePickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtc
                            }
                            val merged = Calendar.getInstance().apply {
                                timeInMillis = occurredAtMillis
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            onDateTimeChange(merged.timeInMillis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val localCal = remember(occurredAtMillis) {
            Calendar.getInstance().apply { timeInMillis = occurredAtMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = localCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = localCal.get(Calendar.MINUTE),
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Jam Transaksi") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val merged = Calendar.getInstance().apply {
                            timeInMillis = occurredAtMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateTimeChange(merged.timeInMillis)
                        showTimePicker = false
                    },
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            },
        )
    }
}
