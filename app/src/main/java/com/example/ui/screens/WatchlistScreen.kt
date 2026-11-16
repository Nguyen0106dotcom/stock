package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PriceTone
import com.example.data.model.StockQuote
import com.example.ui.components.StockCard
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.viewmodel.MarketUiState
import com.example.ui.viewmodel.WatchlistFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    uiState: MarketUiState,
    isSplitLayout: Boolean,
    onSelectTicker: (String) -> Unit,
    onManualRefresh: () -> Unit,
    onCycleRefreshInterval: () -> Unit,
    onToggleTheme: () -> Unit,
    onFilterChange: (WatchlistFilter) -> Unit,
    onOpenAddTicker: () -> Unit,
    onOpenAlertForQuote: (StockQuote) -> Unit,
    onDismissAlertBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Header Status Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "VN Pulse",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (uiState.sessionInfo.isLiveTrading) {
                                    marketColors.up.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .alpha(if (uiState.sessionInfo.isLiveTrading) pulseAlpha else 0.7f)
                                            .clip(CircleShape)
                                            .background(
                                                if (uiState.sessionInfo.isLiveTrading) marketColors.up
                                                else marketColors.reference
                                            ),
                                    )
                                    Text(
                                        text = uiState.sessionInfo.labelVi,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.sessionInfo.isLiveTrading) {
                                            marketColors.up
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "GMT+7 ${uiState.sessionInfo.vietnamTimeFormatted} · ${uiState.sessionInfo.detailVi}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button"),
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                        )
                    }

                    IconButton(
                        onClick = onManualRefresh,
                        modifier = Modifier.testTag("refresh_button"),
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Market Breadth + Polling Interval Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val upCount = uiState.quotes.count { it.priceTone == PriceTone.UP || it.priceTone == PriceTone.CEILING }
                    val refCount = uiState.quotes.count { it.priceTone == PriceTone.REFERENCE }
                    val downCount = uiState.quotes.count { it.priceTone == PriceTone.DOWN || it.priceTone == PriceTone.FLOOR }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BreadthBadge(label = "Tăng", count = upCount, color = marketColors.up)
                        BreadthBadge(label = "TC", count = refCount, color = marketColors.reference)
                        BreadthBadge(label = "Giảm", count = downCount, color = marketColors.down)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val intervalLabel = if (uiState.refreshIntervalSeconds <= 0) {
                            "Tạm dừng"
                        } else {
                            "Live ${uiState.refreshIntervalSeconds}s"
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, marketColors.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onCycleRefreshInterval)
                                .testTag("interval_cycle_chip"),
                        ) {
                            Text(
                                text = intervalLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onOpenAddTicker)
                                .testTag("add_ticker_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.cd_add_ticker),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Thêm mã",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Triggered Alert Banner
        AnimatedVisibility(visible = uiState.activeAlertBanner != null) {
            uiState.activeAlertBanner?.let { event ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = event.message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismissAlertBanner, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Đóng")
                        }
                    }
                }
            }
        }

        // Filter Chips + Sync Timestamp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WatchlistFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.labelVi, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                    )
                }
            }
            if (uiState.lastSyncedMillis > 0L) {
                val source = uiState.quotes.firstOrNull()?.sourceLabel ?: "Live"
                Text(
                    text = "$source · ${MarketFormatters.formatTimeHms(uiState.lastSyncedMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Error Banner (if offline/rate-limited)
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onManualRefresh) {
                        Text("Thử lại")
                    }
                }
            }
        }

        // Main List with Pull-to-Refresh
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing && !uiState.isInitialLoading,
            onRefresh = onManualRefresh,
            modifier = Modifier.weight(1f),
        ) {
            when {
                uiState.isInitialLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Đang kết nối bảng giá chứng khoán Việt Nam...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                uiState.filteredQuotes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Text(
                                text = "Không có mã phù hợp trong bộ lọc",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onManualRefresh) {
                                Text("Tải lại dữ liệu")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("watchlist_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.filteredQuotes,
                            key = { it.symbol },
                        ) { quote ->
                            val alert = uiState.priceAlerts.firstOrNull {
                                it.symbol.equals(quote.symbol, ignoreCase = true)
                            }
                            StockCard(
                                quote = quote,
                                isSelected = isSplitLayout && uiState.selectedSymbol == quote.symbol,
                                alert = alert,
                                onClick = { onSelectTicker(quote.symbol) },
                                onAlertClick = { onOpenAlertForQuote(quote) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadthBadge(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = TabularNumberStyle.copy(
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = color,
        )
    }
}
