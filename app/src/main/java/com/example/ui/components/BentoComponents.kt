package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.ui.FinanceDashboardStats
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.walletIconFor
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AppTab(val route: String, val label: String) {
    HOME("beranda", "Beranda"),
    POCKETS("kantong", "Kantong"),
    HISTORY("riwayat", "Riwayat"),
    ACCOUNT("akun", "Akun"),
}

fun AppTab.localizedLabel(strings: AppStrings): String = when (this) {
    AppTab.HOME -> strings.navHome
    AppTab.POCKETS -> strings.navPockets
    AppTab.HISTORY -> strings.navHistory
    AppTab.ACCOUNT -> strings.navAccount
}

fun pocketIconFor(key: String): ImageVector = pocketCategoryFor(key).icon

@Composable
fun BentoSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.bentoColors.cardSurface,
    cornerRadius: Dp = 24.dp,
    borderColor: Color = MaterialTheme.bentoColors.borderSubtle,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun HomeHeroPagerCard(
    stats: FinanceDashboardStats,
    walletCount: Int,
    hideBalances: Boolean,
    onToggleHideBalances: () -> Unit,
    currencyConfig: CurrencyDisplayConfig = FormatUtils.activeCurrencyConfig,
    onEditPaydayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val animatedProgress by animateFloatAsState(
        targetValue = stats.paydayStats.cycleProgress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f),
        label = "payday_progress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_hero_pager"),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 216.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(bento.heroStart, bento.heroEnd)
                        )
                    )
                    .padding(20.dp)
            ) {
                if (page == 0) {
                    // Slide 1: Total Saldo Keseluruhan dari Semua Dompet
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_slide_total_balance"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.heroTotalBalanceTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = bento.onHero.copy(alpha = 0.84f),
                                )
                                Text(
                                    text = "$walletCount ${strings.heroAllWalletsBadge}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = bento.onHero.copy(alpha = 0.68f),
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(bento.onHero.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "${currencyConfig.flagEmoji} ${currencyConfig.code}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bento.onHero,
                                    )
                                }
                                IconButton(
                                    onClick = onToggleHideBalances,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("hero_privacy_toggle"),
                                ) {
                                    Icon(
                                        imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = strings.privacyHideBalancesTitle,
                                        tint = bento.onHero,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (hideBalances) "••••••••" else FormatUtils.formatMoney(stats.totalBalance, currencyConfig, withSpace = true),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 35.sp),
                            color = bento.onHero,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (hideBalances) strings.heroHiddenHint else "${strings.sectionWallets}: $walletCount • ${FormatUtils.formatExchangeRateLine(currencyConfig)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.onHero.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = bento.heroDivider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = strings.heroIncomeMonth,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = bento.onHero.copy(alpha = 0.72f),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hideBalances) "••••••" else "+${FormatUtils.formatCompactMoney(stats.monthIncome, currencyConfig)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bento.onHero,
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = strings.heroExpenseMonth,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = bento.onHero.copy(alpha = 0.72f),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (hideBalances) "••••••" else "-${FormatUtils.formatCompactMoney(stats.monthExpense, currencyConfig)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bento.onHero,
                                )
                            }
                        }
                    }
                } else {
                    // Slide 2: Sisa Saldo Hari Ini yang Dapat Digunakan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hero_slide_daily_remaining"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = strings.heroTodayRemainingTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = bento.onHero.copy(alpha = 0.84f),
                                modifier = Modifier.weight(1f),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .testTag("edit_payday_button")
                                        .clip(RoundedCornerShape(50))
                                        .background(bento.onHero.copy(alpha = 0.14f))
                                        .clickable(onClick = onEditPaydayClick)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = strings.heroSetPayday,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bento.onHero,
                                    )
                                }
                                IconButton(
                                    onClick = onToggleHideBalances,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = if (hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = strings.privacyHideBalancesTitle,
                                        tint = bento.onHero,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (hideBalances) "••••••••" else FormatUtils.formatMoney(stats.dailySafeRemaining, currencyConfig, withSpace = true),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 35.sp),
                            color = bento.onHero,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = strings.heroTodaySafeSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.onHero.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stats.paydayStats.countdownLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = bento.onHero.copy(alpha = 0.88f),
                            )
                            Text(
                                text = stats.paydayStats.scheduleBadgeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = bento.onHero.copy(alpha = 0.88f),
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(bento.heroTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(bento.onHero)
                            )
                        }
                    }
                }
            }
        }

        // Interactive 2-Slide Pill Switcher / Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                0 to strings.heroTotalBalanceTitle,
                1 to strings.heroTodayRemainingTitle,
            ).forEachIndexed { idx, (targetPage, label) ->
                val isSelected = pagerState.currentPage == targetPage
                if (idx > 0) Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .testTag("hero_indicator_$targetPage")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) bento.accentTile else bento.cardSurface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else bento.borderSubtle.copy(alpha = 0.55f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) bento.onAccentTile else bento.mutedText.copy(alpha = 0.45f)
                                )
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) bento.onAccentTile else bento.mutedText,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletBentoCard(
    wallet: WalletEntity,
    currencyConfig: CurrencyDisplayConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideBalances: Boolean = false,
) {
    val bento = MaterialTheme.bentoColors
    val subtitleLabel = when (wallet.type) {
        WalletType.CASH -> "Uang Tunai"
        WalletType.EWALLET -> "E-Wallet: ${wallet.providerShortName}"
        WalletType.BANK -> "Bank: ${wallet.providerShortName}"
    }

    Surface(
        modifier = modifier
            .width(154.dp)
            .testTag("wallet_card_${wallet.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = bento.nestedCardSurface,
        border = BorderStroke(
            width = if (wallet.isDefault) 1.5.dp else 1.dp,
            color = if (wallet.isDefault) bento.highlightLabel.copy(alpha = 0.75f) else bento.borderSubtle.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WalletLogoBadge(
                    providerCode = wallet.providerCode,
                    walletType = wallet.type,
                    shortName = wallet.providerShortName,
                    size = 34.dp,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bento.cardSurface)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (wallet.isDefault) "Utama" else wallet.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (wallet.isDefault) bento.highlightLabel else bento.mutedText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = wallet.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = subtitleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = bento.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hideBalances) "••••••" else FormatUtils.formatMoney(wallet.balance, currencyConfig),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (!hideBalances && wallet.balance < 0L) bento.expenseColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun QuickActionBentoGrid(
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onBillsClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionTile(
                title = strings.actionAddIncome,
                icon = Icons.Rounded.Add,
                highlighted = false,
                testTag = "quick_action_income",
                onClick = onIncomeClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                title = strings.actionAddExpense,
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                highlighted = true,
                testTag = "quick_action_expense",
                onClick = onExpenseClick,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionTile(
                title = strings.actionScanReceipt,
                icon = Icons.Rounded.DocumentScanner,
                highlighted = false,
                testTag = "quick_action_scan_receipt",
                onClick = onScanReceiptClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                title = strings.actionBills,
                icon = Icons.Rounded.Payments,
                highlighted = false,
                testTag = "quick_action_bills",
                onClick = onBillsClick,
                modifier = Modifier.weight(1f),
            )
        }

        // Compact small "Lainnya" box revealing all quick actions modal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Target, Dompet Pintar, Riwayat & Kurs",
                style = MaterialTheme.typography.labelSmall,
                color = bento.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp),
            )

            Surface(
                modifier = Modifier
                    .testTag("quick_action_more")
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onMoreClick),
                shape = RoundedCornerShape(14.dp),
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = strings.actionMore,
                        tint = bento.highlightLabel,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = strings.actionMore,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bento.iconBadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "+6",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = bento.highlightLabel,
                        )
                    }
                }
            }
        }
    }
}

private data class QuickActionMenuEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val highlighted: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
fun AllQuickActionsSheet(
    onDismiss: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onBillsClick: () -> Unit,
    onTargetClick: () -> Unit,
    onSmartPocketClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTransferWalletClick: () -> Unit,
    onAddWalletClick: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current

    val actions = listOf(
        QuickActionMenuEntry(
            title = strings.actionAddIncome,
            subtitle = "Tambah pemasukan ke dompet",
            icon = Icons.Rounded.Add,
            testTag = "all_action_income",
            onClick = { onDismiss(); onIncomeClick() },
        ),
        QuickActionMenuEntry(
            title = strings.actionAddExpense,
            subtitle = "Catat belanja & tempat",
            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
            highlighted = true,
            testTag = "all_action_expense",
            onClick = { onDismiss(); onExpenseClick() },
        ),
        QuickActionMenuEntry(
            title = strings.actionScanReceipt,
            subtitle = "Kamera & galeri struk AI",
            icon = Icons.Rounded.DocumentScanner,
            testTag = "all_action_scan_receipt",
            onClick = { onDismiss(); onScanReceiptClick() },
        ),
        QuickActionMenuEntry(
            title = strings.actionBills,
            subtitle = "Catat Utang, Piutang & Cicil",
            icon = Icons.Rounded.Payments,
            testTag = "all_action_bills",
            onClick = { onDismiss(); onBillsClick() },
        ),
        QuickActionMenuEntry(
            title = strings.actionAddPocket,
            subtitle = "Target tabungan atau batas",
            icon = Icons.Rounded.TrackChanges,
            testTag = "all_action_target",
            onClick = { onDismiss(); onTargetClick() },
        ),
        QuickActionMenuEntry(
            title = strings.pocketsSmartButton,
            subtitle = "Rekomendasi alokasi otomatis",
            icon = Icons.Rounded.AutoAwesome,
            testTag = "all_action_smart_pocket",
            onClick = { onDismiss(); onSmartPocketClick() },
        ),
        QuickActionMenuEntry(
            title = strings.navHistory,
            subtitle = "Cari & cek semua catatan",
            icon = Icons.Rounded.History,
            testTag = "all_action_history",
            onClick = { onDismiss(); onHistoryClick() },
        ),
        QuickActionMenuEntry(
            title = strings.actionTransfer,
            subtitle = "Pindah saldo antar dompet",
            icon = Icons.Rounded.SwapHoriz,
            testTag = "all_action_transfer",
            onClick = { onDismiss(); onTransferWalletClick() },
        ),
        QuickActionMenuEntry(
            title = "Tambah Dompet",
            subtitle = "Tunai, Bank & E-Wallet ID",
            icon = Icons.Rounded.AccountBalanceWallet,
            testTag = "all_action_add_wallet",
            onClick = { onDismiss(); onAddWalletClick() },
        ),
        QuickActionMenuEntry(
            title = "Kurs Global",
            subtitle = "Tukar mata uang dunia",
            icon = Icons.Rounded.Language,
            testTag = "all_action_currency",
            onClick = { onDismiss(); onCurrencyClick() },
        ),
    )

    BentoModalSheetDialog(
        title = strings.allQuickActionsTitle,
        subtitle = strings.allQuickActionsSubtitle,
        headerIcon = Icons.Rounded.Apps,
        closeButtonTestTag = "all_quick_actions_drag_handle",
        onDismiss = onDismiss,
    ) {
        actions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { entry ->
                    val container = if (entry.highlighted) bento.accentTile else bento.nestedCardSurface
                    val content = if (entry.highlighted) bento.onAccentTile else MaterialTheme.colorScheme.onSurface
                    val sub = if (entry.highlighted) bento.onAccentTile.copy(alpha = 0.78f) else bento.mutedText
                    val badge = if (entry.highlighted) bento.accentIconContainer else bento.cardSurface

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(entry.testTag)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = entry.onClick),
                        shape = RoundedCornerShape(20.dp),
                        color = container,
                        border = if (entry.highlighted) null else BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badge),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = entry.title,
                                    tint = if (entry.highlighted) bento.onAccentTile else bento.highlightLabel,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = content,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = entry.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sub,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    highlighted: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val containerColor = if (highlighted) bento.accentTile else bento.cardSurface
    val contentColor = if (highlighted) bento.onAccentTile else MaterialTheme.colorScheme.onSurface
    val iconBoxColor = if (highlighted) bento.accentIconContainer else bento.iconBadgeBg
    val borderColor = if (highlighted) Color.Transparent else bento.borderSubtle

    Surface(
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = if (highlighted) null else BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBoxColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PocketBentoCard(
    pocket: PocketEntity,
    isFeaturedLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyConfig: CurrencyDisplayConfig = FormatUtils.activeCurrencyConfig,
) {
    val bento = MaterialTheme.bentoColors
    val slotColor = bento.pocketColors[pocket.colorSlot.coerceIn(0, bento.pocketColors.lastIndex)]
    val animatedFraction by animateFloatAsState(
        targetValue = pocket.progressFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "pocket_progress_${pocket.id}",
    )

    val isOverBudget = pocket.pocketMode == PocketMode.BUDGET_LIMIT && pocket.currentAmount > pocket.targetAmount
    val barColor = when {
        isOverBudget -> bento.expenseColor
        pocket.pocketMode == PocketMode.BUDGET_LIMIT && pocket.progressFraction < 0.2f -> bento.warningColor
        else -> slotColor
    }

    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .testTag("pocket_card_${pocket.id}")
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = bento.nestedCardSurface,
        border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(if (isFeaturedLayout) 16.dp else 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isFeaturedLayout) 44.dp else 36.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(slotColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = pocketIconFor(pocket.iconKey),
                        contentDescription = pocket.name,
                        tint = slotColor,
                        modifier = Modifier.size(if (isFeaturedLayout) 22.dp else 18.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bento.cardSurface)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = pocket.pocketMode.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isFeaturedLayout) 14.dp else 10.dp))

            Text(
                text = pocket.name,
                style = if (isFeaturedLayout) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isFeaturedLayout) {
                val primaryAmountText = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                    if (isOverBudget) "Lebih ${FormatUtils.formatMoney(pocket.currentAmount - pocket.targetAmount, currencyConfig)}"
                    else FormatUtils.formatMoney(pocket.remainingAmount, currencyConfig)
                } else {
                    "${FormatUtils.formatMoney(pocket.currentAmount, currencyConfig)} / ${FormatUtils.formatMoney(pocket.targetAmount, currencyConfig)}"
                }

                Text(
                    text = primaryAmountText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isOverBudget) bento.expenseColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subtitleText = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                    "Terpakai ${FormatUtils.formatMoney(pocket.currentAmount, currencyConfig)} / ${FormatUtils.formatMoney(pocket.targetAmount, currencyConfig)}"
                } else {
                    val pct = ((pocket.currentAmount.toDouble() / pocket.targetAmount.coerceAtLeast(1L)) * 100)
                        .toInt()
                        .coerceIn(0, 999)
                    "$pct% tercapai menuju target"
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                val ratioText = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                    "Sisa ${FormatUtils.formatCompactMoney(pocket.remainingAmount, currencyConfig)}"
                } else {
                    "${FormatUtils.formatCompactMoney(pocket.currentAmount, currencyConfig)} / ${FormatUtils.formatCompactMoney(pocket.targetAmount, currencyConfig)}"
                }

                Text(
                    text = ratioText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) bento.expenseColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (pocket.pocketMode == PocketMode.BUDGET_LIMIT) {
                        "${FormatUtils.formatCompactMoney(pocket.currentAmount, currencyConfig)} / ${FormatUtils.formatCompactMoney(pocket.targetAmount, currencyConfig)}"
                    } else {
                        "${((pocket.currentAmount.toDouble() / pocket.targetAmount.coerceAtLeast(1L)) * 100).toInt()}% tercapai"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(bento.cardSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.coerceAtLeast(0.03f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyConfig: CurrencyDisplayConfig = FormatUtils.activeCurrencyConfig,
    wallet: WalletEntity? = null,
) {
    val bento = MaterialTheme.bentoColors
    val isIncome = transaction.txType == TransactionType.INCOME
    val amountColor = if (isIncome) bento.incomeColor else bento.expenseColor
    val prefix = if (isIncome) "+" else "-"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_row_${transaction.id}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = bento.nestedCardSurface,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(amountColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = transaction.txType.label,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$prefix${FormatUtils.formatMoney(transaction.amount, currencyConfig)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (transaction.walletName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(bento.cardSurface)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WalletLogoBadge(
                                providerCode = wallet?.providerCode ?: "",
                                walletType = wallet?.type ?: if (transaction.walletName.contains("tunai", ignoreCase = true)) WalletType.CASH else WalletType.BANK,
                                shortName = wallet?.providerShortName ?: transaction.walletName,
                                size = 14.dp,
                            )
                            Text(
                                text = transaction.walletName,
                                style = MaterialTheme.typography.labelSmall,
                                color = bento.incomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    val pocketOrCategory = transaction.pocketName.ifBlank { transaction.categoryLabel }
                    if (pocketOrCategory.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(bento.cardSurface)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = pocketIconFor(transaction.categoryKey),
                                contentDescription = null,
                                tint = if (transaction.pocketName.isNotBlank()) bento.highlightLabel else bento.mutedText,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = pocketOrCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (transaction.pocketName.isNotBlank()) bento.highlightLabel else bento.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = FormatUtils.formatTimeShort(transaction.occurredAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                }

                if (transaction.txType == TransactionType.EXPENSE && transaction.locationName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bento.cardSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = bento.highlightLabel,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = transaction.locationName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.mutedText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BentoBottomBar(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onCenterRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bento.cardSurface,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = bento.borderSubtle.copy(alpha = 0.5f), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    tab = AppTab.HOME,
                    selected = currentTab == AppTab.HOME,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onSelectTab(AppTab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    tab = AppTab.POCKETS,
                    selected = currentTab == AppTab.POCKETS,
                    selectedIcon = Icons.Rounded.AccountBalanceWallet,
                    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                    onClick = { onSelectTab(AppTab.POCKETS) },
                    modifier = Modifier.weight(1f),
                )

                // Center elevated Bento CTA button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("bottom_nav_record")
                            .size(50.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(bento.accentTile)
                            .clickable(onClick = onCenterRecordClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Catat Transaksi",
                            tint = bento.onAccentTile,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Catat",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bento.mutedText,
                    )
                }

                BottomNavItem(
                    tab = AppTab.HISTORY,
                    selected = currentTab == AppTab.HISTORY,
                    selectedIcon = Icons.Rounded.History,
                    unselectedIcon = Icons.Outlined.History,
                    onClick = { onSelectTab(AppTab.HISTORY) },
                    modifier = Modifier.weight(1f),
                )
                BottomNavItem(
                    tab = AppTab.ACCOUNT,
                    selected = currentTab == AppTab.ACCOUNT,
                    selectedIcon = Icons.Rounded.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    onClick = { onSelectTab(AppTab.ACCOUNT) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: AppTab,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    val tabLabel = tab.localizedLabel(strings)
    val activeColor = MaterialTheme.colorScheme.onSurface
    val inactiveColor = bento.mutedText

    Column(
        modifier = modifier
            .testTag("bottom_nav_${tab.route}")
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = tabLabel,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tabLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else inactiveColor,
            maxLines = 1,
        )
    }
}

@Composable
fun BentoNavigationRail(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onCenterRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    NavigationRail(
        modifier = modifier.border(1.dp, bento.borderSubtle.copy(alpha = 0.4f)),
        containerColor = bento.cardSurface,
        header = {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .testTag("rail_record_button")
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bento.accentTile)
                    .clickable(onClick = onCenterRecordClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Catat Transaksi",
                    tint = bento.onAccentTile,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        },
    ) {
        val items = listOf(
            Triple(AppTab.HOME, Icons.Rounded.Home, Icons.Outlined.Home),
            Triple(AppTab.POCKETS, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            Triple(AppTab.HISTORY, Icons.Rounded.History, Icons.Outlined.History),
            Triple(AppTab.ACCOUNT, Icons.Rounded.Person, Icons.Outlined.Person),
        )
        items.forEach { (tab, selIcon, unselIcon) ->
            val selected = currentTab == tab
            val tabLabel = tab.localizedLabel(strings)
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) selIcon else unselIcon,
                        contentDescription = tabLabel,
                    )
                },
                label = { Text(tabLabel) },
            )
        }
    }
}

/**
 * Scroll-prioritized Bento modal sheet overlay.
 * - The scrollable form body always keeps full verticalScroll priority down to the very bottom
 *   action button and all the way up to scroll offset 0 without dismissing.
 * - Dismisses ONLY by holding the centered top drag handle bar and sliding downward (or Back press).
 * - Uses Activity-window WindowInsets.safeDrawing so bottom buttons never clip under the phone bezel/IME.
 */
@Composable
fun BentoModalSheetDialog(
    title: String,
    subtitle: String? = null,
    headerIcon: ImageVector? = null,
    headerBadge: (@Composable () -> Unit)? = null,
    closeButtonTestTag: String = "sheet_drag_handle",
    onDismiss: () -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    val maxDragPx = with(density) { 480.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    var isDraggingHandle by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    val dragProgress = (offsetY.value / dismissThresholdPx).coerceIn(0f, 1f)
    val backdropAlpha = 0.58f * (1f - dragProgress * 0.45f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 680.dp
            val sheetAlignment = if (isWideScreen) Alignment.Center else Alignment.BottomCenter
            val sheetShape = if (isWideScreen) {
                RoundedCornerShape(28.dp)
            } else {
                RoundedCornerShape(28.dp)
            }
            val maxSheetHeight = if (isWideScreen) maxHeight * 0.90f else maxHeight * 0.96f

            Surface(
                modifier = Modifier
                    .align(sheetAlignment)
                    .padding(horizontal = if (isWideScreen) 24.dp else 8.dp, vertical = if (isWideScreen) 16.dp else 8.dp)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight),
                shape = sheetShape,
                color = bento.cardSurface,
                border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.65f)),
                shadowElevation = 16.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Dedicated Top Drag-Handle Header (NO 'X' button):
                    // User holds the centered top bar and slides downward to dismiss.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(closeButtonTestTag)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDraggingHandle = true
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            val next = (offsetY.value + dragAmount).coerceIn(0f, maxDragPx)
                                            offsetY.snapTo(next)
                                        }
                                    },
                                    onDragEnd = {
                                        isDraggingHandle = false
                                        scope.launch {
                                            if (offsetY.value >= dismissThresholdPx) {
                                                offsetY.animateTo(
                                                    targetValue = maxDragPx,
                                                    animationSpec = tween(durationMillis = 160),
                                                )
                                                onDismiss()
                                            } else {
                                                offsetY.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingHandle = false
                                        scope.launch {
                                            offsetY.animateTo(0f, animationSpec = spring())
                                        }
                                    },
                                )
                            }
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (isDraggingHandle) 68.dp else 54.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDraggingHandle) bento.highlightLabel
                                        else bento.mutedText.copy(alpha = 0.45f)
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (headerBadge != null) {
                                headerBadge()
                            } else if (headerIcon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bento.accentTile),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = headerIcon,
                                        contentDescription = title,
                                        tint = bento.onAccentTile,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = subtitle?.takeIf { it.isNotBlank() }
                                        ?: "Tahan garis atas & geser ke bawah untuk menutup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bento.mutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = bento.borderSubtle.copy(alpha = 0.5f))

                    // Scroll-prioritized Form Body (includes action footer inside scroll so
                    // nothing is ever trapped or cut off near the bottom bezel or keyboard)
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        content()

                        if (footer != null) {
                            HorizontalDivider(color = bento.borderSubtle.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                content = footer,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}


