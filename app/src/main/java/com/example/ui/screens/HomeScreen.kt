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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BillStatus
import com.example.data.local.BillType
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionEntity
import com.example.data.local.WalletEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.BudgetBarChartCard
import com.example.ui.components.HomeHeroPagerCard
import com.example.ui.components.OverBudgetWarningBanner
import com.example.ui.components.PocketBentoCard
import com.example.ui.components.QuickActionBentoGrid
import com.example.ui.components.SkeletonBox
import com.example.ui.components.TransactionItemRow
import com.example.ui.components.WalletBentoCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

@Composable
fun HomeScreen(
    uiState: FinanceUiState,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onTargetClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onManagePocketsClick: () -> Unit,
    onEditPaydayClick: () -> Unit,
    onPocketClick: (PocketEntity) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onAccountClick: () -> Unit,
    onQuickToggleTheme: () -> Unit,
    onCurrencyClick: () -> Unit,
    onAddWalletClick: () -> Unit,
    onTransferWalletClick: () -> Unit,
    onWalletClick: (WalletEntity) -> Unit,
    onScanReceiptClick: () -> Unit,
    onBillsClick: () -> Unit = {},
    onMoreActionsClick: () -> Unit = {},
    onToggleHideBalances: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    val account = uiState.activeAccount ?: return
    val initial = account.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "K"
    val currencyConfig = uiState.currencyConfig

    val featuredPocket = uiState.pockets.firstOrNull { it.featured } ?: uiState.pockets.firstOrNull()
    val secondaryPockets = uiState.pockets.filter { it.id != featuredPocket?.id }.take(4)
    val recentTransactions = uiState.transactions.take(3)
    val exceededPockets = uiState.pockets.filter {
        it.pocketMode == PocketMode.BUDGET_LIMIT && it.targetAmount > 0L && it.currentAmount > it.targetAmount
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Greeting Header + Currency Switcher Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onAccountClick)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("home_avatar_badge")
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bento.accentTile),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = bento.onAccentTile,
                            )
                        }

                        Column {
                            Text(
                                text = FormatUtils.formatTodayHeader(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.mutedText,
                            )
                            Text(
                                text = "Halo, ${account.displayName}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Global Currency Switcher Pill with Country Flag
                        Surface(
                            modifier = Modifier
                                .testTag("home_currency_button")
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onCurrencyClick),
                            shape = RoundedCornerShape(14.dp),
                            color = bento.cardSurface,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    text = currencyConfig.flagEmoji,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = currencyConfig.code,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        IconButton(
                            onClick = onQuickToggleTheme,
                            modifier = Modifier.testTag("home_theme_toggle"),
                        ) {
                            Icon(
                                imageVector = if (bento.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Ganti Tema Terang atau Gelap",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("home_cloud_status_button")
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(bento.cardSurface)
                                .clickable(onClick = onAccountClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                uiState.isSyncingCloud -> {
                                    SkeletonBox(
                                        modifier = Modifier.size(22.dp),
                                        cornerRadius = 11.dp,
                                    )
                                }
                                !uiState.isOnline -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = "Mode Offline",
                                        tint = if (uiState.pendingSyncCount > 0) bento.warningColor else bento.mutedText,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                uiState.pendingSyncCount > 0 -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = "Menunggu Sinkron Cloud",
                                        tint = bento.highlightLabel,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = "Tersinkron Otomatis ke Cloud",
                                        tint = bento.incomeColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.isOnline || uiState.isSyncingCloud || uiState.pendingSyncCount > 0) {
                item {
                    val isSyncing = uiState.isSyncingCloud
                    val statusTint = when {
                        isSyncing -> bento.highlightLabel
                        !uiState.isOnline -> bento.warningColor
                        else -> bento.highlightLabel
                    }
                    val statusLabel = when {
                        isSyncing -> "Menyinkronkan data ke cloud di latar belakang..."
                        !uiState.isOnline && uiState.pendingSyncCount > 0 ->
                            "Mode Offline • ${uiState.pendingSyncCount} perubahan akan otomatis tersinkron saat online"
                        !uiState.isOnline ->
                            "Mode Offline • Aplikasi tetap bisa dijalankan penuh tanpa internet"
                        else ->
                            "Terhubung • Menyiapkan sinkronisasi ${uiState.pendingSyncCount} perubahan..."
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_sync_status_banner")
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onAccountClick),
                        shape = RoundedCornerShape(14.dp),
                        color = statusTint.copy(alpha = 0.13f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isSyncing) {
                                SkeletonBox(
                                    modifier = Modifier.size(16.dp),
                                    cornerRadius = 8.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = if (!uiState.isOnline) Icons.Rounded.CloudOff else Icons.Rounded.CloudSync,
                                    contentDescription = null,
                                    tint = statusTint,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Hero 2-Slide Pager + Quick Actions
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        HomeHeroPagerCard(
                            stats = uiState.stats,
                            walletCount = uiState.wallets.size,
                            hideBalances = uiState.preferences.hideBalances,
                            onToggleHideBalances = onToggleHideBalances,
                            currencyConfig = currencyConfig,
                            onEditPaydayClick = onEditPaydayClick,
                            modifier = Modifier.weight(1.15f),
                        )
                        QuickActionBentoGrid(
                            onIncomeClick = onIncomeClick,
                            onExpenseClick = onExpenseClick,
                            onScanReceiptClick = onScanReceiptClick,
                            onBillsClick = onBillsClick,
                            onMoreClick = onMoreActionsClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HomeHeroPagerCard(
                            stats = uiState.stats,
                            walletCount = uiState.wallets.size,
                            hideBalances = uiState.preferences.hideBalances,
                            onToggleHideBalances = onToggleHideBalances,
                            currencyConfig = currencyConfig,
                            onEditPaydayClick = onEditPaydayClick,
                        )
                        QuickActionBentoGrid(
                            onIncomeClick = onIncomeClick,
                            onExpenseClick = onExpenseClick,
                            onScanReceiptClick = onScanReceiptClick,
                            onBillsClick = onBillsClick,
                            onMoreClick = onMoreActionsClick,
                        )
                    }
                }
            }

            // Active Tagihan (Utang & Piutang) Summary Card if user has pending bills
            val activeBills = uiState.bills.filter { it.parsedStatus != BillStatus.LUNAS }
            if (activeBills.isNotEmpty()) {
                val activeUtang = activeBills.filter { it.parsedType == BillType.UTANG }.sumOf { it.remainingAmount }
                val activePiutang = activeBills.filter { it.parsedType == BillType.PIUTANG }.sumOf { it.remainingAmount }
                item {
                    BentoSurfaceCard(
                        onClick = onBillsClick,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.testTag("home_bills_summary_card"),
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
                                    .background(bento.warningColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Payments,
                                    contentDescription = strings.actionBills,
                                    tint = bento.warningColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${strings.actionBills} Aktif (${activeBills.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val utangStr = if (uiState.preferences.hideBalances) "••••" else FormatUtils.formatCompactMoney(activeUtang, currencyConfig)
                                val piutangStr = if (uiState.preferences.hideBalances) "••••" else FormatUtils.formatCompactMoney(activePiutang, currencyConfig)
                                Text(
                                    text = "Utang: $utangStr • Piutang: $piutangStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bento.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Buka Tagihan",
                                tint = bento.highlightLabel,
                            )
                        }
                    }
                }
            }

            if (exceededPockets.isNotEmpty()) {
                item {
                    OverBudgetWarningBanner(
                        exceededPockets = exceededPockets,
                        onManagePocketsClick = onManagePocketsClick,
                        currencyConfig = currencyConfig,
                    )
                }
            }

            // Multidompet Bento Card
            item {
                BentoSurfaceCard(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.testTag("home_wallets_section"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.sectionWallets,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${uiState.wallets.size} • Tunai, E-Wallet & Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.mutedText,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (uiState.wallets.size >= 2) {
                                Surface(
                                    modifier = Modifier
                                        .testTag("home_transfer_wallet_button")
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onTransferWalletClick),
                                    shape = RoundedCornerShape(10.dp),
                                    color = bento.nestedCardSurface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapHoriz,
                                            contentDescription = strings.actionTransfer,
                                            tint = bento.highlightLabel,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = strings.actionTransfer,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = bento.highlightLabel,
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .testTag("home_add_wallet_button")
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onAddWalletClick),
                                shape = RoundedCornerShape(10.dp),
                                color = bento.accentTile,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = strings.actionAddWallet,
                                        tint = bento.onAccentTile,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = strings.actionAddWallet,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bento.onAccentTile,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.wallets.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(bento.nestedCardSurface)
                                .clickable(onClick = onAddWalletClick)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = bento.highlightLabel,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "Tambahkan dompet Tunai, Bank (BRI, Jago, BCA, dll), atau E-Wallet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.mutedText,
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.wallets, key = { it.id }) { wallet ->
                                WalletBentoCard(
                                    wallet = wallet,
                                    currencyConfig = currencyConfig,
                                    onClick = { onWalletClick(wallet) },
                                    hideBalances = uiState.preferences.hideBalances,
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Budget vs Actual Spending Bar Chart
            item {
                BudgetBarChartCard(
                    monthlyPoints = uiState.stats.monthlyComparison,
                    monthlyBudget = uiState.stats.monthlyBudget,
                    onEditBudgetClick = onEditPaydayClick,
                    currencyConfig = currencyConfig,
                )
            }

            // Kantong (Target Pockets) Bento Section
            item {
                BentoSurfaceCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.sectionFeaturedPockets,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier = Modifier
                                .testTag("home_manage_pockets")
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onManagePocketsClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Atur",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = bento.highlightLabel,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (featuredPocket == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(bento.nestedCardSurface)
                                .clickable(onClick = onTargetClick)
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = bento.highlightLabel,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Belum ada kantong target. Ketuk untuk buat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.mutedText,
                            )
                        }
                    } else {
                        PocketBentoCard(
                            pocket = featuredPocket,
                            isFeaturedLayout = true,
                            onClick = { onPocketClick(featuredPocket) },
                            modifier = Modifier.fillMaxWidth(),
                            currencyConfig = currencyConfig,
                        )

                        if (secondaryPockets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            secondaryPockets.chunked(2).forEachIndexed { rowIndex, pair ->
                                if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    PocketBentoCard(
                                        pocket = pair[0],
                                        isFeaturedLayout = false,
                                        onClick = { onPocketClick(pair[0]) },
                                        modifier = Modifier.weight(1f),
                                        currencyConfig = currencyConfig,
                                    )
                                    if (pair.size > 1) {
                                        PocketBentoCard(
                                            pocket = pair[1],
                                            isFeaturedLayout = false,
                                            onClick = { onPocketClick(pair[1]) },
                                            modifier = Modifier.weight(1f),
                                            currencyConfig = currencyConfig,
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions with Note & Wallet Preview
            item {
                BentoSurfaceCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.sectionRecentTransactions,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier = Modifier
                                .testTag("home_see_all_history")
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onHistoryClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = strings.actionViewAll,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = bento.highlightLabel,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(bento.nestedCardSurface)
                                .clickable(onClick = onExpenseClick)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Belum ada transaksi. Ketuk Pemasukan atau Pengeluaran untuk mulai mencatat beserta detail belanja.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bento.mutedText,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recentTransactions.forEach { tx ->
                                TransactionItemRow(
                                    transaction = tx,
                                    onClick = { onTransactionClick(tx) },
                                    currencyConfig = currencyConfig,
                                    wallet = uiState.wallets.firstOrNull { it.id == tx.walletId },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
