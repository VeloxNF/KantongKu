package com.example.util

import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PaydayFrequency
import com.example.data.local.PaydayWeekday
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object FormatUtils {
    private val indonesiaLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

    @Volatile
    var activeCurrencyConfig: CurrencyDisplayConfig = CurrencyDisplayConfig()

    fun convertFromIdr(
        amountIdr: Long,
        config: CurrencyDisplayConfig = activeCurrencyConfig,
    ): Double {
        if (config.isBaseIdr) return amountIdr.toDouble()
        return amountIdr.toDouble() * config.rateFromIdr
    }

    fun convertToIdr(
        foreignAmount: Double,
        config: CurrencyDisplayConfig = activeCurrencyConfig,
    ): Long {
        if (config.isBaseIdr) return foreignAmount.roundToLong()
        val rate = config.rateFromIdr.takeIf { it > 0.0 } ?: 1.0
        return (foreignAmount / rate).roundToLong()
    }

    fun formatMoney(
        amountIdr: Long,
        config: CurrencyDisplayConfig = activeCurrencyConfig,
        withSpace: Boolean = false,
    ): String {
        val converted = convertFromIdr(amountIdr, config)
        val isNegative = converted < -1e-9
        val absVal = abs(converted)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = config.groupingSeparator
            decimalSeparator = config.decimalSeparator
        }

        val decimals = if (config.isBaseIdr) 0 else config.decimalDigits.coerceIn(0, 3)
        val pattern = if (decimals == 0) {
            "#,##0"
        } else {
            "#,##0." + "0".repeat(decimals)
        }

        val df = DecimalFormat(pattern, symbols)
        val numericText = if (decimals == 0) {
            df.format(absVal.roundToLong())
        } else {
            val rounded = BigDecimal.valueOf(absVal)
                .setScale(decimals, RoundingMode.HALF_UP)
                .toDouble()
            df.format(rounded)
        }

        val sign = if (isNegative) "-" else ""
        val gap = if (withSpace || config.symbol.length > 2) " " else ""
        return "$sign${config.symbol}$gap$numericText"
    }

    fun formatCompactMoney(
        amountIdr: Long,
        config: CurrencyDisplayConfig = activeCurrencyConfig,
    ): String {
        val converted = convertFromIdr(amountIdr, config)
        val isNegative = converted < -1e-9
        val absVal = abs(converted)
        val sign = if (isNegative) "-" else ""
        val sym = config.symbol
        val gap = if (sym.length > 2) " " else ""

        if (config.isBaseIdr) {
            val absLong = abs(amountIdr)
            return when {
                absLong >= 1_000_000_000L -> "${sign}$sym${formatOneDecimal(absLong / 1_000_000_000.0, config)}M"
                absLong >= 1_000_000L -> {
                    val jt = absLong / 1_000_000.0
                    if (absLong % 1_000_000L == 0L) "${sign}$sym${absLong / 1_000_000L}jt"
                    else "${sign}$sym${formatOneDecimal(jt, config)}jt"
                }
                absLong >= 1_000L -> "${sign}$sym${absLong / 1_000L}rb"
                else -> "${sign}$sym$absLong"
            }
        }

        return when {
            absVal >= 1_000_000_000.0 -> "${sign}$sym$gap${formatOneDecimal(absVal / 1_000_000_000.0, config)}B"
            absVal >= 1_000_000.0 -> "${sign}$sym$gap${formatOneDecimal(absVal / 1_000_000.0, config)}M"
            absVal >= 1_000.0 -> "${sign}$sym$gap${formatOneDecimal(absVal / 1_000.0, config)}K"
            else -> formatMoney(amountIdr, config, withSpace = false)
        }
    }

    private fun formatOneDecimal(value: Double, config: CurrencyDisplayConfig): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = config.groupingSeparator
            decimalSeparator = config.decimalSeparator
        }
        return DecimalFormat("#,##0.#", symbols).format(value)
    }

    fun formatExchangeRateLine(config: CurrencyDisplayConfig): String {
        if (config.isBaseIdr) return "Mata uang dasar: Rupiah Indonesia (IDR)"
        val perOneForeignIdr = (1.0 / (config.rateFromIdr.takeIf { it > 0.0 } ?: 1.0)).roundToLong()
        val idrSymbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formattedIdr = DecimalFormat("#,##0", idrSymbols).format(perOneForeignIdr)
        return "1 ${config.code} ≈ Rp$formattedIdr"
    }

    fun formatRawNumberWithConfig(
        rawValue: Double,
        config: CurrencyDisplayConfig,
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = config.groupingSeparator
            decimalSeparator = config.decimalSeparator
        }
        val decimals = config.decimalDigits.coerceIn(0, 3)
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        return DecimalFormat(pattern, symbols).format(rawValue)
    }

    // Backwards-compatible aliases that obey activeCurrencyConfig dynamically across screens
    fun formatRupiah(amount: Long, withSpace: Boolean = false): String =
        formatMoney(amountIdr = amount, config = activeCurrencyConfig, withSpace = withSpace)

    fun formatCompactRupiah(amount: Long): String =
        formatCompactMoney(amountIdr = amount, config = activeCurrencyConfig)

    fun formatTodayHeader(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("EEEE, d MMMM", indonesiaLocale)
        return fmt.format(Date(nowMillis)).replaceFirstChar { it.uppercase(indonesiaLocale) }
    }

    fun formatDateGroup(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hari Ini"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Kemarin"
            else -> SimpleDateFormat("EEEE, d MMM yyyy", indonesiaLocale).format(Date(millis))
                .replaceFirstChar { it.uppercase(indonesiaLocale) }
        }
    }

    fun formatTimeShort(millis: Long): String =
        SimpleDateFormat("HH:mm", indonesiaLocale).format(Date(millis))

    fun formatDateTimeShort(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", indonesiaLocale).format(Date(millis))

    fun searchableDateTokens(millis: Long): String {
        val date = Date(millis)
        val group = formatDateGroup(millis)
        val full = SimpleDateFormat("EEEE, d MMMM yyyy", indonesiaLocale).format(date)
        val slash = SimpleDateFormat("dd/MM/yyyy d/M/yyyy", indonesiaLocale).format(date)
        val iso = SimpleDateFormat("yyyy-MM-dd dd-MM-yyyy", indonesiaLocale).format(date)
        return "$group $full $slash $iso".lowercase(indonesiaLocale)
    }

    data class PaydayStats(
        val frequency: PaydayFrequency,
        val daysUntilPayday: Int,
        val hoursUntilPayday: Int,
        val minutesUntilPayday: Int,
        val cycleProgress: Float,
        val paydayDay: Int,
        val paydayWeekday: Int,
        val paydayHour: Int,
        val paydayMinute: Int,
        val countdownLabel: String,
        val scheduleBadgeLabel: String,
        val profileSummaryLabel: String,
    )

    fun formatHourMinute(hour: Int, minute: Int): String =
        String.format(indonesiaLocale, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formatPaydaySummary(account: LocalAccountEntity): String {
        return calculatePaydayStats(
            frequency = account.parsedPaydayFrequency,
            paydayDay = account.paydayDay,
            paydayWeekday = account.paydayWeekday,
            paydayHour = account.paydayHour,
            paydayMinute = account.paydayMinute,
        ).profileSummaryLabel
    }

    fun calculatePaydayStats(
        frequency: PaydayFrequency = PaydayFrequency.MONTHLY,
        paydayDay: Int = 25,
        paydayWeekday: Int = 5,
        paydayHour: Int = 8,
        paydayMinute: Int = 0,
        nowMillis: Long = System.currentTimeMillis(),
    ): PaydayStats {
        val safePayday = paydayDay.coerceIn(1, 31)
        val safeWeekday = paydayWeekday.coerceIn(1, 7)
        val safeHour = paydayHour.coerceIn(0, 23)
        val safeMinute = paydayMinute.coerceIn(0, 59)
        val timeText = formatHourMinute(safeHour, safeMinute)

        return when (frequency) {
            PaydayFrequency.DAILY -> {
                val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val todayTarget = (nowCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, safeHour)
                    set(Calendar.MINUTE, safeMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val isPaydayMoment = nowCal.get(Calendar.HOUR_OF_DAY) == safeHour &&
                    nowCal.get(Calendar.MINUTE) == safeMinute

                val nextTarget = (todayTarget.clone() as Calendar).apply {
                    if (timeInMillis <= nowMillis && !isPaydayMoment) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val diffMillis = (nextTarget.timeInMillis - nowMillis).coerceAtLeast(0L)
                val hoursLeft = (diffMillis / 3_600_000L).toInt()
                val minutesLeft = ((diffMillis % 3_600_000L) / 60_000L).toInt()
                val dayMs = 86_400_000L
                val progress = if (isPaydayMoment) {
                    1f
                } else {
                    ((dayMs - diffMillis).toFloat() / dayMs.toFloat()).coerceIn(0.05f, 1f)
                }

                val countdown = when {
                    isPaydayMoment -> "Waktunya gajian hari ini!"
                    hoursLeft > 0 && minutesLeft > 0 -> "$hoursLeft jam $minutesLeft mnt lagi gajian"
                    hoursLeft > 0 -> "$hoursLeft jam lagi gajian"
                    minutesLeft > 0 -> "$minutesLeft menit lagi gajian"
                    else -> "Sebentar lagi gajian!"
                }

                PaydayStats(
                    frequency = PaydayFrequency.DAILY,
                    daysUntilPayday = 1,
                    hoursUntilPayday = hoursLeft,
                    minutesUntilPayday = minutesLeft,
                    cycleProgress = progress,
                    paydayDay = safePayday,
                    paydayWeekday = safeWeekday,
                    paydayHour = safeHour,
                    paydayMinute = safeMinute,
                    countdownLabel = countdown,
                    scheduleBadgeLabel = "Tiap hari • $timeText",
                    profileSummaryLabel = "Gajian harian • jam $timeText",
                )
            }

            PaydayFrequency.WEEKLY -> {
                val weekdayEnum = PaydayWeekday.fromIso(safeWeekday)
                val today = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                var daysUntil = 0
                val probe = today.clone() as Calendar
                while (probe.get(Calendar.DAY_OF_WEEK) != weekdayEnum.calendarConstant && daysUntil < 7) {
                    probe.add(Calendar.DAY_OF_YEAR, 1)
                    daysUntil++
                }

                val progress = if (daysUntil == 0) {
                    1f
                } else {
                    ((7 - daysUntil).toFloat() / 7f).coerceIn(0.05f, 1f)
                }

                PaydayStats(
                    frequency = PaydayFrequency.WEEKLY,
                    daysUntilPayday = daysUntil,
                    hoursUntilPayday = daysUntil * 24,
                    minutesUntilPayday = 0,
                    cycleProgress = progress,
                    paydayDay = safePayday,
                    paydayWeekday = safeWeekday,
                    paydayHour = safeHour,
                    paydayMinute = safeMinute,
                    countdownLabel = if (daysUntil == 0) "Hari ini gajian!" else "$daysUntil hari lagi gajian",
                    scheduleBadgeLabel = "Tiap ${weekdayEnum.fullLabel}",
                    profileSummaryLabel = "Gajian tiap ${weekdayEnum.fullLabel}",
                )
            }

            PaydayFrequency.MONTHLY -> {
                val today = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val currentMaxDay = today.getActualMaximum(Calendar.DAY_OF_MONTH)
                val thisMonthPayday = safePayday.coerceAtMost(currentMaxDay)
                val currentDayOfMonth = today.get(Calendar.DAY_OF_MONTH)

                val nextPayday = (today.clone() as Calendar).apply {
                    if (currentDayOfMonth <= thisMonthPayday) {
                        set(Calendar.DAY_OF_MONTH, thisMonthPayday)
                    } else {
                        add(Calendar.MONTH, 1)
                        val nextMax = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, safePayday.coerceAtMost(nextMax))
                    }
                }

                val prevPayday = (nextPayday.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                    val prevMax = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, safePayday.coerceAtMost(prevMax))
                }

                val msPerDay = 86_400_000L
                val daysUntil = kotlin.math.max(0, ((nextPayday.timeInMillis - today.timeInMillis) / msPerDay).toInt())
                val totalCycleDays = kotlin.math.max(1, ((nextPayday.timeInMillis - prevPayday.timeInMillis) / msPerDay).toInt())
                val elapsedDays = (totalCycleDays - daysUntil).coerceIn(0, totalCycleDays)
                val progress = if (daysUntil == 0) 1f else (elapsedDays.toFloat() / totalCycleDays.toFloat()).coerceIn(0.05f, 1f)

                PaydayStats(
                    frequency = PaydayFrequency.MONTHLY,
                    daysUntilPayday = daysUntil,
                    hoursUntilPayday = daysUntil * 24,
                    minutesUntilPayday = 0,
                    cycleProgress = progress,
                    paydayDay = safePayday,
                    paydayWeekday = safeWeekday,
                    paydayHour = safeHour,
                    paydayMinute = safeMinute,
                    countdownLabel = if (daysUntil == 0) "Hari ini gajian!" else "$daysUntil hari lagi gajian",
                    scheduleBadgeLabel = "tgl $safePayday",
                    profileSummaryLabel = "Gajian tgl $safePayday",
                )
            }
        }
    }

    fun startOfCurrentMonthMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
