package com.example.data.model

import com.squareup.moshi.JsonClass

enum class SecurityType(val labelVi: String) {
    STOCK("Cổ phiếu"),
    WARRANT("Chứng quyền"),
}

enum class PriceTickDirection {
    UP,
    DOWN,
    NONE,
}

enum class PriceTone {
    CEILING,
    UP,
    REFERENCE,
    DOWN,
    FLOOR,
}

enum class ChartTimeframe(val label: String, val entradeResolution: String, val vndResolution: String, val lookbackDays: Long) {
    INTRADAY_1D("1D", "5", "5", 4L),
    WEEK_1W("1W", "30", "60", 10L),
    MONTH_1M("1M", "1D", "D", 35L),
    MONTH_3M("3M", "1D", "D", 95L),
    YEAR_1Y("1Y", "1D", "D", 365L),
}

enum class ChartStyle(val labelVi: String) {
    AREA("Đường giá"),
    CANDLE("Nến Nhật"),
}

data class TickerMetadata(
    val symbol: String,
    val companyName: String,
    val shortDescription: String,
    val exchange: String,
    val securityType: SecurityType,
    val sector: String,
    val underlyingSymbol: String? = null,
    val issuer: String? = null,
    val vietstockUrl: String,
)

data class OrderBookLevel(
    val price: Double,
    val volume: Long,
)

data class StockQuote(
    val symbol: String,
    val companyName: String,
    val shortDescription: String,
    val exchange: String,
    val securityType: SecurityType,
    val sector: String,
    val underlyingSymbol: String? = null,
    val issuer: String? = null,
    val vietstockUrl: String = "",
    val price: Double,
    val refPrice: Double,
    val ceilingPrice: Double,
    val floorPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val avgPrice: Double,
    val change: Double,
    val changePercent: Double,
    val lastVolume: Long,
    val totalVolume: Long,
    val foreignBuyVol: Long,
    val foreignSellVol: Long,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val sparkline: List<Double>,
    val lastUpdatedMillis: Long,
    val sourceLabel: String,
    val tickDirection: PriceTickDirection = PriceTickDirection.NONE,
) {
    val displayPrice: Double
        get() = if (price > 0.0) price else refPrice

    val priceTone: PriceTone
        get() {
            val p = displayPrice
            return when {
                ceilingPrice > 0.0 && p >= ceilingPrice - 0.001 -> PriceTone.CEILING
                floorPrice > 0.0 && p <= floorPrice + 0.001 -> PriceTone.FLOOR
                refPrice <= 0.0 -> PriceTone.REFERENCE
                p > refPrice + 0.001 -> PriceTone.UP
                p < refPrice - 0.001 -> PriceTone.DOWN
                else -> PriceTone.REFERENCE
            }
        }
}

data class OhlcCandle(
    val timestampSec: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

@JsonClass(generateAdapter = true)
data class PriceAlert(
    val symbol: String,
    val targetAbove: Double? = null,
    val targetBelow: Double? = null,
    val enabled: Boolean = true,
)

data class TriggeredAlertEvent(
    val symbol: String,
    val message: String,
    val currentPrice: Double,
    val timestampMillis: Long,
)

data class MarketSessionInfo(
    val labelVi: String,
    val isLiveTrading: Boolean,
    val vietnamTimeFormatted: String,
    val detailVi: String,
)

object KnownVietnamTickers {
    val DEFAULT_SYMBOLS = listOf("HHV", "KOS", "CHPG2625", "C4G")

    private val directory: Map<String, TickerMetadata> = mapOf(
        "HHV" to TickerMetadata(
            symbol = "HHV",
            companyName = "CTCP Đầu tư Hạ tầng Giao thông Đèo Cả",
            shortDescription = "Hạ tầng giao thông · Hầm đường bộ & Cao tốc",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Xây dựng & Hạ tầng",
            vietstockUrl = "https://finance.vietstock.vn/HHV-ctcp-dau-tu-ha-tang-giao-thong-deo-ca.htm",
        ),
        "KOS" to TickerMetadata(
            symbol = "KOS",
            companyName = "CTCP Kosy",
            shortDescription = "Bất động sản đô thị & Năng lượng tái tạo",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Bất động sản & Năng lượng",
            vietstockUrl = "https://finance.vietstock.vn/KOS-ctcp-kosy.htm",
        ),
        "CHPG2625" to TickerMetadata(
            symbol = "CHPG2625",
            companyName = "Chứng quyền HPG/ACBS/8M/102",
            shortDescription = "Chứng quyền mua trên cổ phiếu cơ sở HPG · Kỳ hạn 8 tháng",
            exchange = "HOSE",
            securityType = SecurityType.WARRANT,
            sector = "Chứng quyền có bảo đảm (CW)",
            underlyingSymbol = "HPG",
            issuer = "ACBS",
            vietstockUrl = "https://finance.vietstock.vn/chung-khoan-phai-sinh/CHPG2625/cw-tong-quan.htm",
        ),
        "C4G" to TickerMetadata(
            symbol = "C4G",
            companyName = "CTCP Tập đoàn CIENCO4",
            shortDescription = "Tổng công ty xây dựng công trình giao thông 4",
            exchange = "UPCOM",
            securityType = SecurityType.STOCK,
            sector = "Xây dựng & Hạ tầng",
            vietstockUrl = "https://finance.vietstock.vn/C4G-ctcp-tap-doan-cienco4.htm",
        ),
        "HPG" to TickerMetadata(
            symbol = "HPG",
            companyName = "Tập đoàn Hòa Phát",
            shortDescription = "Sản xuất thép & Nông nghiệp (Mã cơ sở của CHPG2625)",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Thép & Vật liệu",
            vietstockUrl = "https://finance.vietstock.vn/HPG-ctcp-tap-doan-hoa-phat.htm",
        ),
        "VCB" to TickerMetadata(
            symbol = "VCB",
            companyName = "Ngân hàng TMCP Ngoại thương Việt Nam",
            shortDescription = "Ngân hàng thương mại hàng đầu Việt Nam",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Ngân hàng",
            vietstockUrl = "https://finance.vietstock.vn/VCB-ngan-hang-tmcp-ngoai-thuong-viet-nam.htm",
        ),
        "FPT" to TickerMetadata(
            symbol = "FPT",
            companyName = "CTCP FPT",
            shortDescription = "Công nghệ thông tin, Viễn thông & Giáo dục",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Công nghệ",
            vietstockUrl = "https://finance.vietstock.vn/FPT-ctcp-fpt.htm",
        ),
        "SSI" to TickerMetadata(
            symbol = "SSI",
            companyName = "CTCP Chứng khoán SSI",
            shortDescription = "Dịch vụ chứng khoán & Ngân hàng đầu tư",
            exchange = "HOSE",
            securityType = SecurityType.STOCK,
            sector = "Chứng khoán",
            vietstockUrl = "https://finance.vietstock.vn/SSI-ctcp-chung-khoan-ssi.htm",
        ),
    )

    fun resolve(symbol: String): TickerMetadata {
        val clean = symbol.trim().uppercase()
        directory[clean]?.let { return it }
        val isWarrant = clean.length >= 6 && clean.startsWith("C") && clean.any { it.isDigit() }
        return if (isWarrant) {
            val underlying = clean.drop(1).takeWhile { it.isLetter() }.ifEmpty { "VN30" }
            TickerMetadata(
                symbol = clean,
                companyName = "Chứng quyền $clean ($underlying)",
                shortDescription = "Chứng quyền có bảo đảm trên mã cơ sở $underlying",
                exchange = "HOSE",
                securityType = SecurityType.WARRANT,
                sector = "Chứng quyền có bảo đảm",
                underlyingSymbol = underlying,
                vietstockUrl = "https://finance.vietstock.vn/chung-khoan-phai-sinh/$clean/cw-tong-quan.htm",
            )
        } else {
            TickerMetadata(
                symbol = clean,
                companyName = "Mã chứng khoán $clean",
                shortDescription = "Cổ phiếu niêm yết thị trường Việt Nam",
                exchange = "VN",
                securityType = SecurityType.STOCK,
                sector = "Thị trường Việt Nam",
                vietstockUrl = "https://finance.vietstock.vn/$clean.htm",
            )
        }
    }
}
