package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChartStyle
import com.example.data.model.OhlcCandle
import com.example.ui.theme.LocalMarketColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.util.MarketFormatters
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun InteractivePriceChart(
    candles: List<OhlcCandle>,
    refPrice: Double,
    chartStyle: ChartStyle,
    isIntraday: Boolean,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val marketColors = LocalMarketColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val crosshairColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    var selectedIndex by remember(candles) { mutableStateOf<Int?>(null) }

    val inspectedCandle = selectedIndex?.let { idx ->
        candles.getOrNull(idx)
    } ?: candles.lastOrNull()

    val minLow = remember(candles, refPrice) {
        val candleMin = candles.minOfOrNull { it.low } ?: 0.0
        if (refPrice > 0.0 && candleMin > 0.0) min(candleMin, refPrice) else candleMin
    }
    val maxHigh = remember(candles, refPrice) {
        val candleMax = candles.maxOfOrNull { it.high } ?: 1.0
        if (refPrice > 0.0) max(candleMax, refPrice) else candleMax
    }

    Column(modifier = modifier) {
        if (inspectedCandle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = MarketFormatters.formatCandleTimestamp(inspectedCandle.timestampSec, isIntraday),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "M:${MarketFormatters.formatBoardPrice(inspectedCandle.open)}  " +
                        "C:${MarketFormatters.formatBoardPrice(inspectedCandle.high)}  " +
                        "T:${MarketFormatters.formatBoardPrice(inspectedCandle.low)}  " +
                        "Đ:${MarketFormatters.formatBoardPrice(inspectedCandle.close)}",
                    style = TabularNumberStyle.copy(
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onPress = { offset ->
                                if (candles.isNotEmpty() && size.width > 0) {
                                    val idx = ((offset.x / size.width) * (candles.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, candles.lastIndex)
                                    selectedIndex = idx
                                }
                                tryAwaitRelease()
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectHorizontalDragGestures(
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                if (candles.isNotEmpty() && size.width > 0) {
                                    val idx = ((change.position.x / size.width) * (candles.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, candles.lastIndex)
                                    selectedIndex = idx
                                }
                            },
                        )
                    },
            ) {
                if (candles.isEmpty()) return@Canvas

                val priceAreaHeight = size.height * 0.74f
                val volTop = size.height * 0.80f
                val volHeight = size.height - volTop
                val span = (maxHigh - minLow).takeIf { it > 0.0001 } ?: 1.0
                val padTop = 8.dp.toPx()
                val usablePriceH = (priceAreaHeight - padTop * 2f).coerceAtLeast(1f)

                fun yForPrice(price: Double): Float {
                    val norm = ((price - minLow) / span).toFloat().coerceIn(0f, 1f)
                    return padTop + usablePriceH * (1f - norm)
                }

                // Horizontal grid lines
                for (i in 0..3) {
                    val y = padTop + usablePriceH * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                // Reference price dashed line
                if (refPrice in minLow..maxHigh) {
                    val refY = yForPrice(refPrice)
                    drawLine(
                        color = marketColors.reference.copy(alpha = 0.65f),
                        start = Offset(0f, refY),
                        end = Offset(size.width, refY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    )
                }

                // Volume histogram
                val maxVol = (candles.maxOfOrNull { it.volume } ?: 1L).coerceAtLeast(1L)
                val barSlot = size.width / candles.size.coerceAtLeast(1)
                val barWidth = (barSlot * 0.62f).coerceIn(2f, 14.dp.toPx())

                candles.forEachIndexed { idx, candle ->
                    val centerX = if (candles.size == 1) size.width / 2f else idx * barSlot + barSlot / 2f
                    val volBarH = (candle.volume.toFloat() / maxVol.toFloat()) * volHeight
                    val isBull = candle.close >= candle.open
                    val barColor = if (isBull) {
                        marketColors.up.copy(alpha = 0.42f)
                    } else {
                        marketColors.down.copy(alpha = 0.42f)
                    }
                    drawRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, size.height - volBarH),
                        size = Size(barWidth, volBarH),
                    )
                }

                // Price series
                if (chartStyle == ChartStyle.AREA) {
                    val linePath = Path()
                    val fillPath = Path()
                    val stepX = if (candles.size <= 1) 0f else size.width / (candles.size - 1)

                    candles.forEachIndexed { idx, candle ->
                        val x = idx * stepX
                        val y = yForPrice(candle.close)
                        if (idx == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, priceAreaHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(size.width, priceAreaHeight)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.30f),
                                lineColor.copy(alpha = 0.0f),
                            ),
                            startY = 0f,
                            endY = priceAreaHeight,
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                } else {
                    candles.forEachIndexed { idx, candle ->
                        val centerX = if (candles.size == 1) size.width / 2f else idx * barSlot + barSlot / 2f
                        val isBull = candle.close >= candle.open
                        val candleColor = if (isBull) marketColors.up else marketColors.down
                        val highY = yForPrice(candle.high)
                        val lowY = yForPrice(candle.low)
                        val openY = yForPrice(candle.open)
                        val closeY = yForPrice(candle.close)
                        val bodyTop = min(openY, closeY)
                        val bodyHeight = max(3f, kotlin.math.abs(closeY - openY))

                        drawLine(
                            color = candleColor,
                            start = Offset(centerX, highY),
                            end = Offset(centerX, lowY),
                            strokeWidth = 2f,
                        )
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(centerX - barWidth / 2f, bodyTop),
                            size = Size(barWidth, bodyHeight),
                        )
                    }
                }

                // Crosshair when scrubbing
                selectedIndex?.let { idx ->
                    candles.getOrNull(idx)?.let { c ->
                        val x = if (chartStyle == ChartStyle.AREA) {
                            val stepX = if (candles.size <= 1) 0f else size.width / (candles.size - 1)
                            idx * stepX
                        } else {
                            idx * barSlot + barSlot / 2f
                        }
                        val y = yForPrice(c.close)
                        drawLine(
                            color = crosshairColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                        )
                        drawLine(
                            color = crosshairColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 7.dp.toPx(),
                            center = Offset(x, y),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
            }

            // Min/Max price labels inside chart edges
            Text(
                text = MarketFormatters.formatBoardPrice(maxHigh),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp),
            )
            Text(
                text = MarketFormatters.formatBoardPrice(minLow),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 44.dp, end = 4.dp),
            )
        }
    }
}

@Composable
fun NormalizedComparisonChart(
    seriesBySymbol: Map<String, List<OhlcCandle>>,
    palette: Map<String, Color>,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    // Normalize each series to % return from its first candle close
    val normalizedMap = remember(seriesBySymbol) {
        seriesBySymbol.mapValues { (_, candles) ->
            val base = candles.firstOrNull()?.close?.takeIf { it > 0.0 } ?: 1.0
            candles.map { ((it.close - base) / base) * 100.0 }
        }
    }

    val allPct = normalizedMap.values.flatten()
    val minPct = (allPct.minOrNull() ?: -5.0).coerceAtMost(0.0)
    val maxPct = (allPct.maxOrNull() ?: 5.0).coerceAtLeast(0.0)

    Box(modifier = modifier.height(220.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val span = (maxPct - minPct).takeIf { it > 0.1 } ?: 1.0
            val padY = 12.dp.toPx()
            val drawH = (size.height - padY * 2f).coerceAtLeast(1f)

            fun yFor(pct: Double): Float {
                val norm = ((pct - minPct) / span).toFloat().coerceIn(0f, 1f)
                return padY + drawH * (1f - norm)
            }

            for (i in 0..4) {
                val y = padY + drawH * (i / 4f)
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
            }

            // 0% baseline
            val zeroY = yFor(0.0)
            drawLine(
                color = Color.Gray.copy(alpha = 0.7f),
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
            )

            normalizedMap.forEach { (symbol, values) ->
                if (values.size < 2) return@forEach
                val color = palette[symbol] ?: Color.Cyan
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                val path = Path()
                values.forEachIndexed { idx, pct ->
                    val x = idx * stepX
                    val y = yFor(pct)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        Text(
            text = MarketFormatters.formatSignedPercent(maxPct),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = MarketFormatters.formatSignedPercent(minPct),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}
