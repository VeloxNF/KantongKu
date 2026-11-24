package com.example.data.currency

import java.util.Currency
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val nameId: String,
    val symbol: String,
    val groupingSeparator: Char,
    val decimalSeparator: Char,
    val decimalDigits: Int,
    val regionLabel: String,
    val countryCode: String,
    val flagEmoji: String,
)

data class CurrencyDisplayConfig(
    val code: String = "IDR",
    val nameId: String = "Rupiah Indonesia",
    val symbol: String = "Rp",
    val rateFromIdr: Double = 1.0,
    val groupingSeparator: Char = '.',
    val decimalSeparator: Char = ',',
    val decimalDigits: Int = 0,
    val countryCode: String = "ID",
    val flagEmoji: String = "\uD83C\uDDEE\uD83C\uDDE9",
    val updatedAtMillis: Long = 0L,
) {
    val isBaseIdr: Boolean
        get() = code == "IDR"
}

object CurrencyCatalog {

    private data class CurrencyOverride(
        val nameId: String,
        val symbol: String,
        val dotGrouping: Boolean,
        val decimalDigits: Int,
        val regionLabel: String,
        val countryCode: String,
    )

    // Currencies whose standard financial punctuation uses '.' for thousands and ',' for decimals
    private val dotGroupingCurrencies = setOf(
        "IDR", "EUR", "VND", "BRL", "TRY", "ARS", "CLP", "COP", "PYG", "UYU",
        "DKK", "NOK", "SEK", "ISK", "PLN", "CZK", "HUF", "RON", "BGN", "RSD",
        "HRK", "BAM", "MKD", "ALL", "CHF", "ZAR", "AOA", "MZN", "VES", "BOB",
    )

    // Complete ISO-4217 Currency -> Primary ISO-3166-1 alpha-2 Country/Region code for flag generation
    private val currencyToCountryCode = mapOf(
        "IDR" to "ID", "USD" to "US", "SGD" to "SG", "MYR" to "MY", "JPY" to "JP",
        "EUR" to "EU", "GBP" to "GB", "AUD" to "AU", "KRW" to "KR", "CNY" to "CN",
        "CNH" to "CN", "SAR" to "SA", "AED" to "AE", "THB" to "TH", "PHP" to "PH",
        "VND" to "VN", "BND" to "BN", "HKD" to "HK", "TWD" to "TW", "INR" to "IN",
        "CHF" to "CH", "CAD" to "CA", "NZD" to "NZ", "TRY" to "TR", "BRL" to "BR",
        "RUB" to "RU", "QAR" to "QA", "KWD" to "KW", "EGP" to "EG",
        "AFN" to "AF", "ALL" to "AL", "AMD" to "AM", "ANG" to "CW", "AOA" to "AO",
        "ARS" to "AR", "AWG" to "AW", "AZN" to "AZ", "BAM" to "BA", "BBD" to "BB",
        "BDT" to "BD", "BGN" to "BG", "BHD" to "BH", "BIF" to "BI", "BMD" to "BM",
        "BOB" to "BO", "BOV" to "BO", "BSD" to "BS", "BTN" to "BT", "BWP" to "BW",
        "BYN" to "BY", "BYR" to "BY", "BZD" to "BZ", "CDF" to "CD", "CHE" to "CH",
        "CHW" to "CH", "CLF" to "CL", "CLP" to "CL", "COP" to "CO", "COU" to "CO",
        "CRC" to "CR", "CUC" to "CU", "CUP" to "CU", "CVE" to "CV", "CZK" to "CZ",
        "DJF" to "DJ", "DKK" to "DK", "DOP" to "DO", "DZD" to "DZ", "ERN" to "ER",
        "ETB" to "ET", "FJD" to "FJ", "FKP" to "FK", "GEL" to "GE", "GHS" to "GH",
        "GIP" to "GI", "GMD" to "GM", "GNF" to "GN", "GTQ" to "GT", "GYD" to "GY",
        "HNL" to "HN", "HRK" to "HR", "HTG" to "HT", "HUF" to "HU", "ILS" to "IL",
        "IQD" to "IQ", "IRR" to "IR", "ISK" to "IS", "JMD" to "JM", "JOD" to "JO",
        "KES" to "KE", "KGS" to "KG", "KHR" to "KH", "KMF" to "KM", "KPW" to "KP",
        "KYD" to "KY", "KZT" to "KZ", "LAK" to "LA", "LBP" to "LB", "LKR" to "LK",
        "LRD" to "LR", "LSL" to "LS", "LYD" to "LY", "MAD" to "MA", "MDL" to "MD",
        "MGA" to "MG", "MKD" to "MK", "MMK" to "MM", "MNT" to "MN", "MOP" to "MO",
        "MRU" to "MR", "MRO" to "MR", "MUR" to "MU", "MVR" to "MV", "MWK" to "MW",
        "MXN" to "MX", "MXV" to "MX", "MZN" to "MZ", "NAD" to "NA", "NGN" to "NG",
        "NIO" to "NI", "NOK" to "NO", "NPR" to "NP", "OMR" to "OM", "PAB" to "PA",
        "PEN" to "PE", "PGK" to "PG", "PKR" to "PK", "PLN" to "PL", "PYG" to "PY",
        "RON" to "RO", "RSD" to "RS", "RWF" to "RW", "SBD" to "SB", "SCR" to "SC",
        "SDG" to "SD", "SEK" to "SE", "SHP" to "SH", "SLE" to "SL", "SLL" to "SL",
        "SOS" to "SO", "SRD" to "SR", "SSP" to "SS", "STN" to "ST", "SVC" to "SV",
        "SYP" to "SY", "SZL" to "SZ", "TJS" to "TJ", "TMT" to "TM", "TND" to "TN",
        "TOP" to "TO", "TTD" to "TT", "TZS" to "TZ", "UAH" to "UA", "UGX" to "UG",
        "USN" to "US", "UYI" to "UY", "UYU" to "UY", "UZS" to "UZ", "VED" to "VE",
        "VES" to "VE", "VEF" to "VE", "VUV" to "VU", "WST" to "WS", "XAF" to "CM",
        "XCD" to "AG", "XCG" to "CW", "XOF" to "SN", "XPF" to "PF", "YER" to "YE",
        "ZAR" to "ZA", "ZMW" to "ZM", "ZWL" to "ZW", "ZWG" to "ZW",
    )

    private val curatedOverrides = mapOf(
        "IDR" to CurrencyOverride("Rupiah Indonesia", "Rp", true, 0, "Indonesia", "ID"),
        "USD" to CurrencyOverride("Dolar Amerika Serikat", "$", false, 2, "Amerika Serikat", "US"),
        "SGD" to CurrencyOverride("Dolar Singapura", "S$", false, 2, "Singapura", "SG"),
        "MYR" to CurrencyOverride("Ringgit Malaysia", "RM", false, 2, "Malaysia", "MY"),
        "JPY" to CurrencyOverride("Yen Jepang", "¥", false, 0, "Jepang", "JP"),
        "EUR" to CurrencyOverride("Euro Uni Eropa", "€", true, 2, "Uni Eropa", "EU"),
        "GBP" to CurrencyOverride("Pound Sterling Inggris", "£", false, 2, "Inggris", "GB"),
        "AUD" to CurrencyOverride("Dolar Australia", "A$", false, 2, "Australia", "AU"),
        "KRW" to CurrencyOverride("Won Korea Selatan", "₩", false, 0, "Korea Selatan", "KR"),
        "CNY" to CurrencyOverride("Yuan Tiongkok", "¥", false, 2, "Tiongkok", "CN"),
        "SAR" to CurrencyOverride("Riyal Arab Saudi", "SAR", false, 2, "Arab Saudi", "SA"),
        "AED" to CurrencyOverride("Dirham Uni Emirat Arab", "AED", false, 2, "Uni Emirat Arab", "AE"),
        "THB" to CurrencyOverride("Baht Thailand", "฿", false, 2, "Thailand", "TH"),
        "PHP" to CurrencyOverride("Peso Filipina", "₱", false, 2, "Filipina", "PH"),
        "VND" to CurrencyOverride("Dong Vietnam", "₫", true, 0, "Vietnam", "VN"),
        "BND" to CurrencyOverride("Dolar Brunei", "B$", false, 2, "Brunei Darussalam", "BN"),
        "HKD" to CurrencyOverride("Dolar Hong Kong", "HK$", false, 2, "Hong Kong", "HK"),
        "TWD" to CurrencyOverride("Dolar Baru Taiwan", "NT$", false, 2, "Taiwan", "TW"),
        "INR" to CurrencyOverride("Rupee India", "₹", false, 2, "India", "IN"),
        "CHF" to CurrencyOverride("Franc Swiss", "CHF", true, 2, "Swiss", "CH"),
        "CAD" to CurrencyOverride("Dolar Kanada", "C$", false, 2, "Kanada", "CA"),
        "NZD" to CurrencyOverride("Dolar Selandia Baru", "NZ$", false, 2, "Selandia Baru", "NZ"),
        "TRY" to CurrencyOverride("Lira Turki", "₺", true, 2, "Turki", "TR"),
        "BRL" to CurrencyOverride("Real Brasil", "R$", true, 2, "Brasil", "BR"),
        "RUB" to CurrencyOverride("Rubel Rusia", "₽", true, 2, "Rusia", "RU"),
        "QAR" to CurrencyOverride("Riyal Qatar", "QAR", false, 2, "Qatar", "QA"),
        "KWD" to CurrencyOverride("Dinar Kuwait", "KWD", false, 3, "Kuwait", "KW"),
        "EGP" to CurrencyOverride("Pound Mesir", "E£", false, 2, "Mesir", "EG"),
    )

    // Baseline fallback exchange rates relative to 1 IDR so conversion works offline/first launch
    val seedRatesPerIdr: Map<String, Double> = mapOf(
        "IDR" to 1.0,
        "USD" to 0.0000628,
        "SGD" to 0.0000842,
        "MYR" to 0.000279,
        "JPY" to 0.00968,
        "EUR" to 0.0000585,
        "GBP" to 0.0000496,
        "AUD" to 0.0000965,
        "KRW" to 0.0875,
        "CNY" to 0.000454,
        "SAR" to 0.000235,
        "AED" to 0.000231,
        "THB" to 0.00216,
        "PHP" to 0.00368,
        "VND" to 1.595,
        "BND" to 0.0000842,
        "HKD" to 0.000489,
        "TWD" to 0.00203,
        "INR" to 0.00531,
        "CHF" to 0.0000554,
        "CAD" to 0.0000879,
        "NZD" to 0.000106,
        "TRY" to 0.00216,
        "BRL" to 0.000362,
        "RUB" to 0.00615,
        "QAR" to 0.000229,
        "KWD" to 0.0000193,
        "EGP" to 0.00310,
    )

    val popularCodes = listOf("IDR", "USD", "SGD", "MYR", "JPY", "EUR", "GBP", "AUD", "KRW", "SAR", "CNY", "THB")

    private val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    private val regionalCurrenciesAllowed = setOf("XAF", "XCD", "XOF", "XPF")

    fun countryCodeToFlagEmoji(countryCode: String): String {
        val cc = countryCode.trim().uppercase(Locale.ROOT)
        if (cc.length != 2 || !cc[0].isLetter() || !cc[1].isLetter()) return "\uD83C\uDF10"
        val first = Character.toChars(0x1F1E6 + (cc[0].code - 'A'.code))
        val second = Character.toChars(0x1F1E6 + (cc[1].code - 'A'.code))
        return String(first) + String(second)
    }

    val allCurrencies: List<CurrencyInfo> by lazy {
        val jvmCurrencies = Currency.getAvailableCurrencies()
            .filter { cur ->
                val code = cur.currencyCode.uppercase(Locale.ROOT)
                code.length == 3 &&
                    (!code.startsWith("X") || code in regionalCurrenciesAllowed) &&
                    cur.defaultFractionDigits >= 0
            }

        val mapped = jvmCurrencies.map { cur ->
            val code = cur.currencyCode.uppercase(Locale.ROOT)
            val override = curatedOverrides[code]
            val cc = override?.countryCode
                ?: currencyToCountryCode[code]
                ?: code.take(2)
            val flag = countryCodeToFlagEmoji(cc)
            val countryDisplay = override?.regionLabel
                ?: runCatching {
                    Locale.Builder().setRegion(cc).build().getDisplayCountry(idLocale)
                }.getOrNull()?.takeIf { it.isNotBlank() && it != cc }
                ?: code

            val usesDotGrouping = override?.dotGrouping ?: (code in dotGroupingCurrencies)
            val displayName = override?.nameId ?: cur.getDisplayName(idLocale).replaceFirstChar {
                it.uppercase(idLocale)
            }
            val symbol = override?.symbol ?: cur.getSymbol(Locale.US).takeIf { it.isNotBlank() && it != code } ?: code
            val decimals = override?.decimalDigits ?: cur.defaultFractionDigits.coerceIn(0, 2)
            CurrencyInfo(
                code = code,
                nameId = displayName,
                symbol = symbol,
                groupingSeparator = if (usesDotGrouping) '.' else ',',
                decimalSeparator = if (usesDotGrouping) ',' else '.',
                decimalDigits = decimals,
                regionLabel = countryDisplay,
                countryCode = cc,
                flagEmoji = flag,
            )
        }.distinctBy { it.code }

        val popularOrder = popularCodes.withIndex().associate { it.value to it.index }
        mapped.sortedWith(
            compareBy<CurrencyInfo> { popularOrder[it.code] ?: Int.MAX_VALUE }
                .thenBy { it.code }
        )
    }

    fun getInfo(code: String): CurrencyInfo {
        val normalized = code.trim().uppercase(Locale.ROOT)
        return allCurrencies.firstOrNull { it.code == normalized }
            ?: curatedOverrides["IDR"]!!.let {
                CurrencyInfo(
                    code = "IDR",
                    nameId = it.nameId,
                    symbol = it.symbol,
                    groupingSeparator = '.',
                    decimalSeparator = ',',
                    decimalDigits = 0,
                    regionLabel = "Indonesia",
                    countryCode = "ID",
                    flagEmoji = countryCodeToFlagEmoji("ID"),
                )
            }
    }

    fun buildDisplayConfig(
        selectedCode: String,
        liveRates: Map<String, Double>,
        updatedAtMillis: Long,
    ): CurrencyDisplayConfig {
        val info = getInfo(selectedCode)
        val rate = if (info.code == "IDR") {
            1.0
        } else {
            liveRates[info.code]?.takeIf { it > 0.0 }
                ?: seedRatesPerIdr[info.code]
                ?: 1.0
        }
        return CurrencyDisplayConfig(
            code = info.code,
            nameId = info.nameId,
            symbol = info.symbol,
            rateFromIdr = rate,
            groupingSeparator = info.groupingSeparator,
            decimalSeparator = info.decimalSeparator,
            decimalDigits = info.decimalDigits,
            countryCode = info.countryCode,
            flagEmoji = info.flagEmoji,
            updatedAtMillis = updatedAtMillis,
        )
    }
}
