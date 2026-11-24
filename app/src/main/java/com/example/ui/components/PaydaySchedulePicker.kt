package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PaydayFrequency
import com.example.data.local.PaydayWeekday
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaydayScheduleSelector(
    frequency: PaydayFrequency,
    onFrequencyChange: (PaydayFrequency) -> Unit,
    paydayDay: Int,
    onPaydayDayChange: (Int) -> Unit,
    paydayWeekday: Int,
    onPaydayWeekdayChange: (Int) -> Unit,
    paydayHour: Int,
    paydayMinute: Int,
    onPaydayTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    var showTimeDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var weekdayMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Siklus Gajian",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Selection Pill: Harian / Mingguan / Bulanan
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bento.nestedCardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PaydayFrequency.entries.forEach { freq ->
                    val selected = frequency == freq
                    val tag = when (freq) {
                        PaydayFrequency.DAILY -> "payday_freq_daily"
                        PaydayFrequency.WEEKLY -> "payday_freq_weekly"
                        PaydayFrequency.MONTHLY -> "payday_freq_monthly"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tag)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) bento.accentTile else bento.nestedCardSurface)
                            .clickable { onFrequencyChange(freq) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = freq.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Dynamic conditional selection depending on Harian / Mingguan / Bulanan
        AnimatedContent(
            targetState = frequency,
            label = "payday_schedule_mode",
        ) { currentFreq ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (currentFreq) {
                    PaydayFrequency.DAILY -> {
                        val timeText = FormatUtils.formatHourMinute(paydayHour, paydayMinute)
                        SelectionDropdownBox(
                            label = "Waktu (Jam) Gajian",
                            valueText = "Setiap hari pukul $timeText",
                            icon = Icons.Rounded.Schedule,
                            testTag = "payday_time_selector",
                            onClick = { showTimeDialog = true },
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                7 to 0,
                                8 to 0,
                                9 to 0,
                                12 to 0,
                                17 to 0,
                                20 to 0,
                            ).forEach { (h, m) ->
                                FilterChip(
                                    selected = paydayHour == h && paydayMinute == m,
                                    onClick = { onPaydayTimeChange(h, m) },
                                    label = { Text(FormatUtils.formatHourMinute(h, m)) },
                                )
                            }
                        }
                    }

                    PaydayFrequency.WEEKLY -> {
                        val selectedWeekday = PaydayWeekday.fromIso(paydayWeekday)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SelectionDropdownBox(
                                label = "Pilih Hari Gajian",
                                valueText = "Setiap hari ${selectedWeekday.fullLabel}",
                                icon = Icons.Rounded.CalendarViewWeek,
                                testTag = "payday_weekday_selector",
                                onClick = { weekdayMenuExpanded = true },
                            )

                            DropdownMenu(
                                expanded = weekdayMenuExpanded,
                                onDismissRequest = { weekdayMenuExpanded = false },
                                modifier = Modifier.background(bento.cardSurface),
                            ) {
                                PaydayWeekday.entries.forEach { dayItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = dayItem.fullLabel,
                                                fontWeight = if (dayItem == selectedWeekday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (dayItem == selectedWeekday) bento.highlightLabel else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        trailingIcon = if (dayItem == selectedWeekday) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = bento.highlightLabel,
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            onPaydayWeekdayChange(dayItem.isoValue)
                                            weekdayMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PaydayWeekday.entries.forEach { dayItem ->
                                FilterChip(
                                    selected = paydayWeekday == dayItem.isoValue,
                                    onClick = { onPaydayWeekdayChange(dayItem.isoValue) },
                                    label = { Text(dayItem.fullLabel) },
                                )
                            }
                        }
                    }

                    PaydayFrequency.MONTHLY -> {
                        SelectionDropdownBox(
                            label = "Pilih Tanggal Gajian",
                            valueText = "Setiap tanggal $paydayDay",
                            icon = Icons.Rounded.CalendarMonth,
                            testTag = "payday_date_selector",
                            onClick = { showDateDialog = true },
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(1, 5, 10, 15, 25, 28, 30).forEach { dateNum ->
                                FilterChip(
                                    selected = paydayDay == dateNum,
                                    onClick = { onPaydayDayChange(dateNum) },
                                    label = { Text("Tgl $dateNum") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        PaydayTimePickerDialog(
            initialHour = paydayHour,
            initialMinute = paydayMinute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                onPaydayTimeChange(h, m)
                showTimeDialog = false
            },
        )
    }

    if (showDateDialog) {
        PaydayDatePickerDialog(
            selectedDay = paydayDay,
            onDismiss = { showDateDialog = false },
            onSelectDay = { day ->
                onPaydayDayChange(day)
                showDateDialog = false
            },
        )
    }
}

@Composable
private fun SelectionDropdownBox(
    label: String,
    valueText: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bento.nestedCardSurface,
        border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bento.cardSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = bento.mutedText,
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Ubah pilihan",
                tint = bento.mutedText,
            )
        }
    }
}

@Composable
private fun PaydayTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(26.dp),
            color = bento.cardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Pilih Jam Gajian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Terpilih: ${FormatUtils.formatHourMinute(selectedHour, selectedMinute)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.highlightLabel,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup pemilih jam",
                            tint = bento.mutedText,
                        )
                    }
                }

                HorizontalDivider(color = bento.borderSubtle.copy(alpha = 0.5f))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Pilih Jam (00 - 23)",
                        style = MaterialTheme.typography.labelLarge,
                        color = bento.mutedText,
                    )

                    val hourRows = (0..23).toList().chunked(6)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        hourRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { hour ->
                                    val isSel = selectedHour == hour
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) bento.accentTile else bento.nestedCardSurface)
                                            .clickable { selectedHour = hour },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "%02d".format(hour),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Pilih Menit",
                        style = MaterialTheme.typography.labelLarge,
                        color = bento.mutedText,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0, 15, 30, 45).forEach { minute ->
                            val isSel = selectedMinute == minute
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) bento.accentTile else bento.nestedCardSurface)
                                    .clickable { selectedMinute = minute },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = ":%02d".format(minute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = bento.borderSubtle.copy(alpha = 0.5f))

                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bento.accentTile,
                            contentColor = bento.onAccentTile,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("payday_time_confirm"),
                    ) {
                        Text(
                            text = "Pilih Jam ${FormatUtils.formatHourMinute(selectedHour, selectedMinute)}",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaydayDatePickerDialog(
    selectedDay: Int,
    onDismiss: () -> Unit,
    onSelectDay: (Int) -> Unit,
) {
    val bento = MaterialTheme.bentoColors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = bento.cardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle.copy(alpha = 0.7f)),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Pilih Tanggal Gajian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Setiap tanggal $selectedDay dalam sebulan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup pemilih tanggal",
                            tint = bento.mutedText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val rows = (1..31).toList().chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { day ->
                                val isSel = selectedDay == day
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) bento.accentTile else bento.nestedCardSurface)
                                        .clickable { onSelectDay(day) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) bento.onAccentTile else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            repeat(7 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
