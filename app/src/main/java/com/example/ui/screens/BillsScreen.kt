package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.BillEntity
import com.example.data.local.BillStatus
import com.example.data.local.BillType
import com.example.ui.components.BentoModalSheetDialog
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

private enum class BillTypeFilter(val label: String) {
    ALL("Semua"),
    UTANG("Utang"),
    PIUTANG("Piutang"),
}

private enum class BillStatusFilter(val label: String) {
    ALL("Semua Status"),
    BELUM_LUNAS("Belum Lunas"),
    CICIL("Cicil"),
    LUNAS("Sudah Lunas"),
}

@Composable
fun BillsScreen(
    bills: List<BillEntity>,
    currencyConfig: CurrencyDisplayConfig,
    hideBalances: Boolean,
    onBack: () -> Unit,
    onAddBillClick: () -> Unit,
    onEditBillClick: (BillEntity, BillStatus?) -> Unit,
    onQuickStatusChange: (billId: String, status: BillStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    BackHandler(onBack = onBack)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf(BillTypeFilter.ALL) }
    var statusFilter by rememberSaveable { mutableStateOf(BillStatusFilter.ALL) }

    val activeUtangRemaining = remember(bills) {
        bills.filter { it.parsedType == BillType.UTANG && it.parsedStatus != BillStatus.LUNAS }
            .sumOf { it.remainingAmount }
    }
    val activeUtangCount = remember(bills) {
        bills.count { it.parsedType == BillType.UTANG && it.parsedStatus != BillStatus.LUNAS }
    }
    val activePiutangRemaining = remember(bills) {
        bills.filter { it.parsedType == BillType.PIUTANG && it.parsedStatus != BillStatus.LUNAS }
            .sumOf { it.remainingAmount }
    }
    val activePiutangCount = remember(bills) {
        bills.count { it.parsedType == BillType.PIUTANG && it.parsedStatus != BillStatus.LUNAS }
    }

    val filteredBills = remember(bills, searchQuery, typeFilter, statusFilter) {
        val q = searchQuery.trim().lowercase()
        bills.filter { bill ->
            val typeOk = when (typeFilter) {
                BillTypeFilter.ALL -> true
                BillTypeFilter.UTANG -> bill.parsedType == BillType.UTANG
                BillTypeFilter.PIUTANG -> bill.parsedType == BillType.PIUTANG
            }
            val statusOk = when (statusFilter) {
                BillStatusFilter.ALL -> true
                BillStatusFilter.BELUM_LUNAS -> bill.parsedStatus == BillStatus.BELUM_LUNAS
                BillStatusFilter.CICIL -> bill.parsedStatus == BillStatus.CICIL
                BillStatusFilter.LUNAS -> bill.parsedStatus == BillStatus.LUNAS
            }
            val queryOk = q.isBlank() ||
                bill.personName.lowercase().contains(q) ||
                bill.note.lowercase().contains(q)
            typeOk && statusOk && queryOk
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .testTag("bills_screen_list"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Header with Back + Add CTA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = bento.cardSurface,
                            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.6f)),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("bills_back_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Tagihan",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "Catatan Utang & Piutang serta cicilan",
                                style = MaterialTheme.typography.bodySmall,
                                color = bento.mutedText,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .testTag("bills_add_button")
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onAddBillClick),
                        shape = RoundedCornerShape(14.dp),
                        color = bento.accentTile,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tambah Tagihan",
                                tint = bento.onAccentTile,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Tambah",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = bento.onAccentTile,
                            )
                        }
                    }
                }
            }

            // Summary Bento Cards (Utang vs Piutang)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoSurfaceCard(
                        containerColor = bento.nestedCardSurface,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            typeFilter = if (typeFilter == BillTypeFilter.UTANG) {
                                BillTypeFilter.ALL
                            } else {
                                BillTypeFilter.UTANG
                            }
                        },
                    ) {
                        Text(
                            text = "Sisa Utang Saya",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hideBalances) "••••••" else FormatUtils.formatCompactMoney(activeUtangRemaining, currencyConfig),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = bento.expenseColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$activeUtangCount belum lunas",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }

                    BentoSurfaceCard(
                        containerColor = bento.nestedCardSurface,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            typeFilter = if (typeFilter == BillTypeFilter.PIUTANG) {
                                BillTypeFilter.ALL
                            } else {
                                BillTypeFilter.PIUTANG
                            }
                        },
                    ) {
                        Text(
                            text = "Sisa Piutang Orang",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hideBalances) "••••••" else FormatUtils.formatCompactMoney(activePiutangRemaining, currencyConfig),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = bento.incomeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$activePiutangCount belum lunas",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                }
            }

            // Search + Type & Status Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama pengutang, pemberi, atau catatan...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = bento.mutedText,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bills_search_input"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BillTypeFilter.entries.forEach { option ->
                            FilterChip(
                                selected = typeFilter == option,
                                onClick = { typeFilter = option },
                                label = { Text(option.label) },
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        BillStatusFilter.entries.forEach { option ->
                            FilterChip(
                                selected = statusFilter == option,
                                onClick = { statusFilter = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }

            if (filteredBills.isEmpty()) {
                item {
                    BentoSurfaceCard(
                        onClick = onAddBillClick,
                        contentPadding = PaddingValues(24.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bento.accentTile.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = bento.highlightLabel,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Text(
                                text = if (bills.isEmpty()) "Belum ada catatan Utang atau Piutang" else "Tidak ada tagihan yang cocok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Ketuk + Tambah untuk mencatat nama pengutang/pemberi, nominal, catatan penggunaan uang, dan status lunas atau cicil.",
                                style = MaterialTheme.typography.bodySmall,
                                color = bento.mutedText,
                            )
                        }
                    }
                }
            } else {
                items(filteredBills, key = { it.id }) { bill ->
                    BillBentoCard(
                        bill = bill,
                        currencyConfig = currencyConfig,
                        hideBalances = hideBalances,
                        onClick = { onEditBillClick(bill, null) },
                        onQuickStatusSelect = { chosenStatus ->
                            if (chosenStatus == BillStatus.CICIL) {
                                onEditBillClick(bill, BillStatus.CICIL)
                            } else if (bill.parsedStatus != chosenStatus) {
                                onQuickStatusChange(bill.id, chosenStatus)
                            }
                        },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun BillBentoCard(
    bill: BillEntity,
    currencyConfig: CurrencyDisplayConfig,
    hideBalances: Boolean,
    onClick: () -> Unit,
    onQuickStatusSelect: (BillStatus) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val isUtang = bill.parsedType == BillType.UTANG
    val typeColor = if (isUtang) bento.expenseColor else bento.incomeColor
    val status = bill.parsedStatus
    val statusColor = when (status) {
        BillStatus.LUNAS -> bento.incomeColor
        BillStatus.CICIL -> bento.warningColor
        BillStatus.BELUM_LUNAS -> bento.expenseColor
    }
    val animatedProgress by animateFloatAsState(
        targetValue = bill.progressFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "bill_progress_${bill.id}",
    )

    BentoSurfaceCard(
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.testTag("bill_card_${bill.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(typeColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = bill.parsedType.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeColor,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Text(
                text = FormatUtils.formatDateTimeShort(bill.updatedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = bento.mutedText,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.personName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (bill.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = bill.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = bento.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hideBalances) "••••••" else FormatUtils.formatMoney(bill.totalAmount, currencyConfig),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (status == BillStatus.CICIL && !hideBalances) {
                    Text(
                        text = "Sisa ${FormatUtils.formatMoney(bill.remainingAmount, currencyConfig)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                }
            }
        }

        if (status == BillStatus.CICIL || status == BillStatus.LUNAS) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (hideBalances) "Sudah dibayar: ••••" else "Sudah dibayar: ${FormatUtils.formatMoney(bill.effectivePaidAmount, currencyConfig)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                )
                Text(
                    text = "${(bill.progressFraction * 100f).toInt().coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(bento.nestedCardSurface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Status Switcher Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BillStatus.entries.forEach { option ->
                val selected = status == option
                val chipColor = when {
                    !selected -> bento.nestedCardSurface
                    option == BillStatus.LUNAS -> bento.incomeColor
                    option == BillStatus.CICIL -> bento.warningColor
                    else -> bento.expenseColor
                }
                val textColor = when {
                    !selected -> bento.mutedText
                    option == BillStatus.CICIL -> Color.Black
                    else -> bento.onHero
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bill_status_${bill.id}_${option.name.lowercase()}")
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onQuickStatusSelect(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = chipColor,
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillEditorSheet(
    existingBill: BillEntity?,
    initialForcedStatus: BillStatus? = null,
    currencyConfig: CurrencyDisplayConfig,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        billType: BillType,
        personName: String,
        totalAmount: Long,
        paidAmount: Long,
        status: BillStatus,
        note: String,
    ) -> Unit,
    onDelete: (billId: String) -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    var billType by rememberSaveable(existingBill?.id) {
        mutableStateOf(existingBill?.parsedType ?: BillType.UTANG)
    }
    var personName by rememberSaveable(existingBill?.id) {
        mutableStateOf(existingBill?.personName ?: "")
    }
    var totalAmountText by rememberSaveable(existingBill?.id, currencyConfig.code) {
        mutableStateOf(
            existingBill?.let { idrToEditableInput(it.totalAmount, currencyConfig) } ?: ""
        )
    }
    var note by rememberSaveable(existingBill?.id) {
        mutableStateOf(existingBill?.note ?: "")
    }
    var status by rememberSaveable(existingBill?.id, initialForcedStatus) {
        mutableStateOf(initialForcedStatus ?: existingBill?.parsedStatus ?: BillStatus.BELUM_LUNAS)
    }
    var paidAmountText by rememberSaveable(existingBill?.id, currencyConfig.code) {
        val initialPaid = existingBill?.paidAmount ?: 0L
        mutableStateOf(if (initialPaid > 0L) idrToEditableInput(initialPaid, currencyConfig) else "")
    }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)
    val totalIdr = editableInputToIdr(totalAmountText, currencyConfig)
    val paidIdr = editableInputToIdr(paidAmountText, currencyConfig)
    val remainingIdr = (totalIdr - paidIdr).coerceAtLeast(0L)

    BentoModalSheetDialog(
        title = if (existingBill == null) "Tambah Tagihan" else "Edit Tagihan",
        subtitle = "Catat Utang atau Piutang beserta status lunas & cicilan",
        headerIcon = Icons.Rounded.Payments,
        closeButtonTestTag = "bill_sheet_drag_handle",
        onDismiss = onDismiss,
        footer = {
            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.expenseColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (existingBill != null) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = bento.expenseColor),
                        border = BorderStroke(1.dp, bento.expenseColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("bill_delete_button"),
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus Tagihan", modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = {
                        val cleanName = personName.trim()
                        if (cleanName.isBlank()) {
                            validationError = "Masukkan ${billType.personFieldLabel.lowercase()} terlebih dahulu."
                            return@Button
                        }
                        if (totalIdr <= 0L) {
                            validationError = "Masukkan jumlah uang lebih dari ${currencyConfig.symbol}0."
                            return@Button
                        }
                        if (status == BillStatus.CICIL && paidIdr > totalIdr) {
                            validationError = "Jumlah yang sudah dicicil melebihi total tagihan. Pilih 'Sudah Lunas' jika sudah terbayar penuh."
                            return@Button
                        }

                        val finalPaid = when (status) {
                            BillStatus.LUNAS -> totalIdr
                            BillStatus.BELUM_LUNAS -> 0L
                            BillStatus.CICIL -> paidIdr.coerceIn(0L, totalIdr)
                        }
                        onSave(
                            existingBill?.id,
                            billType,
                            cleanName,
                            totalIdr,
                            finalPaid,
                            status,
                            note.trim(),
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("bill_save_button"),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Tagihan", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        // 1. Utang vs Piutang Toggle
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Jenis Tagihan",
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
                    BillType.entries.forEach { option ->
                        val selected = billType == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_type_${option.name.lowercase()}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        !selected -> Color.Transparent
                                        option == BillType.UTANG -> bento.expenseColor
                                        else -> bento.incomeColor
                                    }
                                )
                                .clickable {
                                    billType = option
                                    validationError = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (option == BillType.UTANG) "Utang (Saya Berutang)" else "Piutang (Orang Utang)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) bento.onHero else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 2. Nama Pengutang / Pemberi Utang
        OutlinedTextField(
            value = personName,
            onValueChange = {
                personName = it.take(80)
                validationError = null
            },
            label = { Text(billType.personFieldLabel) },
            placeholder = {
                Text(if (billType == BillType.UTANG) "Contoh: Budi / Warung Pak Joko" else "Contoh: Andi / Teman Kantor")
            },
            leadingIcon = {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = bento.highlightLabel)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bill_person_input"),
        )

        // 3. Jumlah Uang
        OutlinedTextField(
            value = totalAmountText,
            onValueChange = { input ->
                totalAmountText = sanitizeCurrencyInput(input, currencyConfig)
                validationError = null
            },
            label = { Text("Jumlah Uang (${currencyConfig.code})") },
            placeholder = { Text(if (currencyConfig.isBaseIdr) "Contoh: 250.000" else "Contoh: 50") },
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
                .testTag("bill_total_input"),
        )

        // 4. Catatan (uang dipakai untuk apa)
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(400) },
            label = { Text("Catatan (Uang Dipakai Untuk Apa)") },
            placeholder = { Text("Contoh: Talangan bayar servis motor / Pinjam uang makan siang") },
            leadingIcon = {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = bento.mutedText)
            },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bill_note_input"),
        )

        // 5. Status Pembayaran: Belum Lunas | Cicil | Sudah Lunas
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Status Pembayaran",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bento.nestedCardSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BillStatus.entries.forEach { option ->
                        val selected = status == option
                        val activeBg = when {
                            !selected -> Color.Transparent
                            option == BillStatus.LUNAS -> bento.incomeColor
                            option == BillStatus.CICIL -> bento.warningColor
                            else -> bento.expenseColor
                        }
                        val activeFg = when {
                            !selected -> MaterialTheme.colorScheme.onSurface
                            option == BillStatus.CICIL -> Color.Black
                            else -> bento.onHero
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_form_status_${option.name.lowercase()}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeBg)
                                .clickable {
                                    status = option
                                    validationError = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = activeFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 6. Cicil -> Muncul kolom jumlah uang yang sudah dicicil + teks abu-abu sisa uang yang belum dibayar
        AnimatedVisibility(visible = status == BillStatus.CICIL) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { input ->
                        paidAmountText = sanitizeCurrencyInput(input, currencyConfig)
                        validationError = null
                    },
                    label = { Text("Jumlah Uang yang Sudah Dicicil (${currencyConfig.code})") },
                    placeholder = { Text(if (currencyConfig.isBaseIdr) "Contoh: 100.000" else "Contoh: 20") },
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
                        .testTag("bill_paid_amount_input"),
                )

                Text(
                    text = if (paidIdr > totalIdr && totalIdr > 0L) {
                        "Nominal cicilan melebihi total tagihan (${FormatUtils.formatMoney(totalIdr, currencyConfig)})"
                    } else {
                        "Sisa uang yang belum dibayar: ${FormatUtils.formatMoney(remainingIdr, currencyConfig)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = bento.mutedText,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("bill_remaining_helper_text"),
                )
            }
        }
    }

    if (confirmDelete && existingBill != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus Tagihan?") },
            text = { Text("Catatan ${existingBill.parsedType.label} dengan ${existingBill.personName} akan dihapus permanen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(existingBill.id)
                        onDismiss()
                    },
                ) {
                    Text("Hapus", color = bento.expenseColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Batal")
                }
            },
        )
    }
}
