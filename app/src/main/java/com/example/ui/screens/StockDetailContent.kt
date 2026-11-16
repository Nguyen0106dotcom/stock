package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChartStyle
import com.example.data.model.ChartTimeframe
import com.example.data.model.KnownVietnamTickers
import com.example.data.model.PriceAlert
import com.example.data.model.SecurityType
import com.example.data.model.StockQuote
import com.example.ui.components.InteractivePriceChart
import com.example.ui.components.OrderBookDepthCard
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.util.toColor
import com.example.ui.viewmodel.MarketUiState

@Composable
fun StockDetailContent(
    quote: StockQuote?,
    uiState: MarketUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectChartStyle: (ChartStyle) -> Unit,
    onRetryChart: () -> Unit,
    onOpenAlertDialog: (StockQuote) -> Unit,
    onRemoveCustomSymbol: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (quote == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Chọn một mã chứng khoán để xem chi tiết",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val marketColors = LocalMarketColors.current
    val toneColor = quote.priceTone.toColor()
    val uriHandler = LocalUriHandler.current
    val alert: PriceAlert? = uiState.priceAlerts.firstOrNull {
        it.symbol.equals(quote.symbol, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = quote.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = quote.exchange,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        if (quote.securityType == SecurityType.WARRANT) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "Chứng quyền · ${quote.underlyingSymbol ?: "HPG"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = quote.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = { onOpenAlertDialog(quote) },
                    modifier = Modifier.testTag("detail_alert_button"),
                ) {
                    val hasAlert = alert != null && alert.enabled
                    Icon(
                        imageVector = if (hasAlert) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.cd_alert),
                        tint = if (hasAlert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Hero Price & Limits Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = MarketFormatters.formatBoardPrice(quote.displayPrice),
                                style = TabularNumberStyle.copy(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = toneColor,
                            )
                            Text(
                                text = "${MarketFormatters.formatFullVnd(quote.displayPrice)} · Nguồn ${quote.sourceLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = toneColor.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = "${MarketFormatters.formatSignedChange(quote.change)} (${MarketFormatters.formatSignedPercent(quote.changePercent)})",
                                style = TabularNumberStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = toneColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Trần / Tham chiếu / Sàn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LimitStatBox(
                            label = "Giá Trần",
                            value = MarketFormatters.formatBoardPrice(quote.ceilingPrice),
                            color = marketColors.ceiling,
                            modifier = Modifier.weight(1f),
                        )
                        LimitStatBox(
                            label = "Tham chiếu",
                            value = MarketFormatters.formatBoardPrice(quote.refPrice),
                            color = marketColors.reference,
                            modifier = Modifier.weight(1f),
                        )
                        LimitStatBox(
                            label = "Giá Sàn",
                            value = MarketFormatters.formatBoardPrice(quote.floorPrice),
                            color = marketColors.floor,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Session Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatMiniItem("Cao nhất", MarketFormatters.formatBoardPrice(quote.highPrice), Modifier.weight(1f))
                        StatMiniItem("Thấp nhất", MarketFormatters.formatBoardPrice(quote.lowPrice), Modifier.weight(1f))
                        StatMiniItem("Trung bình", MarketFormatters.formatBoardPrice(quote.avgPrice), Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatMiniItem("Tổng KL", MarketFormatters.formatVolumeCompact(quote.totalVolume), Modifier.weight(1f))
                        StatMiniItem("NN Mua", MarketFormatters.formatVolumeCompact(quote.foreignBuyVol), Modifier.weight(1f))
                        StatMiniItem("NN Bán", MarketFormatters.formatVolumeCompact(quote.foreignSellVol), Modifier.weight(1f))
                    }
                }
            }
        }

        // Interactive Price & Volume Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Biểu đồ giá & khối lượng",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = uiState.chartStyle == ChartStyle.AREA,
                                onClick = { onSelectChartStyle(ChartStyle.AREA) },
                                label = {
                                    Icon(
                                        imageVector = Icons.Filled.ShowChart,
                                        contentDescription = "Đường giá",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                            FilterChip(
                                selected = uiState.chartStyle == ChartStyle.CANDLE,
                                onClick = { onSelectChartStyle(ChartStyle.CANDLE) },
                                label = {
                                    Icon(
                                        imageVector = Icons.Filled.CandlestickChart,
                                        contentDescription = "Nến Nhật",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timeframe tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChartTimeframe.entries.forEach { tf ->
                            FilterChip(
                                selected = uiState.selectedTimeframe == tf,
                                onClick = { onSelectTimeframe(tf) },
                                label = { Text(tf.label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("timeframe_${tf.label.lowercase()}"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when {
                        uiState.isChartLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        uiState.chartError != null && uiState.chartCandles.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = uiState.chartError,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = onRetryChart) {
                                        Text("Tải lại biểu đồ")
                                    }
                                }
                            }
                        }

                        uiState.chartCandles.isNotEmpty() -> {
                            InteractivePriceChart(
                                candles = uiState.chartCandles,
                                refPrice = quote.refPrice,
                                chartStyle = uiState.chartStyle,
                                isIntraday = uiState.selectedTimeframe == ChartTimeframe.INTRADAY_1D ||
                                    uiState.selectedTimeframe == ChartTimeframe.WEEK_1W,
                                lineColor = toneColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        // Order Book Depth Card
        item {
            OrderBookDepthCard(quote = quote)
        }

        // Company / Warrant Profile & Vietstock Reference Link
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (quote.securityType == SecurityType.WARRANT) "Thông tin chứng quyền" else "Hồ sơ doanh nghiệp",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quote.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nhóm ngành: ${quote.sector}" +
                            (quote.issuer?.let { " · Tổ chức phát hành: $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (quote.vietstockUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { uriHandler.openUri(quote.vietstockUrl) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_vietstock_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Xem trên Vietstock")
                            }
                        }

                        if (quote.symbol !in KnownVietnamTickers.DEFAULT_SYMBOLS) {
                            OutlinedButton(
                                onClick = {
                                    onRemoveCustomSymbol(quote.symbol)
                                    if (showBackButton) onBack()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_remove_ticker),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Xóa mã", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LimitStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TabularNumberStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = color,
            )
        }
    }
}

@Composable
private fun StatMiniItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TabularNumberStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
