package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kantongku_prefs")

data class AppPreferencesState(
    val activeAccountId: String? = null,
    val themeScheme: ThemeScheme = ThemeScheme.BENTO_NEON,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val autoCloudSync: Boolean = true,
    val lastCloudBackupMillis: Long = 0L,
    val biometricLockEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val selectedCurrencyCode: String = "IDR",
    val appLanguage: String = "id",
    val hideBalances: Boolean = false,
    val cachedExchangeRatesJson: String = "",
    val exchangeRatesUpdatedAtMillis: Long = 0L,
    val pendingSyncUpserts: Set<String> = emptySet(),
    val pendingSyncDeletes: Set<String> = emptySet(),
) {
    val pendingSyncCount: Int
        get() = pendingSyncUpserts.size + pendingSyncDeletes.size
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        val THEME_SCHEME = stringPreferencesKey("theme_scheme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_CLOUD_SYNC = booleanPreferencesKey("auto_cloud_sync")
        val LAST_CLOUD_BACKUP = longPreferencesKey("last_cloud_backup_millis")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val SELECTED_CURRENCY_CODE = stringPreferencesKey("selected_currency_code")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val CACHED_RATES_JSON = stringPreferencesKey("cached_exchange_rates_json")
        val RATES_UPDATED_AT = longPreferencesKey("exchange_rates_updated_at_millis")
        val PENDING_SYNC_UPSERTS = stringSetPreferencesKey("pending_sync_upserts")
        val PENDING_SYNC_DELETES = stringSetPreferencesKey("pending_sync_deletes")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        AppPreferencesState(
            activeAccountId = prefs[Keys.ACTIVE_ACCOUNT_ID]?.takeIf { it.isNotBlank() },
            themeScheme = ThemeScheme.fromRaw(prefs[Keys.THEME_SCHEME]),
            themeMode = ThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            autoCloudSync = prefs[Keys.AUTO_CLOUD_SYNC] ?: true,
            lastCloudBackupMillis = prefs[Keys.LAST_CLOUD_BACKUP] ?: 0L,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: true,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            selectedCurrencyCode = prefs[Keys.SELECTED_CURRENCY_CODE]?.takeIf { it.isNotBlank() } ?: "IDR",
            appLanguage = prefs[Keys.APP_LANGUAGE]?.takeIf { it in setOf("zh", "id", "en", "ja", "de", "ko", "ru") } ?: "id",
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
            cachedExchangeRatesJson = prefs[Keys.CACHED_RATES_JSON] ?: "",
            exchangeRatesUpdatedAtMillis = prefs[Keys.RATES_UPDATED_AT] ?: 0L,
            pendingSyncUpserts = prefs[Keys.PENDING_SYNC_UPSERTS] ?: emptySet(),
            pendingSyncDeletes = prefs[Keys.PENDING_SYNC_DELETES] ?: emptySet(),
        )
    }

    suspend fun setActiveAccountId(accountId: String?) {
        context.dataStore.edit { prefs ->
            if (accountId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_ACCOUNT_ID)
            } else {
                prefs[Keys.ACTIVE_ACCOUNT_ID] = accountId
            }
        }
    }

    suspend fun setThemeScheme(scheme: ThemeScheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_SCHEME] = scheme.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAutoCloudSync(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CLOUD_SYNC] = enabled
        }
    }

    suspend fun setLastCloudBackupMillis(millis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_CLOUD_BACKUP] = millis
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setSelectedCurrencyCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_CURRENCY_CODE] = code.trim().uppercase().ifBlank { "IDR" }
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        val safe = languageCode.trim().lowercase().takeIf { it in setOf("zh", "id", "en", "ja", "de", "ko", "ru") } ?: "id"
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = safe
        }
    }

    suspend fun setHideBalances(hidden: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIDE_BALANCES] = hidden
        }
    }

    suspend fun saveExchangeRates(ratesJson: String, updatedAtMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_RATES_JSON] = ratesJson
            prefs[Keys.RATES_UPDATED_AT] = updatedAtMillis
        }
    }

    suspend fun enqueueSyncUpserts(vararg keys: String) {
        val validKeys = keys.filter { it.isNotBlank() }.toSet()
        if (validKeys.isEmpty()) return
        context.dataStore.edit { prefs ->
            val currentUpserts = prefs[Keys.PENDING_SYNC_UPSERTS] ?: emptySet()
            val currentDeletes = prefs[Keys.PENDING_SYNC_DELETES] ?: emptySet()
            prefs[Keys.PENDING_SYNC_UPSERTS] = currentUpserts + validKeys
            prefs[Keys.PENDING_SYNC_DELETES] = currentDeletes - validKeys
        }
    }

    suspend fun enqueueSyncDeletes(vararg keys: String) {
        val validKeys = keys.filter { it.isNotBlank() }.toSet()
        if (validKeys.isEmpty()) return
        context.dataStore.edit { prefs ->
            val currentUpserts = prefs[Keys.PENDING_SYNC_UPSERTS] ?: emptySet()
            val currentDeletes = prefs[Keys.PENDING_SYNC_DELETES] ?: emptySet()
            prefs[Keys.PENDING_SYNC_UPSERTS] = currentUpserts - validKeys
            prefs[Keys.PENDING_SYNC_DELETES] = currentDeletes + validKeys
        }
    }

    suspend fun markSyncKeysCompleted(
        upserted: Set<String> = emptySet(),
        deleted: Set<String> = emptySet(),
    ) {
        if (upserted.isEmpty() && deleted.isEmpty()) return
        context.dataStore.edit { prefs ->
            val currentUpserts = prefs[Keys.PENDING_SYNC_UPSERTS] ?: emptySet()
            val currentDeletes = prefs[Keys.PENDING_SYNC_DELETES] ?: emptySet()
            if (upserted.isNotEmpty()) {
                prefs[Keys.PENDING_SYNC_UPSERTS] = currentUpserts - upserted
            }
            if (deleted.isNotEmpty()) {
                prefs[Keys.PENDING_SYNC_DELETES] = currentDeletes - deleted
            }
        }
    }

    suspend fun clearPendingSync() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_SYNC_UPSERTS)
            prefs.remove(Keys.PENDING_SYNC_DELETES)
        }
    }
}
