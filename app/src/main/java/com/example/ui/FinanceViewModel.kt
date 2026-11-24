package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ai.ReceiptScannerAi
import com.example.data.ai.ReceiptTransactionKind
import com.example.data.ai.ScannedReceiptDraft
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.currency.CurrencyCatalog
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.currency.ExchangeRateClient
import com.example.data.local.AppPreferencesState
import com.example.data.local.BillEntity
import com.example.data.local.BillStatus
import com.example.data.local.BillType
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PaydayFrequency
import com.example.data.local.PocketEntity
import com.example.data.local.PocketMode
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.local.WalletType
import com.example.data.repository.CategoryBudgetAlert
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SmartPocketAllocationItem
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeScheme
import com.example.util.FormatUtils
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class MonthlyBudgetPoint(
    val year: Int,
    val monthIndex: Int,
    val shortLabel: String,
    val fullLabel: String,
    val budgetAmount: Long,
    val actualSpent: Long,
    val isCurrentMonth: Boolean,
)

data class FinanceDashboardStats(
    val totalBalance: Long = 0L,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val dailySafeRemaining: Long = 0L,
    val monthlyBudget: Long = 0L,
    val paydayStats: FormatUtils.PaydayStats = FormatUtils.calculatePaydayStats(),
    val monthlyComparison: List<MonthlyBudgetPoint> = emptyList(),
)

data class FinanceUiState(
    val isReady: Boolean = false,
    val activeAccount: LocalAccountEntity? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val pockets: List<PocketEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val bills: List<BillEntity> = emptyList(),
    val stats: FinanceDashboardStats = FinanceDashboardStats(),
    val currencyConfig: CurrencyDisplayConfig = CurrencyDisplayConfig(),
    val exchangeRates: Map<String, Double> = CurrencyCatalog.seedRatesPerIdr,
    val isRefreshingRates: Boolean = false,
    val preferences: AppPreferencesState = AppPreferencesState(),
    val googleUser: FirebaseUser? = null,
    val isOnline: Boolean = true,
    val isSyncingCloud: Boolean = false,
    val pendingSyncCount: Int = 0,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
)

private data class LocalTransientState(
    val isBusy: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
)

private data class SyncEnvironment(
    val uid: String?,
    val isOnline: Boolean,
    val autoSync: Boolean,
)

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data class Analyzing(val previewBitmap: Bitmap) : ReceiptScanUiState
    data class Preview(val previewBitmap: Bitmap, val draft: ScannedReceiptDraft) : ReceiptScanUiState
    data class Error(
        val previewBitmap: Bitmap?,
        val message: String,
        val fallbackDraft: ScannedReceiptDraft,
    ) : ReceiptScanUiState
}

sealed interface AppUpdateCheckState {
    data object Idle : AppUpdateCheckState
    data object Checking : AppUpdateCheckState
    data class UpToDate(
        val versionName: String,
        val versionCode: Int,
        val checkedAtMillis: Long,
    ) : AppUpdateCheckState
    data class Offline(
        val versionName: String,
        val versionCode: Int,
    ) : AppUpdateCheckState
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val repository: FinanceRepository,
) : ViewModel() {

    private val indonesiaLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    private val transientState = MutableStateFlow(LocalTransientState())
    private val _budgetAlerts = MutableSharedFlow<CategoryBudgetAlert>(extraBufferCapacity = 8)
    val budgetAlerts: SharedFlow<CategoryBudgetAlert> = _budgetAlerts.asSharedFlow()

    private val receiptScannerAi = ReceiptScannerAi()
    private val _receiptScanState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptScanState: StateFlow<ReceiptScanUiState> = _receiptScanState
    private val _appUpdateState = MutableStateFlow<AppUpdateCheckState>(AppUpdateCheckState.Idle)
    val appUpdateState: StateFlow<AppUpdateCheckState> = _appUpdateState
    private var scanJob: Job? = null

    private val authUserFlow = GoogleAuthHelper.authStateFlow()

    init {
        // Ensure active Google account always has at least one wallet locally
        viewModelScope.launch {
            repository.activeAccountFlow
                .filterNotNull()
                .map { it.id }
                .distinctUntilChanged()
                .collect { accountId ->
                    repository.ensureStarterWalletIfEmpty(accountId)
                }
        }

        // Automatically refresh live currency rates when online and stale (> 6 hours)
        viewModelScope.launch {
            repository.isOnlineFlow
                .collect { online ->
                    if (online) {
                        val prefs = repository.preferencesFlow.first()
                        val staleThresholdMs = 6L * 3_600_000L
                        if (System.currentTimeMillis() - prefs.exchangeRatesUpdatedAtMillis > staleThresholdMs) {
                            refreshExchangeRates(silent = true)
                        }
                    }
                }
        }

        val syncEnvFlow = combine(
            authUserFlow.map { it?.uid }.distinctUntilChanged(),
            repository.isOnlineFlow,
            repository.preferencesFlow.map { it.autoCloudSync }.distinctUntilChanged(),
        ) { uid, online, autoSync ->
            SyncEnvironment(uid = uid, isOnline = online, autoSync = autoSync)
        }.distinctUntilChanged()

        // 1. Background push sync: runs immediately when internet becomes available or new local mutations arrive
        viewModelScope.launch {
            syncEnvFlow.collectLatest { env ->
                if (env.uid != null && env.isOnline && env.autoSync) {
                    repository.syncPendingChangesNow()
                    repository.syncTriggerFlow.collect {
                        repository.syncPendingChangesNow()
                    }
                }
            }
        }

        // 2. Real-time Firestore listener sync: merges remote updates into Room whenever online & signed in
        viewModelScope.launch {
            syncEnvFlow.flatMapLatest { env ->
                val uid = env.uid
                if (uid.isNullOrBlank() || !env.isOnline || !env.autoSync) {
                    flowOf(Unit)
                } else {
                    merge(
                        repository.observeRemoteProfile(uid)
                            .map { profile -> repository.mergeRemoteProfile(uid, profile) }
                            .catch { handleRealtimeStreamError(it, "profile") },
                        repository.observeRemoteWallets(uid)
                            .map { wallets -> repository.mergeRemoteWallets(uid, wallets) }
                            .catch { handleRealtimeStreamError(it, "wallets") },
                        repository.observeRemotePockets(uid)
                            .map { pockets -> repository.mergeRemotePockets(uid, pockets) }
                            .catch { handleRealtimeStreamError(it, "pockets") },
                        repository.observeRemoteTransactions(uid)
                            .map { transactions -> repository.mergeRemoteTransactions(uid, transactions) }
                            .catch { handleRealtimeStreamError(it, "transactions") },
                        repository.observeRemoteBills(uid)
                            .map { bills -> repository.mergeRemoteBills(uid, bills) }
                            .catch { handleRealtimeStreamError(it, "bills") },
                    )
                }
            }.collect { }
        }
    }

    private fun handleRealtimeStreamError(error: Throwable, collectionName: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w("FinanceSync", "Realtime sync warning on $collectionName", firestoreError ?: error)
    }

    private val coreDataFlow = combine(
        repository.activeAccountFlow,
        repository.activeWalletsFlow,
        repository.activePocketsFlow,
        repository.activeTransactionsFlow,
        repository.activeBillsFlow,
    ) { account, wallets, pockets, transactions, bills ->
        CoreAccountData(account, wallets, pockets, transactions, bills)
    }

    private val networkAndSyncFlow = combine(
        repository.isOnlineFlow,
        repository.isSyncingCloudFlow,
        authUserFlow,
    ) { isOnline, isSyncingCloud, googleUser ->
        NetworkAndSyncMeta(isOnline, isSyncingCloud, googleUser)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        coreDataFlow,
        repository.preferencesFlow,
        networkAndSyncFlow,
        transientState,
    ) { core, prefs, netMeta, transient ->
        val decodedRates = ExchangeRateClient.decodeRatesFromJson(prefs.cachedExchangeRatesJson)
        val currencyConfig = CurrencyCatalog.buildDisplayConfig(
            selectedCode = prefs.selectedCurrencyCode,
            liveRates = decodedRates,
            updatedAtMillis = prefs.exchangeRatesUpdatedAtMillis,
        )
        FormatUtils.activeCurrencyConfig = currencyConfig

        val stats = computeStats(core.account, core.wallets, core.transactions)
        FinanceUiState(
            isReady = true,
            activeAccount = core.account,
            wallets = core.wallets,
            pockets = core.pockets,
            transactions = core.transactions,
            bills = core.bills,
            stats = stats,
            currencyConfig = currencyConfig,
            exchangeRates = decodedRates,
            isRefreshingRates = transient.isRefreshingRates,
            preferences = prefs,
            googleUser = netMeta.googleUser,
            isOnline = netMeta.isOnline,
            isSyncingCloud = netMeta.isSyncingCloud,
            pendingSyncCount = prefs.pendingSyncCount,
            isBusy = transient.isBusy,
            feedbackMessage = transient.feedbackMessage,
            errorMessage = transient.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = FinanceUiState(isReady = false),
    )

    private fun computeStats(
        account: LocalAccountEntity?,
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>,
    ): FinanceDashboardStats {
        if (account == null) return FinanceDashboardStats()
        val now = System.currentTimeMillis()
        val monthStart = FormatUtils.startOfCurrentMonthMillis(now)
        val paydayStats = FormatUtils.calculatePaydayStats(
            frequency = account.parsedPaydayFrequency,
            paydayDay = account.paydayDay,
            paydayWeekday = account.paydayWeekday,
            paydayHour = account.paydayHour,
            paydayMinute = account.paydayMinute,
            nowMillis = now,
        )

        var txNetBalance = 0L
        var monthIncome = 0L
        var monthExpense = 0L

        val expenseByYearMonth = mutableMapOf<Pair<Int, Int>, Long>()
        val txCal = Calendar.getInstance()

        for (tx in transactions) {
            if (tx.txType == TransactionType.INCOME) {
                txNetBalance += tx.amount
                if (tx.occurredAtMillis >= monthStart) monthIncome += tx.amount
            } else {
                txNetBalance -= tx.amount
                if (tx.occurredAtMillis >= monthStart) monthExpense += tx.amount
                txCal.timeInMillis = tx.occurredAtMillis
                val key = txCal.get(Calendar.YEAR) to txCal.get(Calendar.MONTH)
                expenseByYearMonth[key] = (expenseByYearMonth[key] ?: 0L) + tx.amount
            }
        }

        val totalBalance = if (wallets.isNotEmpty()) {
            wallets.sumOf { it.balance }
        } else {
            txNetBalance
        }

        val shortFmt = SimpleDateFormat("MMM", indonesiaLocale)
        val fullFmt = SimpleDateFormat("MMMM yyyy", indonesiaLocale)
        val currentCal = Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = currentCal.get(Calendar.YEAR)
        val currentMonth = currentCal.get(Calendar.MONTH)

        val monthlyComparison = (-5..0).map { offset ->
            val cal = (currentCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, offset)
            }
            val yr = cal.get(Calendar.YEAR)
            val mo = cal.get(Calendar.MONTH)
            val actual = expenseByYearMonth[yr to mo] ?: 0L
            MonthlyBudgetPoint(
                year = yr,
                monthIndex = mo,
                shortLabel = shortFmt.format(cal.time).replaceFirstChar { it.uppercase(indonesiaLocale) },
                fullLabel = fullFmt.format(cal.time).replaceFirstChar { it.uppercase(indonesiaLocale) },
                budgetAmount = account.monthlyBudget,
                actualSpent = actual,
                isCurrentMonth = yr == currentYear && mo == currentMonth,
            )
        }

        val daysRemaining = max(1, paydayStats.daysUntilPayday)
        val dailySafe = when {
            totalBalance > 0L -> totalBalance / daysRemaining
            account.monthlyBudget > 0L && monthExpense > 0L ->
                ((account.monthlyBudget - monthExpense).coerceAtLeast(0L)) / daysRemaining
            else -> 0L
        }

        return FinanceDashboardStats(
            totalBalance = totalBalance,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            dailySafeRemaining = dailySafe,
            monthlyBudget = account.monthlyBudget,
            paydayStats = paydayStats,
            monthlyComparison = monthlyComparison,
        )
    }

    fun syncGoogleAccountSession(user: FirebaseUser) {
        viewModelScope.launch {
            val result = repository.ensureGoogleAccountSession(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
            )
            result.onFailure { e ->
                transientState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Gagal menyiapkan sesi akun Google")
                }
            }
        }
    }

    fun onSignedOut() {
        viewModelScope.launch {
            repository.clearSessionOnSignOut()
            transientState.update {
                it.copy(isBusy = false, feedbackMessage = "Berhasil keluar dari akun Google.")
            }
        }
    }

    fun onAppResumed() {
        val online = repository.refreshNetworkState()
        if (online) {
            repository.triggerBackgroundSyncIfEligible()
        }
    }

    fun refreshConnectivityAndSync(): Boolean {
        val online = repository.refreshNetworkState()
        if (online) {
            viewModelScope.launch {
                repository.syncPendingChangesNow()
            }
        }
        return online
    }

    fun updateProfile(
        displayName: String,
        paydayFrequency: PaydayFrequency,
        paydayDay: Int,
        paydayWeekday: Int,
        paydayHour: Int,
        paydayMinute: Int,
        monthlyBudget: Long,
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.updateProfile(
                accountId = accountId,
                displayName = displayName,
                paydayFrequency = paydayFrequency,
                paydayDay = paydayDay,
                paydayWeekday = paydayWeekday,
                paydayHour = paydayHour,
                paydayMinute = paydayMinute,
                monthlyBudget = monthlyBudget,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isBusy = false, feedbackMessage = "Profil berhasil diperbarui") },
                    onFailure = { e -> state.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal menyimpan profil") },
                )
            }
        }
    }

    fun saveWallet(
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
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.saveWallet(
                accountId = accountId,
                existingId = existingId,
                name = name,
                walletType = walletType,
                providerCode = providerCode,
                providerShortName = providerShortName,
                providerCompanyName = providerCompanyName,
                accountNumberHint = accountNumberHint,
                balance = balance,
                colorSlot = colorSlot,
                isDefault = isDefault,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { saved ->
                        state.copy(isBusy = false, feedbackMessage = "Dompet '${saved.name}' disimpan")
                    },
                    onFailure = { e ->
                        state.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal menyimpan dompet")
                    },
                )
            }
        }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch {
            val result = repository.deleteWallet(walletId)
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(feedbackMessage = "Dompet dihapus") },
                    onFailure = { e -> state.copy(errorMessage = e.localizedMessage ?: "Gagal menghapus dompet") },
                )
            }
        }
    }

    fun transferBetweenWallets(
        fromWalletId: String,
        toWalletId: String,
        amount: Long,
        note: String,
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.transferBetweenWallets(
                accountId = accountId,
                fromWalletId = fromWalletId,
                toWalletId = toWalletId,
                amount = amount,
                note = note,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isBusy = false, feedbackMessage = "Saldo antar dompet berhasil dipindahkan") },
                    onFailure = { e -> state.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal memindahkan saldo") },
                )
            }
        }
    }

    fun selectCurrency(code: String) {
        viewModelScope.launch {
            repository.setSelectedCurrencyCode(code)
            val prefs = repository.preferencesFlow.first()
            val rates = ExchangeRateClient.decodeRatesFromJson(prefs.cachedExchangeRatesJson)
            val nextConfig = CurrencyCatalog.buildDisplayConfig(code, rates, prefs.exchangeRatesUpdatedAtMillis)
            FormatUtils.activeCurrencyConfig = nextConfig
            transientState.update {
                it.copy(feedbackMessage = "Mata uang diubah ke ${nextConfig.code} (${nextConfig.nameId})")
            }
            if (prefs.exchangeRatesUpdatedAtMillis == 0L && repository.refreshNetworkState()) {
                refreshExchangeRates(silent = true)
            }
        }
    }

    fun refreshExchangeRates(silent: Boolean = false) {
        viewModelScope.launch {
            transientState.update { it.copy(isRefreshingRates = true) }
            val result = repository.refreshLiveExchangeRates()
            transientState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            isRefreshingRates = false,
                            feedbackMessage = if (silent) state.feedbackMessage else "Kurs mata uang dunia diperbarui",
                        )
                    },
                    onFailure = { e ->
                        state.copy(
                            isRefreshingRates = false,
                            errorMessage = if (silent) state.errorMessage else (e.localizedMessage ?: "Gagal mengambil kurs terbaru (pakai data tersimpan)"),
                        )
                    },
                )
            }
        }
    }

    fun savePocket(
        existingId: String?,
        name: String,
        mode: PocketMode,
        targetAmount: Long,
        currentAmount: Long,
        iconKey: String,
        colorSlot: Int,
        featured: Boolean,
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.savePocket(
                accountId = accountId,
                existingId = existingId,
                name = name,
                mode = mode,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                iconKey = iconKey,
                colorSlot = colorSlot,
                featured = featured,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { saved ->
                        state.copy(isBusy = false, feedbackMessage = "Kantong '${saved.pocket.name}' disimpan")
                    },
                    onFailure = { e ->
                        state.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal menyimpan kantong")
                    },
                )
            }
            result.getOrNull()?.budgetAlert?.let { alert ->
                if (uiState.value.preferences.budgetAlertsEnabled) {
                    _budgetAlerts.emit(alert)
                }
            }
        }
    }

    fun deletePocket(pocketId: String) {
        viewModelScope.launch {
            val result = repository.deletePocket(pocketId)
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(feedbackMessage = "Kantong dihapus") },
                    onFailure = { e -> state.copy(errorMessage = e.localizedMessage ?: "Gagal menghapus kantong") },
                )
            }
        }
    }

    fun saveTransaction(
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
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.recordOrUpdateTransaction(
                accountId = accountId,
                existingId = existingId,
                title = title,
                note = note,
                amount = amount,
                type = type,
                pocketId = pocketId,
                walletId = walletId,
                categoryKey = categoryKey,
                categoryLabel = categoryLabel,
                locationName = locationName,
                occurredAtMillis = occurredAtMillis,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { saved ->
                        state.copy(isBusy = false, feedbackMessage = "${saved.transaction.txType.label} '${saved.transaction.title}' dicatat")
                    },
                    onFailure = { e ->
                        state.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal mencatat transaksi")
                    },
                )
            }
            result.getOrNull()?.budgetAlert?.let { alert ->
                if (uiState.value.preferences.budgetAlertsEnabled) {
                    _budgetAlerts.emit(alert)
                }
            }
        }
    }

    fun analyzeReceiptBitmap(bitmap: Bitmap) {
        scanJob?.cancel()
        val snapshot = uiState.value
        val fallbackDraft = buildEmptyReceiptDraft(snapshot.wallets)
        if (!snapshot.isOnline && !repository.refreshNetworkState()) {
            _receiptScanState.value = ReceiptScanUiState.Error(
                previewBitmap = bitmap,
                message = "Mode offline aktif. AI butuh internet untuk membaca foto struk otomatis, namun kamu tetap bisa isi manual sambil melihat foto struk ini.",
                fallbackDraft = fallbackDraft,
            )
            return
        }

        _receiptScanState.value = ReceiptScanUiState.Analyzing(bitmap)
        scanJob = viewModelScope.launch {
            try {
                val draft = receiptScannerAi.analyzeReceipt(
                    bitmap = bitmap,
                    wallets = snapshot.wallets,
                    pockets = snapshot.pockets,
                    exchangeRates = snapshot.exchangeRates,
                )
                _receiptScanState.value = ReceiptScanUiState.Preview(
                    previewBitmap = bitmap,
                    draft = draft,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ReceiptScanner", "Receipt scan failed", e)
                _receiptScanState.value = ReceiptScanUiState.Error(
                    previewBitmap = bitmap,
                    message = e.localizedMessage ?: "Gagal memindai struk. Periksa koneksi atau isi manual dari foto struk.",
                    fallbackDraft = fallbackDraft,
                )
            }
        }
    }

    fun switchFailedScanToManualPreview() {
        val current = _receiptScanState.value
        if (current is ReceiptScanUiState.Error && current.previewBitmap != null) {
            _receiptScanState.value = ReceiptScanUiState.Preview(
                previewBitmap = current.previewBitmap,
                draft = current.fallbackDraft,
            )
        }
    }

    fun clearReceiptScanState() {
        scanJob?.cancel()
        scanJob = null
        _receiptScanState.value = ReceiptScanUiState.Idle
    }

    private fun buildEmptyReceiptDraft(wallets: List<WalletEntity>): ScannedReceiptDraft {
        val defaultWallet = wallets.firstOrNull { it.isDefault } ?: wallets.firstOrNull()
        val secondWallet = wallets.firstOrNull { it.id != defaultWallet?.id }
        return ScannedReceiptDraft(
            kind = ReceiptTransactionKind.EXPENSE,
            title = "",
            locationName = "",
            occurredAtMillis = System.currentTimeMillis(),
            amountIdr = 0L,
            detectedCurrencyCode = "IDR",
            originalForeignAmountText = "",
            subtotalText = "",
            taxAndAdminText = "",
            discountText = "",
            amountCheckReasoning = "",
            categoryKey = "OTHER",
            matchedWalletId = defaultWallet?.id.orEmpty(),
            matchedFromWalletId = defaultWallet?.id.orEmpty(),
            matchedToWalletId = secondWallet?.id.orEmpty(),
            matchedPocketId = "",
            note = "",
            confidenceLevel = "MANUAL",
            warningNote = "Silakan periksa dan lengkapi nominal serta rincian dari foto struk.",
        )
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(feedbackMessage = "Transaksi dihapus & saldo disesuaikan") },
                    onFailure = { e -> state.copy(errorMessage = e.localizedMessage ?: "Gagal menghapus transaksi") },
                )
            }
        }
    }

    fun setThemeScheme(scheme: ThemeScheme) {
        viewModelScope.launch { repository.setThemeScheme(scheme) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAutoCloudSync(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoCloudSync(enabled) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricLockEnabled(enabled) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBudgetAlertsEnabled(enabled) }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch { repository.setAppLanguage(languageCode) }
    }

    fun setHideBalances(hidden: Boolean) {
        viewModelScope.launch { repository.setHideBalances(hidden) }
    }

    fun clearExchangeRatesCache() {
        viewModelScope.launch {
            repository.clearExchangeRatesCache()
            transientState.update { it.copy(feedbackMessage = "Cache kurs mata uang dibersihkan") }
        }
    }

    fun checkAppUpdate() {
        viewModelScope.launch {
            _appUpdateState.value = AppUpdateCheckState.Checking
            val online = repository.refreshNetworkState()
            if (!online) {
                _appUpdateState.value = AppUpdateCheckState.Offline(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                )
            } else {
                _appUpdateState.value = AppUpdateCheckState.UpToDate(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    checkedAtMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    suspend fun exportTransactionsCsv(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val txs = uiState.value.transactions
            require(txs.isNotEmpty()) { "Belum ada transaksi untuk diekspor." }
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileFmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
            val file = File(exportDir, "kantongku_transaksi_${fileFmt.format(Date())}.csv")
            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", indonesiaLocale)

            fun csvCell(raw: String): String {
                val escaped = raw.replace("\"", "\"\"")
                return "\"$escaped\""
            }

            val builder = StringBuilder()
            builder.append('\uFEFF') // UTF-8 BOM for Excel / Google Sheets compatibility
            builder.appendLine("Tanggal,Jenis,Judul,Kategori,Nama Tempat,Dompet,Kantong,Nominal (IDR),Catatan")
            for (tx in txs) {
                builder.appendLine(
                    listOf(
                        csvCell(dateFmt.format(Date(tx.occurredAtMillis))),
                        csvCell(tx.txType.label),
                        csvCell(tx.title),
                        csvCell(tx.categoryLabel.ifBlank { tx.categoryKey }),
                        csvCell(if (tx.txType == TransactionType.EXPENSE) tx.locationName else ""),
                        csvCell(tx.walletName),
                        csvCell(tx.pocketName),
                        tx.amount.toString(),
                        csvCell(tx.note),
                    ).joinToString(",")
                )
            }
            file.writeText(builder.toString(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun applySmartPocketAllocation(
        items: List<SmartPocketAllocationItem>,
        updatedMonthlyBudget: Long?,
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.applySmartPocketAllocation(
                accountId = accountId,
                items = items,
                updatedMonthlyBudget = updatedMonthlyBudget,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { count ->
                        state.copy(
                            isBusy = false,
                            feedbackMessage = "Kantong Pintar diterapkan ke $count kantong!",
                        )
                    },
                    onFailure = { e ->
                        state.copy(
                            isBusy = false,
                            errorMessage = e.localizedMessage ?: "Gagal menerapkan Kantong Pintar",
                        )
                    },
                )
            }
        }
    }

    fun saveBill(
        existingId: String?,
        billType: BillType,
        personName: String,
        totalAmount: Long,
        paidAmount: Long,
        status: BillStatus,
        note: String,
    ) {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.saveBill(
                accountId = accountId,
                existingId = existingId,
                billType = billType,
                personName = personName,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                status = status,
                note = note,
            )
            transientState.update { state ->
                result.fold(
                    onSuccess = { saved ->
                        state.copy(
                            isBusy = false,
                            feedbackMessage = "${saved.parsedType.label} '${saved.personName}' (${saved.parsedStatus.label}) disimpan",
                        )
                    },
                    onFailure = { e ->
                        state.copy(
                            isBusy = false,
                            errorMessage = e.localizedMessage ?: "Gagal menyimpan tagihan",
                        )
                    },
                )
            }
        }
    }

    fun updateBillStatus(
        billId: String,
        status: BillStatus,
        paidAmount: Long? = null,
    ) {
        viewModelScope.launch {
            val result = repository.updateBillStatus(billId, status, paidAmount)
            transientState.update { state ->
                result.fold(
                    onSuccess = { updated ->
                        state.copy(
                            feedbackMessage = "${updated.parsedType.label} '${updated.personName}' diubah ke ${updated.parsedStatus.label}",
                        )
                    },
                    onFailure = { e ->
                        state.copy(errorMessage = e.localizedMessage ?: "Gagal memperbarui status tagihan")
                    },
                )
            }
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            val result = repository.deleteBill(billId)
            transientState.update { state ->
                result.fold(
                    onSuccess = { state.copy(feedbackMessage = "Catatan tagihan dihapus") },
                    onFailure = { e -> state.copy(errorMessage = e.localizedMessage ?: "Gagal menghapus tagihan") },
                )
            }
        }
    }

    fun backupToCloudNow() {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.backupNowToCloud(accountId)
            transientState.update {
                result.fold(
                    onSuccess = { summary ->
                        it.copy(
                            isBusy = false,
                            feedbackMessage = "Sinkron berhasil: ${summary.walletsBackedUp} dompet, ${summary.pocketsBackedUp} kantong, ${summary.transactionsBackedUp} transaksi & ${summary.billsBackedUp} tagihan",
                        )
                    },
                    onFailure = { e ->
                        it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal menyinkronkan ke cloud")
                    },
                )
            }
        }
    }

    fun restoreFromCloudNow() {
        val accountId = uiState.value.activeAccount?.id ?: return
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, errorMessage = null) }
            val result = repository.restoreFromCloudToAccount(accountId)
            transientState.update {
                result.fold(
                    onSuccess = { (pocketsCount, txCount) ->
                        it.copy(
                            isBusy = false,
                            feedbackMessage = "Dipulihkan: $pocketsCount kantong & $txCount transaksi dari cloud",
                        )
                    },
                    onFailure = { e ->
                        it.copy(isBusy = false, errorMessage = e.localizedMessage ?: "Gagal memulihkan dari cloud")
                    },
                )
            }
        }
    }

    fun showMessage(msg: String) {
        transientState.update { it.copy(feedbackMessage = msg) }
    }

    fun showError(msg: String) {
        transientState.update { it.copy(errorMessage = msg) }
    }

    fun clearMessages() {
        transientState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    private data class CoreAccountData(
        val account: LocalAccountEntity?,
        val wallets: List<WalletEntity>,
        val pockets: List<PocketEntity>,
        val transactions: List<TransactionEntity>,
        val bills: List<BillEntity>,
    )

    private data class NetworkAndSyncMeta(
        val isOnline: Boolean,
        val isSyncingCloud: Boolean,
        val googleUser: FirebaseUser?,
    )
}
