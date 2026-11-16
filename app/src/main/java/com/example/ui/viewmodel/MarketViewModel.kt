package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WatchlistPreferences
import com.example.data.model.ChartStyle
import com.example.data.model.ChartTimeframe
import com.example.data.model.KnownVietnamTickers
import com.example.data.model.MarketSessionInfo
import com.example.data.model.OhlcCandle
import com.example.data.model.PriceAlert
import com.example.data.model.SecurityType
import com.example.data.model.StockQuote
import com.example.data.model.TriggeredAlertEvent
import com.example.data.repository.StockMarketRepository
import com.example.ui.util.MarketFormatters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class WatchlistFilter(val labelVi: String) {
    ALL("Tất cả"),
    STOCKS("Cổ phiếu"),
    WARRANTS("Chứng quyền"),
}

data class MarketUiState(
    val symbols: List<String> = KnownVietnamTickers.DEFAULT_SYMBOLS,
    val quotes: List<StockQuote> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncedMillis: Long = 0L,
    val refreshIntervalSeconds: Int = 5,
    val isDarkTheme: Boolean = true,
    val sessionInfo: MarketSessionInfo,
    val filter: WatchlistFilter = WatchlistFilter.ALL,
    val selectedSymbol: String = KnownVietnamTickers.DEFAULT_SYMBOLS.first(),
    val selectedTimeframe: ChartTimeframe = ChartTimeframe.INTRADAY_1D,
    val chartStyle: ChartStyle = ChartStyle.AREA,
    val chartCandles: List<OhlcCandle> = emptyList(),
    val isChartLoading: Boolean = false,
    val chartError: String? = null,
    val comparisonSeries: Map<String, List<OhlcCandle>> = emptyMap(),
    val isComparisonLoading: Boolean = false,
    val priceAlerts: List<PriceAlert> = emptyList(),
    val activeAlertBanner: TriggeredAlertEvent? = null,
) {
    val filteredQuotes: List<StockQuote>
        get() = when (filter) {
            WatchlistFilter.ALL -> quotes
            WatchlistFilter.STOCKS -> quotes.filter { it.securityType == SecurityType.STOCK }
            WatchlistFilter.WARRANTS -> quotes.filter { it.securityType == SecurityType.WARRANT }
        }

    val selectedQuote: StockQuote?
        get() = quotes.firstOrNull { it.symbol == selectedSymbol } ?: quotes.firstOrNull()
}

class MarketViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = WatchlistPreferences(application.applicationContext)
    private val repository = StockMarketRepository()

    private val _uiState = MutableStateFlow(
        MarketUiState(sessionInfo = repository.currentVietnamSessionInfo())
    )
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var chartJob: Job? = null
    private var comparisonJob: Job? = null
    private val notifiedAlertKeys = mutableSetOf<String>()

    init {
        observePreferences()
        startVietnamClockTicker()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.watchlistFlow.collect { symbols ->
                val currentSelected = _uiState.value.selectedSymbol
                val nextSelected = if (currentSelected in symbols) currentSelected else symbols.firstOrNull() ?: "HHV"
                _uiState.update { it.copy(symbols = symbols, selectedSymbol = nextSelected) }
                restartPollingLoop()
                loadChart(_uiState.value.selectedSymbol, _uiState.value.selectedTimeframe)
            }
        }
        viewModelScope.launch {
            preferences.refreshSecondsFlow.collect { seconds ->
                _uiState.update { it.copy(refreshIntervalSeconds = seconds) }
                restartPollingLoop()
            }
        }
        viewModelScope.launch {
            preferences.darkThemeFlow.collect { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
        viewModelScope.launch {
            preferences.priceAlertsFlow.collect { alerts ->
                _uiState.update { it.copy(priceAlerts = alerts) }
            }
        }
    }

    private fun startVietnamClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(sessionInfo = repository.currentVietnamSessionInfo()) }
                delay(1000L)
            }
        }
    }

    private fun restartPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            refreshQuotesInternal(isManual = false)
            while (isActive) {
                val intervalSec = _uiState.value.refreshIntervalSeconds
                if (intervalSec <= 0) {
                    delay(1000L)
                    continue
                }
                delay(intervalSec * 1000L)
                refreshQuotesInternal(isManual = false)
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            refreshQuotesInternal(isManual = true)
            loadChart(_uiState.value.selectedSymbol, _uiState.value.selectedTimeframe)
        }
    }

    private suspend fun refreshQuotesInternal(isManual: Boolean) {
        val symbols = _uiState.value.symbols
        if (symbols.isEmpty()) return

        _uiState.update {
            it.copy(
                isRefreshing = true,
                errorMessage = if (isManual) null else it.errorMessage,
            )
        }

        val prevMap = _uiState.value.quotes.associateBy { it.symbol }
        val result = repository.fetchLiveQuotes(symbols, prevMap)

        result.onSuccess { newQuotes ->
            val now = System.currentTimeMillis()
            _uiState.update { state ->
                state.copy(
                    quotes = newQuotes,
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                    lastSyncedMillis = now,
                )
            }
            evaluateAlerts(newQuotes, now)
        }.onFailure { err ->
            _uiState.update { state ->
                state.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = err.localizedMessage ?: "Không thể tải dữ liệu trực tiếp",
                )
            }
        }
    }

    private fun evaluateAlerts(quotes: List<StockQuote>, nowMillis: Long) {
        val alerts = _uiState.value.priceAlerts.filter { it.enabled }
        if (alerts.isEmpty()) return

        for (alert in alerts) {
            val quote = quotes.firstOrNull { it.symbol.equals(alert.symbol, ignoreCase = true) } ?: continue
            val price = quote.displayPrice
            if (price <= 0.0) continue

            val above = alert.targetAbove
            if (above != null && price >= above) {
                val key = "${quote.symbol}_ABOVE_$above"
                if (notifiedAlertKeys.add(key)) {
                    _uiState.update {
                        it.copy(
                            activeAlertBanner = TriggeredAlertEvent(
                                symbol = quote.symbol,
                                message = "${quote.symbol} đã đạt ${MarketFormatters.formatBoardPrice(price)} (vượt ngưỡng ${MarketFormatters.formatBoardPrice(above)})",
                                currentPrice = price,
                                timestampMillis = nowMillis,
                            )
                        )
                    }
                }
            }

            val below = alert.targetBelow
            if (below != null && price <= below) {
                val key = "${quote.symbol}_BELOW_$below"
                if (notifiedAlertKeys.add(key)) {
                    _uiState.update {
                        it.copy(
                            activeAlertBanner = TriggeredAlertEvent(
                                symbol = quote.symbol,
                                message = "${quote.symbol} chạm ${MarketFormatters.formatBoardPrice(price)} (dưới ngưỡng ${MarketFormatters.formatBoardPrice(below)})",
                                currentPrice = price,
                                timestampMillis = nowMillis,
                            )
                        )
                    }
                }
            }
        }
    }

    fun dismissAlertBanner() {
        _uiState.update { it.copy(activeAlertBanner = null) }
    }

    fun selectSymbol(symbol: String) {
        val clean = symbol.trim().uppercase()
        if (_uiState.value.selectedSymbol != clean) {
            _uiState.update { it.copy(selectedSymbol = clean) }
            loadChart(clean, _uiState.value.selectedTimeframe)
        } else if (_uiState.value.chartCandles.isEmpty()) {
            loadChart(clean, _uiState.value.selectedTimeframe)
        }
    }

    fun selectTimeframe(timeframe: ChartTimeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadChart(_uiState.value.selectedSymbol, timeframe)
    }

    fun setChartStyle(style: ChartStyle) {
        _uiState.update { it.copy(chartStyle = style) }
    }

    fun setFilter(filter: WatchlistFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun loadChart(symbol: String = _uiState.value.selectedSymbol, timeframe: ChartTimeframe = _uiState.value.selectedTimeframe) {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            _uiState.update { it.copy(isChartLoading = true, chartError = null) }
            repository.fetchOhlcChart(symbol, timeframe)
                .onSuccess { candles ->
                    _uiState.update {
                        it.copy(
                            chartCandles = candles,
                            isChartLoading = false,
                            chartError = null,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isChartLoading = false,
                            chartError = err.localizedMessage ?: "Không thể tải biểu đồ",
                        )
                    }
                }
        }
    }

    fun loadComparisonCharts(timeframe: ChartTimeframe = ChartTimeframe.MONTH_1M) {
        comparisonJob?.cancel()
        comparisonJob = viewModelScope.launch {
            val symbols = _uiState.value.symbols
            _uiState.update { it.copy(isComparisonLoading = true) }
            val resultMap = mutableMapOf<String, List<OhlcCandle>>()
            for (sym in symbols) {
                repository.fetchOhlcChart(sym, timeframe).onSuccess { candles ->
                    if (candles.isNotEmpty()) {
                        resultMap[sym] = candles
                    }
                }
            }
            _uiState.update {
                it.copy(
                    comparisonSeries = resultMap,
                    isComparisonLoading = false,
                )
            }
        }
    }

    fun addSymbol(symbol: String) {
        viewModelScope.launch {
            preferences.addSymbol(symbol)
        }
    }

    fun removeSymbol(symbol: String) {
        viewModelScope.launch {
            preferences.removeSymbol(symbol)
        }
    }

    fun resetDefaultSymbols() {
        viewModelScope.launch {
            preferences.resetDefaultSymbols()
        }
    }

    fun setRefreshIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            preferences.setRefreshSeconds(seconds)
        }
    }

    fun toggleDarkTheme() {
        val next = !_uiState.value.isDarkTheme
        viewModelScope.launch {
            preferences.setDarkTheme(next)
        }
    }

    fun savePriceAlert(symbol: String, targetAbove: Double?, targetBelow: Double?, enabled: Boolean = true) {
        notifiedAlertKeys.removeAll { it.startsWith("${symbol.uppercase()}_") }
        viewModelScope.launch {
            preferences.upsertAlert(
                PriceAlert(
                    symbol = symbol.uppercase(),
                    targetAbove = targetAbove,
                    targetBelow = targetBelow,
                    enabled = enabled,
                )
            )
        }
    }

    fun removePriceAlert(symbol: String) {
        notifiedAlertKeys.removeAll { it.startsWith("${symbol.uppercase()}_") }
        viewModelScope.launch {
            preferences.removeAlert(symbol)
        }
    }
}
