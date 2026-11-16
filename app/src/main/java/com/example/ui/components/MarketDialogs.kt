package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PriceAlert
import com.example.ui.util.MarketFormatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTickerDialog(
    existingSymbols: List<String>,
    onDismiss: () -> Unit,
    onAddTicker: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val suggestions = listOf("HPG", "VCB", "FPT", "SSI", "VND", "TCB").filter { it !in existingSymbols }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm mã theo dõi", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nhập mã cổ phiếu hoặc chứng quyền Việt Nam (HOSE, HNX, UPCOM).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(12) },
                    label = { Text("Mã chứng khoán (VD: HPG)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_ticker_input"),
                )
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Gợi ý nhanh:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        suggestions.forEach { sym ->
                            AssistChip(
                                onClick = {
                                    onAddTicker(sym)
                                    onDismiss()
                                },
                                label = { Text(sym) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        onAddTicker(input)
                        onDismiss()
                    }
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_ticker_button"),
            ) {
                Text("Thêm mã")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}

@Composable
fun PriceAlertDialog(
    symbol: String,
    currentPrice: Double,
    existingAlert: PriceAlert?,
    onDismiss: () -> Unit,
    onSave: (targetAbove: Double?, targetBelow: Double?) -> Unit,
    onDelete: () -> Unit,
) {
    var aboveText by remember(existingAlert) {
        mutableStateOf(existingAlert?.targetAbove?.let { MarketFormatters.formatBoardPrice(it) } ?: "")
    }
    var belowText by remember(existingAlert) {
        mutableStateOf(existingAlert?.targetBelow?.let { MarketFormatters.formatBoardPrice(it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cảnh báo giá · $symbol",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Giá hiện tại: ${MarketFormatters.formatBoardPrice(currentPrice)} (${MarketFormatters.formatFullVnd(currentPrice)}). Nhập theo đơn vị nghìn đồng (VD: 12.50).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = aboveText,
                    onValueChange = { aboveText = it },
                    label = { Text("Báo khi giá >= (Chốt lời)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_above_input"),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = belowText,
                    onValueChange = { belowText = it },
                    label = { Text("Báo khi giá <= (Cắt lỗ / Mua)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_below_input"),
                )
                if (existingAlert != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                        ) {
                            Text("Xóa cảnh báo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val above = aboveText.trim().replace(",", ".").toDoubleOrNull()
                    val below = belowText.trim().replace(",", ".").toDoubleOrNull()
                    onSave(above, below)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_alert_button"),
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
    )
}
