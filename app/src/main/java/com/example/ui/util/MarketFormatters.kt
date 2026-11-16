package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.PriceTone
import com.example.ui.theme.LocalMarketColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

object MarketFormatters {
    private val vnSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private val priceBoardFmt = DecimalFormat("#,##0.00", vnSymbols)
    private val intFmt = DecimalFormat("#,##0", vnSymbols)

    fun formatBoardPrice(price: Double): String {
        if (price <= 0.0) return "--"
        return priceBoardFmt.format(price)
    }

    fun formatFullVnd(price: Double): String {
        if (price <= 0.0) return "--"
        val vnd = (price * 1000.0).roundToLong()
        return "${intFmt.format(vnd)} ₫"
    }

    fun formatSignedChange(change: Double): String {
        val sign = if (change > 0.0001) "+" else if (change < -0.0001) "-" else ""
        return "$sign${priceBoardFmt.format(abs(change))}"
    }

    fun formatSignedPercent(pct: Double): String {
        val sign = if (pct > 0.0001) "+" else if (pct < -0.0001) "-" else ""
        return "$sign${String.format(Locale.US, "%.2f", abs(pct))}%"
    }

    fun formatVolumeCompact(volume: Long): String {
        if (volume <= 0L) return "0"
        return when {
            volume >= 1_000_000L -> String.format(Locale.US, "%.2fM", volume / 1_000_000.0)
            volume >= 1_000L -> String.format(Locale.US, "%.1fK", volume / 1_000.0)
            else -> intFmt.format(volume)
        }
    }

    fun formatVolumeExact(volume: Long): String {
        if (volume <= 0L) return "0"
        return intFmt.format(volume)
    }

    fun formatTimeHms(millis: Long): String {
        if (millis <= 0L) return "--:--:--"
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        return fmt.format(Date(millis))
    }

    fun formatCandleTimestamp(timestampSec: Long, isIntraday: Boolean): String {
        val pattern = if (isIntraday) "HH:mm dd/MM" else "dd/MM/yyyy"
        val fmt = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        return fmt.format(Date(timestampSec * 1000L))
    }
}

@Composable
fun PriceTone.toColor(): Color {
    val market = LocalMarketColors.current
    return when (this) {
        PriceTone.CEILING -> market.ceiling
        PriceTone.UP -> market.up
        PriceTone.REFERENCE -> market.reference
        PriceTone.DOWN -> market.down
        PriceTone.FLOOR -> market.floor
    }
}
