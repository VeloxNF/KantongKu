package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocalAccountEntity::class,
        PocketEntity::class,
        TransactionEntity::class,
        WalletEntity::class,
        BillEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun pocketDao(): PocketDao
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_accounts ADD COLUMN paydayFrequency TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE local_accounts ADD COLUMN paydayWeekday INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE local_accounts ADD COLUMN paydayHour INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE local_accounts ADD COLUMN paydayMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wallets` (
                        `id` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `walletType` TEXT NOT NULL,
                        `providerCode` TEXT NOT NULL,
                        `providerShortName` TEXT NOT NULL,
                        `providerCompanyName` TEXT NOT NULL,
                        `accountNumberHint` TEXT NOT NULL,
                        `balance` INTEGER NOT NULL,
                        `colorSlot` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_accountId` ON `wallets` (`accountId`)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN walletId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN walletName TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryKey TEXT NOT NULL DEFAULT 'OTHER'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bills` (
                        `id` TEXT NOT NULL,
                        `accountId` TEXT NOT NULL,
                        `billType` TEXT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bills_accountId` ON `bills` (`accountId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kantongku_finance.db",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
