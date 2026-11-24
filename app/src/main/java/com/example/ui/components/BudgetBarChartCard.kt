package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.ui.MonthlyBudgetPoint
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import kotlin.math.max

@Composable
fun BudgetBarChartCard(
    monthlyPoints: List<MonthlyBudgetPoint>,
    monthlyBudget: Long,
    onEditBudgetClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyConfig: CurrencyDisplayConfig = FormatUtils.activeCurrencyConfig,
) {
    if (monthlyPoints.isEmpty()) return

    val bento = MaterialTheme.bentoColors
    val defaultIndex = (monthlyPoints.indexOfLast { it.isCurrentMonth }).takeIf { it >= 0 }
        ?: monthlyPoints.lastIndex
    var selectedIndex by remember(monthlyPoints.size) { mutableIntStateOf(defaultIndex) }
    LaunchedEffect(monthlyPoints.size) {
        if (selectedIndex !in monthlyPoints.indices) {
            selectedIndex = defaultIndex
        }
    }

    val selectedPoint = monthlyPoints[selectedIndex.coerceIn(monthlyPoints.indices)]
    val maxDataValue = remember(monthlyPoints) {
        val peak = monthlyPoints.maxOfOrNull { max(it.budgetAmount, it.actualSpent) } ?: 0L
        if (peak <= 0L) 1_000_000L else peak
    }

    // Round Y-axis ceiling to a clean step so gridlines look balanced
    val chartCeiling = remember(maxDataValue) {
        val raw = maxDataValue.toDouble()
        val magnitude = when {
            raw >= 10_000_000.0 -> 2_500_000.0
            raw >= 2_000_000.0 -> 1_000_000.0
            raw >= 500_000.0 -> 250_000.0
            else -> 100_000.0
        }
        ((kotlin.math.ceil(raw / magnitude) * magnitude).toLong()).coerceAtLeast(300_000L)
    }

    val budgetBarColor = bento.heroStart
    val spendingOkColor = if (bento.isDark) bento.accentTile else bento.highlightLabel
    val overBudgetColor = bento.expenseColor

    BentoSurfaceCard(
        contentPadding = PaddingValues(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_vs_spending_chart_card"),
    ) {
        // Header
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
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bento.iconBadgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = "Grafik Anggaran Bulanan",
                        tint = bento.highlightLabel,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "Anggaran vs Pengeluaran",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (monthlyBudget > 0L) {
                            "6 bulan terakhir • Target ${FormatUtils.formatCompactMoney(monthlyBudget, currencyConfig)}/bln"
                        } else {
                            "6 bulan terakhir • Anggaran bulanan belum diatur"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .testTag("chart_edit_budget_button")
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bento.nestedCardSurface)
                    .clickable(onClick = onEditBudgetClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Atur",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = bento.highlightLabel,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Month Breakdown Bento Pill
        SelectedMonthSummaryPill(
            point = selectedPoint,
            currencyConfig = currencyConfig,
            budgetColor = budgetBarColor,
            spendingOkColor = spendingOkColor,
            overBudgetColor = overBudgetColor,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChartLegendItem(color = budgetBarColor, label = "Anggaran")
            ChartLegendItem(color = spendingOkColor, label = "Pengeluaran")
            ChartLegendItem(color = overBudgetColor, label = "Melebihi")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Fractions for all 6 months
        val animatedBudgetRatios = monthlyPoints.mapIndexed { idx, pt ->
            val target = (pt.budgetAmount.toFloat() / chartCeiling.toFloat()).coerceIn(0f, 1f)
            animateFloatAsState(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f),
                label = "budget_bar_$idx",
            ).value
        }
        val animatedSpendRatios = monthlyPoints.mapIndexed { idx, pt ->
            val target = (pt.actualSpent.toFloat() / chartCeiling.toFloat()).coerceIn(0f, 1f)
            animateFloatAsState(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f),
                label = "spend_bar_$idx",
            ).value
        }

        val textMeasurer = rememberTextMeasurer()
        val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = bento.mutedText)
        val gridLineColor = bento.borderSubtle.copy(alpha = 0.65f)
        val selectedHighlightColor = bento.nestedCardSurface
        val leftGutterDp = 54.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .testTag("budget_bar_chart_canvas"),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(monthlyPoints.size) {
                        detectTapGestures { tapOffset ->
                            val gutterPx = leftGutterDp.toPx()
                            val plotWidth = (size.width - gutterPx).coerceAtLeast(1f)
                            if (tapOffset.x >= gutterPx) {
                                val groupWidth = plotWidth / monthlyPoints.size
                                val tapped = ((tapOffset.x - gutterPx) / groupWidth)
                                    .toInt()
                                    .coerceIn(0, monthlyPoints.lastIndex)
                                selectedIndex = tapped
                            }
                        }
                    },
            ) {
                val leftGutterPx = leftGutterDp.toPx()
                val topPaddingPx = 8.dp.toPx()
                val bottomPaddingPx = 6.dp.toPx()
                val plotWidth = (size.width - leftGutterPx).coerceAtLeast(1f)
                val plotHeight = (size.height - topPaddingPx - bottomPaddingPx).coerceAtLeast(1f)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val gridSteps = 3
                for (step in 0..gridSteps) {
                    val ratio = step.toFloat() / gridSteps.toFloat()
                    val y = topPaddingPx + plotHeight * (1f - ratio)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(leftGutterPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (step == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (step == 0) null else dashEffect,
                    )

                    val tickAmount = (chartCeiling * ratio).toLong()
                    val tickText = if (tickAmount == 0L) "${currencyConfig.symbol}0" else FormatUtils.formatCompactMoney(tickAmount, currencyConfig)
                    val measured = textMeasurer.measure(tickText, axisLabelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (leftGutterPx - measured.size.width - 8.dp.toPx()).coerceAtLeast(0f),
                            y = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height),
                        ),
                    )
                }

                val groupCount = monthlyPoints.size
                val groupWidth = plotWidth / groupCount
                val barGapPx = (groupWidth * 0.08f).coerceIn(2.dp.toPx(), 6.dp.toPx())
                val maxBarWidthPx = 18.dp.toPx()
                val barWidthPx = ((groupWidth * 0.56f - barGapPx) / 2f).coerceIn(6.dp.toPx(), maxBarWidthPx)
                val pairWidthPx = barWidthPx * 2f + barGapPx
                val cornerRadiusPx = 6.dp.toPx()

                monthlyPoints.forEachIndexed { index, point ->
                    val groupLeft = leftGutterPx + index * groupWidth
                    val isSelected = index == selectedIndex

                    if (isSelected) {
                        drawRoundRect(
                            color = selectedHighlightColor.copy(alpha = 0.85f),
                            topLeft = Offset(groupLeft + 3.dp.toPx(), 0f),
                            size = Size((groupWidth - 6.dp.toPx()).coerceAtLeast(1f), size.height),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        )
                    }

                    val pairLeft = groupLeft + (groupWidth - pairWidthPx) / 2f
                    val baselineY = topPaddingPx + plotHeight

                    // Budget Bar
                    val budgetRatio = animatedBudgetRatios[index]
                    val budgetBarHeight = (plotHeight * budgetRatio).let {
                        if (point.budgetAmount > 0L) it.coerceAtLeast(4.dp.toPx()) else 0f
                    }
                    if (budgetBarHeight > 0f) {
                        drawRoundRect(
                            color = budgetBarColor.copy(alpha = if (isSelected) 0.92f else 0.45f),
                            topLeft = Offset(pairLeft, baselineY - budgetBarHeight),
                            size = Size(barWidthPx, budgetBarHeight),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        )
                    }

                    // Actual Spending Bar
                    val spendRatio = animatedSpendRatios[index]
                    val isOver = point.budgetAmount > 0L && point.actualSpent > point.budgetAmount
                    val baseSpendColor = if (isOver) overBudgetColor else spendingOkColor
                    val spendBarHeight = if (point.actualSpent > 0L) {
                        (plotHeight * spendRatio).coerceAtLeast(4.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                    drawRoundRect(
                        color = if (point.actualSpent > 0L) {
                            baseSpendColor.copy(alpha = if (isSelected) 1f else 0.72f)
                        } else {
                            gridLineColor
                        },
                        topLeft = Offset(pairLeft + barWidthPx + barGapPx, baselineY - spendBarHeight),
                        size = Size(barWidthPx, spendBarHeight),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    )
                }
            }
        }

        // Interactive Month X-Axis Labels aligned with bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = leftGutterDp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            monthlyPoints.forEachIndexed { index, point ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chart_month_${index}")
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) bento.nestedCardSurface else Color.Transparent)
                        .clickable { selectedIndex = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = point.shortLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) bento.highlightLabel else bento.mutedText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedMonthSummaryPill(
    point: MonthlyBudgetPoint,
    currencyConfig: CurrencyDisplayConfig,
    budgetColor: Color,
    spendingOkColor: Color,
    overBudgetColor: Color,
) {
    val bento = MaterialTheme.bentoColors
    val isOver = point.budgetAmount > 0L && point.actualSpent > point.budgetAmount
    val pct = if (point.budgetAmount > 0L) {
        ((point.actualSpent.toDouble() / point.budgetAmount.toDouble()) * 100.0).toInt().coerceAtLeast(0)
    } else {
        0
    }
    val statusText = when {
        point.budgetAmount <= 0L -> "Belum ada target"
        isOver -> "Melebihi ($pct%)"
        pct >= 85 -> "Waspada ($pct%)"
        else -> "Terkendali ($pct%)"
    }
    val statusColor = when {
        point.budgetAmount <= 0L -> bento.mutedText
        isOver -> overBudgetColor
        pct >= 85 -> bento.warningColor
        else -> bento.incomeColor
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bento.nestedCardSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (point.isCurrentMonth) "${point.fullLabel} (Bulan Ini)" else point.fullLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(budgetColor),
                        )
                        Text(
                            text = "Anggaran",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatMoney(point.budgetAmount, currencyConfig),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOver) overBudgetColor else spendingOkColor),
                        )
                        Text(
                            text = "Realisasi Pengeluaran",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatMoney(point.actualSpent, currencyConfig),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) overBudgetColor else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
) {
    val bento = MaterialTheme.bentoColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = bento.mutedText,
        )
    }
}
