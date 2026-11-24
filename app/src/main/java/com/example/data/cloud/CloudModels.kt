package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val paydayFrequency: String = "MONTHLY",
    val paydayDay: Long = 25L,
    val paydayWeekday: Long = 5L,
    val paydayHour: Long = 8L,
    val paydayMinute: Long = 0L,
    val monthlyBudget: Long = 3_000_000L,
    val themeScheme: String = "BENTO_NEON",
    val themeMode: String = "DARK",
    val currencyCode: String = "IDR",
    val languageCode: String = "id",
    val hideBalances: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to displayName.trim().ifBlank { "Pengguna" }.take(60),
        "paydayFrequency" to (if (paydayFrequency in setOf("DAILY", "WEEKLY", "MONTHLY")) paydayFrequency else "MONTHLY"),
        "paydayDay" to paydayDay.coerceIn(1L, 31L),
        "paydayWeekday" to paydayWeekday.coerceIn(1L, 7L),
        "paydayHour" to paydayHour.coerceIn(0L, 23L),
        "paydayMinute" to paydayMinute.coerceIn(0L, 59L),
        "monthlyBudget" to monthlyBudget.coerceIn(0L, 100_000_000_000L),
        "themeScheme" to themeScheme,
        "themeMode" to themeMode,
        "currencyCode" to currencyCode.trim().uppercase().take(3).ifBlank { "IDR" },
        "languageCode" to (languageCode.trim().lowercase().takeIf { it in setOf("zh", "id", "en", "ja", "de", "ko", "ru") } ?: "id"),
        "hideBalances" to hideBalances,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.trim().ifBlank { "Pengguna" }.take(60),
        "paydayFrequency" to (if (paydayFrequency in setOf("DAILY", "WEEKLY", "MONTHLY")) paydayFrequency else "MONTHLY"),
        "paydayDay" to paydayDay.coerceIn(1L, 31L),
        "paydayWeekday" to paydayWeekday.coerceIn(1L, 7L),
        "paydayHour" to paydayHour.coerceIn(0L, 23L),
        "paydayMinute" to paydayMinute.coerceIn(0L, 59L),
        "monthlyBudget" to monthlyBudget.coerceIn(0L, 100_000_000_000L),
        "themeScheme" to themeScheme,
        "themeMode" to themeMode,
        "currencyCode" to currencyCode.trim().uppercase().take(3).ifBlank { "IDR" },
        "languageCode" to (languageCode.trim().lowercase().takeIf { it in setOf("zh", "id", "en", "ja", "de", "ko", "ru") } ?: "id"),
        "hideBalances" to hideBalances,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CloudWallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val walletType: String = "CASH",
    val providerCode: String = "CASH_IDR",
    val providerShortName: String = "Tunai",
    val providerCompanyName: String = "Dompet Uang Tunai",
    val accountNumberHint: String = "",
    val balance: Long = 0L,
    val colorSlot: Long = 0L,
    val isDefault: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().ifBlank { "Dompet" }.take(50),
        "walletType" to (if (walletType in setOf("CASH", "EWALLET", "BANK")) walletType else "CASH"),
        "providerCode" to providerCode.take(40).ifBlank { "CASH_IDR" },
        "providerShortName" to providerShortName.take(60).ifBlank { "Tunai" },
        "providerCompanyName" to providerCompanyName.take(120),
        "accountNumberHint" to accountNumberHint.take(40),
        "balance" to balance.coerceIn(-100_000_000_000L, 100_000_000_000L),
        "colorSlot" to colorSlot.coerceIn(0L, 5L),
        "isDefault" to isDefault,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().ifBlank { "Dompet" }.take(50),
        "walletType" to (if (walletType in setOf("CASH", "EWALLET", "BANK")) walletType else "CASH"),
        "providerCode" to providerCode.take(40).ifBlank { "CASH_IDR" },
        "providerShortName" to providerShortName.take(60).ifBlank { "Tunai" },
        "providerCompanyName" to providerCompanyName.take(120),
        "accountNumberHint" to accountNumberHint.take(40),
        "balance" to balance.coerceIn(-100_000_000_000L, 100_000_000_000L),
        "colorSlot" to colorSlot.coerceIn(0L, 5L),
        "isDefault" to isDefault,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CloudPocket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val mode: String = "BUDGET_LIMIT",
    val targetAmount: Long = 1_000_000L,
    val currentAmount: Long = 0L,
    val iconKey: String = "FOOD",
    val colorSlot: Long = 0L,
    val featured: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().ifBlank { "Kantong" }.take(50),
        "mode" to if (mode == "SAVINGS_GOAL") "SAVINGS_GOAL" else "BUDGET_LIMIT",
        "targetAmount" to targetAmount.coerceIn(1L, 100_000_000_000L),
        "currentAmount" to currentAmount.coerceIn(0L, 100_000_000_000L),
        "iconKey" to iconKey.take(24).ifBlank { "OTHER" },
        "colorSlot" to colorSlot.coerceIn(0L, 5L),
        "featured" to featured,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().ifBlank { "Kantong" }.take(50),
        "mode" to if (mode == "SAVINGS_GOAL") "SAVINGS_GOAL" else "BUDGET_LIMIT",
        "targetAmount" to targetAmount.coerceIn(1L, 100_000_000_000L),
        "currentAmount" to currentAmount.coerceIn(0L, 100_000_000_000L),
        "iconKey" to iconKey.take(24).ifBlank { "OTHER" },
        "colorSlot" to colorSlot.coerceIn(0L, 5L),
        "featured" to featured,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CloudTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val note: String = "",
    val amount: Long = 0L,
    val type: String = "EXPENSE",
    val pocketId: String = "",
    val pocketName: String = "",
    val walletId: String = "",
    val walletName: String = "",
    val categoryKey: String = "OTHER",
    val categoryLabel: String = "",
    val locationName: String = "",
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().ifBlank { "Transaksi" }.take(80),
        "note" to note.trim().take(500),
        "amount" to amount.coerceIn(1L, 100_000_000_000L),
        "type" to if (type == "INCOME") "INCOME" else "EXPENSE",
        "pocketId" to pocketId.take(128),
        "pocketName" to pocketName.take(50),
        "walletId" to walletId.take(128),
        "walletName" to walletName.take(50),
        "categoryKey" to categoryKey.take(24).ifBlank { "OTHER" },
        "categoryLabel" to categoryLabel.take(60),
        "locationName" to if (type == "EXPENSE") locationName.trim().take(100) else "",
        "occurredAtMillis" to occurredAtMillis.coerceAtLeast(1L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "title" to title.trim().ifBlank { "Transaksi" }.take(80),
        "note" to note.trim().take(500),
        "amount" to amount.coerceIn(1L, 100_000_000_000L),
        "type" to if (type == "INCOME") "INCOME" else "EXPENSE",
        "pocketId" to pocketId.take(128),
        "pocketName" to pocketName.take(50),
        "walletId" to walletId.take(128),
        "walletName" to walletName.take(50),
        "categoryKey" to categoryKey.take(24).ifBlank { "OTHER" },
        "categoryLabel" to categoryLabel.take(60),
        "locationName" to if (type == "EXPENSE") locationName.trim().take(100) else "",
        "occurredAtMillis" to occurredAtMillis.coerceAtLeast(1L),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CloudBill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val billType: String = "UTANG",
    val personName: String = "",
    val totalAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val status: String = "BELUM_LUNAS",
    val note: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    private fun normalizedStatus(): String =
        if (status in setOf("BELUM_LUNAS", "CICIL", "LUNAS")) status else "BELUM_LUNAS"

    private fun normalizedTotal(): Long = totalAmount.coerceIn(1L, 100_000_000_000L)

    private fun normalizedPaid(): Long {
        val total = normalizedTotal()
        return when (normalizedStatus()) {
            "LUNAS" -> total
            "BELUM_LUNAS" -> 0L
            else -> paidAmount.coerceIn(0L, total)
        }
    }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "billType" to if (billType == "PIUTANG") "PIUTANG" else "UTANG",
        "personName" to personName.trim().ifBlank { "Pihak" }.take(80),
        "totalAmount" to normalizedTotal(),
        "paidAmount" to normalizedPaid(),
        "status" to normalizedStatus(),
        "note" to note.trim().take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "billType" to if (billType == "PIUTANG") "PIUTANG" else "UTANG",
        "personName" to personName.trim().ifBlank { "Pihak" }.take(80),
        "totalAmount" to normalizedTotal(),
        "paidAmount" to normalizedPaid(),
        "status" to normalizedStatus(),
        "note" to note.trim().take(400),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

