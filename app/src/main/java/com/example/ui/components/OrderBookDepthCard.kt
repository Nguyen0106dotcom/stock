package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderBookLevel
import com.example.data.model.PriceTone
import com.example.data.model.StockQuote
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.util.toColor
import kotlin.math.max

@Composable
fun OrderBookDepthCard(
    quote: StockQuote,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    val maxLevelVol = max(
        quote.bids.maxOfOrNull { it.volume } ?: 1L,
        quote.asks.maxOfOrNull { it.volume } ?: 1L,
    ).coerceAtLeast(1L)

    val totalBidVol = quote.bids.sumOf { it.volume }
    val totalAskVol = quote.asks.sumOf { it.volume }
    val totalBookVol = (totalBidVol + totalAskVol).coerceAtLeast(1L)
    val bidRatio = (totalBidVol.toFloat() / totalBookVol.toFloat()).coerceIn(0.05f, 0.95f)

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "Sổ lệnh 3 bước giá",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Độ sâu thị trường",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bid vs Ask pressure bar
            if (totalBidVol > 0L || totalAskVol > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(bidRatio)
                            .fillMaxHeight()
                            .background(marketColors.up),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - bidRatio)
                            .fillMaxHeight()
                            .background(marketColors.down),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Dư mua ${(bidRatio * 100).toInt()}% (${MarketFormatters.formatVolumeCompact(totalBidVol)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = marketColors.up,
                    )
                    Text(
                        text = "Dư bán ${100 - (bidRatio * 100).toInt()}% (${MarketFormatters.formatVolumeCompact(totalAskVol)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = marketColors.down,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Column headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("KL Mua", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Giá Mua", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Giá Bán", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KL Bán", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (quote.bids.isEmpty() && quote.asks.isEmpty()) {
                Text(
                    text = "Chưa có dữ liệu đặt lệnh trực tiếp (ngoài phiên hoặc chưa mở cửa).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                for (i in 0 until 3) {
                    val bid = quote.bids.getOrNull(i)
                    val ask = quote.asks.getOrNull(i)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OrderBookSideCell(
                            level = bid,
                            maxVol = maxLevelVol,
                            refPrice = quote.refPrice,
                            ceiling = quote.ceilingPrice,
                            floor = quote.floorPrice,
                            isBidSide = true,
                            barColor = marketColors.up.copy(alpha = 0.18f),
                            modifier = Modifier.weight(1f),
                        )
                        OrderBookSideCell(
                            level = ask,
                            maxVol = maxLevelVol,
                            refPrice = quote.refPrice,
                            ceiling = quote.ceilingPrice,
                            floor = quote.floorPrice,
                            isBidSide = false,
                            barColor = marketColors.down.copy(alpha = 0.18f),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderBookSideCell(
    level: OrderBookLevel?,
    maxVol: Long,
    refPrice: Double,
    ceiling: Double,
    floor: Double,
    isBidSide: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val priceColor = if (level == null || level.price <= 0.0) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        toneForPrice(level.price, refPrice, ceiling, floor).toColor()
    }

    val fillFraction = if (level != null && level.volume > 0L) {
        (level.volume.toFloat() / maxVol.toFloat()).coerceIn(0.05f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        if (fillFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .align(if (isBidSide) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(barColor),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBidSide) {
                Text(
                    text = level?.let { MarketFormatters.formatVolumeCompact(it.volume) } ?: "--",
                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = level?.let { MarketFormatters.formatBoardPrice(it.price) } ?: "--",
                    style = TabularNumberStyle.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = priceColor,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = level?.let { MarketFormatters.formatBoardPrice(it.price) } ?: "--",
                    style = TabularNumberStyle.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = priceColor,
                )
                Text(
                    text = level?.let { MarketFormatters.formatVolumeCompact(it.volume) } ?: "--",
                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private fun toneForPrice(price: Double, ref: Double, ceiling: Double, floor: Double): PriceTone {
    return when {
        ceiling > 0.0 && price >= ceiling - 0.001 -> PriceTone.CEILING
        floor > 0.0 && price <= floor + 0.001 -> PriceTone.FLOOR
        ref <= 0.0 -> PriceTone.REFERENCE
        price > ref + 0.001 -> PriceTone.UP
        price < ref - 0.001 -> PriceTone.DOWN
        else -> PriceTone.REFERENCE
    }
}
