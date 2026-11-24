package com.example.data.ai

import com.example.data.local.LocalAccountEntity
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.FinanceDashboardStats
import com.example.util.FormatUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiExtractedTransaction(
    val type: TransactionType,
    val amount: Long,
    val title: String,
    val note: String,
    val pocketName: String,
)

data class AiParsedTurnResult(
    val reply: String,
    val actionType: String,
    val transactions: List<AiExtractedTransaction>,
)

sealed interface AiStreamChunk {
    data class PartialReply(val text: String) : AiStreamChunk
    data class Completed(val result: AiParsedTurnResult) : AiStreamChunk
}

class FinanceAiAssistant {

    private val chatResponseSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Jawaban singkat, ramah, dalam bahasa Indonesia. Jika transaksi dicatat, konfirmasi nominal, judul, catatan, dan nama kantongnya. Jika nominal belum jelas, tanyakan nominalnya. Jika topik di luar keuangan KantongKu, tolak dengan sopan dan arahkan kembali ke pencatatan keuangan."
            ),
            "actionType" to Schema.enumeration(
                values = listOf("NONE", "CREATE_TRANSACTION", "QUERY_SUMMARY"),
                description = "Gunakan CREATE_TRANSACTION jika pengguna menyebut pengeluaran atau pemasukan dengan nominal yang jelas untuk dicatat otomatis. Gunakan QUERY_SUMMARY jika pengguna menanyakan saldo, sisa harian, target kantong, atau riwayat. Gunakan NONE untuk sapaan, klarifikasi nominal yang kurang, atau penolakan topik di luar aplikasi."
            ),
            "transactions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "type" to Schema.enumeration(
                            values = listOf("EXPENSE", "INCOME"),
                            description = "EXPENSE untuk beli, bayar, jajan, transfer keluar, ongkos; INCOME untuk gaji, bonus, terima uang, masuk tabungan, cashback."
                        ),
                        "amount" to Schema.integer(
                            description = "Nominal bulat positif dalam Rupiah (IDR). Contoh: 25rb = 25000, 1.5jt = 1500000."
                        ),
                        "title" to Schema.string(
                            description = "Judul singkat transaksi dalam bahasa Indonesia (maksimal 60 karakter)."
                        ),
                        "note" to Schema.string(
                            description = "Catatan detail isi pembelian atau sumber pemasukan dalam bahasa Indonesia."
                        ),
                        "pocketName" to Schema.string(
                            description = "Nama persis salah satu kantong dari daftar KONTEKS KANTONGKU jika cocok, atau string kosong jika tidak ada yang cocok."
                        ),
                    ),
                    optionalProperties = listOf("pocketName"),
                ),
                description = "Daftar transaksi baru yang harus dicatat otomatis ke aplikasi. Kosongkan ([]) jika tidak ada nominal baru untuk dicatat."
            ),
        ),
        optionalProperties = listOf("transactions"),
    )

    private val systemPrompt = """
        Kamu adalah Asisten Keuangan Khusus aplikasi KantongKu.
        TUGAS UTAMAMU:
        1. Membantu pengguna mencatat PENGELUARAN dan PEMASUKAN secara otomatis dari bahasa sehari-hari (contoh: "barusan beli kopi 25rb dan makan siang 30rb", "terima gaji 5 juta", "isi bensin 50k pakai transport", "nabung 200rb ke dana darurat").
        2. Membantu pengguna memantau kondisi keuangannya di KantongKu berdasarkan KONTEKS KANTONGKU TERKINI (saldo total, sisa jatah hari ini, hitung mundur gajian, progres/sisa tiap Kantong, dan riwayat transaksi terbaru).
        3. Selalu isi field `note` secara detail agar pengguna tahu dibelanjakan untuk apa saja atau berasal dari sumber mana.
        4. Cocokkan `pocketName` dengan nama kantong yang ada di daftar Kantong milik pengguna jika relevan (misal beli makan/minum -> "Makan/Minum", ojek/bensin -> "Transportasi", menabung -> "Dana Darurat"). Jika tidak cocok dengan kantong mana pun, isi "".
        5. Jika pengguna menyebut membeli/menerima sesuatu TETAPI nominal uang belum disebutkan, JANGAN mengarang nominal — set `actionType` ke "NONE", `transactions` ke [], dan tanyakan nominalnya dengan sopan.
        6. BATASAN KETAT: Kamu HANYA melayani hal-hal yang berkaitan dengan aplikasi KantongKu (catat pemasukan, catat pengeluaran, cek target kantong, sisa anggaran harian, dan tips ringkas pengelolaan uang di KantongKu). Jika pengguna bertanya topik di luar aplikasi (seperti koding, politik, hiburan, soal ujian, resep masakan, atau obrolan umum yang tidak berkaitan dengan pencatatan uang), tolak dengan sopan dan ingatkan bahwa kamu khusus membantu pencatatan dan pemantauan keuangan di KantongKu.
        7. Gunakan Bahasa Indonesia yang natural, ringkas, dan jelas.
    """.trimIndent()

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = chatResponseSchema
            },
            systemInstruction = content { text(systemPrompt) },
        )
    }

    private var activeChat: Chat? = null

    fun resetSession() {
        activeChat = null
    }

    private fun getOrStartChat(): Chat {
        val existing = activeChat
        if (existing != null) return existing
        val created = generativeModel.startChat()
        activeChat = created
        return created
    }

    fun sendMessageStream(
        userMessage: String,
        account: LocalAccountEntity,
        stats: FinanceDashboardStats,
        pockets: List<PocketEntity>,
        recentTransactions: List<TransactionEntity>,
    ): Flow<AiStreamChunk> = flow {
        val chat = getOrStartChat()
        val contextualPrompt = buildContextualPrompt(
            userMessage = userMessage,
            account = account,
            stats = stats,
            pockets = pockets,
            recentTransactions = recentTransactions,
        )

        val accumulatedJson = StringBuilder()
        var lastPreview = ""

        withTimeout(60_000L) {
            chat.sendMessageStream(contextualPrompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulatedJson.append(piece)
                    val preview = extractReplyPreviewFromPartialJson(accumulatedJson.toString())
                    if (!preview.isNullOrBlank() && preview != lastPreview) {
                        lastPreview = preview
                        emit(AiStreamChunk.PartialReply(preview))
                    }
                }
            }
        }

        val rawJson = accumulatedJson.toString()
        if (rawJson.isBlank()) {
            throw IllegalStateException("Asisten AI tidak mengembalikan respons.")
        }

        emit(AiStreamChunk.Completed(parseStructuredResult(rawJson)))
    }

    private fun parseStructuredResult(rawJson: String): AiParsedTurnResult {
        val obj = JSONObject(rawJson)
        val reply = obj.optString("reply", "").trim()
            .ifBlank { "Catatan telah diproses." }
        val actionType = obj.optString("actionType", "NONE")
        val txList = mutableListOf<AiExtractedTransaction>()

        val txArray = obj.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val item = txArray.optJSONObject(i) ?: continue
                val typeStr = item.optString("type", "EXPENSE")
                val amount = item.optLong("amount", 0L)
                val title = item.optString("title", "").trim().take(80)
                val note = item.optString("note", "").trim().take(500)
                val pocketName = item.optString("pocketName", "").trim()
                if (amount > 0L && title.isNotBlank()) {
                    txList.add(
                        AiExtractedTransaction(
                            type = TransactionType.fromRaw(typeStr),
                            amount = amount,
                            title = title,
                            note = note,
                            pocketName = pocketName,
                        )
                    )
                }
            }
        }

        return AiParsedTurnResult(
            reply = reply,
            actionType = actionType,
            transactions = txList,
        )
    }

    private fun buildContextualPrompt(
        userMessage: String,
        account: LocalAccountEntity,
        stats: FinanceDashboardStats,
        pockets: List<PocketEntity>,
        recentTransactions: List<TransactionEntity>,
    ): String {
        val pocketsBlock = if (pockets.isEmpty()) {
            "- Belum ada kantong"
        } else {
            pockets.joinToString("\n") { pkt ->
                val modeDesc = if (pkt.pocketMode == PocketMode.BUDGET_LIMIT) {
                    "Batas Pengeluaran | Terpakai ${FormatUtils.formatRupiah(pkt.currentAmount)} / Target ${FormatUtils.formatRupiah(pkt.targetAmount)} (Sisa ${FormatUtils.formatRupiah(pkt.remainingAmount)})"
                } else {
                    "Target Tabungan | Terkumpul ${FormatUtils.formatRupiah(pkt.currentAmount)} / Target ${FormatUtils.formatRupiah(pkt.targetAmount)}"
                }
                "- ${pkt.name} [$modeDesc]"
            }
        }

        val recentBlock = if (recentTransactions.isEmpty()) {
            "- Belum ada transaksi"
        } else {
            recentTransactions.take(5).joinToString("\n") { tx ->
                val sign = if (tx.txType == TransactionType.INCOME) "+" else "-"
                val pocketTag = if (tx.pocketName.isNotBlank()) " [${tx.pocketName}]" else ""
                "- ${FormatUtils.formatDateTimeShort(tx.occurredAtMillis)}: $sign${FormatUtils.formatRupiah(tx.amount)} ${tx.title}$pocketTag (Catatan: ${tx.note.ifBlank { "-" }})"
            }
        }

        return """
            [KONTEKS KANTONGKU TERKINI]
            Pengguna: ${account.displayName}
            Tanggal hari ini: ${FormatUtils.formatTodayHeader()}
            Jadwal gajian: ${stats.paydayStats.scheduleBadgeLabel} (${stats.paydayStats.countdownLabel})
            Saldo total: ${FormatUtils.formatRupiah(stats.totalBalance)}
            Sisa jatah harian: ${FormatUtils.formatRupiah(stats.dailySafeRemaining)}
            Pemasukan bulan ini: ${FormatUtils.formatRupiah(stats.monthIncome)}
            Pengeluaran bulan ini: ${FormatUtils.formatRupiah(stats.monthExpense)}
            Daftar Kantong:
            $pocketsBlock
            Transaksi Terbaru:
            $recentBlock

            [PESAN PENGGUNA]
            $userMessage
        """.trimIndent()
    }

    private fun extractReplyPreviewFromPartialJson(partialJson: String): String? {
        val marker = "\"reply\""
        val keyIdx = partialJson.indexOf(marker)
        if (keyIdx < 0) return null
        val colonIdx = partialJson.indexOf(':', startIndex = keyIdx + marker.length)
        if (colonIdx < 0) return null
        var i = colonIdx + 1
        while (i < partialJson.length && partialJson[i].isWhitespace()) i++
        if (i >= partialJson.length || partialJson[i] != '"') return null
        i++

        val out = StringBuilder()
        var escaped = false
        while (i < partialJson.length) {
            val ch = partialJson[i]
            if (escaped) {
                when (ch) {
                    'n' -> out.append('\n')
                    't' -> out.append('\t')
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    else -> out.append(ch)
                }
                escaped = false
            } else {
                when (ch) {
                    '\\' -> escaped = true
                    '"' -> return out.toString()
                    else -> out.append(ch)
                }
            }
            i++
        }
        return out.toString()
    }
}
