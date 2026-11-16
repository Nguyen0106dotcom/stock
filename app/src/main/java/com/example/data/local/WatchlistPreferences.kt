package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.KnownVietnamTickers
import com.example.data.model.PriceAlert
import com.example.data.remote.MarketNetworkModule
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.marketDataStore: DataStore<Preferences> by preferencesDataStore(name = "vn_pulse_prefs")

class WatchlistPreferences(private val context: Context) {
    private object Keys {
        val WATCHLIST_SYMBOLS = stringPreferencesKey("watchlist_symbols")
        val REFRESH_SECONDS = intPreferencesKey("refresh_seconds")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val PRICE_ALERTS_JSON = stringPreferencesKey("price_alerts_json")
    }

    private val alertListType = Types.newParameterizedType(List::class.java, PriceAlert::class.java)
    private val alertAdapter = MarketNetworkModule.moshi.adapter<List<PriceAlert>>(alertListType)

    val watchlistFlow: Flow<List<String>> = context.marketDataStore.data.map { prefs ->
        val raw = prefs[Keys.WATCHLIST_SYMBOLS]
        if (raw.isNullOrBlank()) {
            KnownVietnamTickers.DEFAULT_SYMBOLS
        } else {
            raw.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
                .distinct()
                .ifEmpty { KnownVietnamTickers.DEFAULT_SYMBOLS }
        }
    }

    val refreshSecondsFlow: Flow<Int> = context.marketDataStore.data.map { prefs ->
        prefs[Keys.REFRESH_SECONDS] ?: 5
    }

    val darkThemeFlow: Flow<Boolean> = context.marketDataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val priceAlertsFlow: Flow<List<PriceAlert>> = context.marketDataStore.data.map { prefs ->
        val raw = prefs[Keys.PRICE_ALERTS_JSON]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { alertAdapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
        }
    }

    suspend fun addSymbol(symbol: String) {
        val clean = symbol.trim().uppercase()
        if (clean.isEmpty()) return
        context.marketDataStore.edit { prefs ->
            val current = parseSymbols(prefs[Keys.WATCHLIST_SYMBOLS])
            if (clean !in current) {
                prefs[Keys.WATCHLIST_SYMBOLS] = (current + clean).joinToString(",")
            }
        }
    }

    suspend fun removeSymbol(symbol: String) {
        val clean = symbol.trim().uppercase()
        context.marketDataStore.edit { prefs ->
            val current = parseSymbols(prefs[Keys.WATCHLIST_SYMBOLS])
            val updated = current.filterNot { it == clean }
            prefs[Keys.WATCHLIST_SYMBOLS] = updated.joinToString(",")
        }
    }

    suspend fun resetDefaultSymbols() {
        context.marketDataStore.edit { prefs ->
            prefs[Keys.WATCHLIST_SYMBOLS] = KnownVietnamTickers.DEFAULT_SYMBOLS.joinToString(",")
        }
    }

    suspend fun setRefreshSeconds(seconds: Int) {
        context.marketDataStore.edit { prefs ->
            prefs[Keys.REFRESH_SECONDS] = seconds
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.marketDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = isDark
        }
    }

    suspend fun upsertAlert(alert: PriceAlert) {
        context.marketDataStore.edit { prefs ->
            val current = runCatching {
                prefs[Keys.PRICE_ALERTS_JSON]?.let { alertAdapter.fromJson(it) }.orEmpty()
            }.getOrDefault(emptyList())
            val filtered = current.filterNot { it.symbol.equals(alert.symbol, ignoreCase = true) }
            val updated = if (alert.targetAbove == null && alert.targetBelow == null) {
                filtered
            } else {
                filtered + alert.copy(symbol = alert.symbol.uppercase())
            }
            prefs[Keys.PRICE_ALERTS_JSON] = alertAdapter.toJson(updated)
        }
    }

    suspend fun removeAlert(symbol: String) {
        context.marketDataStore.edit { prefs ->
            val current = runCatching {
                prefs[Keys.PRICE_ALERTS_JSON]?.let { alertAdapter.fromJson(it) }.orEmpty()
            }.getOrDefault(emptyList())
            val updated = current.filterNot { it.symbol.equals(symbol, ignoreCase = true) }
            prefs[Keys.PRICE_ALERTS_JSON] = alertAdapter.toJson(updated)
        }
    }

    private fun parseSymbols(raw: String?): List<String> {
        return if (raw.isNullOrBlank()) {
            KnownVietnamTickers.DEFAULT_SYMBOLS
        } else {
            raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        }
    }
}
