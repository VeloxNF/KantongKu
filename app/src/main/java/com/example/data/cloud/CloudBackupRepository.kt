package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.data.local.BillEntity
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PocketEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeScheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

data class CloudBackupSummary(
    val pocketsBackedUp: Int,
    val transactionsBackedUp: Int,
    val walletsBackedUp: Int = 0,
    val billsBackedUp: Int = 0,
    val syncedAtMillis: Long,
)

data class CloudRestorePayload(
    val profile: UserCloudProfile?,
    val wallets: List<CloudWallet>,
    val pockets: List<CloudPocket>,
    val transactions: List<CloudTransaction>,
    val bills: List<CloudBill> = emptyList(),
)

class CloudBackupRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google untuk sinkronisasi cloud.")
    }

    private suspend fun <T> awaitCloud(timeoutMs: Long = 15_000L, block: suspend () -> T): T {
        return withTimeoutOrNull(timeoutMs) { block() }
            ?: throw IOException("Koneksi cloud lambat atau terputus.")
    }

    fun observeCloudProfile(userId: String): Flow<UserCloudProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna berubah." }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserCloudProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeCloudWallets(userId: String): Flow<List<CloudWallet>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna berubah." }
        val colRef = db.collection("users").document(uid).collection("wallets")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudWallet::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeCloudPockets(userId: String): Flow<List<CloudPocket>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna berubah." }
        val colRef = db.collection("users").document(uid).collection("pockets")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudPocket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeCloudTransactions(userId: String): Flow<List<CloudTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna berubah." }
        val colRef = db.collection("users").document(uid).collection("transactions")
        emitAll(
            colRef.orderBy("occurredAtMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeCloudBills(userId: String): Flow<List<CloudBill>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna berubah." }
        val colRef = db.collection("users").document(uid).collection("bills")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBill::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertProfile(
        account: LocalAccountEntity,
        themeScheme: ThemeScheme,
        themeMode: ThemeMode,
        currencyCode: String,
        languageCode: String = "id",
        hideBalances: Boolean = false,
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val cloudProfile = UserCloudProfile(
                userId = uid,
                displayName = account.displayName.trim().ifBlank { "Pengguna" }.take(60),
                paydayFrequency = account.parsedPaydayFrequency.name,
                paydayDay = account.paydayDay.toLong().coerceIn(1L, 31L),
                paydayWeekday = account.paydayWeekday.toLong().coerceIn(1L, 7L),
                paydayHour = account.paydayHour.toLong().coerceIn(0L, 23L),
                paydayMinute = account.paydayMinute.toLong().coerceIn(0L, 59L),
                monthlyBudget = account.monthlyBudget.coerceAtLeast(0L),
                themeScheme = themeScheme.name,
                themeMode = themeMode.name,
                currencyCode = currencyCode,
                languageCode = languageCode,
                hideBalances = hideBalances,
            )
            awaitCloud {
                val existing = userRef.get().await()
                if (existing.exists()) {
                    userRef.update(cloudProfile.toUpdateMap()).await()
                } else {
                    userRef.set(cloudProfile.toCreateMap(uid)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun backupAllToCloud(
        account: LocalAccountEntity,
        wallets: List<WalletEntity>,
        pockets: List<PocketEntity>,
        transactions: List<TransactionEntity>,
        bills: List<BillEntity> = emptyList(),
        themeScheme: ThemeScheme,
        themeMode: ThemeMode,
        currencyCode: String,
        languageCode: String = "id",
        hideBalances: Boolean = false,
    ): Result<CloudBackupSummary> {
        var currentPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            currentPath = userRef.path

            val cloudProfile = UserCloudProfile(
                userId = uid,
                displayName = account.displayName.trim().ifBlank { "Pengguna" }.take(60),
                paydayFrequency = account.parsedPaydayFrequency.name,
                paydayDay = account.paydayDay.toLong().coerceIn(1L, 31L),
                paydayWeekday = account.paydayWeekday.toLong().coerceIn(1L, 7L),
                paydayHour = account.paydayHour.toLong().coerceIn(0L, 23L),
                paydayMinute = account.paydayMinute.toLong().coerceIn(0L, 59L),
                monthlyBudget = account.monthlyBudget.coerceAtLeast(0L),
                themeScheme = themeScheme.name,
                themeMode = themeMode.name,
                currencyCode = currencyCode,
                languageCode = languageCode,
                hideBalances = hideBalances,
            )

            awaitCloud(timeoutMs = 30_000L) {
                val existingUser = userRef.get().await()
                if (existingUser.exists()) {
                    userRef.update(cloudProfile.toUpdateMap()).await()
                } else {
                    userRef.set(cloudProfile.toCreateMap(uid)).await()
                }

                // Backup wallets
                val walletsCol = userRef.collection("wallets")
                currentPath = walletsCol.path
                val existingWalletIds = walletsCol.get().await().documents.map { it.id }.toSet()
                for (wallet in wallets) {
                    val safeId = wallet.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
                    if (safeId.isBlank()) continue
                    val wRef = walletsCol.document(safeId)
                    currentPath = wRef.path
                    val cloudWallet = CloudWallet(
                        userId = uid,
                        name = wallet.name,
                        walletType = wallet.walletType,
                        providerCode = wallet.providerCode,
                        providerShortName = wallet.providerShortName,
                        providerCompanyName = wallet.providerCompanyName,
                        accountNumberHint = wallet.accountNumberHint,
                        balance = wallet.balance,
                        colorSlot = wallet.colorSlot.toLong(),
                        isDefault = wallet.isDefault,
                    )
                    if (safeId in existingWalletIds) {
                        wRef.update(cloudWallet.toUpdateMap()).await()
                    } else {
                        wRef.set(cloudWallet.toCreateMap(uid)).await()
                    }
                }

                // Backup pockets
                val pocketsCol = userRef.collection("pockets")
                currentPath = pocketsCol.path
                val existingPocketIds = pocketsCol.get().await().documents.map { it.id }.toSet()
                for (pocket in pockets) {
                    val safeId = pocket.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
                    if (safeId.isBlank()) continue
                    val pRef = pocketsCol.document(safeId)
                    currentPath = pRef.path
                    val cloudPocket = CloudPocket(
                        userId = uid,
                        name = pocket.name,
                        mode = pocket.mode,
                        targetAmount = pocket.targetAmount,
                        currentAmount = pocket.currentAmount,
                        iconKey = pocket.iconKey,
                        colorSlot = pocket.colorSlot.toLong(),
                        featured = pocket.featured,
                    )
                    if (safeId in existingPocketIds) {
                        pRef.update(cloudPocket.toUpdateMap()).await()
                    } else {
                        pRef.set(cloudPocket.toCreateMap(uid)).await()
                    }
                }

                // Backup transactions
                val txCol = userRef.collection("transactions")
                currentPath = txCol.path
                val existingTxIds = txCol.get().await().documents.map { it.id }.toSet()
                for (tx in transactions) {
                    val safeId = tx.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
                    if (safeId.isBlank() || tx.amount <= 0L) continue
                    val txRef = txCol.document(safeId)
                    currentPath = txRef.path
                    val cloudTx = CloudTransaction(
                        userId = uid,
                        title = tx.title,
                        note = tx.note,
                        amount = tx.amount,
                        type = tx.type,
                        pocketId = tx.pocketId,
                        pocketName = tx.pocketName,
                        walletId = tx.walletId,
                        walletName = tx.walletName,
                        categoryKey = tx.categoryKey,
                        categoryLabel = tx.categoryLabel,
                        locationName = if (tx.txType == TransactionType.EXPENSE) tx.locationName else "",
                        occurredAtMillis = tx.occurredAtMillis,
                    )
                    if (safeId in existingTxIds) {
                        txRef.update(cloudTx.toUpdateMap()).await()
                    } else {
                        txRef.set(cloudTx.toCreateMap(uid)).await()
                    }
                }

                // Backup bills (Utang & Piutang)
                val billsCol = userRef.collection("bills")
                currentPath = billsCol.path
                val existingBillIds = billsCol.get().await().documents.map { it.id }.toSet()
                for (bill in bills) {
                    val safeId = bill.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
                    if (safeId.isBlank() || bill.totalAmount <= 0L) continue
                    val bRef = billsCol.document(safeId)
                    currentPath = bRef.path
                    val cloudBill = CloudBill(
                        userId = uid,
                        billType = bill.billType,
                        personName = bill.personName,
                        totalAmount = bill.totalAmount,
                        paidAmount = bill.effectivePaidAmount,
                        status = bill.status,
                        note = bill.note,
                    )
                    if (safeId in existingBillIds) {
                        bRef.update(cloudBill.toUpdateMap()).await()
                    } else {
                        bRef.set(cloudBill.toCreateMap(uid)).await()
                    }
                }
            }

            Result.success(
                CloudBackupSummary(
                    pocketsBackedUp = pockets.size,
                    transactionsBackedUp = transactions.size,
                    walletsBackedUp = wallets.size,
                    billsBackedUp = bills.size,
                    syncedAtMillis = System.currentTimeMillis(),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun upsertSingleWallet(wallet: WalletEntity): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/wallets/${wallet.id}"
        return try {
            val uid = requireUserId()
            val safeId = wallet.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Wallet ID invalid" }
            val ref = db.collection("users").document(uid).collection("wallets").document(safeId)
            path = ref.path
            val cloudWallet = CloudWallet(
                userId = uid,
                name = wallet.name,
                walletType = wallet.walletType,
                providerCode = wallet.providerCode,
                providerShortName = wallet.providerShortName,
                providerCompanyName = wallet.providerCompanyName,
                accountNumberHint = wallet.accountNumberHint,
                balance = wallet.balance,
                colorSlot = wallet.colorSlot.toLong(),
                isDefault = wallet.isDefault,
            )
            awaitCloud {
                if (ref.get().await().exists()) {
                    ref.update(cloudWallet.toUpdateMap()).await()
                } else {
                    ref.set(cloudWallet.toCreateMap(uid)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudWallet(walletId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/wallets/$walletId"
        return try {
            val uid = requireUserId()
            val safeId = walletId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Wallet ID invalid" }
            val ref = db.collection("users").document(uid).collection("wallets").document(safeId)
            path = ref.path
            awaitCloud { ref.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertSinglePocket(pocket: PocketEntity): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/pockets/${pocket.id}"
        return try {
            val uid = requireUserId()
            val safeId = pocket.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Pocket ID invalid" }
            val ref = db.collection("users").document(uid).collection("pockets").document(safeId)
            path = ref.path
            val cloudPocket = CloudPocket(
                userId = uid,
                name = pocket.name,
                mode = pocket.mode,
                targetAmount = pocket.targetAmount,
                currentAmount = pocket.currentAmount,
                iconKey = pocket.iconKey,
                colorSlot = pocket.colorSlot.toLong(),
                featured = pocket.featured,
            )
            awaitCloud {
                if (ref.get().await().exists()) {
                    ref.update(cloudPocket.toUpdateMap()).await()
                } else {
                    ref.set(cloudPocket.toCreateMap(uid)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudPocket(pocketId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/pockets/$pocketId"
        return try {
            val uid = requireUserId()
            val safeId = pocketId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Pocket ID invalid" }
            val ref = db.collection("users").document(uid).collection("pockets").document(safeId)
            path = ref.path
            awaitCloud { ref.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertSingleTransaction(tx: TransactionEntity): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/${tx.id}"
        return try {
            val uid = requireUserId()
            val safeId = tx.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Transaction ID invalid" }
            val ref = db.collection("users").document(uid).collection("transactions").document(safeId)
            path = ref.path
            val cloudTx = CloudTransaction(
                userId = uid,
                title = tx.title,
                note = tx.note,
                amount = tx.amount,
                type = tx.type,
                pocketId = tx.pocketId,
                pocketName = tx.pocketName,
                walletId = tx.walletId,
                walletName = tx.walletName,
                categoryKey = tx.categoryKey,
                categoryLabel = tx.categoryLabel,
                locationName = if (tx.txType == TransactionType.EXPENSE) tx.locationName else "",
                occurredAtMillis = tx.occurredAtMillis,
            )
            awaitCloud {
                if (ref.get().await().exists()) {
                    ref.update(cloudTx.toUpdateMap()).await()
                } else {
                    ref.set(cloudTx.toCreateMap(uid)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudTransaction(txId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$txId"
        return try {
            val uid = requireUserId()
            val safeId = txId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Transaction ID invalid" }
            val ref = db.collection("users").document(uid).collection("transactions").document(safeId)
            path = ref.path
            awaitCloud { ref.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertSingleBill(bill: BillEntity): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/bills/${bill.id}"
        return try {
            val uid = requireUserId()
            val safeId = bill.id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Bill ID invalid" }
            val ref = db.collection("users").document(uid).collection("bills").document(safeId)
            path = ref.path
            val cloudBill = CloudBill(
                userId = uid,
                billType = bill.billType,
                personName = bill.personName,
                totalAmount = bill.totalAmount,
                paidAmount = bill.effectivePaidAmount,
                status = bill.status,
                note = bill.note,
            )
            awaitCloud {
                if (ref.get().await().exists()) {
                    ref.update(cloudBill.toUpdateMap()).await()
                } else {
                    ref.set(cloudBill.toCreateMap(uid)).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBill(billId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/bills/$billId"
        return try {
            val uid = requireUserId()
            val safeId = billId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(128)
            require(safeId.isNotBlank()) { "Bill ID invalid" }
            val ref = db.collection("users").document(uid).collection("bills").document(safeId)
            path = ref.path
            awaitCloud { ref.delete().await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<CloudRestorePayload> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val payload = awaitCloud(timeoutMs = 20_000L) {
                path = userRef.path
                val profile = userRef.get().await().toObject(
                    UserCloudProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )

                path = "users/$uid/wallets"
                val wallets = userRef.collection("wallets")
                    .get()
                    .await()
                    .toObjects(CloudWallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

                path = "users/$uid/pockets"
                val pockets = userRef.collection("pockets")
                    .get()
                    .await()
                    .toObjects(CloudPocket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

                path = "users/$uid/transactions"
                val transactions = userRef.collection("transactions")
                    .orderBy("occurredAtMillis", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(CloudTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

                path = "users/$uid/bills"
                val bills = userRef.collection("bills")
                    .get()
                    .await()
                    .toObjects(CloudBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

                CloudRestorePayload(
                    profile = profile,
                    wallets = wallets,
                    pockets = pockets,
                    transactions = transactions,
                    bills = bills,
                )
            }
            Result.success(payload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }
}
