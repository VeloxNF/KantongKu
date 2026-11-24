package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PocketMode(val label: String, val shortLabel: String) {
    BUDGET_LIMIT("Batas Pengeluaran", "Batas"),
    SAVINGS_GOAL("Target Tabungan", "Tabungan");

    companion object {
        fun fromRaw(raw: String?): PocketMode =
            entries.firstOrNull { it.name == raw } ?: BUDGET_LIMIT
    }
}

enum class TransactionType(val label: String) {
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class PaydayFrequency(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan");

    companion object {
        fun fromRaw(raw: String?): PaydayFrequency =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class PaydayWeekday(
    val isoValue: Int,
    val fullLabel: String,
    val shortLabel: String,
    val calendarConstant: Int,
) {
    MONDAY(1, "Senin", "Sen", java.util.Calendar.MONDAY),
    TUESDAY(2, "Selasa", "Sel", java.util.Calendar.TUESDAY),
    WEDNESDAY(3, "Rabu", "Rab", java.util.Calendar.WEDNESDAY),
    THURSDAY(4, "Kamis", "Kam", java.util.Calendar.THURSDAY),
    FRIDAY(5, "Jumat", "Jum", java.util.Calendar.FRIDAY),
    SATURDAY(6, "Sabtu", "Sab", java.util.Calendar.SATURDAY),
    SUNDAY(7, "Minggu", "Min", java.util.Calendar.SUNDAY);

    companion object {
        fun fromIso(iso: Int): PaydayWeekday =
            entries.firstOrNull { it.isoValue == iso } ?: FRIDAY
    }
}

@Entity(
    tableName = "local_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class LocalAccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val paydayFrequency: String = PaydayFrequency.MONTHLY.name,
    val paydayDay: Int = 25,
    val paydayWeekday: Int = 5,
    val paydayHour: Int = 8,
    val paydayMinute: Int = 0,
    val monthlyBudget: Long = 3_000_000L,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val parsedPaydayFrequency: PaydayFrequency
        get() = PaydayFrequency.fromRaw(paydayFrequency)
}

@Entity(
    tableName = "pockets",
    indices = [Index(value = ["accountId"])]
)
data class PocketEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val mode: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val iconKey: String,
    val colorSlot: Int,
    val featured: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val pocketMode: PocketMode
        get() = PocketMode.fromRaw(mode)

    val remainingAmount: Long
        get() = if (pocketMode == PocketMode.BUDGET_LIMIT) {
            targetAmount - currentAmount
        } else {
            (targetAmount - currentAmount).coerceAtLeast(0L)
        }

    val progressFraction: Float
        get() {
            if (targetAmount <= 0L) return 0f
            return if (pocketMode == PocketMode.BUDGET_LIMIT) {
                // Regress: remaining / target (starts at 1f and shrinks as you spend)
                ((targetAmount - currentAmount).toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
            } else {
                // Progress: saved / target (starts at 0f and grows to 1f)
                (currentAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
            }
        }
}

enum class WalletType(val label: String, val groupLabel: String) {
    CASH("Tunai", "Tunai"),
    EWALLET("E-Wallet", "Elektronik"),
    BANK("Bank", "Elektronik");

    companion object {
        fun fromRaw(raw: String?): WalletType =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

@Entity(
    tableName = "wallets",
    indices = [Index(value = ["accountId"])]
)
data class WalletEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val name: String,
    val walletType: String = WalletType.CASH.name,
    val providerCode: String = "CASH_IDR",
    val providerShortName: String = "Tunai",
    val providerCompanyName: String = "Dompet Uang Tunai",
    val accountNumberHint: String = "",
    val balance: Long = 0L,
    val colorSlot: Int = 0,
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val type: WalletType
        get() = WalletType.fromRaw(walletType)
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["accountId"]), Index(value = ["pocketId"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val title: String,
    val note: String,
    val amount: Long,
    val type: String,
    val pocketId: String,
    val pocketName: String,
    val walletId: String = "",
    val walletName: String = "",
    val categoryKey: String = "OTHER",
    val categoryLabel: String = "",
    val locationName: String = "",
    val occurredAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    val txType: TransactionType
        get() = TransactionType.fromRaw(type)
}

enum class BillType(val label: String, val personFieldLabel: String) {
    UTANG("Utang", "Nama Pemberi Utang"),
    PIUTANG("Piutang", "Nama Pengutang");

    companion object {
        fun fromRaw(raw: String?): BillType =
            entries.firstOrNull { it.name == raw } ?: UTANG
    }
}

enum class BillStatus(val label: String) {
    BELUM_LUNAS("Belum Lunas"),
    CICIL("Cicil"),
    LUNAS("Sudah Lunas");

    companion object {
        fun fromRaw(raw: String?): BillStatus =
            entries.firstOrNull { it.name == raw } ?: BELUM_LUNAS
    }
}

@Entity(
    tableName = "bills",
    indices = [Index(value = ["accountId"])],
)
data class BillEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val billType: String = BillType.UTANG.name,
    val personName: String,
    val totalAmount: Long,
    val paidAmount: Long = 0L,
    val status: String = BillStatus.BELUM_LUNAS.name,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val parsedType: BillType
        get() = BillType.fromRaw(billType)

    val parsedStatus: BillStatus
        get() = BillStatus.fromRaw(status)

    val effectivePaidAmount: Long
        get() = when (parsedStatus) {
            BillStatus.LUNAS -> totalAmount.coerceAtLeast(0L)
            BillStatus.BELUM_LUNAS -> 0L
            BillStatus.CICIL -> paidAmount.coerceIn(0L, totalAmount.coerceAtLeast(0L))
        }

    val remainingAmount: Long
        get() = (totalAmount.coerceAtLeast(0L) - effectivePaidAmount).coerceAtLeast(0L)

    val progressFraction: Float
        get() = if (totalAmount <= 0L) 0f
        else (effectivePaidAmount.toFloat() / totalAmount.toFloat()).coerceIn(0f, 1f)
}

