package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.data.wallet.IndonesianWalletCatalog
import com.example.data.wallet.WalletProviderOption
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.WalletLogoBadge
import com.example.ui.components.addDeltaInActiveCurrency
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

fun walletIconFor(type: WalletType): ImageVector = when (type) {
    WalletType.CASH -> Icons.Rounded.Payments
    WalletType.EWALLET -> Icons.Rounded.PhoneAndroid
    WalletType.BANK -> Icons.Rounded.AccountBalance
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletEditorSheet(
    existingWallet: WalletEntity?,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        name: String,
        walletType: WalletType,
        providerCode: String,
        providerShortName: String,
        providerCompanyName: String,
        accountNumberHint: String,
        balance: Long,
        colorSlot: Int,
        isDefault: Boolean,
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val bento = MaterialTheme.bentoColors

    var walletType by rememberSaveable(existingWallet?.id) {
        mutableStateOf(existingWallet?.type ?: WalletType.BANK)
    }
    val initialProvider = remember(existingWallet?.id, walletType) {
        existingWallet?.let { IndonesianWalletCatalog.findByCode(it.providerCode) }
            ?: when (walletType) {
                WalletType.CASH -> IndonesianWalletCatalog.cashOption
                WalletType.EWALLET -> IndonesianWalletCatalog.allEWalletProviders.first()
                WalletType.BANK -> IndonesianWalletCatalog.allIndonesianBanks.first()
            }
    }

    var providerCode by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.providerCode ?: initialProvider.code) }
    var providerShortName by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.providerShortName ?: initialProvider.shortName) }
    var providerCompanyName by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.providerCompanyName ?: initialProvider.companyName) }
    var name by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.name ?: initialProvider.defaultWalletName) }
    var accountNumberHint by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.accountNumberHint ?: "") }
    var balanceText by rememberSaveable(existingWallet?.id, currencyConfig.code) {
        mutableStateOf(
            if (existingWallet != null && existingWallet.balance > 0L) {
                idrToEditableInput(existingWallet.balance, currencyConfig)
            } else {
                ""
            }
        )
    }
    var colorSlot by rememberSaveable(existingWallet?.id) { mutableIntStateOf(existingWallet?.colorSlot ?: 0) }
    var isDefault by rememberSaveable(existingWallet?.id) { mutableStateOf(existingWallet?.isDefault ?: false) }
    var showProviderPicker by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)
    val compactPrimaryLabel = when (walletType) {
        WalletType.BANK -> "Bank: $providerShortName"
        WalletType.EWALLET -> "E-Wallet: $providerShortName"
        WalletType.CASH -> "Uang Tunai"
    }
    val compactSubLabel = when (walletType) {
        WalletType.CASH -> "Kas fisik di dompet & saku"
        else -> providerCompanyName
    }

    BentoModalSheetDialog(
        title = if (existingWallet == null) "Tambah Dompet Baru" else "Atur Dompet",
        subtitle = "Tunai, E-Wallet & seluruh Bank Indonesia",
        headerBadge = {
            WalletLogoBadge(
                providerCode = providerCode,
                walletType = walletType,
                shortName = providerShortName,
                size = 40.dp,
            )
        },
        closeButtonTestTag = "wallet_sheet_drag_handle",
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
                if (existingWallet != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(existingWallet.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("wallet_delete_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Hapus Dompet",
                            tint = bento.expenseColor,
                        )
                    }
                }

                Button(
                    onClick = {
                        val parsedBalance = if (balanceText.isBlank()) 0L
                        else editableInputToIdr(balanceText, currencyConfig)
                        if (name.isBlank()) {
                            errorMsg = "Nama dompet wajib diisi."
                        } else {
                            onSave(
                                existingWallet?.id,
                                name.trim(),
                                walletType,
                                providerCode,
                                providerShortName,
                                providerCompanyName,
                                accountNumberHint.trim(),
                                parsedBalance,
                                colorSlot,
                                isDefault,
                            )
                            onDismiss()
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
                        .testTag("wallet_save_button"),
                ) {
                    Text(
                        text = "Simpan Dompet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) {
        Text(
            text = "Jenis Dompet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WalletType.entries.forEach { typeOption ->
                    val selected = walletType == typeOption
                    val label = when (typeOption) {
                        WalletType.CASH -> "Tunai"
                        WalletType.EWALLET -> "E-Wallet"
                        WalletType.BANK -> "Bank"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wallet_type_${typeOption.name.lowercase()}")
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) bento.accentTile else bento.nestedCardSurface)
                            .clickable {
                                walletType = typeOption
                                val nextProvider = IndonesianWalletCatalog.providersFor(typeOption).first()
                                providerCode = nextProvider.code
                                providerShortName = nextProvider.shortName
                                providerCompanyName = nextProvider.companyName
                                name = if (typeOption == WalletType.CASH) {
                                    if (existingWallet?.type == WalletType.CASH && existingWallet.name.isNotBlank()) {
                                        existingWallet.name
                                    } else {
                                        "Dompet Tunai"
                                    }
                                } else {
                                    nextProvider.defaultWalletName
                                }
                                errorMsg = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Compact Minimalist Provider Preview / Select Field (for Cash, E-Wallet, and Bank)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wallet_provider_select_field")
                .clip(RoundedCornerShape(15.dp))
                .then(
                    if (walletType != WalletType.CASH) Modifier.clickable { showProviderPicker = true }
                    else Modifier
                ),
            shape = RoundedCornerShape(15.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.65f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WalletLogoBadge(
                    providerCode = providerCode,
                    walletType = walletType,
                    shortName = providerShortName,
                    size = 38.dp,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = compactPrimaryLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = compactSubLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (walletType != WalletType.CASH) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Pilih Penyedia",
                        tint = bento.mutedText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        val isNameLocked = walletType != WalletType.CASH
        OutlinedTextField(
            value = name,
            onValueChange = {
                if (!isNameLocked) {
                    name = it.take(50)
                    errorMsg = null
                }
            },
            enabled = !isNameLocked,
            readOnly = isNameLocked,
            label = {
                Text(if (isNameLocked) "Nama Bank / E-Wallet (Otomatis dari Pilihan)" else "Nama Dompet Tunai")
            },
            placeholder = { Text("Contoh: Dompet Tunai Saku") },
            supportingText = if (isNameLocked) {
                {
                    Text(
                        text = "Nama dikunci sesuai penyedia yang dipilih di atas (ketuk kotak di atas untuk ganti).",
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = bento.mutedText,
                disabledBorderColor = bento.borderSubtle.copy(alpha = 0.6f),
                disabledContainerColor = bento.nestedCardSurface.copy(alpha = 0.65f),
                disabledLabelColor = bento.mutedText,
                disabledSupportingTextColor = bento.mutedText,
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wallet_name_input"),
        )

        OutlinedTextField(
            value = balanceText,
            onValueChange = { raw ->
                balanceText = sanitizeCurrencyInput(raw, currencyConfig)
                errorMsg = null
            },
            label = { Text("Saldo Dompet (${currencyConfig.code})") },
            placeholder = { Text("0") },
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
                .testTag("wallet_balance_input"),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(100_000L, 500_000L, 1_000_000L, 5_000_000L).forEach { deltaIdr ->
                FilterChip(
                    selected = false,
                    onClick = {
                        balanceText = addDeltaInActiveCurrency(balanceText, deltaIdr, currencyConfig)
                    },
                    label = { Text("+${FormatUtils.formatCompactMoney(deltaIdr, currencyConfig)}") },
                )
            }
        }

        if (walletType != WalletType.CASH) {
            OutlinedTextField(
                value = accountNumberHint,
                onValueChange = { accountNumberHint = it.take(40) },
                label = {
                    Text(if (walletType == WalletType.BANK) "No. Rekening (Opsional)" else "Nomor HP / ID (Opsional)")
                },
                placeholder = {
                    Text(if (walletType == WalletType.BANK) "Contoh: **** 4821" else "Contoh: 0812-xxxx-9900")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_hint_input"),
            )
        }

        Text(
            text = "Warna Kartu Dompet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            bento.pocketColors.forEachIndexed { idx, color ->
                val selected = colorSlot == idx
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Jadikan Dompet Utama",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Dipilih otomatis saat mencatat transaksi baru",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.mutedText,
                )
            }
            Switch(
                checked = isDefault,
                onCheckedChange = { isDefault = it },
            )
        }
    }

    if (showProviderPicker) {
        WalletProviderSearchPickerDialog(
            walletType = walletType,
            selectedCode = providerCode,
            onDismiss = { showProviderPicker = false },
            onSelect = { selected ->
                providerCode = selected.code
                providerShortName = selected.shortName
                providerCompanyName = selected.companyName
                name = selected.defaultWalletName
                errorMsg = null
                showProviderPicker = false
            },
        )
    }
}

@Composable
fun WalletProviderSearchPickerDialog(
    walletType: WalletType,
    selectedCode: String,
    onDismiss: () -> Unit,
    onSelect: (WalletProviderOption) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val providers = remember(walletType) { IndonesianWalletCatalog.providersFor(walletType) }
    val groups = remember(providers) {
        listOf("Semua") + providers.map { it.categoryGroup }.distinct()
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Semua") }

    val filtered = remember(providers, searchQuery, selectedGroup) {
        val q = searchQuery.trim().lowercase()
        providers.filter { item ->
            val groupOk = selectedGroup == "Semua" || item.categoryGroup == selectedGroup
            val queryOk = q.isBlank() ||
                item.selectionPreviewLabel.lowercase().contains(q) ||
                item.shortName.lowercase().contains(q) ||
                item.companyName.lowercase().contains(q) ||
                item.keywords.lowercase().contains(q)
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
                .heightIn(max = 660.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (walletType == WalletType.BANK) "Pilih Bank Indonesia" else "Pilih E-Wallet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${providers.size} penyedia terdaftar • PT & ${walletType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (walletType == WalletType.BANK) {
                                "Cari bank (Jago, BRI, BCA, Mandiri, SeaBank...)"
                            } else {
                                "Cari e-wallet (GoPay, OVO, DANA, ShopeePay...)"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_provider_search_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tidak ditemukan \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filtered, key = { it.code }) { option ->
                            val isSelected = option.code == selectedCode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wallet_provider_option_${option.code.lowercase()}")
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelect(option) },
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
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    WalletLogoBadge(
                                        providerCode = option.code,
                                        walletType = option.walletType,
                                        shortName = option.shortName,
                                        size = 36.dp,
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
                                                text = option.compactTypeNameLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = bento.cardSurface,
                                            ) {
                                                Text(
                                                    text = option.categoryGroup,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bento.mutedText,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                        Text(
                                            text = option.companyName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bento.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Terpilih",
                                            tint = bento.highlightLabel,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletTransferSheet(
    wallets: List<WalletEntity>,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onTransfer: (fromWalletId: String, toWalletId: String, amountIdr: Long, note: String) -> Unit,
) {
    if (wallets.size < 2) return
    val bento = MaterialTheme.bentoColors
    var fromId by rememberSaveable { mutableStateOf(wallets.first().id) }
    var toId by rememberSaveable { mutableStateOf(wallets.getOrNull(1)?.id ?: wallets.first().id) }
    var amountText by rememberSaveable(currencyConfig.code) { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)

    BentoModalSheetDialog(
        title = "Transfer Antar Dompet",
        subtitle = "Pindahkan saldo antar Tunai, E-Wallet, atau Bank",
        headerIcon = Icons.Rounded.SwapHoriz,
        closeButtonTestTag = "wallet_transfer_drag_handle",
        onDismiss = onDismiss,
        footer = {
            if (!errorMsg.isNullOrBlank()) {
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }
            Button(
                onClick = {
                    val amountIdr = editableInputToIdr(amountText, currencyConfig)
                    when {
                        fromId == toId -> errorMsg = "Dompet asal dan tujuan harus berbeda."
                        amountIdr <= 0L -> errorMsg = "Masukkan nominal transfer lebih dari 0."
                        else -> {
                            onTransfer(fromId, toId, amountIdr, note.trim())
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
                    .testTag("wallet_transfer_submit_button"),
            ) {
                Text(
                    text = "Pindahkan Saldo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    ) {
        Text(
            text = "Dari Dompet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            wallets.forEach { w ->
                FilterChip(
                    selected = fromId == w.id,
                    onClick = { fromId = w.id; errorMsg = null },
                    leadingIcon = {
                        WalletLogoBadge(
                            providerCode = w.providerCode,
                            walletType = w.type,
                            shortName = w.providerShortName,
                            size = 18.dp,
                        )
                    },
                    label = { Text("${w.name} • ${FormatUtils.formatCompactMoney(w.balance, currencyConfig)}") },
                )
            }
        }

        Text(
            text = "Ke Dompet Tujuan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            wallets.forEach { w ->
                FilterChip(
                    selected = toId == w.id,
                    onClick = { toId = w.id; errorMsg = null },
                    leadingIcon = {
                        WalletLogoBadge(
                            providerCode = w.providerCode,
                            walletType = w.type,
                            shortName = w.providerShortName,
                            size = 18.dp,
                        )
                    },
                    label = { Text("${w.name} • ${FormatUtils.formatCompactMoney(w.balance, currencyConfig)}") },
                )
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = sanitizeCurrencyInput(it, currencyConfig)
                errorMsg = null
            },
            label = { Text("Nominal Transfer (${currencyConfig.code})") },
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
                .testTag("wallet_transfer_amount_input"),
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(120) },
            label = { Text("Catatan (Opsional)") },
            placeholder = { Text("Contoh: Top up GoPay dari Bank Jago") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
