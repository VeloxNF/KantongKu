package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM local_accounts ORDER BY createdAtMillis ASC")
    fun observeAllAccounts(): Flow<List<LocalAccountEntity>>

    @Query("SELECT * FROM local_accounts WHERE id = :id LIMIT 1")
    fun observeAccountById(id: String): Flow<LocalAccountEntity?>

    @Query("SELECT * FROM local_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): LocalAccountEntity?

    @Query("SELECT * FROM local_accounts WHERE username = :username LIMIT 1")
    suspend fun getAccountByUsername(username: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: LocalAccountEntity)

    @Update
    suspend fun updateAccount(account: LocalAccountEntity)
}

@Dao
interface PocketDao {
    @Query("SELECT * FROM pockets WHERE accountId = :accountId ORDER BY featured DESC, createdAtMillis ASC")
    fun observePocketsForAccount(accountId: String): Flow<List<PocketEntity>>

    @Query("SELECT * FROM pockets WHERE accountId = :accountId ORDER BY featured DESC, createdAtMillis ASC")
    suspend fun getPocketsForAccount(accountId: String): List<PocketEntity>

    @Query("SELECT * FROM pockets WHERE id = :id LIMIT 1")
    suspend fun getPocketById(id: String): PocketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPocket(pocket: PocketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPockets(pockets: List<PocketEntity>)

    @Query("DELETE FROM pockets WHERE id = :id")
    suspend fun deletePocketById(id: String)

    @Query("UPDATE pockets SET featured = 0 WHERE accountId = :accountId AND id != :featuredId")
    suspend fun clearOtherFeatured(accountId: String, featuredId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY occurredAtMillis DESC, createdAtMillis DESC")
    fun observeTransactionsForAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY occurredAtMillis DESC, createdAtMillis DESC")
    suspend fun getTransactionsForAccount(accountId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("UPDATE transactions SET pocketId = '', pocketName = '' WHERE pocketId = :pocketId")
    suspend fun detachPocketFromTransactions(pocketId: String)

    @Query("UPDATE transactions SET walletId = '', walletName = '' WHERE walletId = :walletId")
    suspend fun detachWalletFromTransactions(walletId: String)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE accountId = :accountId
          AND pocketId = :pocketId
          AND type = 'EXPENSE'
          AND occurredAtMillis >= :startMillis
          AND occurredAtMillis < :endMillis
        """
    )
    suspend fun getCategoryExpenseSumForRange(
        accountId: String,
        pocketId: String,
        startMillis: Long,
        endMillis: Long,
    ): Long
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE accountId = :accountId ORDER BY isDefault DESC, createdAtMillis ASC")
    fun observeWalletsForAccount(accountId: String): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE accountId = :accountId ORDER BY isDefault DESC, createdAtMillis ASC")
    suspend fun getWalletsForAccount(accountId: String): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getWalletById(id: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallet(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWallets(wallets: List<WalletEntity>)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteWalletById(id: String)

    @Query("UPDATE wallets SET isDefault = 0 WHERE accountId = :accountId AND id != :defaultWalletId")
    suspend fun clearOtherDefault(accountId: String, defaultWalletId: String)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE accountId = :accountId ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    fun observeBillsForAccount(accountId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE accountId = :accountId ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    suspend fun getBillsForAccount(accountId: String): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBills(bills: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: String)
}

