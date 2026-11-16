package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChartStyle
import com.example.data.model.ChartTimeframe
import com.example.ui.components.InteractivePriceChart
import com.example.ui.components.NormalizedComparisonChart
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.PulseBlue
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseGold
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.util.toColor
import com.example.ui.viewmodel.MarketUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartComparisonScreen(
    uiState: MarketUiState,
    onSelectSymbol: (String) -> Unit,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectChartStyle: (ChartStyle) -> Unit,
    onLoadComparison: (ChartTimeframe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    var comparisonTimeframe by remember { mutableStateOf(ChartTimeframe.MONTH_1M) }

    LaunchedEffect(uiState.symbols, comparisonTimeframe) {
        onLoadComparison(comparisonTimeframe)
    }

    val seriesPalette = remember(uiState.symbols) {
        val colors = listOf(
            PulseEmerald,
            PulseBlue,
            Color(0xFFE879F9),
            PulseGold,
            Color(0xFFF43F5E),
            Color(0xFF22D3EE),
        )
        uiState.symbols.mapIndexed { idx, sym ->
            sym to colors[idx % colors.size]
        }.toMap()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Biểu đồ & So sánh hiệu suất",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "So sánh tương quan % biến động giữa các mã theo thời gian thực",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Normalized Comparison Card
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
                            text = "So sánh % biến động",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(ChartTimeframe.WEEK_1W, ChartTimeframe.MONTH_1M, ChartTimeframe.MONTH_3M).forEach { tf ->
                                FilterChip(
                                    selected = comparisonTimeframe == tf,
                                    onClick = { comparisonTimeframe = tf },
                                    label = { Text(tf.label, style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend with cumulative % return
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        uiState.symbols.forEach { sym ->
                            val candles = uiState.comparisonSeries[sym].orEmpty()
                            val pctReturn = if (candles.size >= 2 && candles.first().close > 0.0) {
                                ((candles.last().close - candles.first().close) / candles.first().close) * 100.0
                            } else {
                                null
                            }
                            val color = seriesPalette[sym] ?: PulseEmerald
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = sym,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (pctReturn != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = MarketFormatters.formatSignedPercent(pctReturn),
                                        style = TabularNumberStyle.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = if (pctReturn >= 0.0) marketColors.up else marketColors.down,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.isComparisonLoading && uiState.comparisonSeries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.comparisonSeries.isNotEmpty()) {
                        NormalizedComparisonChart(
                            seriesBySymbol = uiState.comparisonSeries,
                            palette = seriesPalette,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Chưa có dữ liệu so sánh",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Single Ticker Deep Chart Selector
        item {
            val selectedQuote = uiState.selectedQuote
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biểu đồ chi tiết từng mã",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.symbols.forEach { sym ->
                            FilterChip(
                                selected = uiState.selectedSymbol == sym,
                                onClick = { onSelectSymbol(sym) },
                                label = { Text(sym, style = MaterialTheme.typography.labelLarge) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChartTimeframe.entries.forEach { tf ->
                                FilterChip(
                                    selected = uiState.selectedTimeframe == tf,
                                    onClick = { onSelectTimeframe(tf) },
                                    label = { Text(tf.label, style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChartStyle.entries.forEach { style ->
                            FilterChip(
                                selected = uiState.chartStyle == style,
                                onClick = { onSelectChartStyle(style) },
                                label = { Text(style.labelVi, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.isChartLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.chartCandles.isNotEmpty()) {
                        val toneColor = selectedQuote?.priceTone?.toColor() ?: PulseEmerald
                        InteractivePriceChart(
                            candles = uiState.chartCandles,
                            refPrice = selectedQuote?.refPrice ?: 0.0,
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

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
