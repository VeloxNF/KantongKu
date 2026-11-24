package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.R
import com.example.data.auth.BiometricAuthHelper
import com.example.data.auth.BiometricAvailability
import com.example.data.auth.findFragmentActivity
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.currency.CurrencyDisplayConfig
import com.example.data.local.LocalAccountEntity
import com.example.data.local.PaydayFrequency
import com.example.ui.AppUpdateCheckState
import com.example.ui.FinanceUiState
import com.example.ui.components.BentoSurfaceCard
import com.example.ui.components.BudgetNotificationSettingsCard
import com.example.ui.components.CurrencyFlagBadge
import com.example.ui.components.PaydayScheduleSelector
import com.example.ui.components.SimpleRowsSkeleton
import com.example.ui.components.SkeletonBox
import com.example.ui.components.editableInputToIdr
import com.example.ui.components.idrToEditableInput
import com.example.ui.components.rememberCurrencyVisualTransformation
import com.example.ui.components.sanitizeCurrencyInput
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeScheme
import com.example.ui.theme.bentoColors
import com.example.util.FormatUtils
import kotlinx.coroutines.launch

private enum class AccountSection {
    HUB,
    PROFILE,
    LANGUAGE_CURRENCY,
    APPEARANCE,
    PRIVACY_SECURITY,
    CLOUD_SYNC,
    UPDATE_CHANGELOG,
}

private data class ChangelogEntry(
    val version: String,
    val dateLabel: String,
    val badge: String,
    val highlights: List<String>,
)

private val APP_CHANGELOG_ENTRIES = listOf(
    ChangelogEntry(
        version = "v1.4.0",
        dateLabel = "Terbaru",
        badge = "Release Aktif",
        highlights = listOf(
            "Beranda kini memiliki 2 slide Hero: Total Saldo Semua Dompet & Sisa Saldo Hari Ini.",
            "Catat Pemasukan & Pengeluaran mendukung pilih Tanggal, Jam, serta Nama Tempat khusus pengeluaran.",
            "Dompet Pintar dirombak: pilih sendiri banyak kategori + ikon tag, lalu buat rekomendasi alokasi otomatis.",
            "Pengaturan dipecah menjadi menu rapi, mode privasi saldo, ekspor CSV, cek pembaruan & 7 bahasa negara.",
            "Loading aplikasi diupgrade menjadi shimmer skeleton di seluruh halaman & scanner struk.",
        ),
    ),
    ChangelogEntry(
        version = "v1.3.0",
        dateLabel = "Rilis Sebelumnya",
        badge = "AI Scanner",
        highlights = listOf(
            "Scan Struk Otomatis via Kamera & Galeri dengan preview edit nominal/judul sebelum simpan.",
            "Masuk dengan Google + sinkronisasi Cloud Firestore otomatis saat internet tersedia.",
        ),
    ),
    ChangelogEntry(
        version = "v1.2.0",
        dateLabel = "Multidompet",
        badge = "Core",
        highlights = listOf(
            "Dukungan multidompet Tunai, E-Wallet, dan Bank Indonesia lengkap dengan logo & pindah saldo.",
            "Pengalih mata uang global dengan konversi kurs real-time dan bendera negara.",
        ),
    ),
    ChangelogEntry(
        version = "v1.0.0",
        dateLabel = "Rilis Awal",
        badge = "Foundation",
        highlights = listOf(
            "Bento UI Material 3, siklus gajian dinamis Harian/Mingguan/Bulanan, kantong target & biometrik.",
        ),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    uiState: FinanceUiState,
    appUpdateState: AppUpdateCheckState,
    onUpdateProfile: (
        displayName: String,
        paydayFrequency: PaydayFrequency,
        paydayDay: Int,
        paydayWeekday: Int,
        paydayHour: Int,
        paydayMinute: Int,
        monthlyBudget: Long,
    ) -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onRefreshExchangeRates: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectThemeScheme: (ThemeScheme) -> Unit,
    onToggleHideBalances: (Boolean) -> Unit,
    onClearRatesCache: () -> Unit,
    onExportCsv: suspend () -> Result<Uri>,
    onCheckUpdate: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreNow: () -> Unit,
    onGoogleSignedOut: () -> Unit,
    biometricAvailability: BiometricAvailability,
    onToggleBiometricLock: (Boolean) -> Unit,
    onLockAppNow: () -> Unit,
    hasNotificationPermission: Boolean,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onExternalFlowActive: (Boolean) -> Unit,
    onAuthFeedback: (String) -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = uiState.activeAccount ?: return
    val googleEmail = uiState.googleUser?.email?.takeIf { it.isNotBlank() } ?: account.username
    val bento = MaterialTheme.bentoColors
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeSection by rememberSaveable { mutableStateOf(AccountSection.HUB) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = activeSection != AccountSection.HUB) {
        activeSection = AccountSection.HUB
    }

    when (activeSection) {
        AccountSection.PROFILE -> {
            ProfileSubScreen(
                account = account,
                accountEmail = googleEmail,
                currencyConfig = uiState.currencyConfig,
                isBusy = uiState.isBusy,
                strings = strings,
                onBack = { activeSection = AccountSection.HUB },
                onSave = { name, freq, paydayDay, weekday, hour, minute, budget ->
                    onUpdateProfile(name, freq, paydayDay, weekday, hour, minute, budget)
                    activeSection = AccountSection.HUB
                },
                modifier = modifier,
            )
            return
        }

        AccountSection.LANGUAGE_CURRENCY -> {
            LanguageAndCurrencySubScreen(
                selectedLanguageCode = uiState.preferences.appLanguage,
                currencyConfig = uiState.currencyConfig,
                isRefreshingRates = uiState.isRefreshingRates,
                strings = strings,
                onSelectLanguage = onSelectLanguage,
                onOpenCurrencyPicker = onOpenCurrencyPicker,
                onRefreshExchangeRates = onRefreshExchangeRates,
                onBack = { activeSection = AccountSection.HUB },
                modifier = modifier,
            )
            return
        }

        AccountSection.APPEARANCE -> {
            AppearanceSubScreen(
                selectedMode = uiState.preferences.themeMode,
                selectedScheme = uiState.preferences.themeScheme,
                strings = strings,
                onSelectThemeMode = onSelectThemeMode,
                onSelectThemeScheme = onSelectThemeScheme,
                onBack = { activeSection = AccountSection.HUB },
                modifier = modifier,
            )
            return
        }

        AccountSection.PRIVACY_SECURITY -> {
            val verifyTitle = stringResource(R.string.biometric_verify_setting_title)
            val verifySubtitle = stringResource(R.string.biometric_verify_setting_subtitle)
            val verifyNegative = stringResource(R.string.biometric_prompt_negative)
            var isExportingCsv by remember { mutableStateOf(false) }

            PrivacyAndSecuritySubScreen(
                hideBalances = uiState.preferences.hideBalances,
                biometricLockEnabled = uiState.preferences.biometricLockEnabled,
                biometricAvailability = biometricAvailability,
                budgetAlertsEnabled = uiState.preferences.budgetAlertsEnabled,
                hasNotificationPermission = hasNotificationPermission,
                isExportingCsv = isExportingCsv,
                strings = strings,
                onToggleHideBalances = { hidden ->
                    onToggleHideBalances(hidden)
                    onAuthFeedback(
                        if (hidden) "Mode privasi saldo diaktifkan"
                        else "Saldo kembali ditampilkan"
                    )
                },
                onToggleBiometricLock = { desiredEnabled ->
                    val host = activity
                    if (host == null) {
                        onToggleBiometricLock(desiredEnabled)
                    } else {
                        BiometricAuthHelper.authenticate(
                            activity = host,
                            title = verifyTitle,
                            subtitle = verifySubtitle,
                            negativeButtonText = verifyNegative,
                            onPromptVisibilityChanged = onExternalFlowActive,
                            onSuccess = {
                                onToggleBiometricLock(desiredEnabled)
                                onAuthFeedback(
                                    if (desiredEnabled) "Kunci biometrik diaktifkan"
                                    else "Kunci biometrik dinonaktifkan"
                                )
                            },
                            onError = { _, msg ->
                                if (msg.isNotBlank()) onAuthError(msg)
                            },
                        )
                    }
                },
                onOpenEnrollSettings = {
                    try {
                        onExternalFlowActive(true)
                        context.startActivity(BiometricAuthHelper.createEnrollIntent())
                    } catch (_: Exception) {
                        onExternalFlowActive(false)
                        onAuthError("Tidak dapat membuka pengaturan keamanan perangkat.")
                    }
                },
                onLockNow = onLockAppNow,
                onToggleBudgetAlerts = { enabled ->
                    onToggleBudgetAlerts(enabled)
                    if (enabled && !hasNotificationPermission) {
                        onRequestNotificationPermission()
                    } else {
                        onAuthFeedback(
                            if (enabled) "Notifikasi batas kategori diaktifkan"
                            else "Notifikasi batas kategori dinonaktifkan"
                        )
                    }
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
                onExportCsvClick = {
                    if (!isExportingCsv) {
                        isExportingCsv = true
                        scope.launch {
                            val result = onExportCsv()
                            isExportingCsv = false
                            result.fold(
                                onSuccess = { uri ->
                                    try {
                                        onExternalFlowActive(true)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, "Riwayat Transaksi KantongKu (.csv)")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, strings.privacyExportCsvTitle)
                                        )
                                    } catch (_: Exception) {
                                        onExternalFlowActive(false)
                                        onAuthError("Tidak ditemukan aplikasi untuk membuka atau membagikan file CSV.")
                                    }
                                },
                                onFailure = { e ->
                                    onAuthError(e.localizedMessage ?: "Gagal mengekspor transaksi ke CSV.")
                                },
                            )
                        }
                    }
                },
                onClearRatesCache = onClearRatesCache,
                onBack = { activeSection = AccountSection.HUB },
                modifier = modifier,
            )
            return
        }

        AccountSection.CLOUD_SYNC -> {
            CloudSyncSubScreen(
                uiState = uiState,
                account = account,
                googleEmail = googleEmail,
                strings = strings,
                onToggleAutoSync = onToggleAutoSync,
                onBackupNow = onBackupNow,
                onRestoreNow = onRestoreNow,
                onSignOutRequest = { showSignOutConfirm = true },
                onBack = { activeSection = AccountSection.HUB },
                modifier = modifier,
            )
        }

        AccountSection.UPDATE_CHANGELOG -> {
            UpdateAndChangelogSubScreen(
                appUpdateState = appUpdateState,
                isOnline = uiState.isOnline,
                strings = strings,
                onCheckUpdate = onCheckUpdate,
                onBack = { activeSection = AccountSection.HUB },
                modifier = modifier,
            )
            return
        }

        AccountSection.HUB -> Unit
    }

    if (activeSection == AccountSection.HUB) {
        val currentLanguage = AppLanguage.fromCode(uiState.preferences.appLanguage)
        val syncSummary = when {
            uiState.isSyncingCloud -> "Menyinkronkan di latar belakang..."
            !uiState.isOnline -> "Offline • ${uiState.pendingSyncCount} tertunda"
            uiState.pendingSyncCount > 0 -> "Mengirim ${uiState.pendingSyncCount} perubahan"
            else -> "Online • Tersinkron"
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = strings.accountHeaderTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = strings.accountHeaderSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.mutedText,
                    )
                }

                // Compact User Profile Card -> opens PROFILE
                item {
                    BentoSurfaceCard(
                        onClick = { activeSection = AccountSection.PROFILE },
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier.testTag("account_profile_card"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bento.accentTile),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = account.displayName.firstOrNull()?.uppercase() ?: "K",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = bento.onAccentTile,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = googleEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bento.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = FormatUtils.formatPaydaySummary(account),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bento.highlightLabel,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = strings.menuProfileTitle,
                                tint = bento.mutedText,
                            )
                        }
                    }
                }

                // Menu 1: Language & Currency
                item {
                    AccountCategoryMenuRow(
                        title = strings.menuLangCurrencyTitle,
                        subtitle = "${currentLanguage.flagEmoji} ${currentLanguage.displayNameId}  •  ${uiState.currencyConfig.flagEmoji} ${uiState.currencyConfig.code}",
                        icon = Icons.Rounded.Language,
                        badgeText = uiState.currencyConfig.code,
                        testTag = "account_menu_language_currency",
                        onClick = { activeSection = AccountSection.LANGUAGE_CURRENCY },
                    )
                }

                // Menu 2: Appearance & Theme
                item {
                    AccountCategoryMenuRow(
                        title = strings.menuAppearanceTitle,
                        subtitle = "${uiState.preferences.themeScheme.displayName} • ${uiState.preferences.themeMode.displayName}",
                        icon = Icons.Rounded.Palette,
                        badgeText = null,
                        testTag = "account_menu_appearance",
                        onClick = { activeSection = AccountSection.APPEARANCE },
                    )
                }

                // Menu 3: Privacy, Security & Data
                item {
                    AccountCategoryMenuRow(
                        title = strings.menuPrivacySecurityTitle,
                        subtitle = strings.menuPrivacySecuritySubtitle,
                        icon = Icons.Rounded.Shield,
                        badgeText = if (uiState.preferences.hideBalances) "Privasi Aktif" else null,
                        testTag = "account_menu_privacy_security",
                        onClick = { activeSection = AccountSection.PRIVACY_SECURITY },
                    )
                }

                // Menu 4: Cloud Sync & Google
                item {
                    AccountCategoryMenuRow(
                        title = strings.menuCloudSyncTitle,
                        subtitle = syncSummary,
                        icon = Icons.Rounded.CloudSync,
                        badgeText = if (!uiState.isOnline) "Offline" else "Online",
                        badgeColor = if (!uiState.isOnline) bento.warningColor else bento.incomeColor,
                        testTag = "account_menu_cloud_sync",
                        onClick = { activeSection = AccountSection.CLOUD_SYNC },
                    )
                }

                // Menu 5: Check Update & Changelog
                item {
                    AccountCategoryMenuRow(
                        title = strings.menuUpdateChangelogTitle,
                        subtitle = strings.menuUpdateChangelogSubtitle,
                        icon = Icons.Rounded.SystemUpdate,
                        badgeText = "v${BuildConfig.VERSION_NAME}",
                        testTag = "account_menu_update_changelog",
                        onClick = { activeSection = AccountSection.UPDATE_CHANGELOG },
                    )
                }

                // Quick Sign Out CTA
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = bento.expenseColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = " ${strings.signOutButton}",
                            color = bento.expenseColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Keluar dari Akun Google?") },
            text = {
                val msg = buildString {
                    append("Untuk masuk kembali nanti, Anda akan memerlukan koneksi internet untuk Masuk dengan Google.")
                    if (uiState.pendingSyncCount > 0) {
                        append("\n\nPerhatian: Masih ada ${uiState.pendingSyncCount} perubahan offline yang belum tersinkron ke cloud.")
                    }
                }
                Text(msg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        GoogleAuthHelper.signOut(credentialManager, scope) {
                            onGoogleSignedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.expenseColor,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun AccountCategoryMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String?,
    testTag: String,
    onClick: () -> Unit,
    badgeColor: Color = MaterialTheme.bentoColors.highlightLabel,
) {
    val bento = MaterialTheme.bentoColors
    BentoSurfaceCard(
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bento.nestedCardSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = bento.highlightLabel,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = bento.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = bento.mutedText,
            )
        }
    }
}

@Composable
private fun SubScreenTopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    backTestTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(backTestTag),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backLabel,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LanguageAndCurrencySubScreen(
    selectedLanguageCode: String,
    currencyConfig: CurrencyDisplayConfig,
    isRefreshingRates: Boolean,
    strings: AppStrings,
    onSelectLanguage: (String) -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onRefreshExchangeRates: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val activeLang = AppLanguage.fromCode(selectedLanguageCode)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuLangCurrencyTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "language_currency_back_button",
            )

            // Alphabetical 7-Language Selector with Country Flags
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                    Column {
                        Text(
                            text = strings.languageSectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = strings.languageSectionDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = bento.mutedText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.alphabeticalList.forEach { lang ->
                        val isSelected = lang == activeLang
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("language_option_${lang.code}")
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectLanguage(lang.code) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) bento.accentTile.copy(alpha = 0.14f) else bento.nestedCardSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) bento.highlightLabel else bento.borderSubtle.copy(alpha = 0.45f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CurrencyFlagBadge(
                                    flagEmoji = lang.flagEmoji,
                                    code = lang.code.uppercase(),
                                    size = 36.dp,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.displayNameId,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = bento.mutedText,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Aktif",
                                        tint = bento.highlightLabel,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Global Currency & Live Exchange Rate Card
            BentoSurfaceCard(
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.testTag("account_currency_card"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CurrencyFlagBadge(
                        flagEmoji = currencyConfig.flagEmoji,
                        code = currencyConfig.code,
                        size = 40.dp,
                    )
                    Column {
                        Text(
                            text = "Mata Uang & Kurs Global",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${currencyConfig.flagEmoji} ${currencyConfig.code} • ${currencyConfig.nameId} (${currencyConfig.symbol})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = bento.highlightLabel,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (isRefreshingRates) {
                    SimpleRowsSkeleton(rowCount = 2)
                } else {
                    Text(
                        text = FormatUtils.formatExchangeRateLine(currencyConfig),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (currencyConfig.updatedAtMillis > 0L) {
                            "Kurs live: ${FormatUtils.formatDateTimeShort(currencyConfig.updatedAtMillis)} • Contoh: ${FormatUtils.formatMoney(1_500_000L, currencyConfig)}"
                        } else {
                            "Contoh format: ${FormatUtils.formatMoney(1_500_000L, currencyConfig)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onOpenCurrencyPicker,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bento.accentTile,
                            contentColor = bento.onAccentTile,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("account_currency_picker_button"),
                    ) {
                        Icon(Icons.Rounded.CurrencyExchange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Pilih Mata Uang", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRefreshExchangeRates,
                        enabled = !isRefreshingRates,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("account_refresh_rates_button"),
                    ) {
                        if (isRefreshingRates) {
                            SkeletonBox(modifier = Modifier.size(18.dp), cornerRadius = 9.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Perbarui Kurs", modifier = Modifier.size(18.dp))
                            Text(" Kurs")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSubScreen(
    selectedMode: ThemeMode,
    selectedScheme: ThemeScheme,
    strings: AppStrings,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectThemeScheme: (ThemeScheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuAppearanceTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "appearance_back_button",
            )

            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                    Text(
                        text = "Tema & Skema Warna",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mode Tampilan",
                    style = MaterialTheme.typography.labelLarge,
                    color = bento.mutedText,
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            label = { Text(mode.displayName) },
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Skema Warna Bento",
                    style = MaterialTheme.typography.labelLarge,
                    color = bento.mutedText,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeScheme.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { scheme ->
                                val selected = selectedScheme == scheme
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_scheme_${scheme.name.lowercase()}")
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onSelectThemeScheme(scheme) },
                                    shape = RoundedCornerShape(18.dp),
                                    color = bento.nestedCardSurface,
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) bento.highlightLabel else bento.borderSubtle,
                                    ),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(scheme.previewPrimary)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(scheme.previewAccent)
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Aktif",
                                                    tint = bento.highlightLabel,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = scheme.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = scheme.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bento.mutedText,
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAndSecuritySubScreen(
    hideBalances: Boolean,
    biometricLockEnabled: Boolean,
    biometricAvailability: BiometricAvailability,
    budgetAlertsEnabled: Boolean,
    hasNotificationPermission: Boolean,
    isExportingCsv: Boolean,
    strings: AppStrings,
    onToggleHideBalances: (Boolean) -> Unit,
    onToggleBiometricLock: (Boolean) -> Unit,
    onOpenEnrollSettings: () -> Unit,
    onLockNow: () -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onExportCsvClick: () -> Unit,
    onClearRatesCache: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuPrivacySecurityTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "privacy_security_back_button",
            )

            // Balance Privacy Switch
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bento.nestedCardSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = bento.highlightLabel,
                            )
                        }
                        Column {
                            Text(
                                text = strings.privacyHideBalancesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = strings.privacyHideBalancesDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = bento.mutedText,
                            )
                        }
                    }
                    Switch(
                        checked = hideBalances,
                        onCheckedChange = onToggleHideBalances,
                        modifier = Modifier.testTag("privacy_hide_balances_switch"),
                    )
                }
            }

            // Biometric Lock Card
            BiometricSecurityCard(
                biometricLockEnabled = biometricLockEnabled,
                availability = biometricAvailability,
                onToggleBiometricLock = onToggleBiometricLock,
                onOpenEnrollSettings = onOpenEnrollSettings,
                onLockNow = onLockNow,
            )

            // Category Budget Alert Notification Card
            BudgetNotificationSettingsCard(
                alertsEnabled = budgetAlertsEnabled,
                hasNotificationPermission = hasNotificationPermission,
                onToggleAlerts = onToggleBudgetAlerts,
                onRequestPermission = onRequestNotificationPermission,
            )

            // Export Data to CSV
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                    Text(
                        text = strings.privacyExportCsvTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.privacyExportCsvDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = bento.mutedText,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isExportingCsv) {
                    SimpleRowsSkeleton(rowCount = 2)
                } else {
                    Button(
                        onClick = onExportCsvClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bento.accentTile,
                            contentColor = bento.onAccentTile,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("privacy_export_csv_button"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" ${strings.privacyExportCsvButton}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Clear Exchange Rates Cache
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                    Text(
                        text = strings.privacyClearRatesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.privacyClearRatesDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = bento.mutedText,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClearRatesCache,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("privacy_clear_rates_button"),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${strings.privacyClearRatesButton}", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CloudSyncSubScreen(
    uiState: FinanceUiState,
    account: LocalAccountEntity,
    googleEmail: String,
    strings: AppStrings,
    onToggleAutoSync: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreNow: () -> Unit,
    onSignOutRequest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val gUser = uiState.googleUser
    val syncStatusColor = when {
        uiState.isSyncingCloud -> bento.highlightLabel
        !uiState.isOnline -> bento.warningColor
        uiState.pendingSyncCount > 0 -> bento.highlightLabel
        else -> bento.incomeColor
    }
    val syncStatusText = when {
        uiState.isSyncingCloud -> "Menyinkronkan data di latar belakang..."
        !uiState.isOnline && uiState.pendingSyncCount > 0 ->
            "Mode Offline • ${uiState.pendingSyncCount} perubahan menunggu internet"
        !uiState.isOnline ->
            "Mode Offline • Tetap berjalan normal tanpa internet"
        uiState.pendingSyncCount > 0 ->
            "Online • Mengirim ${uiState.pendingSyncCount} perubahan..."
        else -> "Online • Tersinkron otomatis"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuCloudSyncTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "cloud_sync_back_button",
            )

            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = "Akun Google & Sinkronisasi Cloud",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Data tetap berjalan lancar saat offline dan otomatis tersinkron ke cloud begitu internet aktif.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bento.mutedText,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = bento.nestedCardSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = gUser?.displayName?.takeIf { it.isNotBlank() } ?: account.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = googleEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.isSyncingCloud) {
                            SimpleRowsSkeleton(rowCount = 2)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = when {
                                        !uiState.isOnline -> Icons.Rounded.CloudOff
                                        uiState.pendingSyncCount > 0 -> Icons.Rounded.CloudSync
                                        else -> Icons.Rounded.CloudDone
                                    },
                                    contentDescription = null,
                                    tint = syncStatusColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = syncStatusText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = syncStatusColor,
                                )
                            }
                            if (uiState.preferences.lastCloudBackupMillis > 0L) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sinkron terakhir: ${FormatUtils.formatDateTimeShort(uiState.preferences.lastCloudBackupMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bento.mutedText,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Sinkron Otomatis Latar Belakang",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Sinkronkan langsung setiap ada perubahan & koneksi internet",
                            style = MaterialTheme.typography.labelSmall,
                            color = bento.mutedText,
                        )
                    }
                    Switch(
                        checked = uiState.preferences.autoCloudSync,
                        onCheckedChange = onToggleAutoSync,
                        modifier = Modifier.testTag("auto_cloud_sync_switch"),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onBackupNow,
                        enabled = uiState.isOnline && !uiState.isBusy && !uiState.isSyncingCloud,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bento.accentTile,
                            contentColor = bento.onAccentTile,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cloud_backup_now_button"),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Sinkronkan", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRestoreNow,
                        enabled = uiState.isOnline && !uiState.isBusy && !uiState.isSyncingCloud,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cloud_restore_now_button"),
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Pulihkan")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSignOutRequest,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cloud_sign_out_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = bento.expenseColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" ${strings.signOutButton}", color = bento.expenseColor)
                }
            }
        }
    }
}

@Composable
private fun UpdateAndChangelogSubScreen(
    appUpdateState: AppUpdateCheckState,
    isOnline: Boolean,
    strings: AppStrings,
    onCheckUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuUpdateChangelogTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "update_changelog_back_button",
            )

            // Version & Check Update Card
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bento.accentTile),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = bento.onAccentTile,
                            )
                        }
                        Column {
                            Text(
                                text = strings.updateCheckTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "KantongKu v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelMedium,
                                color = bento.mutedText,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                (if (isOnline) bento.incomeColor else bento.warningColor).copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) bento.incomeColor else bento.warningColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (appUpdateState) {
                    AppUpdateCheckState.Idle -> {
                        Text(
                            text = "Ketuk tombol di bawah untuk memeriksa apakah ada pembaruan aplikasi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }

                    AppUpdateCheckState.Checking -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.updateCheckingLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = bento.highlightLabel,
                                fontWeight = FontWeight.SemiBold,
                            )
                            SimpleRowsSkeleton(rowCount = 2)
                        }
                    }

                    is AppUpdateCheckState.UpToDate -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bento.incomeColor.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = bento.incomeColor,
                                )
                                Column {
                                    Text(
                                        text = strings.updateUpToDateLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = bento.incomeColor,
                                    )
                                    Text(
                                        text = "Dicek: ${FormatUtils.formatDateTimeShort(appUpdateState.checkedAtMillis)} • v${appUpdateState.versionName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bento.mutedText,
                                    )
                                }
                            }
                        }
                    }

                    is AppUpdateCheckState.Offline -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bento.warningColor.copy(alpha = 0.13f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = bento.warningColor,
                                )
                                Text(
                                    text = strings.updateOfflineLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCheckUpdate,
                    enabled = appUpdateState !is AppUpdateCheckState.Checking,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_update_button"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${strings.updateCheckButton}", fontWeight = FontWeight.Bold)
                }
            }

            // Changelog Timeline
            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NewReleases,
                        contentDescription = null,
                        tint = bento.highlightLabel,
                    )
                    Text(
                        text = strings.changelogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    APP_CHANGELOG_ENTRIES.forEachIndexed { idx, entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = bento.nestedCardSurface,
                            border = BorderStroke(
                                width = if (idx == 0) 1.5.dp else 1.dp,
                                color = if (idx == 0) bento.highlightLabel.copy(alpha = 0.65f) else bento.borderSubtle.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${entry.version} • ${entry.dateLabel}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(bento.cardSurface)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = entry.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (idx == 0) bento.highlightLabel else bento.mutedText,
                                        )
                                    }
                                }
                                entry.highlights.forEach { bullet ->
                                    Text(
                                        text = "• $bullet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = bento.mutedText,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileSubScreen(
    account: LocalAccountEntity,
    accountEmail: String,
    currencyConfig: CurrencyDisplayConfig,
    isBusy: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onSave: (
        displayName: String,
        paydayFrequency: PaydayFrequency,
        paydayDay: Int,
        paydayWeekday: Int,
        paydayHour: Int,
        paydayMinute: Int,
        monthlyBudget: Long,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    var displayName by remember(account.id) { mutableStateOf(account.displayName) }
    var paydayFrequency by remember(account.id) { mutableStateOf(account.parsedPaydayFrequency) }
    var paydayDay by remember(account.id) { mutableIntStateOf(account.paydayDay.coerceIn(1, 31)) }
    var paydayWeekday by remember(account.id) { mutableIntStateOf(account.paydayWeekday.coerceIn(1, 7)) }
    var paydayHour by remember(account.id) { mutableIntStateOf(account.paydayHour.coerceIn(0, 23)) }
    var paydayMinute by remember(account.id) { mutableIntStateOf(account.paydayMinute.coerceIn(0, 59)) }
    var budgetText by remember(account.id, currencyConfig.code) {
        mutableStateOf(idrToEditableInput(account.monthlyBudget, currencyConfig))
    }
    val currencyTransformation = rememberCurrencyVisualTransformation(currencyConfig)
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SubScreenTopBar(
                title = strings.menuProfileTitle,
                backLabel = strings.backLabel,
                onBack = onBack,
                backTestTag = "profile_back_button",
            )

            BentoSurfaceCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(bento.accentTile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = bento.onAccentTile,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Column {
                        Text(
                            text = accountEmail,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Ubah nama panggilan & preferensi siklus gajian",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it.take(60)
                        errorMsg = null
                    },
                    label = { Text("Nama Profil") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                )

                Spacer(modifier = Modifier.height(14.dp))

                PaydayScheduleSelector(
                    frequency = paydayFrequency,
                    onFrequencyChange = {
                        paydayFrequency = it
                        errorMsg = null
                    },
                    paydayDay = paydayDay,
                    onPaydayDayChange = {
                        paydayDay = it
                        errorMsg = null
                    },
                    paydayWeekday = paydayWeekday,
                    onPaydayWeekdayChange = {
                        paydayWeekday = it
                        errorMsg = null
                    },
                    paydayHour = paydayHour,
                    paydayMinute = paydayMinute,
                    onPaydayTimeChange = { h, m ->
                        paydayHour = h
                        paydayMinute = m
                        errorMsg = null
                    },
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = sanitizeCurrencyInput(it, currencyConfig)
                        errorMsg = null
                    },
                    label = { Text("Anggaran Bulanan (${currencyConfig.code})") },
                    prefix = { Text("${currencyConfig.symbol} ") },
                    visualTransformation = currencyTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (!currencyConfig.isBaseIdr && currencyConfig.decimalDigits > 0) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Number
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_budget_input"),
                )

                if (!errorMsg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bento.expenseColor,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val budget = editableInputToIdr(budgetText, currencyConfig)
                        when {
                            displayName.isBlank() -> errorMsg = "Nama profil tidak boleh kosong."
                            else -> onSave(
                                displayName.trim(),
                                paydayFrequency,
                                paydayDay,
                                paydayWeekday,
                                paydayHour,
                                paydayMinute,
                                budget,
                            )
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bento.accentTile,
                        contentColor = bento.onAccentTile,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_save_button"),
                ) {
                    Text(
                        text = "Simpan Perubahan Profil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
