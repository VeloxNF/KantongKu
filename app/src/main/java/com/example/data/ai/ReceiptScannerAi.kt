package com.example.data.ai

import android.graphics.Bitmap
import com.example.data.currency.CurrencyCatalog
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.WalletEntity
import com.example.ui.components.POCKET_CATEGORY_CATALOG
import com.example.ui.components.pocketCategoryFor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToLong

enum class ReceiptTransactionKind(val label: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan"),
    TRANSFER("Pindah Saldo");

    companion object {
        fun fromRaw(raw: String?): ReceiptTransactionKind =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: EXPENSE
    }
}

data class ScannedReceiptDraft(
    val kind: ReceiptTransactionKind,
    val title: String,
    val locationName: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val amountIdr: Long,
    val detectedCurrencyCode: String,
    val originalForeignAmountText: String,
    val subtotalText: String,
    val taxAndAdminText: String,
    val discountText: String,
    val amountCheckReasoning: String,
    val categoryKey: String,
    val matchedWalletId: String,
    val matchedFromWalletId: String,
    val matchedToWalletId: String,
    val matchedPocketId: String,
    val note: String,
    val confidenceLevel: String,
    val warningNote: String,
)

class ReceiptScannerAi {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "transactionKind" to Schema.enumeration(
                values = listOf("EXPENSE", "INCOME", "TRANSFER"),
                description = "EXPENSE untuk struk belanja/merchant/pembayaran QRIS/tagihan keluar. INCOME untuk bukti transfer masuk, gaji, refund, komisi masuk. TRANSFER untuk pemindahan saldo antar rekening/e-wallet milik pengguna sendiri (termasuk top-up e-wallet dari bank milik sendiri).",
            ),
            "title" to Schema.string(
                description = "Nama merchant, sumber transfer, atau deskripsi transaksi singkat dalam Bahasa Indonesia (maksimal 60 karakter).",
            ),
            "locationName" to Schema.string(
                description = "Khusus untuk EXPENSE: nama tempat/toko/cabang/lokasi tertulis pada struk (misal 'Indomaret Sudirman', 'Starbucks Grand Indonesia', 'SPBU Pertamina 31.123'); kosongkan '' jika INCOME/TRANSFER atau tidak tertera.",
            ),
            "detectedCurrencyCode" to Schema.string(
                description = "Kode mata uang 3 huruf ISO-4217 yang tertera pada struk (default 'IDR' jika Rupiah/Rp). Contoh: IDR, USD, SGD, MYR, JPY.",
            ),
            "rawNumericAmountString" to Schema.string(
                description = "Angka TOTAL AKHIR yang benar-benar dibayar/diterima/ditransfer dalam digit polos (dan titik desimal hanya jika mata uang asing punya sen bukan .00, misal '12.50'). Untuk IDR, HILANGKAN .00 / ,00 desimal nol di akhir dan semua pemisah ribuan sehingga Rp 45.000,00 ditulis '45000'. Jangan pernah mengambil angka Tunai/Cash atau Kembalian.",
            ),
            "subtotalAmount" to Schema.string(
                description = "Subtotal sebelum pajak/biaya/diskon jika tertulis di struk (atau '' jika tidak ada).",
            ),
            "taxAndAdminAmount" to Schema.string(
                description = "Pajak/PPN/Service/Biaya Admin/Ongkir jika tertulis di struk (atau '' jika tidak ada).",
            ),
            "discountAmount" to Schema.string(
                description = "Potongan/Diskon/Promo jika tertulis di struk (atau '' jika tidak ada).",
            ),
            "amountCheckReasoning" to Schema.string(
                description = "Verifikasi aritmetika singkat dalam Bahasa Indonesia yang membuktikan GRAND TOTAL sesuai rincian item, pajak, dan diskon, dan membedakannya dari Uang Tunai / Kembalian.",
            ),
            "categoryKey" to Schema.enumeration(
                values = POCKET_CATEGORY_CATALOG.map { it.key },
                description = "Pilih TEPAT SATU key kategori paling cocok dari daftar katalog KantongKu.",
            ),
            "matchedWalletId" to Schema.string(
                description = "ID dompet pengguna dari DAFTAR DOMPET yang paling cocok sebagai sumber dana (EXPENSE) atau tujuan dana (INCOME). Kosongkan '' jika tidak terlihat jelas di struk.",
            ),
            "matchedFromWalletId" to Schema.string(
                description = "Hanya untuk TRANSFER: ID dompet asal dari DAFTAR DOMPET, atau '' jika tidak pasti.",
            ),
            "matchedToWalletId" to Schema.string(
                description = "Hanya untuk TRANSFER: ID dompet tujuan dari DAFTAR DOMPET, atau '' jika tidak pasti.",
            ),
            "matchedPocketId" to Schema.string(
                description = "ID kantong dari DAFTAR KANTONG jika sesuai dengan kategori belanja/pemasukan. Kosongkan '' jika tidak ada kantong yang cocok.",
            ),
            "note" to Schema.string(
                description = "Catatan detail isi struk: daftar item & jumlah, biaya admin/pajak, nomor referensi/tanggal jika ada (Bahasa Indonesia, maks 450 karakter).",
            ),
            "confidenceLevel" to Schema.enumeration(
                values = listOf("HIGH", "MEDIUM", "LOW"),
                description = "HIGH jika teks dan nominal total terbaca tajam dan cocok aritmetikanya; MEDIUM jika sebagian buram; LOW jika foto sangat kabur atau bukan struk.",
            ),
            "warningNote" to Schema.string(
                description = "Peringatan singkat jika gambar terpotong, buram, atau angka total perlu dicek ulang oleh pengguna; kosongkan '' jika terbaca jelas dan yakin.",
            ),
        ),
        optionalProperties = listOf(
            "locationName",
            "subtotalAmount",
            "taxAndAdminAmount",
            "discountAmount",
            "matchedWalletId",
            "matchedFromWalletId",
            "matchedToWalletId",
            "matchedPocketId",
            "warningNote",
        ),
    )

    private val systemPrompt = """
        Kamu adalah mesin OCR & Audit Keuangan Struk Presisi Tinggi untuk aplikasi KantongKu di Indonesia.
        Tugasmu membaca foto struk fisik (kertas thermal kasir, struk ATM/EDC, bon makan) MAUPUN tangkapan layar struk digital (m-banking BCA, BRI/BRImo, Mandiri/Livin, BNI, Jago, SeaBank, GoPay, OVO, DANA, ShopeePay, QRIS, Grab/Gojek, marketplace).

        ATURAN AKURASI MUTLAK (WAJIB DIPATUHI):
        1. BACA SEMUA BARIS ITEM DAN HITUNG ULANG: Periksa harga tiap item, subtotal, diskon/voucher, PPN/PB1/service charge, biaya admin, ongkir, hingga GRAND TOTAL / TOTAL BAYAR / TOTAL TAGIHAN / TOTAL TRANSAKSI.
        2. JANGAN PERNAH TERTUKAR NOMINAL:
           - Untuk struk kasir fisik: pilih GRAND TOTAL / TOTAL BELANJA. JANGAN ambil 'TUNAI / CASH / BAYAR / UANG DITERIMA' dan JANGAN ambil 'KEMBALI / CHANGE'.
           - Untuk bukti transfer bank/e-wallet: jumlahkan jika biaya admin terpisah hanya apabila memang tercatat keluar, atau ambil Total Debit / Nominal Transfer yang sesuai bukti.
           - Hati-hati terhadap font thermal yang mirip (0 vs 8, 1 vs 7, 3 vs 8, 5 vs 6, 9 vs 4): validasikan menggunakan penjumlahan item + pajak - diskon di `amountCheckReasoning`!
        3. PEMFORMATAN RUPIAH (IDR) vs MATA UANG ASING:
           - Struk Indonesia sering menulis `Rp 45.000`, `Rp45,000`, atau `Rp 45.000,00` / `45,000.00`. Jika `detectedCurrencyCode == "IDR"`, `.00` atau `,00` di akhir adalah sen nol — HILANGKAN, sehingga menjadi `"45000"` (bukan `"4500000"`). Pemisah ribuan `.` atau `,` harus dihapus agar menjadi integer bersih.
           - Jika mata uang asing (USD, SGD, MYR, EUR, dll.) memiliki sen bukan nol (misal `$12.50`), tulis `"12.50"`.
        4. BEDAKAN JENIS TRANSAKSI:
           - `EXPENSE`: pembelian barang/makanan/bensin/tiket/obat/bayar tagihan/QRIS ke merchant/transfer ke pihak lain.
           - `INCOME`: transfer masuk (`Dana Masuk`, `Transfer Masuk`, `Gaji`, `Refund`, `Cashback`).
           - `TRANSFER`: pemindahan saldo antar dompet milik pengguna sendiri (misal top-up GoPay/OVO/DANA dari rekening bank pengguna sendiri, atau transfer antar 2 dompet di DAFTAR DOMPET pengguna).
        5. KATEGORI & KANTONG:
           - Pilih `categoryKey` paling spesifik dari katalog yang diberikan (misal kopi -> COFFEE, minimarket -> GROCERIES, bensin -> FUEL, bioskop -> CINEMA, apotek -> PHARMACY, listrik/air -> UTILITIES, gaji -> SALARY).
           - Cocokkan `matchedPocketId` HANYA jika kantong yang relevan benar-benar ada di DAFTAR KANTONG pengguna; jika tidak ada, isi `""`.
        6. KEJUJURAN: Jika gambar bukan struk atau terlalu kabur hingga angka total tidak terbaca, set `confidenceLevel = "LOW"`, tulis `"0"` jika tidak ada angka valid, dan jelaskan di `warningNote`. Jangan mengarang angka.
    """.trimIndent()

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.0f
                responseMimeType = "application/json"
                responseSchema = receiptSchema
            },
            systemInstruction = content { text(systemPrompt) },
        )
    }

    suspend fun analyzeReceipt(
        bitmap: Bitmap,
        wallets: List<WalletEntity>,
        pockets: List<PocketEntity>,
        exchangeRates: Map<String, Double>,
    ): ScannedReceiptDraft {
        val promptText = buildPromptContext(wallets, pockets)
        val inputContent = content {
            image(bitmap)
            text(promptText)
        }

        val response = try {
            withTimeout(60_000L) {
                generativeModel.generateContent(inputContent)
            }
        } catch (e: CancellationException) {
            throw e
        }

        val rawJson = response.text.orEmpty().trim()
        if (rawJson.isBlank()) {
            throw IllegalStateException("AI tidak mengembalikan hasil pembacaan struk.")
        }

        return parseResponse(
            rawJson = rawJson,
            wallets = wallets,
            pockets = pockets,
            exchangeRates = exchangeRates,
        )
    }

    private fun buildPromptContext(
        wallets: List<WalletEntity>,
        pockets: List<PocketEntity>,
    ): String {
        val walletBlock = if (wallets.isEmpty()) {
            "- Belum ada dompet terdaftar"
        } else {
            wallets.joinToString("\n") { w ->
                val defaultTag = if (w.isDefault) " [DOMPET UTAMA]" else ""
                "- id=\"${w.id}\" | nama=\"${w.name}\" | jenis=${w.walletType} | bank/ewallet=\"${w.providerShortName} (${w.providerCompanyName})\"$defaultTag"
            }
        }

        val pocketBlock = if (pockets.isEmpty()) {
            "- Belum ada kantong alokasi terdaftar"
        } else {
            pockets.joinToString("\n") { p ->
                val modeTag = if (p.pocketMode == PocketMode.BUDGET_LIMIT) "Alokasi Pengeluaran" else "Target Tabungan"
                "- id=\"${p.id}\" | nama=\"${p.name}\" | iconKey=${p.iconKey} | mode=$modeTag"
            }
        }

        val catalogBlock = POCKET_CATEGORY_CATALOG.joinToString("\n") { cat ->
            "- ${cat.key}: ${cat.label} (${cat.keywords})"
        }

        return """
            Analisis gambar struk/bukti transaksi terlampir dengan akurasi maksimal dan keluarkan JSON terstruktur.

            [DAFTAR DOMPET PENGGUNA]
            $walletBlock

            [DAFTAR KANTONG PENGGUNA]
            $pocketBlock

            [KATALOG KATEGORI RESMI]
            $catalogBlock
        """.trimIndent()
    }

    private fun parseResponse(
        rawJson: String,
        wallets: List<WalletEntity>,
        pockets: List<PocketEntity>,
        exchangeRates: Map<String, Double>,
    ): ScannedReceiptDraft {
        val cleanJson = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val obj = JSONObject(cleanJson)

        val kind = ReceiptTransactionKind.fromRaw(obj.optString("transactionKind", "EXPENSE"))
        val title = obj.optString("title", "").trim().ifBlank { "Transaksi Struk" }.take(80)
        val currencyCode = obj.optString("detectedCurrencyCode", "IDR")
            .trim()
            .uppercase()
            .take(3)
            .ifBlank { "IDR" }
        val rawAmountStr = obj.optString("rawNumericAmountString", "0").trim()

        val parsedForeignAmount = parseRawAmountValue(rawAmountStr, currencyCode)
        val amountIdr = convertForeignToIdr(
            amount = parsedForeignAmount,
            currencyCode = currencyCode,
            exchangeRates = exchangeRates,
        )

        val validCategoryKeys = POCKET_CATEGORY_CATALOG.map { it.key }.toSet()
        val rawCatKey = obj.optString("categoryKey", "OTHER").trim().uppercase()
        val categoryKey = if (rawCatKey in validCategoryKeys) rawCatKey else "OTHER"

        val validWalletIds = wallets.map { it.id }.toSet()
        val defaultWalletId = (wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull())?.id.orEmpty()

        val aiWalletId = obj.optString("matchedWalletId", "").trim()
        val resolvedWalletId = if (aiWalletId in validWalletIds) aiWalletId else defaultWalletId

        val aiFromId = obj.optString("matchedFromWalletId", "").trim()
        val aiToId = obj.optString("matchedToWalletId", "").trim()
        val resolvedFromId = if (aiFromId in validWalletIds) aiFromId else defaultWalletId
        val resolvedToId = if (aiToId in validWalletIds && aiToId != resolvedFromId) {
            aiToId
        } else {
            wallets.firstOrNull { it.id != resolvedFromId }?.id.orEmpty()
        }

        val validPocketIds = pockets.map { it.id }.toSet()
        val aiPocketId = obj.optString("matchedPocketId", "").trim()
        val matchedPocketId = when {
            kind == ReceiptTransactionKind.TRANSFER -> ""
            aiPocketId in validPocketIds -> aiPocketId
            else -> pockets.firstOrNull { it.iconKey.equals(categoryKey, ignoreCase = true) }?.id.orEmpty()
        }

        val subtotalText = obj.optString("subtotalAmount", "").trim()
        val taxAndAdminText = obj.optString("taxAndAdminAmount", "").trim()
        val discountText = obj.optString("discountAmount", "").trim()
        val reasoning = obj.optString("amountCheckReasoning", "").trim()
        val note = obj.optString("note", "").trim().take(500)
        val locationName = if (kind == ReceiptTransactionKind.EXPENSE) {
            obj.optString("locationName", "").trim().take(100)
        } else {
            ""
        }
        val confidence = obj.optString("confidenceLevel", "HIGH").trim().uppercase()
        val warning = obj.optString("warningNote", "").trim()

        val resolvedCategory = pocketCategoryFor(categoryKey)

        return ScannedReceiptDraft(
            kind = kind,
            title = title.ifBlank { resolvedCategory.label },
            locationName = locationName,
            occurredAtMillis = System.currentTimeMillis(),
            amountIdr = amountIdr.coerceAtLeast(0L),
            detectedCurrencyCode = currencyCode,
            originalForeignAmountText = if (currencyCode != "IDR" && parsedForeignAmount > 0.0) {
                "$currencyCode $rawAmountStr"
            } else {
                ""
            },
            subtotalText = subtotalText,
            taxAndAdminText = taxAndAdminText,
            discountText = discountText,
            amountCheckReasoning = reasoning,
            categoryKey = categoryKey,
            matchedWalletId = resolvedWalletId,
            matchedFromWalletId = resolvedFromId,
            matchedToWalletId = resolvedToId,
            matchedPocketId = matchedPocketId,
            note = note,
            confidenceLevel = confidence,
            warningNote = warning,
        )
    }

    private fun parseRawAmountValue(raw: String, currencyCode: String): Double {
        if (raw.isBlank()) return 0.0
        var cleaned = raw.replace(Regex("[^0-9.,]"), "")
        if (cleaned.isBlank()) return 0.0

        if (currencyCode == "IDR") {
            // Strip trailing decimal zero cents (.00 or ,00) common on Indonesian POS & bank receipts
            if (cleaned.endsWith(".00") || cleaned.endsWith(",00")) {
                cleaned = cleaned.dropLast(3)
            }
            val digitsOnly = cleaned.filter { it.isDigit() }
            return digitsOnly.toDoubleOrNull() ?: 0.0
        }

        // Foreign currency: detect Indonesian style 1.250,50 vs US style 1,250.50 vs plain 12.50
        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 -> {
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
            lastComma >= 0 -> {
                val afterComma = cleaned.length - lastComma - 1
                if (afterComma in 1..2) cleaned.replace(',', '.') else cleaned.replace(",", "")
            }
            lastDot >= 0 -> {
                val afterDot = cleaned.length - lastDot - 1
                if (afterDot in 1..2) cleaned else cleaned.replace(".", "")
            }
            else -> cleaned
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun convertForeignToIdr(
        amount: Double,
        currencyCode: String,
        exchangeRates: Map<String, Double>,
    ): Long {
        if (amount <= 0.0) return 0L
        if (currencyCode == "IDR") return amount.roundToLong()
        val rateFromIdr = exchangeRates[currencyCode]?.takeIf { it > 0.0 }
            ?: CurrencyCatalog.seedRatesPerIdr[currencyCode]?.takeIf { it > 0.0 }
            ?: return amount.roundToLong()
        return (amount / rateFromIdr).roundToLong().coerceAtLeast(1L)
    }
}
