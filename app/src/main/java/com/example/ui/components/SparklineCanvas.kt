package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SparklineCanvas(
    points: List<Double>,
    refPrice: Double,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) {
            val midY = size.height / 2f
            drawLine(
                color = lineColor.copy(alpha = 0.35f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 2f,
            )
            return@Canvas
        }

        val allValues = if (refPrice > 0.0) points + refPrice else points
        val minVal = allValues.minOrNull() ?: 0.0
        val maxVal = allValues.maxOrNull() ?: 1.0
        val span = (maxVal - minVal).takeIf { it > 0.0001 } ?: 1.0
        val padY = size.height * 0.12f
        val drawH = (size.height - padY * 2f).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val norm = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
            return padY + drawH * (1f - norm)
        }

        if (refPrice > 0.0) {
            val refY = yFor(refPrice)
            drawLine(
                color = lineColor.copy(alpha = 0.28f),
                start = Offset(0f, refY),
                end = Offset(size.width, refY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
            )
        }

        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { idx, value ->
            val x = idx * stepX
            val y = yFor(value)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.28f),
                    lineColor.copy(alpha = 0.01f),
                ),
            ),
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val lastY = yFor(points.last())
        drawCircle(
            color = lineColor,
            radius = 5f,
            center = Offset(size.width, lastY),
        )
    }
}
