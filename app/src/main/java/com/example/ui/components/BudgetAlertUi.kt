package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.PocketEntity
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

@Composable
fun OverBudgetWarningBanner(
    exceededPockets: List<PocketEntity>,
    onManagePocketsClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyConfig: CurrencyDisplayConfig = FormatUtils.activeCurrencyConfig,
) {
    if (exceededPockets.isEmpty()) return
    val bento = MaterialTheme.bentoColors
    val primary = exceededPockets.first()
    val overage = (primary.currentAmount - primary.targetAmount).coerceAtLeast(0L)
    val titleText = if (exceededPockets.size == 1) {
        "Kategori ${primary.name} melebihi target!"
    } else {
        "${exceededPockets.size} kategori melebihi target bulanan!"
    }
    val detailText = if (exceededPockets.size == 1) {
        "Terpakai ${FormatUtils.formatMoney(primary.currentAmount, currencyConfig)} / ${FormatUtils.formatMoney(primary.targetAmount, currencyConfig)} (+${FormatUtils.formatMoney(overage, currencyConfig)}). Ketuk untuk atur."
    } else {
        val names = exceededPockets.joinToString(", ") { it.name }
        "Kategori: $names. Ketuk untuk tinjau anggaran."
    }

    BentoSurfaceCard(
        containerColor = bento.expenseColor.copy(alpha = 0.14f),
        borderColor = bento.expenseColor.copy(alpha = 0.65f),
        contentPadding = PaddingValues(14.dp),
        onClick = onManagePocketsClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("over_budget_warning_banner"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bento.expenseColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "Peringatan Target Terlampaui",
                    tint = bento.expenseColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bento.expenseColor,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun BudgetNotificationSettingsCard(
    alertsEnabled: Boolean,
    hasNotificationPermission: Boolean,
    onToggleAlerts: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors

    BentoSurfaceCard(
        contentPadding = PaddingValues(18.dp),
        modifier = modifier.testTag("budget_notification_card"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = bento.highlightLabel,
            )
            Text(
                text = "Notifikasi Target Kategori",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasNotificationPermission) {
                "Kirim notifikasi otomatis saat pengeluaran kategori melewati batas target bulanan."
            } else {
                "Izin notifikasi belum aktif. Aktifkan izin agar peringatan muncul di bilah notifikasi."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (!hasNotificationPermission && alertsEnabled) bento.warningColor else bento.mutedText,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Peringatan Batas Pengeluaran",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Aktif untuk semua kantong mode Batas",
                        style = MaterialTheme.typography.labelMedium,
                        color = bento.mutedText,
                    )
                }
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onToggleAlerts,
                    modifier = Modifier.testTag("budget_alerts_switch"),
                )
            }
        }

        if (alertsEnabled && !hasNotificationPermission) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_notification_permission_button"),
            ) {
                Text("Izinkan Notifikasi Perangkat")
            }
        }
    }
}
