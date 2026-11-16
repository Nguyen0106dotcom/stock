package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PriceAlert
import com.example.data.model.PriceTickDirection
import com.example.data.model.SecurityType
import com.example.data.model.StockQuote
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import com.example.ui.util.toColor
import kotlinx.coroutines.delay

@Composable
fun StockCard(
    quote: StockQuote,
    isSelected: Boolean,
    alert: PriceAlert?,
    onClick: () -> Unit,
    onAlertClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    val toneColor = quote.priceTone.toColor()

    var flashTick by remember { mutableStateOf(PriceTickDirection.NONE) }
    LaunchedEffect(quote.lastUpdatedMillis, quote.price) {
        if (quote.tickDirection != PriceTickDirection.NONE) {
            flashTick = quote.tickDirection
            delay(420L)
            flashTick = PriceTickDirection.NONE
        }
    }

    val flashBgColor by animateColorAsState(
        targetValue = when (flashTick) {
            PriceTickDirection.UP -> marketColors.up.copy(alpha = 0.24f)
            PriceTickDirection.DOWN -> marketColors.down.copy(alpha = 0.24f)
            PriceTickDirection.NONE -> toneColor.copy(alpha = 0.12f)
        },
        animationSpec = tween(durationMillis = 320),
        label = "price_flash",
    )

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        marketColors.border
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stock_card_${quote.symbol.lowercase()}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Symbol + Badges + Live Price Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = quote.symbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = quote.exchange,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (quote.securityType == SecurityType.WARRANT) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "CW · ${quote.underlyingSymbol ?: "HPG"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = quote.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sparkline
                SparklineCanvas(
                    points = quote.sparkline,
                    refPrice = quote.refPrice,
                    lineColor = toneColor,
                    modifier = Modifier
                        .width(68.dp)
                        .height(34.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Price + Change Column
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = flashBgColor,
                    ) {
                        Text(
                            text = MarketFormatters.formatBoardPrice(quote.displayPrice),
                            style = TabularNumberStyle.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = toneColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${MarketFormatters.formatSignedChange(quote.change)} (${MarketFormatters.formatSignedPercent(quote.changePercent)})",
                        style = TabularNumberStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = toneColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Intraday Limit Range Bar (Sàn -> Thấp -> Khớp -> Cao -> Trần)
            DayPriceRangeBar(
                floor = quote.floorPrice,
                ceiling = quote.ceilingPrice,
                ref = quote.refPrice,
                current = quote.displayPrice,
                toneColor = toneColor,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricChip(
                        label = "VND",
                        value = MarketFormatters.formatFullVnd(quote.displayPrice),
                    )
                    MetricChip(
                        label = "KL",
                        value = MarketFormatters.formatVolumeCompact(quote.totalVolume),
                    )
                    if (quote.refPrice > 0.0) {
                        MetricChip(
                            label = "TC",
                            value = MarketFormatters.formatBoardPrice(quote.refPrice),
                            valueColor = marketColors.reference,
                        )
                    }
                }

                IconButton(
                    onClick = onAlertClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("alert_button_${quote.symbol.lowercase()}"),
                ) {
                    val hasActiveAlert = alert != null && alert.enabled && (alert.targetAbove != null || alert.targetBelow != null)
                    Icon(
                        imageVector = if (hasActiveAlert) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.cd_alert),
                        tint = if (hasActiveAlert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = TabularNumberStyle.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = valueColor,
        )
    }
}

@Composable
private fun DayPriceRangeBar(
    floor: Double,
    ceiling: Double,
    ref: Double,
    current: Double,
    toneColor: Color,
) {
    val marketColors = LocalMarketColors.current
    if (floor <= 0.0 || ceiling <= floor) return

    val span = (ceiling - floor).coerceAtLeast(0.01)
    val currentFraction = ((current - floor) / span).toFloat().coerceIn(0f, 1f)
    val refFraction = ((ref - floor) / span).toFloat().coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Sàn ${MarketFormatters.formatBoardPrice(floor)}",
                style = MaterialTheme.typography.labelSmall,
                color = marketColors.floor,
            )
            Text(
                text = "Trần ${MarketFormatters.formatBoardPrice(ceiling)}",
                style = MaterialTheme.typography.labelSmall,
                color = marketColors.ceiling,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            // Reference marker
            Row(modifier = Modifier.fillMaxWidth()) {
                if (refFraction > 0.01f) {
                    Spacer(modifier = Modifier.weight(refFraction))
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(marketColors.reference),
                )
                if (1f - refFraction > 0.01f) {
                    Spacer(modifier = Modifier.weight(1f - refFraction))
                }
            }
            // Current price dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentFraction > 0.02f) {
                    Spacer(modifier = Modifier.weight(currentFraction))
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(toneColor),
                )
                if (1f - currentFraction > 0.02f) {
                    Spacer(modifier = Modifier.weight(1f - currentFraction))
                }
            }
        }
    }
}
