package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.BiometricAuthHelper
import com.example.data.auth.BiometricAvailability
import com.example.data.auth.findFragmentActivity
import com.example.data.cloud.CloudBackupRepository
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.NetworkMonitor
import com.example.data.local.AppDatabase
import com.example.data.local.BillEntity
import com.example.data.local.BillStatus
import com.example.data.local.PocketEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.WalletEntity
import com.example.data.repository.CategoryBudgetAlert
import com.example.data.repository.FinanceRepository
import com.example.notifications.BudgetAlertNotifier
import com.example.ui.FinanceUiState
import com.example.ui.FinanceViewModel
import com.example.ui.components.AllQuickActionsSheet
import com.example.ui.components.AppTab
import com.example.ui.components.BentoBottomBar
import com.example.ui.components.BentoNavigationRail
import com.example.ui.components.CurrencySwitcherDialog
import com.example.ui.components.HomeDashboardSkeleton
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.appStringsFor
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.BillEditorSheet
import com.example.ui.screens.BillsScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PaydaySettingsSheet
import com.example.ui.screens.PocketEditorSheet
import com.example.ui.screens.PocketsScreen
import com.example.ui.screens.ReceiptScannerSheet
import com.example.ui.screens.SmartPocketSheet
import com.example.ui.screens.TransactionDetailSheet
import com.example.ui.screens.TransactionEditorSheet
import com.example.ui.screens.WalletEditorSheet
import com.example.ui.screens.WalletTransferSheet
import com.example.ui.theme.KantongKuTheme
import com.example.ui.theme.ThemeMode
import com.example.util.FormatUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.cloud.configureAppCheck(this, intent)
        BudgetAlertNotifier.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            KantongKuApp()
        }
    }
}

@Composable
private fun KantongKuApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FinanceViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                val prefs = PreferencesManager(app)
                val firestore = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                val cloudRepo = CloudBackupRepository(firestore, Firebase.auth)
                val networkMonitor = NetworkMonitor(app)
                val repo = FinanceRepository(db, prefs, cloudRepo, Firebase.auth, networkMonitor)
                FinanceViewModel(repo)
            }
        }
    )

    LaunchedEffect(Unit) {
        BudgetAlertNotifier.createNotificationChannel(context)
        if (Firebase.auth.currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostActivity = remember(context) { context.findFragmentActivity() }

    var biometricAvailability by remember {
        mutableStateOf(BiometricAuthHelper.checkAvailability(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(BudgetAlertNotifier.hasNotificationPermission(context))
    }
    var pendingNotificationAlert by remember { mutableStateOf<CategoryBudgetAlert?>(null) }

    var isSessionUnlocked by rememberSaveable { mutableStateOf(false) }
    var wasSignedOut by rememberSaveable { mutableStateOf(false) }
    var isExternalFlowActive by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isExternalFlowActive = false
        hasNotificationPermission = granted && BudgetAlertNotifier.hasNotificationPermission(context)
        val pending = pendingNotificationAlert
        pendingNotificationAlert = null
        if (pending != null) {
            if (hasNotificationPermission) {
                BudgetAlertNotifier.showCategoryExceededNotification(context, pending)
            } else {
                viewModel.showMessage(
                    "Peringatan: ${pending.categoryName} melebihi target (${FormatUtils.formatRupiah(pending.spentAmount)} / ${FormatUtils.formatRupiah(pending.targetAmount)})"
                )
            }
        }
    }

    fun requestNotificationPermissionOrOpenSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BudgetAlertNotifier.hasNotificationPermission(context)
        ) {
            val activity = hostActivity
            val shouldOpenSettings = activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
                hasNotificationPermission.not()
            // Always try standard runtime permission request first unless system permanently denied
            isExternalFlowActive = true
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
                isExternalFlowActive = false
                if (shouldOpenSettings) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            }
        } else if (!BudgetAlertNotifier.hasNotificationPermission(context)) {
            try {
                isExternalFlowActive = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                isExternalFlowActive = false
            }
        }
    }

    LaunchedEffect(uiState.isReady, uiState.googleUser?.uid, uiState.activeAccount?.id) {
        if (!uiState.isReady) return@LaunchedEffect
        val gUser = uiState.googleUser
        if (gUser == null) {
            wasSignedOut = true
            isSessionUnlocked = false
        } else {
            if (uiState.activeAccount?.id != gUser.uid) {
                viewModel.syncGoogleAccountSession(gUser)
            }
            if (wasSignedOut) {
                wasSignedOut = false
                isSessionUnlocked = true
            }
        }
    }

    DisposableEffect(lifecycleOwner, hostActivity, uiState.googleUser?.uid, uiState.preferences.biometricLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    biometricAvailability = BiometricAuthHelper.checkAvailability(context)
                    hasNotificationPermission = BudgetAlertNotifier.hasNotificationPermission(context)
                    isExternalFlowActive = false
                    viewModel.onAppResumed()
                }
                Lifecycle.Event.ON_STOP -> {
                    val changingConfigs = hostActivity?.isChangingConfigurations == true
                    if (!changingConfigs &&
                        !isExternalFlowActive &&
                        uiState.googleUser != null &&
                        uiState.preferences.biometricLockEnabled &&
                        biometricAvailability.isReady
                    ) {
                        isSessionUnlocked = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val googleUser = uiState.googleUser
    val activeAccount = uiState.activeAccount
    val isAccountProvisioned = googleUser != null && activeAccount != null && activeAccount.id == googleUser.uid

    val shouldRequireBiometricLock = uiState.isReady &&
        isAccountProvisioned &&
        uiState.preferences.biometricLockEnabled &&
        biometricAvailability.isReady &&
        !isSessionUnlocked

    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val appStrings = remember(uiState.preferences.appLanguage) {
        appStringsFor(uiState.preferences.appLanguage)
    }

    KantongKuTheme(
        themeScheme = uiState.preferences.themeScheme,
        themeMode = uiState.preferences.themeMode,
    ) {
        CompositionLocalProvider(LocalAppStrings provides appStrings) {
            when {
                !uiState.isReady -> {
                    HomeDashboardSkeleton()
                }

                googleUser == null -> {
                    GoogleAuthScreen(
                        isOnline = uiState.isOnline,
                        externalError = uiState.errorMessage,
                        onRefreshConnectivity = viewModel::refreshConnectivityAndSync,
                        onAuthSuccess = {
                            viewModel.clearMessages()
                            Firebase.auth.currentUser?.let { viewModel.syncGoogleAccountSession(it) }
                        },
                        onAuthError = viewModel::showError,
                    )
                }

                !isAccountProvisioned -> {
                    HomeDashboardSkeleton()
                }

                else -> {
                    androidx.compose.runtime.key(googleUser.uid) {
                        if (shouldRequireBiometricLock) {
                            BiometricLockScreen(
                                account = activeAccount!!,
                                onUnlocked = { isSessionUnlocked = true },
                                onFallbackToPinLogin = {
                                    isSessionUnlocked = false
                                    GoogleAuthHelper.signOut(credentialManager, scope) {
                                        viewModel.onSignedOut()
                                    }
                                },
                                onPromptVisibilityChanged = { active ->
                                    isExternalFlowActive = active
                                },
                            )
                        } else {
                            MainAuthenticatedApp(
                                uiState = uiState,
                                appUpdateState = appUpdateState,
                                viewModel = viewModel,
                                biometricAvailability = biometricAvailability,
                                hasNotificationPermission = hasNotificationPermission,
                                onTriggerBudgetNotification = { alert ->
                                    if (BudgetAlertNotifier.hasNotificationPermission(context)) {
                                        BudgetAlertNotifier.showCategoryExceededNotification(context, alert)
                                        viewModel.showMessage(
                                            "Peringatan anggaran: ${alert.categoryName} melebihi target (${FormatUtils.formatRupiah(alert.spentAmount)} / ${FormatUtils.formatRupiah(alert.targetAmount)})"
                                        )
                                    } else if (BudgetAlertNotifier.requiresRuntimePermission()) {
                                        pendingNotificationAlert = alert
                                        isExternalFlowActive = true
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.showMessage(
                                            "Peringatan anggaran: ${alert.categoryName} melebihi target (${FormatUtils.formatRupiah(alert.spentAmount)} / ${FormatUtils.formatRupiah(alert.targetAmount)})"
                                        )
                                    }
                                },
                                onRequestNotificationPermission = ::requestNotificationPermissionOrOpenSettings,
                                onLockAppNow = { isSessionUnlocked = false },
                                onExternalFlowActive = { active -> isExternalFlowActive = active },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainAuthenticatedApp(
    uiState: FinanceUiState,
    appUpdateState: com.example.ui.AppUpdateCheckState,
    viewModel: FinanceViewModel,
    biometricAvailability: BiometricAvailability,
    hasNotificationPermission: Boolean,
    onTriggerBudgetNotification: (CategoryBudgetAlert) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onLockAppNow: () -> Unit,
    onExternalFlowActive: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppTab.HOME.route
    val currentTab = AppTab.entries.firstOrNull { it.route == currentRoute } ?: AppTab.HOME

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.budgetAlerts.collect { alert ->
            onTriggerBudgetNotification(alert)
        }
    }

    LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    var txEditorOpen by rememberSaveable { mutableStateOf(false) }
    var txEditorInitialType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var inspectingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    var pocketEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingPocket by remember { mutableStateOf<PocketEntity?>(null) }
    var smartPocketOpen by rememberSaveable { mutableStateOf(false) }

    var walletEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var walletTransferOpen by rememberSaveable { mutableStateOf(false) }
    var currencyDialogOpen by rememberSaveable { mutableStateOf(false) }
    var receiptScannerOpen by rememberSaveable { mutableStateOf(false) }
    val receiptScanState by viewModel.receiptScanState.collectAsStateWithLifecycle()

    var allQuickActionsOpen by rememberSaveable { mutableStateOf(false) }
    var billEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }
    var billForcedStatus by remember { mutableStateOf<BillStatus?>(null) }

    var paydayEditorOpen by rememberSaveable { mutableStateOf(false) }

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(AppTab.HOME.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openBillsRoute() {
        navController.navigate("tagihan") {
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    BentoBottomBar(
                        currentTab = currentTab,
                        onSelectTab = ::navigateToTab,
                        onCenterRecordClick = {
                            editingTransaction = null
                            txEditorInitialType = TransactionType.EXPENSE
                            txEditorOpen = true
                        },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    BentoNavigationRail(
                        currentTab = currentTab,
                        onSelectTab = ::navigateToTab,
                        onCenterRecordClick = {
                            editingTransaction = null
                            txEditorInitialType = TransactionType.EXPENSE
                            txEditorOpen = true
                        },
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = AppTab.HOME.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(AppTab.HOME.route) {
                        HomeScreen(
                            uiState = uiState,
                            onIncomeClick = {
                                editingTransaction = null
                                txEditorInitialType = TransactionType.INCOME
                                txEditorOpen = true
                            },
                            onExpenseClick = {
                                editingTransaction = null
                                txEditorInitialType = TransactionType.EXPENSE
                                txEditorOpen = true
                            },
                            onTargetClick = {
                                editingPocket = null
                                pocketEditorOpen = true
                            },
                            onHistoryClick = { navigateToTab(AppTab.HISTORY) },
                            onManagePocketsClick = { navigateToTab(AppTab.POCKETS) },
                            onEditPaydayClick = { paydayEditorOpen = true },
                            onPocketClick = { pocket ->
                                editingPocket = pocket
                                pocketEditorOpen = true
                            },
                            onTransactionClick = { tx -> inspectingTransaction = tx },
                            onAccountClick = { navigateToTab(AppTab.ACCOUNT) },
                            onQuickToggleTheme = {
                                val nextMode = if (uiState.preferences.themeMode == ThemeMode.LIGHT) {
                                    ThemeMode.DARK
                                } else {
                                    ThemeMode.LIGHT
                                }
                                viewModel.setThemeMode(nextMode)
                            },
                            onCurrencyClick = { currencyDialogOpen = true },
                            onAddWalletClick = {
                                editingWallet = null
                                walletEditorOpen = true
                            },
                            onTransferWalletClick = { walletTransferOpen = true },
                            onWalletClick = { wallet ->
                                editingWallet = wallet
                                walletEditorOpen = true
                            },
                            onScanReceiptClick = {
                                viewModel.clearReceiptScanState()
                                receiptScannerOpen = true
                            },
                            onBillsClick = ::openBillsRoute,
                            onMoreActionsClick = { allQuickActionsOpen = true },
                            onToggleHideBalances = {
                                viewModel.setHideBalances(!uiState.preferences.hideBalances)
                            },
                        )
                    }

                    composable("tagihan") {
                        BillsScreen(
                            bills = uiState.bills,
                            currencyConfig = uiState.currencyConfig,
                            hideBalances = uiState.preferences.hideBalances,
                            onBack = { navController.popBackStack() },
                            onAddBillClick = {
                                editingBill = null
                                billForcedStatus = null
                                billEditorOpen = true
                            },
                            onEditBillClick = { bill, forcedStatus ->
                                editingBill = bill
                                billForcedStatus = forcedStatus
                                billEditorOpen = true
                            },
                            onQuickStatusChange = { billId, nextStatus ->
                                viewModel.updateBillStatus(billId, nextStatus)
                            },
                        )
                    }

                    composable(AppTab.POCKETS.route) {
                        PocketsScreen(
                            pockets = uiState.pockets,
                            currencyConfig = uiState.currencyConfig,
                            onAddPocketClick = {
                                editingPocket = null
                                pocketEditorOpen = true
                            },
                            onSmartPocketClick = { smartPocketOpen = true },
                            onPocketClick = { pocket ->
                                editingPocket = pocket
                                pocketEditorOpen = true
                            },
                        )
                    }

                    composable(AppTab.HISTORY.route) {
                        HistoryScreen(
                            transactions = uiState.transactions,
                            stats = uiState.stats,
                            currencyConfig = uiState.currencyConfig,
                            onTransactionClick = { tx -> inspectingTransaction = tx },
                            wallets = uiState.wallets,
                        )
                    }

                    composable(AppTab.ACCOUNT.route) {
                        AccountScreen(
                            uiState = uiState,
                            appUpdateState = appUpdateState,
                            onUpdateProfile = viewModel::updateProfile,
                            onOpenCurrencyPicker = { currencyDialogOpen = true },
                            onRefreshExchangeRates = { viewModel.refreshExchangeRates(silent = false) },
                            onSelectLanguage = viewModel::setAppLanguage,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onSelectThemeScheme = viewModel::setThemeScheme,
                            onToggleHideBalances = viewModel::setHideBalances,
                            onClearRatesCache = viewModel::clearExchangeRatesCache,
                            onExportCsv = { viewModel.exportTransactionsCsv(context) },
                            onCheckUpdate = viewModel::checkAppUpdate,
                            onToggleAutoSync = viewModel::setAutoCloudSync,
                            onBackupNow = viewModel::backupToCloudNow,
                            onRestoreNow = viewModel::restoreFromCloudNow,
                            onGoogleSignedOut = viewModel::onSignedOut,
                            biometricAvailability = biometricAvailability,
                            onToggleBiometricLock = viewModel::setBiometricLockEnabled,
                            onLockAppNow = onLockAppNow,
                            hasNotificationPermission = hasNotificationPermission,
                            onToggleBudgetAlerts = viewModel::setBudgetAlertsEnabled,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onExternalFlowActive = onExternalFlowActive,
                            onAuthFeedback = viewModel::showMessage,
                            onAuthError = viewModel::showError,
                        )
                    }
                }
            }
        }
    }

    if (txEditorOpen) {
        TransactionEditorSheet(
            initialType = txEditorInitialType,
            existingTransaction = editingTransaction,
            pockets = uiState.pockets,
            wallets = uiState.wallets,
            currencyConfig = uiState.currencyConfig,
            onDismiss = {
                txEditorOpen = false
                editingTransaction = null
            },
            onOpenReceiptScanner = {
                viewModel.clearReceiptScanState()
                receiptScannerOpen = true
            },
            onSave = viewModel::saveTransaction,
        )
    }

    if (receiptScannerOpen) {
        ReceiptScannerSheet(
            scanState = receiptScanState,
            wallets = uiState.wallets,
            pockets = uiState.pockets,
            currencyConfig = uiState.currencyConfig,
            onAnalyzeBitmap = viewModel::analyzeReceiptBitmap,
            onResetToCapture = viewModel::clearReceiptScanState,
            onSwitchToManualPreview = viewModel::switchFailedScanToManualPreview,
            onDismiss = {
                receiptScannerOpen = false
                viewModel.clearReceiptScanState()
            },
            onExternalFlowActive = onExternalFlowActive,
            onSaveTransaction = viewModel::saveTransaction,
            onSaveTransfer = viewModel::transferBetweenWallets,
            onShowError = viewModel::showError,
            onOpenManualTransaction = {
                receiptScannerOpen = false
                viewModel.clearReceiptScanState()
                editingTransaction = null
                txEditorInitialType = TransactionType.EXPENSE
                txEditorOpen = true
            },
        )
    }

    if (allQuickActionsOpen) {
        AllQuickActionsSheet(
            onDismiss = { allQuickActionsOpen = false },
            onIncomeClick = {
                editingTransaction = null
                txEditorInitialType = TransactionType.INCOME
                txEditorOpen = true
            },
            onExpenseClick = {
                editingTransaction = null
                txEditorInitialType = TransactionType.EXPENSE
                txEditorOpen = true
            },
            onScanReceiptClick = {
                viewModel.clearReceiptScanState()
                receiptScannerOpen = true
            },
            onBillsClick = ::openBillsRoute,
            onTargetClick = {
                editingPocket = null
                pocketEditorOpen = true
            },
            onSmartPocketClick = { smartPocketOpen = true },
            onHistoryClick = { navigateToTab(AppTab.HISTORY) },
            onTransferWalletClick = { walletTransferOpen = true },
            onAddWalletClick = {
                editingWallet = null
                walletEditorOpen = true
            },
            onCurrencyClick = { currencyDialogOpen = true },
        )
    }

    if (billEditorOpen) {
        BillEditorSheet(
            existingBill = editingBill,
            initialForcedStatus = billForcedStatus,
            currencyConfig = uiState.currencyConfig,
            onDismiss = {
                billEditorOpen = false
                editingBill = null
                billForcedStatus = null
            },
            onSave = viewModel::saveBill,
            onDelete = viewModel::deleteBill,
        )
    }

    inspectingTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            currencyConfig = uiState.currencyConfig,
            wallet = uiState.wallets.firstOrNull { it.id == tx.walletId },
            onDismiss = { inspectingTransaction = null },
            onEdit = {
                inspectingTransaction = null
                editingTransaction = tx
                txEditorInitialType = tx.txType
                txEditorOpen = true
            },
            onDelete = {
                inspectingTransaction = null
                viewModel.deleteTransaction(tx.id)
            },
        )
    }

    if (pocketEditorOpen) {
        PocketEditorSheet(
            existingPocket = editingPocket,
            currencyConfig = uiState.currencyConfig,
            onDismiss = {
                pocketEditorOpen = false
                editingPocket = null
            },
            onSave = viewModel::savePocket,
            onDelete = viewModel::deletePocket,
        )
    }

    if (smartPocketOpen) {
        SmartPocketSheet(
            existingPockets = uiState.pockets,
            currentMonthlyBudget = uiState.activeAccount?.monthlyBudget ?: 3_000_000L,
            currencyConfig = uiState.currencyConfig,
            onDismiss = { smartPocketOpen = false },
            onApply = viewModel::applySmartPocketAllocation,
        )
    }

    if (walletEditorOpen) {
        WalletEditorSheet(
            existingWallet = editingWallet,
            currencyConfig = uiState.currencyConfig,
            onDismiss = {
                walletEditorOpen = false
                editingWallet = null
            },
            onSave = viewModel::saveWallet,
            onDelete = viewModel::deleteWallet,
        )
    }

    if (walletTransferOpen) {
        WalletTransferSheet(
            wallets = uiState.wallets,
            currencyConfig = uiState.currencyConfig,
            onDismiss = { walletTransferOpen = false },
            onTransfer = viewModel::transferBetweenWallets,
        )
    }

    if (currencyDialogOpen) {
        CurrencySwitcherDialog(
            currentConfig = uiState.currencyConfig,
            exchangeRates = uiState.exchangeRates,
            isRefreshingRates = uiState.isRefreshingRates,
            onRefreshRates = { viewModel.refreshExchangeRates(silent = false) },
            onSelectCurrency = viewModel::selectCurrency,
            onDismiss = { currencyDialogOpen = false },
        )
    }

    if (paydayEditorOpen) {
        uiState.activeAccount?.let { acc ->
            PaydaySettingsSheet(
                currentName = acc.displayName,
                currentPaydayFrequency = acc.parsedPaydayFrequency,
                currentPaydayDay = acc.paydayDay,
                currentPaydayWeekday = acc.paydayWeekday,
                currentPaydayHour = acc.paydayHour,
                currentPaydayMinute = acc.paydayMinute,
                currentMonthlyBudget = acc.monthlyBudget,
                currencyConfig = uiState.currencyConfig,
                onDismiss = { paydayEditorOpen = false },
                onSave = viewModel::updateProfile,
            )
        }
    }
    }
}
