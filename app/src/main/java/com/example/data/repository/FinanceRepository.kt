package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.cloud.CloudBackupRepository
import com.example.data.cloud.CloudBackupSummary
import com.example.data.cloud.CloudBill
import com.example.data.cloud.CloudPocket
import com.example.data.cloud.CloudTransaction
import com.example.data.cloud.CloudWallet
import com.example.data.cloud.NetworkMonitor
import com.example.data.cloud.UserCloudProfile
import com.example.data.currency.CurrencyCatalog
import com.example.data.currency.ExchangeRateClient
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferencesState
import com.example.data.local.BillEntity
import com.example.data.local.BillStatus
import com.example.data.local.BillType
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PaydayFrequency
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.PreferencesManager
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeScheme
import com.example.util.FormatUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class CategoryBudgetAlert(
    val pocketId: String,
    val categoryName: String,
    val spentAmount: Long,
    val targetAmount: Long,
) {
    val overageAmount: Long
        get() = (spentAmount - targetAmount).coerceAtLeast(0L)
}

data class TransactionSaveResult(
    val transaction: TransactionEntity,
    val budgetAlert: CategoryBudgetAlert? = null,
)

data class PocketSaveResult(
    val pocket: PocketEntity,
    val budgetAlert: CategoryBudgetAlert? = null,
)

data class SmartPocketAllocationItem(
    val existingPocketId: String?,
    val name: String,
    val iconKey: String,
    val colorSlot: Int,
    val targetAmount: Long,
)

class FinanceRepository(
    private val db: AppDatabase,
    private val prefs: PreferencesManager,
    private val cloudRepo: CloudBackupRepository,
    private val auth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor,
) {
    private val accountDao = db.accountDao()
    private val pocketDao = db.pocketDao()
    private val transactionDao = db.transactionDao()
    private val walletDao = db.walletDao()
    private val billDao = db.billDao()

    private val accountBootstrapMutex = Mutex()
    private val syncMutex = Mutex()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloudFlow: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _syncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val syncTriggerFlow: SharedFlow<Unit> = _syncTrigger.asSharedFlow()

    val isOnlineFlow: StateFlow<Boolean> = networkMonitor.isOnline
    val preferencesFlow: Flow<AppPreferencesState> = prefs.preferencesFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAccountFlow: Flow<LocalAccountEntity?> = prefs.preferencesFlow.flatMapLatest { state ->
        val id = state.activeAccountId
        if (id.isNullOrBlank()) flowOf(null) else accountDao.observeAccountById(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWalletsFlow: Flow<List<WalletEntity>> = prefs.preferencesFlow.flatMapLatest { state ->
        val id = state.activeAccountId
        if (id.isNullOrBlank()) flowOf(emptyList()) else walletDao.observeWalletsForAccount(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activePocketsFlow: Flow<List<PocketEntity>> = prefs.preferencesFlow.flatMapLatest { state ->
        val id = state.activeAccountId
        if (id.isNullOrBlank()) flowOf(emptyList()) else pocketDao.observePocketsForAccount(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeTransactionsFlow: Flow<List<TransactionEntity>> = prefs.preferencesFlow.flatMapLatest { state ->
        val id = state.activeAccountId
        if (id.isNullOrBlank()) flowOf(emptyList()) else transactionDao.observeTransactionsForAccount(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBillsFlow: Flow<List<BillEntity>> = prefs.preferencesFlow.flatMapLatest { state ->
        val id = state.activeAccountId
        if (id.isNullOrBlank()) flowOf(emptyList()) else billDao.observeBillsForAccount(id)
    }

    suspend fun ensureStarterWalletIfEmpty(accountId: String): WalletEntity? {
        val existing = walletDao.getWalletsForAccount(accountId)
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val txNet = transactionDao.getTransactionsForAccount(accountId).sumOf { tx ->
                if (tx.txType == TransactionType.INCOME) tx.amount else -tx.amount
            }
            val starterWallet = WalletEntity(
                id = "wal_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                accountId = accountId,
                name = "Dompet Tunai",
                walletType = WalletType.CASH.name,
                providerCode = "CASH_IDR",
                providerShortName = "Tunai / Kas Fisik",
                providerCompanyName = "Dompet Uang Tunai",
                accountNumberHint = "",
                balance = txNet.coerceAtLeast(0L),
                colorSlot = 0,
                isDefault = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
            walletDao.upsertWallet(starterWallet)
            return starterWallet
        }
        return null
    }

    suspend fun ensureGoogleAccountSession(
        uid: String,
        displayName: String?,
        email: String?,
    ): Result<LocalAccountEntity> = accountBootstrapMutex.withLock {
        return try {
            require(uid.isNotBlank()) { "ID pengguna Google tidak valid." }

            val existing = accountDao.getAccountById(uid)
            if (existing != null) {
                val resolvedName = existing.displayName.ifBlank {
                    displayName?.trim()?.takeIf { it.isNotBlank() }
                        ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                        ?: "Pengguna"
                }
                val finalAccount = if (resolvedName != existing.displayName) {
                    existing.copy(displayName = resolvedName).also { accountDao.updateAccount(it) }
                } else {
                    existing
                }
                val createdWallet = ensureStarterWalletIfEmpty(uid)
                if (createdWallet != null) {
                    prefs.enqueueSyncUpserts(walletSyncKey(createdWallet.id))
                }
                prefs.setActiveAccountId(uid)
                _syncTrigger.tryEmit(Unit)
                return Result.success(finalAccount)
            }

            val baseUsername = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: uid
            val usernameConflict = accountDao.getAccountByUsername(baseUsername)
            val safeUsername = if (usernameConflict == null) {
                baseUsername
            } else {
                "${baseUsername.take(24)}_${uid.take(6)}"
            }
            val resolvedDisplayName = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: "Pengguna"
            val now = System.currentTimeMillis()

            val newAccount = LocalAccountEntity(
                id = uid,
                username = safeUsername,
                displayName = resolvedDisplayName.take(60),
                passwordHash = "",
                passwordSalt = "",
                paydayDay = 25,
                monthlyBudget = 3_000_000L,
                createdAtMillis = now,
            )

            accountDao.insertAccount(newAccount)
            prefs.setActiveAccountId(uid)

            var hydratedFromCloud = false
            if (networkMonitor.refreshNow()) {
                val cloudPayload = cloudRepo.restoreFromCloud().getOrNull()
                if (cloudPayload != null &&
                    (cloudPayload.profile != null ||
                        cloudPayload.wallets.isNotEmpty() ||
                        cloudPayload.pockets.isNotEmpty() ||
                        cloudPayload.transactions.isNotEmpty() ||
                        cloudPayload.bills.isNotEmpty())
                ) {
                    applyCloudRestorePayload(uid, newAccount, cloudPayload)
                    hydratedFromCloud = true
                }
            }

            if (!hydratedFromCloud) {
                val starterWallet = WalletEntity(
                    id = "wal_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                    accountId = uid,
                    name = "Dompet Tunai",
                    walletType = WalletType.CASH.name,
                    providerCode = "CASH_IDR",
                    providerShortName = "Tunai / Kas Fisik",
                    providerCompanyName = "Dompet Uang Tunai",
                    accountNumberHint = "",
                    balance = 0L,
                    colorSlot = 0,
                    isDefault = true,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
                val starterPockets = listOf(
                    PocketEntity(
                        id = "pkt_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        accountId = uid,
                        name = "Makan/Minum",
                        mode = PocketMode.BUDGET_LIMIT.name,
                        targetAmount = 1_500_000L,
                        currentAmount = 0L,
                        iconKey = "FOOD",
                        colorSlot = 0,
                        featured = true,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                    PocketEntity(
                        id = "pkt_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        accountId = uid,
                        name = "Transportasi",
                        mode = PocketMode.BUDGET_LIMIT.name,
                        targetAmount = 500_000L,
                        currentAmount = 0L,
                        iconKey = "TRANSPORT",
                        colorSlot = 1,
                        featured = false,
                        createdAtMillis = now + 1,
                        updatedAtMillis = now + 1,
                    ),
                    PocketEntity(
                        id = "pkt_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        accountId = uid,
                        name = "Dana Darurat",
                        mode = PocketMode.SAVINGS_GOAL.name,
                        targetAmount = 5_000_000L,
                        currentAmount = 0L,
                        iconKey = "SAVINGS",
                        colorSlot = 2,
                        featured = false,
                        createdAtMillis = now + 2,
                        updatedAtMillis = now + 2,
                    ),
                )
                db.withTransaction {
                    walletDao.upsertWallet(starterWallet)
                    pocketDao.upsertPockets(starterPockets)
                }
                val keys = buildList {
                    add(KEY_PROFILE)
                    add(walletSyncKey(starterWallet.id))
                    starterPockets.forEach { add(pocketSyncKey(it.id)) }
                }
                prefs.enqueueSyncUpserts(*keys.toTypedArray())
            } else {
                val fallbackWallet = ensureStarterWalletIfEmpty(uid)
                if (fallbackWallet != null) {
                    prefs.enqueueSyncUpserts(walletSyncKey(fallbackWallet.id))
                }
            }

            _syncTrigger.tryEmit(Unit)
            Result.success(accountDao.getAccountById(uid) ?: newAccount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearSessionOnSignOut() {
        prefs.setActiveAccountId(null)
        prefs.clearPendingSync()
    }

    suspend fun updateProfile(
        accountId: String,
        displayName: String,
        paydayFrequency: PaydayFrequency,
        paydayDay: Int,
        paydayWeekday: Int,
        paydayHour: Int,
        paydayMinute: Int,
        monthlyBudget: Long,
    ): Result<Unit> {
        return try {
            val cleanName = displayName.trim()
            require(cleanName.length in 1..60) { "Nama wajib diisi (1–60 karakter)." }
            val existing = accountDao.getAccountById(accountId)
                ?: return Result.failure(IllegalStateException("Akun aktif tidak ditemukan."))

            val updated = existing.copy(
                displayName = cleanName,
                paydayFrequency = paydayFrequency.name,
                paydayDay = paydayDay.coerceIn(1, 31),
                paydayWeekday = paydayWeekday.coerceIn(1, 7),
                paydayHour = paydayHour.coerceIn(0, 23),
                paydayMinute = paydayMinute.coerceIn(0, 59),
                monthlyBudget = monthlyBudget.coerceAtLeast(0L),
            )
            accountDao.updateAccount(updated)
            enqueueUpsertAndTriggerSync(KEY_PROFILE)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveWallet(
        accountId: String,
        existingId: String?,
        name: String,
        walletType: WalletType,
        providerCode: String,
        providerShortName: String,
        providerCompanyName: String,
        accountNumberHint: String,
        balance: Long,
        colorSlot: Int,
        isDefault: Boolean,
    ): Result<WalletEntity> {
        return try {
            val cleanName = name.trim()
            require(cleanName.length in 1..50) { "Nama dompet wajib diisi (maks 50 karakter)." }

            val now = System.currentTimeMillis()
            val id = existingId?.takeIf { it.isNotBlank() }
                ?: "wal_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val existing = existingId?.let { walletDao.getWalletById(it) }
            val existingWallets = walletDao.getWalletsForAccount(accountId)
            val resolvedDefault = isDefault || existingWallets.isEmpty()

            val wallet = WalletEntity(
                id = id,
                accountId = accountId,
                name = cleanName,
                walletType = walletType.name,
                providerCode = providerCode.ifBlank { "CASH_IDR" },
                providerShortName = providerShortName.ifBlank { cleanName },
                providerCompanyName = providerCompanyName,
                accountNumberHint = accountNumberHint.trim().take(40),
                balance = balance,
                colorSlot = colorSlot.coerceIn(0, 5),
                isDefault = resolvedDefault,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )

            val affectedIds = mutableListOf(wallet.id)
            db.withTransaction {
                if (resolvedDefault) {
                    walletDao.clearOtherDefault(accountId, wallet.id)
                    existingWallets.filter { it.id != wallet.id && it.isDefault }
                        .forEach { affectedIds += it.id }
                }
                walletDao.upsertWallet(wallet)
            }

            enqueueUpsertAndTriggerSync(*affectedIds.map { walletSyncKey(it) }.toTypedArray())
            Result.success(wallet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWallet(walletId: String): Result<Unit> {
        return try {
            db.withTransaction {
                transactionDao.detachWalletFromTransactions(walletId)
                walletDao.deleteWalletById(walletId)
            }
            prefs.enqueueSyncDeletes(walletSyncKey(walletId))
            triggerBackgroundSyncIfEligible()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transferBetweenWallets(
        accountId: String,
        fromWalletId: String,
        toWalletId: String,
        amount: Long,
        note: String,
    ): Result<Unit> {
        return try {
            require(fromWalletId != toWalletId) { "Pilih dompet tujuan yang berbeda." }
            require(amount > 0L) { "Nominal transfer harus lebih dari 0." }
            val now = System.currentTimeMillis()
            var updatedFrom: WalletEntity? = null
            var updatedTo: WalletEntity? = null

            db.withTransaction {
                val from = walletDao.getWalletById(fromWalletId)
                    ?: throw IllegalStateException("Dompet asal tidak ditemukan.")
                val to = walletDao.getWalletById(toWalletId)
                    ?: throw IllegalStateException("Dompet tujuan tidak ditemukan.")

                val nextFrom = from.copy(balance = from.balance - amount, updatedAtMillis = now)
                val nextTo = to.copy(balance = to.balance + amount, updatedAtMillis = now)
                walletDao.upsertWallet(nextFrom)
                walletDao.upsertWallet(nextTo)
                updatedFrom = nextFrom
                updatedTo = nextTo
            }

            val syncKeys = listOfNotNull(
                updatedFrom?.let { walletSyncKey(it.id) },
                updatedTo?.let { walletSyncKey(it.id) },
            ).toTypedArray()
            enqueueUpsertAndTriggerSync(*syncKeys)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePocket(
        accountId: String,
        existingId: String?,
        name: String,
        mode: PocketMode,
        targetAmount: Long,
        currentAmount: Long,
        iconKey: String,
        colorSlot: Int,
        featured: Boolean,
    ): Result<PocketSaveResult> {
        return try {
            val cleanName = name.trim()
            require(cleanName.length in 1..50) { "Nama kantong wajib diisi (maks 50 karakter)." }
            require(targetAmount > 0L) { "Nominal target harus lebih dari 0." }
            require(currentAmount >= 0L) { "Nominal saat ini tidak boleh negatif." }

            val now = System.currentTimeMillis()
            val id = existingId?.takeIf { it.isNotBlank() }
                ?: "pkt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val existing = existingId?.let { pocketDao.getPocketById(it) }

            val pocket = PocketEntity(
                id = id,
                accountId = accountId,
                name = cleanName,
                mode = mode.name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                iconKey = iconKey.ifBlank { "OTHER" },
                colorSlot = colorSlot.coerceIn(0, 5),
                featured = featured,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )

            var budgetAlert: CategoryBudgetAlert? = null
            val affectedIds = mutableListOf(pocket.id)
            db.withTransaction {
                if (featured) {
                    val others = pocketDao.getPocketsForAccount(accountId)
                        .filter { it.id != pocket.id && it.featured }
                    others.forEach { affectedIds += it.id }
                    pocketDao.clearOtherFeatured(accountId, pocket.id)
                }
                pocketDao.upsertPocket(pocket)
                budgetAlert = evaluateCategoryBudgetAlert(accountId, pocket)
            }

            enqueueUpsertAndTriggerSync(*affectedIds.map { pocketSyncKey(it) }.toTypedArray())
            Result.success(PocketSaveResult(pocket = pocket, budgetAlert = budgetAlert))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePocket(pocketId: String): Result<Unit> {
        return try {
            db.withTransaction {
                transactionDao.detachPocketFromTransactions(pocketId)
                pocketDao.deletePocketById(pocketId)
            }
            prefs.enqueueSyncDeletes(pocketSyncKey(pocketId))
            triggerBackgroundSyncIfEligible()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordOrUpdateTransaction(
        accountId: String,
        existingId: String?,
        title: String,
        note: String,
        amount: Long,
        type: TransactionType,
        pocketId: String?,
        walletId: String?,
        categoryKey: String = "OTHER",
        categoryLabel: String = "",
        locationName: String = "",
        occurredAtMillis: Long = System.currentTimeMillis(),
    ): Result<TransactionSaveResult> {
        return try {
            val cleanTitle = title.trim()
            val cleanNote = note.trim().take(500)
            require(cleanTitle.length in 1..80) { "Judul transaksi wajib diisi (maks 80 karakter)." }
            require(amount > 0L) { "Nominal harus lebih dari 0." }

            val now = System.currentTimeMillis()
            val id = existingId?.takeIf { it.isNotBlank() }
                ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            var savedTx: TransactionEntity? = null
            var updatedOldPocket: PocketEntity? = null
            var updatedNewPocket: PocketEntity? = null
            var updatedOldWallet: WalletEntity? = null
            var updatedNewWallet: WalletEntity? = null
            var budgetAlert: CategoryBudgetAlert? = null

            db.withTransaction {
                val previousTx = existingId?.let { transactionDao.getTransactionById(it) }

                if (previousTx != null && previousTx.pocketId.isNotBlank()) {
                    val prevPocket = pocketDao.getPocketById(previousTx.pocketId)
                    if (prevPocket != null) {
                        val reverted = applyPocketDelta(
                            pocket = prevPocket,
                            txType = previousTx.txType,
                            amount = previousTx.amount,
                            revert = true,
                            now = now,
                        )
                        pocketDao.upsertPocket(reverted)
                        updatedOldPocket = reverted
                    }
                }

                if (previousTx != null && previousTx.walletId.isNotBlank()) {
                    val prevWallet = walletDao.getWalletById(previousTx.walletId)
                    if (prevWallet != null) {
                        val revertedWallet = applyWalletDelta(
                            wallet = prevWallet,
                            txType = previousTx.txType,
                            amount = previousTx.amount,
                            revert = true,
                            now = now,
                        )
                        walletDao.upsertWallet(revertedWallet)
                        updatedOldWallet = revertedWallet
                    }
                }

                val targetPocket = pocketId?.takeIf { it.isNotBlank() }?.let { pocketDao.getPocketById(it) }
                if (targetPocket != null) {
                    val applied = applyPocketDelta(
                        pocket = targetPocket,
                        txType = type,
                        amount = amount,
                        revert = false,
                        now = now,
                    )
                    pocketDao.upsertPocket(applied)
                    updatedNewPocket = applied
                }

                val targetWallet = walletId?.takeIf { it.isNotBlank() }?.let { walletDao.getWalletById(it) }
                if (targetWallet != null) {
                    val appliedWallet = applyWalletDelta(
                        wallet = targetWallet,
                        txType = type,
                        amount = amount,
                        revert = false,
                        now = now,
                    )
                    walletDao.upsertWallet(appliedWallet)
                    updatedNewWallet = appliedWallet
                }

                val resolvedCatKey = categoryKey.trim().ifBlank {
                    targetPocket?.iconKey ?: "OTHER"
                }.take(24)
                val resolvedCatLabel = categoryLabel.trim().ifBlank {
                    com.example.ui.components.pocketCategoryFor(resolvedCatKey).label
                }.take(60)

                val cleanLocation = if (type == TransactionType.EXPENSE) {
                    locationName.trim().take(100)
                } else {
                    ""
                }

                val entity = TransactionEntity(
                    id = id,
                    accountId = accountId,
                    title = cleanTitle,
                    note = cleanNote,
                    amount = amount,
                    type = type.name,
                    pocketId = targetPocket?.id ?: "",
                    pocketName = targetPocket?.name ?: "",
                    walletId = targetWallet?.id ?: "",
                    walletName = targetWallet?.name ?: "",
                    categoryKey = resolvedCatKey,
                    categoryLabel = resolvedCatLabel,
                    locationName = cleanLocation,
                    occurredAtMillis = occurredAtMillis.coerceAtLeast(1L),
                    createdAtMillis = previousTx?.createdAtMillis ?: now,
                    updatedAtMillis = now,
                )
                transactionDao.upsertTransaction(entity)
                savedTx = entity

                if (type == TransactionType.EXPENSE && updatedNewPocket != null) {
                    budgetAlert = evaluateCategoryBudgetAlert(accountId, updatedNewPocket!!)
                }
            }

            val finalTx = savedTx!!
            val syncKeys = buildList {
                add(txSyncKey(finalTx.id))
                updatedOldPocket?.let { add(pocketSyncKey(it.id)) }
                updatedNewPocket?.let { add(pocketSyncKey(it.id)) }
                updatedOldWallet?.let { add(walletSyncKey(it.id)) }
                updatedNewWallet?.let { add(walletSyncKey(it.id)) }
            }.toTypedArray()
            enqueueUpsertAndTriggerSync(*syncKeys)

            Result.success(TransactionSaveResult(transaction = finalTx, budgetAlert = budgetAlert))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            var updatedPocket: PocketEntity? = null
            var updatedWallet: WalletEntity? = null
            db.withTransaction {
                val existing = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
                if (existing.pocketId.isNotBlank()) {
                    val pocket = pocketDao.getPocketById(existing.pocketId)
                    if (pocket != null) {
                        val reverted = applyPocketDelta(
                            pocket = pocket,
                            txType = existing.txType,
                            amount = existing.amount,
                            revert = true,
                            now = now,
                        )
                        pocketDao.upsertPocket(reverted)
                        updatedPocket = reverted
                    }
                }
                if (existing.walletId.isNotBlank()) {
                    val wallet = walletDao.getWalletById(existing.walletId)
                    if (wallet != null) {
                        val revertedWallet = applyWalletDelta(
                            wallet = wallet,
                            txType = existing.txType,
                            amount = existing.amount,
                            revert = true,
                            now = now,
                        )
                        walletDao.upsertWallet(revertedWallet)
                        updatedWallet = revertedWallet
                    }
                }
                transactionDao.deleteTransactionById(transactionId)
            }
            prefs.enqueueSyncDeletes(txSyncKey(transactionId))
            val upsertKeys = listOfNotNull(
                updatedPocket?.let { pocketSyncKey(it.id) },
                updatedWallet?.let { walletSyncKey(it.id) },
            ).toTypedArray()
            if (upsertKeys.isNotEmpty()) {
                prefs.enqueueSyncUpserts(*upsertKeys)
            }
            triggerBackgroundSyncIfEligible()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyWalletDelta(
        wallet: WalletEntity,
        txType: TransactionType,
        amount: Long,
        revert: Boolean,
        now: Long,
    ): WalletEntity {
        val sign = if (revert) -1L else 1L
        val delta = if (txType == TransactionType.INCOME) amount * sign else -amount * sign
        return wallet.copy(
            balance = wallet.balance + delta,
            updatedAtMillis = now,
        )
    }

    private fun applyPocketDelta(
        pocket: PocketEntity,
        txType: TransactionType,
        amount: Long,
        revert: Boolean,
        now: Long,
    ): PocketEntity {
        val sign = if (revert) -1L else 1L
        return when (pocket.pocketMode) {
            PocketMode.BUDGET_LIMIT -> {
                if (txType == TransactionType.EXPENSE) {
                    val nextSpent = (pocket.currentAmount + amount * sign).coerceAtLeast(0L)
                    pocket.copy(currentAmount = nextSpent, updatedAtMillis = now)
                } else {
                    // Adding income to an expense-allocation pocket expands its allocation limit
                    val nextTarget = (pocket.targetAmount + amount * sign).coerceAtLeast(1L)
                    pocket.copy(targetAmount = nextTarget, updatedAtMillis = now)
                }
            }
            PocketMode.SAVINGS_GOAL -> {
                val delta = if (txType == TransactionType.INCOME) amount * sign else -amount * sign
                val nextSaved = (pocket.currentAmount + delta).coerceAtLeast(0L)
                pocket.copy(currentAmount = nextSaved, updatedAtMillis = now)
            }
        }
    }

    private suspend fun evaluateCategoryBudgetAlert(
        accountId: String,
        pocket: PocketEntity,
    ): CategoryBudgetAlert? {
        if (pocket.pocketMode != PocketMode.BUDGET_LIMIT || pocket.targetAmount <= 0L) {
            return null
        }
        val monthStart = FormatUtils.startOfCurrentMonthMillis()
        val monthSpentFromRoom = transactionDao.getCategoryExpenseSumForRange(
            accountId = accountId,
            pocketId = pocket.id,
            startMillis = monthStart,
            endMillis = Long.MAX_VALUE,
        )
        val effectiveSpent = maxOf(pocket.currentAmount, monthSpentFromRoom)
        return if (effectiveSpent > pocket.targetAmount) {
            CategoryBudgetAlert(
                pocketId = pocket.id,
                categoryName = pocket.name,
                spentAmount = effectiveSpent,
                targetAmount = pocket.targetAmount,
            )
        } else {
            null
        }
    }

    suspend fun setThemeScheme(scheme: ThemeScheme) {
        prefs.setThemeScheme(scheme)
        enqueueUpsertAndTriggerSync(KEY_PROFILE)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        prefs.setThemeMode(mode)
        enqueueUpsertAndTriggerSync(KEY_PROFILE)
    }

    suspend fun setAutoCloudSync(enabled: Boolean) {
        prefs.setAutoCloudSync(enabled)
        if (enabled) {
            triggerBackgroundSyncIfEligible()
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.setBiometricLockEnabled(enabled)
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        prefs.setBudgetAlertsEnabled(enabled)
    }

    suspend fun setSelectedCurrencyCode(code: String) {
        val normalized = CurrencyCatalog.getInfo(code).code
        prefs.setSelectedCurrencyCode(normalized)
        enqueueUpsertAndTriggerSync(KEY_PROFILE)
    }

    suspend fun setAppLanguage(languageCode: String) {
        prefs.setAppLanguage(languageCode)
        enqueueUpsertAndTriggerSync(KEY_PROFILE)
    }

    suspend fun setHideBalances(hidden: Boolean) {
        prefs.setHideBalances(hidden)
        enqueueUpsertAndTriggerSync(KEY_PROFILE)
    }

    suspend fun clearExchangeRatesCache() {
        prefs.saveExchangeRates("", 0L)
    }

    suspend fun refreshLiveExchangeRates(): Result<Long> {
        val result = ExchangeRateClient.fetchLiveRates()
        result.onSuccess { snapshot ->
            val encoded = ExchangeRateClient.encodeRatesToJson(snapshot.rates)
            prefs.saveExchangeRates(encoded, snapshot.updatedAtMillis)
        }
        return result.map { it.updatedAtMillis }
    }

    suspend fun applySmartPocketAllocation(
        accountId: String,
        items: List<SmartPocketAllocationItem>,
        updatedMonthlyBudget: Long?,
    ): Result<Int> {
        return try {
            require(items.isNotEmpty()) { "Pilih minimal satu kantong untuk diatur." }
            require(items.all { it.targetAmount > 0L && it.name.isNotBlank() }) {
                "Setiap kantong harus memiliki nama dan target lebih dari 0."
            }

            val now = System.currentTimeMillis()
            val updatedPockets = mutableListOf<PocketEntity>()
            var updatedAccount: LocalAccountEntity? = null

            db.withTransaction {
                val existingById = pocketDao.getPocketsForAccount(accountId).associateBy { it.id }

                items.forEachIndexed { index, item ->
                    val existing = item.existingPocketId?.let { existingById[it] }
                    val pocket = if (existing != null) {
                        existing.copy(
                            name = item.name.trim().take(50),
                            mode = PocketMode.BUDGET_LIMIT.name,
                            targetAmount = item.targetAmount,
                            iconKey = item.iconKey.ifBlank { existing.iconKey },
                            colorSlot = item.colorSlot.coerceIn(0, 5),
                            updatedAtMillis = now + index,
                        )
                    } else {
                        PocketEntity(
                            id = "pkt_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                            accountId = accountId,
                            name = item.name.trim().take(50),
                            mode = PocketMode.BUDGET_LIMIT.name,
                            targetAmount = item.targetAmount,
                            currentAmount = 0L,
                            iconKey = item.iconKey.ifBlank { "OTHER" },
                            colorSlot = item.colorSlot.coerceIn(0, 5),
                            featured = existingById.isEmpty() && index == 0,
                            createdAtMillis = now + index,
                            updatedAtMillis = now + index,
                        )
                    }
                    updatedPockets += pocket
                }

                pocketDao.upsertPockets(updatedPockets)

                if (updatedMonthlyBudget != null && updatedMonthlyBudget > 0L) {
                    accountDao.getAccountById(accountId)?.let { acc ->
                        val nextAcc = acc.copy(monthlyBudget = updatedMonthlyBudget)
                        accountDao.updateAccount(nextAcc)
                        updatedAccount = nextAcc
                    }
                }
            }

            val syncKeys = buildList {
                updatedPockets.forEach { add(pocketSyncKey(it.id)) }
                if (updatedAccount != null) add(KEY_PROFILE)
            }.toTypedArray()
            enqueueUpsertAndTriggerSync(*syncKeys)

            Result.success(updatedPockets.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBill(
        accountId: String,
        existingId: String?,
        billType: BillType,
        personName: String,
        totalAmount: Long,
        paidAmount: Long,
        status: BillStatus,
        note: String,
    ): Result<BillEntity> {
        return try {
            val cleanName = personName.trim()
            val cleanNote = note.trim().take(400)
            require(cleanName.length in 1..80) { "Nama pengutang/pihak wajib diisi (maks 80 karakter)." }
            require(totalAmount > 0L) { "Jumlah uang harus lebih dari 0." }

            val resolvedPaid = when (status) {
                BillStatus.LUNAS -> totalAmount
                BillStatus.BELUM_LUNAS -> 0L
                BillStatus.CICIL -> paidAmount.coerceIn(0L, totalAmount)
            }
            val now = System.currentTimeMillis()
            val id = existingId?.takeIf { it.isNotBlank() }
                ?: "bil_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val existing = existingId?.let { billDao.getBillById(it) }

            val bill = BillEntity(
                id = id,
                accountId = accountId,
                billType = billType.name,
                personName = cleanName,
                totalAmount = totalAmount,
                paidAmount = resolvedPaid,
                status = status.name,
                note = cleanNote,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
            billDao.upsertBill(bill)
            enqueueUpsertAndTriggerSync(billSyncKey(bill.id))
            Result.success(bill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBillStatus(
        billId: String,
        status: BillStatus,
        paidAmount: Long? = null,
    ): Result<BillEntity> {
        return try {
            val existing = billDao.getBillById(billId)
                ?: return Result.failure(IllegalStateException("Tagihan tidak ditemukan."))
            val resolvedPaid = when (status) {
                BillStatus.LUNAS -> existing.totalAmount
                BillStatus.BELUM_LUNAS -> 0L
                BillStatus.CICIL -> (paidAmount ?: existing.paidAmount).coerceIn(0L, existing.totalAmount)
            }
            val updated = existing.copy(
                status = status.name,
                paidAmount = resolvedPaid,
                updatedAtMillis = System.currentTimeMillis(),
            )
            billDao.upsertBill(updated)
            enqueueUpsertAndTriggerSync(billSyncKey(updated.id))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBill(billId: String): Result<Unit> {
        return try {
            billDao.deleteBillById(billId)
            prefs.enqueueSyncDeletes(billSyncKey(billId))
            triggerBackgroundSyncIfEligible()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingChangesNow(): Result<Int> = syncMutex.withLock {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Belum masuk dengan Google."))
        if (!networkMonitor.refreshNow()) {
            return Result.failure(IllegalStateException("Tidak ada koneksi internet."))
        }
        val prefState = prefs.preferencesFlow.first()
        val pendingDeletes = prefState.pendingSyncDeletes
        val pendingUpserts = prefState.pendingSyncUpserts
        if (pendingDeletes.isEmpty() && pendingUpserts.isEmpty()) {
            return Result.success(0)
        }

        _isSyncingCloud.value = true
        return try {
            val completedDeletes = mutableSetOf<String>()
            val completedUpserts = mutableSetOf<String>()
            var lastError: Throwable? = null

            for (key in pendingDeletes) {
                val res = when {
                    key.startsWith(PREFIX_WALLET) -> cloudRepo.deleteCloudWallet(key.removePrefix(PREFIX_WALLET))
                    key.startsWith(PREFIX_POCKET) -> cloudRepo.deleteCloudPocket(key.removePrefix(PREFIX_POCKET))
                    key.startsWith(PREFIX_TX) -> cloudRepo.deleteCloudTransaction(key.removePrefix(PREFIX_TX))
                    key.startsWith(PREFIX_BILL) -> cloudRepo.deleteCloudBill(key.removePrefix(PREFIX_BILL))
                    else -> Result.success(Unit)
                }
                if (res.isSuccess) {
                    completedDeletes += key
                } else {
                    lastError = res.exceptionOrNull()
                }
            }

            for (key in pendingUpserts) {
                val res = when {
                    key == KEY_PROFILE -> {
                        val account = accountDao.getAccountById(uid)
                        if (account == null) {
                            Result.success(Unit)
                        } else {
                            cloudRepo.upsertProfile(
                                account = account,
                                themeScheme = prefState.themeScheme,
                                themeMode = prefState.themeMode,
                                currencyCode = prefState.selectedCurrencyCode,
                                languageCode = prefState.appLanguage,
                                hideBalances = prefState.hideBalances,
                            )
                        }
                    }
                    key.startsWith(PREFIX_WALLET) -> {
                        val wallet = walletDao.getWalletById(key.removePrefix(PREFIX_WALLET))
                        if (wallet == null) Result.success(Unit) else cloudRepo.upsertSingleWallet(wallet)
                    }
                    key.startsWith(PREFIX_POCKET) -> {
                        val pocket = pocketDao.getPocketById(key.removePrefix(PREFIX_POCKET))
                        if (pocket == null) Result.success(Unit) else cloudRepo.upsertSinglePocket(pocket)
                    }
                    key.startsWith(PREFIX_TX) -> {
                        val tx = transactionDao.getTransactionById(key.removePrefix(PREFIX_TX))
                        if (tx == null) Result.success(Unit) else cloudRepo.upsertSingleTransaction(tx)
                    }
                    key.startsWith(PREFIX_BILL) -> {
                        val bill = billDao.getBillById(key.removePrefix(PREFIX_BILL))
                        if (bill == null) Result.success(Unit) else cloudRepo.upsertSingleBill(bill)
                    }
                    else -> Result.success(Unit)
                }
                if (res.isSuccess) {
                    completedUpserts += key
                } else {
                    lastError = res.exceptionOrNull()
                }
            }

            prefs.markSyncKeysCompleted(
                upserted = completedUpserts,
                deleted = completedDeletes,
            )

            val syncedCount = completedDeletes.size + completedUpserts.size
            if (syncedCount > 0) {
                prefs.setLastCloudBackupMillis(System.currentTimeMillis())
            }
            if (lastError != null && syncedCount == 0) {
                Result.failure(lastError)
            } else {
                Result.success(syncedCount)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncingCloud.value = false
        }
    }

    fun observeRemoteProfile(uid: String): Flow<UserCloudProfile?> = cloudRepo.observeCloudProfile(uid)
    fun observeRemoteWallets(uid: String): Flow<List<CloudWallet>> = cloudRepo.observeCloudWallets(uid)
    fun observeRemotePockets(uid: String): Flow<List<CloudPocket>> = cloudRepo.observeCloudPockets(uid)
    fun observeRemoteTransactions(uid: String): Flow<List<CloudTransaction>> = cloudRepo.observeCloudTransactions(uid)
    fun observeRemoteBills(uid: String): Flow<List<CloudBill>> = cloudRepo.observeCloudBills(uid)

    suspend fun mergeRemoteProfile(accountId: String, profile: UserCloudProfile?) {
        if (profile == null) return
        val pref = prefs.preferencesFlow.first()
        if (KEY_PROFILE in pref.pendingSyncUpserts || KEY_PROFILE in pref.pendingSyncDeletes) return

        val existing = accountDao.getAccountById(accountId) ?: return
        val updated = existing.copy(
            displayName = profile.displayName.ifBlank { existing.displayName },
            paydayFrequency = PaydayFrequency.fromRaw(profile.paydayFrequency).name,
            paydayDay = profile.paydayDay.toInt().coerceIn(1, 31),
            paydayWeekday = profile.paydayWeekday.toInt().coerceIn(1, 7),
            paydayHour = profile.paydayHour.toInt().coerceIn(0, 23),
            paydayMinute = profile.paydayMinute.toInt().coerceIn(0, 59),
            monthlyBudget = profile.monthlyBudget.coerceAtLeast(0L),
        )
        if (updated != existing) {
            accountDao.updateAccount(updated)
        }
        val remoteScheme = ThemeScheme.fromRaw(profile.themeScheme)
        val remoteMode = ThemeMode.fromRaw(profile.themeMode)
        if (remoteScheme != pref.themeScheme) prefs.setThemeScheme(remoteScheme)
        if (remoteMode != pref.themeMode) prefs.setThemeMode(remoteMode)
        if (profile.currencyCode.length == 3 && !profile.currencyCode.equals(pref.selectedCurrencyCode, ignoreCase = true)) {
            prefs.setSelectedCurrencyCode(profile.currencyCode)
        }
        if (profile.languageCode.isNotBlank() && !profile.languageCode.equals(pref.appLanguage, ignoreCase = true)) {
            prefs.setAppLanguage(profile.languageCode)
        }
        if (profile.hideBalances != pref.hideBalances) {
            prefs.setHideBalances(profile.hideBalances)
        }
        prefs.setLastCloudBackupMillis(System.currentTimeMillis())
    }

    suspend fun mergeRemoteWallets(accountId: String, remoteWallets: List<CloudWallet>) {
        if (remoteWallets.isEmpty()) return
        val pref = prefs.preferencesFlow.first()
        val localById = walletDao.getWalletsForAccount(accountId).associateBy { it.id }
        val toUpsert = mutableListOf<WalletEntity>()

        for (cw in remoteWallets) {
            if (cw.id.isBlank()) continue
            val key = walletSyncKey(cw.id)
            if (key in pref.pendingSyncUpserts || key in pref.pendingSyncDeletes) continue

            val remoteMillis = cw.updatedAt?.toDate()?.time ?: cw.createdAt?.toDate()?.time ?: 0L
            val local = localById[cw.id]
            if (local == null || remoteMillis > local.updatedAtMillis + 500L) {
                toUpsert += WalletEntity(
                    id = cw.id,
                    accountId = accountId,
                    name = cw.name,
                    walletType = WalletType.fromRaw(cw.walletType).name,
                    providerCode = cw.providerCode,
                    providerShortName = cw.providerShortName,
                    providerCompanyName = cw.providerCompanyName,
                    accountNumberHint = cw.accountNumberHint,
                    balance = cw.balance,
                    colorSlot = cw.colorSlot.toInt().coerceIn(0, 5),
                    isDefault = cw.isDefault,
                    createdAtMillis = local?.createdAtMillis ?: cw.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMillis = maxOf(remoteMillis, local?.updatedAtMillis ?: 0L),
                )
            }
        }

        if (toUpsert.isNotEmpty()) {
            walletDao.upsertWallets(toUpsert)
            prefs.setLastCloudBackupMillis(System.currentTimeMillis())
        }
    }

    suspend fun mergeRemotePockets(accountId: String, remotePockets: List<CloudPocket>) {
        if (remotePockets.isEmpty()) return
        val pref = prefs.preferencesFlow.first()
        val localById = pocketDao.getPocketsForAccount(accountId).associateBy { it.id }
        val toUpsert = mutableListOf<PocketEntity>()

        for (cp in remotePockets) {
            if (cp.id.isBlank()) continue
            val key = pocketSyncKey(cp.id)
            if (key in pref.pendingSyncUpserts || key in pref.pendingSyncDeletes) continue

            val remoteMillis = cp.updatedAt?.toDate()?.time ?: cp.createdAt?.toDate()?.time ?: 0L
            val local = localById[cp.id]
            if (local == null || remoteMillis > local.updatedAtMillis + 500L) {
                toUpsert += PocketEntity(
                    id = cp.id,
                    accountId = accountId,
                    name = cp.name,
                    mode = PocketMode.fromRaw(cp.mode).name,
                    targetAmount = cp.targetAmount,
                    currentAmount = cp.currentAmount,
                    iconKey = cp.iconKey,
                    colorSlot = cp.colorSlot.toInt().coerceIn(0, 5),
                    featured = cp.featured,
                    createdAtMillis = local?.createdAtMillis ?: cp.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMillis = maxOf(remoteMillis, local?.updatedAtMillis ?: 0L),
                )
            }
        }

        if (toUpsert.isNotEmpty()) {
            pocketDao.upsertPockets(toUpsert)
            prefs.setLastCloudBackupMillis(System.currentTimeMillis())
        }
    }

    suspend fun mergeRemoteTransactions(accountId: String, remoteTransactions: List<CloudTransaction>) {
        if (remoteTransactions.isEmpty()) return
        val pref = prefs.preferencesFlow.first()
        val localById = transactionDao.getTransactionsForAccount(accountId).associateBy { it.id }
        val toUpsert = mutableListOf<TransactionEntity>()

        for (ct in remoteTransactions) {
            if (ct.id.isBlank()) continue
            val key = txSyncKey(ct.id)
            if (key in pref.pendingSyncUpserts || key in pref.pendingSyncDeletes) continue

            val remoteMillis = ct.updatedAt?.toDate()?.time ?: ct.createdAt?.toDate()?.time ?: 0L
            val local = localById[ct.id]
            if (local == null || remoteMillis > local.updatedAtMillis + 500L) {
                toUpsert += TransactionEntity(
                    id = ct.id,
                    accountId = accountId,
                    title = ct.title,
                    note = ct.note,
                    amount = ct.amount,
                    type = TransactionType.fromRaw(ct.type).name,
                    pocketId = ct.pocketId,
                    pocketName = ct.pocketName,
                    walletId = ct.walletId,
                    walletName = ct.walletName,
                    categoryKey = ct.categoryKey.ifBlank { "OTHER" },
                    categoryLabel = ct.categoryLabel,
                    locationName = if (TransactionType.fromRaw(ct.type) == TransactionType.EXPENSE) ct.locationName else "",
                    occurredAtMillis = ct.occurredAtMillis,
                    createdAtMillis = local?.createdAtMillis ?: ct.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMillis = maxOf(remoteMillis, local?.updatedAtMillis ?: 0L),
                )
            }
        }

        if (toUpsert.isNotEmpty()) {
            transactionDao.upsertTransactions(toUpsert)
            prefs.setLastCloudBackupMillis(System.currentTimeMillis())
        }
    }

    suspend fun mergeRemoteBills(accountId: String, remoteBills: List<CloudBill>) {
        if (remoteBills.isEmpty()) return
        val pref = prefs.preferencesFlow.first()
        val localById = billDao.getBillsForAccount(accountId).associateBy { it.id }
        val toUpsert = mutableListOf<BillEntity>()

        for (cb in remoteBills) {
            if (cb.id.isBlank()) continue
            val key = billSyncKey(cb.id)
            if (key in pref.pendingSyncUpserts || key in pref.pendingSyncDeletes) continue

            val remoteMillis = cb.updatedAt?.toDate()?.time ?: cb.createdAt?.toDate()?.time ?: 0L
            val local = localById[cb.id]
            if (local == null || remoteMillis > local.updatedAtMillis + 500L) {
                val total = cb.totalAmount.coerceAtLeast(1L)
                val status = BillStatus.fromRaw(cb.status)
                val paid = when (status) {
                    BillStatus.LUNAS -> total
                    BillStatus.BELUM_LUNAS -> 0L
                    BillStatus.CICIL -> cb.paidAmount.coerceIn(0L, total)
                }
                toUpsert += BillEntity(
                    id = cb.id,
                    accountId = accountId,
                    billType = BillType.fromRaw(cb.billType).name,
                    personName = cb.personName,
                    totalAmount = total,
                    paidAmount = paid,
                    status = status.name,
                    note = cb.note,
                    createdAtMillis = local?.createdAtMillis ?: cb.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAtMillis = maxOf(remoteMillis, local?.updatedAtMillis ?: 0L),
                )
            }
        }

        if (toUpsert.isNotEmpty()) {
            billDao.upsertBills(toUpsert)
            prefs.setLastCloudBackupMillis(System.currentTimeMillis())
        }
    }

    suspend fun backupNowToCloud(accountId: String): Result<CloudBackupSummary> = syncMutex.withLock {
        if (!networkMonitor.refreshNow()) {
            return Result.failure(IllegalStateException("Tidak ada koneksi internet. Perubahan tersimpan offline & otomatis tersinkron saat online."))
        }
        val account = accountDao.getAccountById(accountId)
            ?: return Result.failure(IllegalStateException("Akun lokal tidak ditemukan."))

        _isSyncingCloud.value = true
        return try {
            // Flush any pending deletes first so deleted items aren't left orphaned on cloud
            val prefState = prefs.preferencesFlow.first()
            for (delKey in prefState.pendingSyncDeletes) {
                when {
                    delKey.startsWith(PREFIX_WALLET) -> cloudRepo.deleteCloudWallet(delKey.removePrefix(PREFIX_WALLET))
                    delKey.startsWith(PREFIX_POCKET) -> cloudRepo.deleteCloudPocket(delKey.removePrefix(PREFIX_POCKET))
                    delKey.startsWith(PREFIX_TX) -> cloudRepo.deleteCloudTransaction(delKey.removePrefix(PREFIX_TX))
                    delKey.startsWith(PREFIX_BILL) -> cloudRepo.deleteCloudBill(delKey.removePrefix(PREFIX_BILL))
                }
            }

            val wallets = walletDao.getWalletsForAccount(accountId)
            val pockets = pocketDao.getPocketsForAccount(accountId)
            val transactions = transactionDao.getTransactionsForAccount(accountId)
            val bills = billDao.getBillsForAccount(accountId)

            val result = cloudRepo.backupAllToCloud(
                account = account,
                wallets = wallets,
                pockets = pockets,
                transactions = transactions,
                bills = bills,
                themeScheme = prefState.themeScheme,
                themeMode = prefState.themeMode,
                currencyCode = prefState.selectedCurrencyCode,
                languageCode = prefState.appLanguage,
                hideBalances = prefState.hideBalances,
            )
            result.onSuccess { summary ->
                prefs.clearPendingSync()
                prefs.setLastCloudBackupMillis(summary.syncedAtMillis)
            }
            result
        } finally {
            _isSyncingCloud.value = false
        }
    }

    suspend fun restoreFromCloudToAccount(accountId: String): Result<Pair<Int, Int>> = syncMutex.withLock {
        if (!networkMonitor.refreshNow()) {
            return Result.failure(IllegalStateException("Tidak ada koneksi internet untuk memulihkan dari cloud."))
        }
        val existingAccount = accountDao.getAccountById(accountId)
            ?: return Result.failure(IllegalStateException("Akun lokal tidak ditemukan."))

        _isSyncingCloud.value = true
        return try {
            val restoreResult = cloudRepo.restoreFromCloud()
            restoreResult.map { payload ->
                applyCloudRestorePayload(accountId, existingAccount, payload)
            }
        } finally {
            _isSyncingCloud.value = false
        }
    }

    private suspend fun applyCloudRestorePayload(
        accountId: String,
        existingAccount: LocalAccountEntity,
        payload: com.example.data.cloud.CloudRestorePayload,
    ): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        val restoredWallets = payload.wallets.map { cw ->
            WalletEntity(
                id = cw.id,
                accountId = accountId,
                name = cw.name,
                walletType = WalletType.fromRaw(cw.walletType).name,
                providerCode = cw.providerCode,
                providerShortName = cw.providerShortName,
                providerCompanyName = cw.providerCompanyName,
                accountNumberHint = cw.accountNumberHint,
                balance = cw.balance,
                colorSlot = cw.colorSlot.toInt().coerceIn(0, 5),
                isDefault = cw.isDefault,
                createdAtMillis = cw.createdAt?.toDate()?.time ?: now,
                updatedAtMillis = cw.updatedAt?.toDate()?.time ?: now,
            )
        }
        val restoredPockets = payload.pockets.map { cp ->
            PocketEntity(
                id = cp.id,
                accountId = accountId,
                name = cp.name,
                mode = PocketMode.fromRaw(cp.mode).name,
                targetAmount = cp.targetAmount,
                currentAmount = cp.currentAmount,
                iconKey = cp.iconKey,
                colorSlot = cp.colorSlot.toInt().coerceIn(0, 5),
                featured = cp.featured,
                createdAtMillis = cp.createdAt?.toDate()?.time ?: now,
                updatedAtMillis = cp.updatedAt?.toDate()?.time ?: now,
            )
        }
        val restoredTransactions = payload.transactions.map { ct ->
            TransactionEntity(
                id = ct.id,
                accountId = accountId,
                title = ct.title,
                note = ct.note,
                amount = ct.amount,
                type = TransactionType.fromRaw(ct.type).name,
                pocketId = ct.pocketId,
                pocketName = ct.pocketName,
                walletId = ct.walletId,
                walletName = ct.walletName,
                categoryKey = ct.categoryKey.ifBlank { "OTHER" },
                categoryLabel = ct.categoryLabel,
                locationName = if (TransactionType.fromRaw(ct.type) == TransactionType.EXPENSE) ct.locationName else "",
                occurredAtMillis = ct.occurredAtMillis,
                createdAtMillis = ct.createdAt?.toDate()?.time ?: now,
                updatedAtMillis = ct.updatedAt?.toDate()?.time ?: now,
            )
        }

        val restoredBills = payload.bills.map { cb ->
            val total = cb.totalAmount.coerceAtLeast(1L)
            val status = BillStatus.fromRaw(cb.status)
            val paid = when (status) {
                BillStatus.LUNAS -> total
                BillStatus.BELUM_LUNAS -> 0L
                BillStatus.CICIL -> cb.paidAmount.coerceIn(0L, total)
            }
            BillEntity(
                id = cb.id,
                accountId = accountId,
                billType = BillType.fromRaw(cb.billType).name,
                personName = cb.personName,
                totalAmount = total,
                paidAmount = paid,
                status = status.name,
                note = cb.note,
                createdAtMillis = cb.createdAt?.toDate()?.time ?: now,
                updatedAtMillis = cb.updatedAt?.toDate()?.time ?: now,
            )
        }

        db.withTransaction {
            payload.profile?.let { prof ->
                accountDao.updateAccount(
                    existingAccount.copy(
                        displayName = prof.displayName.ifBlank { existingAccount.displayName },
                        paydayFrequency = PaydayFrequency.fromRaw(prof.paydayFrequency).name,
                        paydayDay = prof.paydayDay.toInt().coerceIn(1, 31),
                        paydayWeekday = prof.paydayWeekday.toInt().coerceIn(1, 7),
                        paydayHour = prof.paydayHour.toInt().coerceIn(0, 23),
                        paydayMinute = prof.paydayMinute.toInt().coerceIn(0, 59),
                        monthlyBudget = prof.monthlyBudget.coerceAtLeast(0L),
                    )
                )
            }
            if (restoredWallets.isNotEmpty()) {
                walletDao.upsertWallets(restoredWallets)
            }
            if (restoredPockets.isNotEmpty()) {
                pocketDao.upsertPockets(restoredPockets)
            }
            if (restoredTransactions.isNotEmpty()) {
                transactionDao.upsertTransactions(restoredTransactions)
            }
            if (restoredBills.isNotEmpty()) {
                billDao.upsertBills(restoredBills)
            }
        }

        payload.profile?.let { prof ->
            prefs.setThemeScheme(ThemeScheme.fromRaw(prof.themeScheme))
            prefs.setThemeMode(ThemeMode.fromRaw(prof.themeMode))
            if (prof.currencyCode.length == 3) {
                prefs.setSelectedCurrencyCode(prof.currencyCode)
            }
            if (prof.languageCode.isNotBlank()) {
                prefs.setAppLanguage(prof.languageCode)
            }
            prefs.setHideBalances(prof.hideBalances)
        }
        prefs.setLastCloudBackupMillis(now)
        return restoredPockets.size to restoredTransactions.size
    }

    private suspend fun enqueueUpsertAndTriggerSync(vararg keys: String) {
        prefs.enqueueSyncUpserts(*keys)
        triggerBackgroundSyncIfEligible()
    }

    fun triggerBackgroundSyncIfEligible() {
        _syncTrigger.tryEmit(Unit)
    }

    fun refreshNetworkState(): Boolean = networkMonitor.refreshNow()

    private companion object {
        const val KEY_PROFILE = "profile"
        const val PREFIX_WALLET = "wallet:"
        const val PREFIX_POCKET = "pocket:"
        const val PREFIX_TX = "tx:"
        const val PREFIX_BILL = "bill:"

        fun walletSyncKey(id: String) = "$PREFIX_WALLET$id"
        fun pocketSyncKey(id: String) = "$PREFIX_POCKET$id"
        fun txSyncKey(id: String) = "$PREFIX_TX$id"
        fun billSyncKey(id: String) = "$PREFIX_BILL$id"
    }
}
