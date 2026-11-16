package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.KnownVietnamTickers
import com.example.data.model.StockQuote
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.util.toColor
import com.example.ui.viewmodel.MarketUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertsAndSettingsScreen(
    uiState: MarketUiState,
    onSelectInterval: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAlertDialog: (StockQuote) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    val intervalOptions = listOf(
        3 to "3 giây",
        5 to "5 giây",
        10 to "10 giây",
        30 to "30 giây",
        0 to "Tạm dừng",
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Cảnh báo & Cài đặt",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Thiết lập ngưỡng giá mục tiêu và chu kỳ cập nhật thời gian thực",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Real-Time Polling Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tần suất cập nhật bảng giá",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        intervalOptions.forEach { (sec, label) ->
                            FilterChip(
                                selected = uiState.refreshIntervalSeconds == sec,
                                onClick = { onSelectInterval(sec) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("setting_interval_$sec"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Giao diện tối (Trading Terminal)",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Tối ưu độ tương phản cho bảng giá chứng khoán",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                        )
                    }
                }
            }
        }

        // Price Alerts List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cảnh báo giá theo mã",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.symbols != KnownVietnamTickers.DEFAULT_SYMBOLS) {
                    OutlinedButton(onClick = onResetDefaults) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.width(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Khôi phục 4 mã gốc")
                    }
                }
            }
        }

        items(
            items = uiState.quotes,
            key = { it.symbol },
        ) { quote ->
            val alert = uiState.priceAlerts.firstOrNull {
                it.symbol.equals(quote.symbol, ignoreCase = true)
            }
            val toneColor = quote.priceTone.toColor()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, marketColors.border),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = quote.symbol,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = MarketFormatters.formatBoardPrice(quote.displayPrice),
                                style = TabularNumberStyle.copy(
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = toneColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (alert != null && (alert.targetAbove != null || alert.targetBelow != null)) {
                            val parts = buildList {
                                alert.targetAbove?.let { add("Vượt >= ${MarketFormatters.formatBoardPrice(it)}") }
                                alert.targetBelow?.let { add("Dưới <= ${MarketFormatters.formatBoardPrice(it)}") }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.width(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = parts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        } else {
                            Text(
                                text = "Chưa đặt ngưỡng cảnh báo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = { onOpenAlertDialog(quote) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Chỉnh sửa cảnh báo ${quote.symbol}",
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
