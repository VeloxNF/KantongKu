package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.currency.CurrencyDisplayConfig
import com.example.util.FormatUtils
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/**
 * Live VisualTransformation that automatically inserts thousands grouping punctuation
 * ('.' for IDR/EUR/VND/etc., ',' for USD/SGD/JPY/GBP/etc.) and maps decimal separator
 * cleanly without cursor jumps while the user types.
 *
 * Canonical stored text uses digits and optional single '.' decimal separator (e.g. "1250000" or "1250.50").
 */
class CurrencyVisualTransformation(
    private val groupingSeparator: Char,
    private val decimalSeparator: Char,
    private val allowDecimals: Boolean,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val dotIdx = if (allowDecimals) raw.indexOf('.') else -1
        val rawIntPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
        val rawDecPart = if (dotIdx >= 0) raw.substring(dotIdx + 1) else null

        val groupedIntBuilder = StringBuilder()
        // Track mapping from raw offset -> transformed offset, and transformed offset -> raw offset
        val origToTrans = IntArray(raw.length + 1)
        val intDigitsLen = rawIntPart.length

        for (i in 0 until intDigitsLen) {
            if (i > 0 && (intDigitsLen - i) % 3 == 0) {
                groupedIntBuilder.append(groupingSeparator)
            }
            origToTrans[i] = groupedIntBuilder.length
            groupedIntBuilder.append(rawIntPart[i])
        }
        origToTrans[intDigitsLen] = groupedIntBuilder.length

        val finalBuilder = StringBuilder(groupedIntBuilder)
        if (rawDecPart != null) {
            origToTrans[intDigitsLen] = finalBuilder.length
            finalBuilder.append(decimalSeparator)
            for (j in rawDecPart.indices) {
                val origIndex = intDigitsLen + 1 + j
                origToTrans[origIndex] = finalBuilder.length
                finalBuilder.append(rawDecPart[j])
            }
            origToTrans[raw.length] = finalBuilder.length
        }

        val transformed = finalBuilder.toString()
        val transToOrig = IntArray(transformed.length + 1)
        var rawCursor = 0
        for (tIdx in 0..transformed.length) {
            while (rawCursor < raw.length && origToTrans[rawCursor] < tIdx) {
                rawCursor++
            }
            transToOrig[tIdx] = rawCursor.coerceIn(0, raw.length)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transformed.length)]
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

@Composable
fun rememberCurrencyVisualTransformation(config: CurrencyDisplayConfig): CurrencyVisualTransformation {
    return remember(config.groupingSeparator, config.decimalSeparator, config.decimalDigits) {
        CurrencyVisualTransformation(
            groupingSeparator = config.groupingSeparator,
            decimalSeparator = config.decimalSeparator,
            allowDecimals = !config.isBaseIdr && config.decimalDigits > 0,
        )
    }
}

/**
 * Sanitizes user typing into canonical numeric string: digits + at most one '.' decimal point
 * if decimals are enabled for the active currency.
 */
fun sanitizeCurrencyInput(
    rawInput: String,
    config: CurrencyDisplayConfig,
    maxIntegerDigits: Int = 12,
): String {
    val allowDecimals = !config.isBaseIdr && config.decimalDigits > 0
    if (!allowDecimals) {
        return rawInput.filter { it.isDigit() }.take(maxIntegerDigits)
    }

    val sb = StringBuilder()
    var hasDecimal = false
    var intCount = 0
    var decCount = 0
    val maxDec = config.decimalDigits.coerceIn(1, 3)

    for (ch in rawInput) {
        when {
            ch.isDigit() -> {
                if (!hasDecimal) {
                    if (intCount < maxIntegerDigits) {
                        sb.append(ch)
                        intCount++
                    }
                } else {
                    if (decCount < maxDec) {
                        sb.append(ch)
                        decCount++
                    }
                }
            }
            (ch == '.' || ch == ',') && !hasDecimal -> {
                if (sb.isEmpty()) {
                    sb.append('0')
                    intCount = 1
                }
                sb.append('.')
                hasDecimal = true
            }
        }
    }
    return sb.toString()
}

fun idrToEditableInput(
    amountIdr: Long,
    config: CurrencyDisplayConfig,
): String {
    if (amountIdr <= 0L) return ""
    if (config.isBaseIdr) return amountIdr.toString()

    val converted = FormatUtils.convertFromIdr(amountIdr, config)
    val decimals = config.decimalDigits.coerceIn(0, 3)
    if (decimals == 0) {
        return converted.roundToLong().coerceAtLeast(0L).toString()
    }
    val bd = BigDecimal.valueOf(converted)
        .setScale(decimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    val plain = bd.toPlainString()
    return if (plain == "0") "" else plain
}

fun editableInputToIdr(
    canonicalInput: String,
    config: CurrencyDisplayConfig,
): Long {
    if (canonicalInput.isBlank()) return 0L
    val parsed = canonicalInput.toDoubleOrNull() ?: return 0L
    if (parsed <= 0.0) return 0L
    return FormatUtils.convertToIdr(parsed, config).coerceAtLeast(1L)
}

fun addDeltaInActiveCurrency(
    currentCanonicalInput: String,
    deltaIdr: Long,
    config: CurrencyDisplayConfig,
): String {
    val currentIdr = editableInputToIdr(currentCanonicalInput, config)
    val nextIdr = (currentIdr + deltaIdr).coerceAtLeast(0L)
    return idrToEditableInput(nextIdr, config)
}
