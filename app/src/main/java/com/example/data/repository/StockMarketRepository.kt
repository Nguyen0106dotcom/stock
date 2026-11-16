package com.example.data.repository

import com.example.data.model.ChartTimeframe
import com.example.data.model.KnownVietnamTickers
import com.example.data.model.MarketSessionInfo
import com.example.data.model.OhlcCandle
import com.example.data.model.OrderBookLevel
import com.example.data.model.PriceTickDirection
import com.example.data.model.StockQuote
import com.example.data.remote.EntradeChartApiService
import com.example.data.remote.MarketNetworkModule
import com.example.data.remote.VnDirectDchartApiService
import com.example.data.remote.VnDirectFinfoApiService
import com.example.data.remote.VpsApiService
import com.example.data.remote.VpsStockDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class StockMarketRepository(
    private val vpsApi: VpsApiService = MarketNetworkModule.vpsApi,
    private val entradeApi: EntradeChartApiService = MarketNetworkModule.entradeApi,
    private val vnDirectFinfoApi: VnDirectFinfoApiService = MarketNetworkModule.vnDirectFinfoApi,
    private val vnDirectDchartApi: VnDirectDchartApiService = MarketNetworkModule.vnDirectDchartApi,
) {
    private val sparklineCache = ConcurrentHashMap<String, Pair<Long, List<Double>>>()

    suspend fun fetchLiveQuotes(
        symbols: List<String>,
        previousQuotes: Map<String, StockQuote>,
    ): Result<List<StockQuote>> = coroutineScope {
        val cleanSymbols = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        if (cleanSymbols.isEmpty()) return@coroutineScope Result.success(emptyList())

        val now = System.currentTimeMillis()

        // Ensure sparklines are refreshed every ~60s per symbol without slowing down 3s/5s live quote polls
        val sparklinesDeferred = cleanSymbols.associateWith { sym ->
            async { getOrRefreshSparkline(sym, now) }
        }

        // 1) Primary Real-Time Price Board Feed: VPS getliststockdata
        val vpsResult = runCatching {
            vpsApi.getListStockData(cleanSymbols.joinToString(","))
        }

        val vpsBySym = vpsResult.getOrNull()
            ?.filter { !it.sym.isNullOrBlank() }
            ?.associateBy { it.sym!!.uppercase() }
            ?: emptyMap()

        // 2) For any symbol missing from VPS or having 0 price, fall back to VNDirect / Entrade
        val quotes = cleanSymbols.map { sym ->
            async {
                val sparkline = runCatching { sparklinesDeferred[sym]?.await() }.getOrNull().orEmpty()
                val prev = previousQuotes[sym]
                val vpsDto = vpsBySym[sym]
                val parsed = if (vpsDto != null && (vpsDto.lastPrice ?: 0.0) > 0.0 || (vpsDto?.r ?: 0.0) > 0.0) {
                    mapVpsDtoToQuote(vpsDto!!, sparkline, now, prev)
                } else {
                    fetchFallbackQuote(sym, sparkline, now, prev)
                }
                parsed
            }
        }.awaitAll().filterNotNull()

        if (quotes.isNotEmpty()) {
            Result.success(quotes)
        } else {
            val error = vpsResult.exceptionOrNull()
                ?: IllegalStateException("Không thể kết nối máy chủ dữ liệu thị trường Việt Nam.")
            Result.failure(error)
        }
    }

    suspend fun fetchOhlcChart(
        symbol: String,
        timeframe: ChartTimeframe,
    ): Result<List<OhlcCandle>> {
        val cleanSym = symbol.trim().uppercase()
        val nowSec = System.currentTimeMillis() / 1000L
        val fromSec = nowSec - timeframe.lookbackDays * 86_400L

        // Primary chart feed: DNSE / Entrade OHLCV API
        val entradeCandles = runCatching {
            val response = entradeApi.getStockOhlc(
                fromSec = fromSec,
                toSec = nowSec,
                symbol = cleanSym,
                resolution = timeframe.entradeResolution,
            )
            val timestamps = response.t.orEmpty()
            val opens = response.o.orEmpty()
            val highs = response.h.orEmpty()
            val lows = response.l.orEmpty()
            val closes = response.c.orEmpty()
            val vols = response.v.orEmpty()
            val count = listOf(timestamps.size, opens.size, highs.size, lows.size, closes.size).minOrNull() ?: 0
            (0 until count).map { i ->
                OhlcCandle(
                    timestampSec = timestamps[i],
                    open = opens[i],
                    high = highs[i],
                    low = lows[i],
                    close = closes[i],
                    volume = vols.getOrElse(i) { 0L },
                )
            }
        }.getOrNull().orEmpty()

        if (entradeCandles.isNotEmpty()) {
            val filtered = if (timeframe == ChartTimeframe.INTRADAY_1D) {
                keepLatestTradingDayCandles(entradeCandles)
            } else {
                entradeCandles
            }
            return Result.success(filtered)
        }

        // Secondary chart feed: VNDirect dchart API
        val vndResult = runCatching {
            val response = vnDirectDchartApi.getHistory(
                resolution = timeframe.vndResolution,
                symbol = cleanSym,
                fromSec = fromSec,
                toSec = nowSec,
            )
            val timestamps = response.t.orEmpty()
            val opens = response.o.orEmpty()
            val highs = response.h.orEmpty()
            val lows = response.l.orEmpty()
            val closes = response.c.orEmpty()
            val vols = response.v.orEmpty()
            val count = listOf(timestamps.size, opens.size, highs.size, lows.size, closes.size).minOrNull() ?: 0
            (0 until count).map { i ->
                OhlcCandle(
                    timestampSec = timestamps[i],
                    open = opens[i],
                    high = highs[i],
                    low = lows[i],
                    close = closes[i],
                    volume = vols.getOrElse(i) { 0L },
                )
            }
        }

        return vndResult.mapCatching { candles ->
            if (candles.isEmpty()) {
                throw IllegalStateException("Chưa có dữ liệu biểu đồ cho mã $cleanSym.")
            }
            if (timeframe == ChartTimeframe.INTRADAY_1D) {
                keepLatestTradingDayCandles(candles)
            } else {
                candles
            }
        }
    }

    private suspend fun getOrRefreshSparkline(symbol: String, nowMillis: Long): List<Double> {
        val cached = sparklineCache[symbol]
        if (cached != null && nowMillis - cached.first < 60_000L && cached.second.isNotEmpty()) {
            return cached.second
        }
        val nowSec = nowMillis / 1000L
        val fromSec = nowSec - 21L * 86_400L
        val points = runCatching {
            val resp = entradeApi.getStockOhlc(
                fromSec = fromSec,
                toSec = nowSec,
                symbol = symbol,
                resolution = "1D",
            )
            resp.c.orEmpty().takeLast(20)
        }.getOrNull().orEmpty().ifEmpty {
            runCatching {
                val resp = vnDirectDchartApi.getHistory(
                    resolution = "D",
                    symbol = symbol,
                    fromSec = fromSec,
                    toSec = nowSec,
                )
                resp.c.orEmpty().takeLast(20)
            }.getOrNull().orEmpty()
        }
        if (points.isNotEmpty()) {
            sparklineCache[symbol] = nowMillis to points
            return points
        }
        return cached?.second.orEmpty()
    }

    private fun mapVpsDtoToQuote(
        dto: VpsStockDto,
        sparkline: List<Double>,
        nowMillis: Long,
        prev: StockQuote?,
    ): StockQuote {
        val sym = dto.sym.orEmpty().uppercase()
        val meta = KnownVietnamTickers.resolve(sym)
        val ref = dto.r ?: 0.0
        val matched = dto.lastPrice ?: 0.0
        val effectivePrice = if (matched > 0.0) matched else ref

        val rawChange = dto.ot?.toDoubleOrNull()
            ?: if (effectivePrice > 0.0 && ref > 0.0) effectivePrice - ref else 0.0
        val signedChange = if (effectivePrice > 0.0 && ref > 0.0) {
            if (effectivePrice < ref && rawChange > 0.0) -rawChange
            else if (effectivePrice > ref && rawChange < 0.0) abs(rawChange)
            else effectivePrice - ref
        } else {
            0.0
        }
        val pct = if (ref > 0.0) (signedChange / ref) * 100.0 else 0.0

        val tickDirection = when {
            prev == null -> PriceTickDirection.NONE
            effectivePrice > prev.displayPrice + 0.0001 -> PriceTickDirection.UP
            effectivePrice < prev.displayPrice - 0.0001 -> PriceTickDirection.DOWN
            else -> PriceTickDirection.NONE
        }

        val bids = listOfNotNull(
            parseOrderLevel(dto.g1),
            parseOrderLevel(dto.g2),
            parseOrderLevel(dto.g3),
        )
        val asks = listOfNotNull(
            parseOrderLevel(dto.g4),
            parseOrderLevel(dto.g5),
            parseOrderLevel(dto.g6),
        )

        val exchangeLabel = when (dto.mc) {
            "10" -> "HOSE"
            "02" -> "HNX"
            "03" -> "UPCOM"
            else -> meta.exchange
        }

        return StockQuote(
            symbol = sym,
            companyName = meta.companyName,
            shortDescription = meta.shortDescription,
            exchange = exchangeLabel,
            securityType = meta.securityType,
            sector = meta.sector,
            underlyingSymbol = meta.underlyingSymbol,
            issuer = meta.issuer,
            vietstockUrl = meta.vietstockUrl,
            price = matched,
            refPrice = ref,
            ceilingPrice = dto.c ?: 0.0,
            floorPrice = dto.f ?: 0.0,
            openPrice = dto.openPrice ?: effectivePrice,
            highPrice = dto.highPrice ?: effectivePrice,
            lowPrice = dto.lowPrice ?: effectivePrice,
            avgPrice = dto.avePrice ?: effectivePrice,
            change = signedChange,
            changePercent = pct,
            lastVolume = (dto.lastVolume ?: 0L) * 10L,
            totalVolume = (dto.lot ?: 0L) * 10L,
            foreignBuyVol = dto.fBVol ?: 0L,
            foreignSellVol = dto.fSVolume ?: 0L,
            bids = bids,
            asks = asks,
            sparkline = if (effectivePrice > 0.0 && sparkline.isNotEmpty()) {
                sparkline.dropLast(1) + effectivePrice
            } else {
                sparkline
            },
            lastUpdatedMillis = nowMillis,
            sourceLabel = "VPS Live",
            tickDirection = tickDirection,
        )
    }

    private suspend fun fetchFallbackQuote(
        symbol: String,
        sparkline: List<Double>,
        nowMillis: Long,
        prev: StockQuote?,
    ): StockQuote? {
        val meta = KnownVietnamTickers.resolve(symbol)
        // Try VNDirect finfo-api first
        val finfoItem = runCatching {
            vnDirectFinfoApi.getStockPrices(
                sort = "date:desc",
                query = "code:$symbol",
                size = 2,
            ).data?.firstOrNull()
        }.getOrNull()

        if (finfoItem != null && (finfoItem.close ?: 0.0) > 0.0) {
            val close = finfoItem.close ?: 0.0
            val change = finfoItem.change ?: 0.0
            val ref = close - change
            val pct = finfoItem.pctChange ?: if (ref > 0.0) (change / ref) * 100.0 else 0.0
            val tickDirection = when {
                prev == null -> PriceTickDirection.NONE
                close > prev.displayPrice + 0.0001 -> PriceTickDirection.UP
                close < prev.displayPrice - 0.0001 -> PriceTickDirection.DOWN
                else -> PriceTickDirection.NONE
            }
            return StockQuote(
                symbol = symbol,
                companyName = meta.companyName,
                shortDescription = meta.shortDescription,
                exchange = meta.exchange,
                securityType = meta.securityType,
                sector = meta.sector,
                underlyingSymbol = meta.underlyingSymbol,
                issuer = meta.issuer,
                price = close,
                refPrice = ref,
                ceilingPrice = if (ref > 0.0) ref * 1.07 else 0.0,
                floorPrice = if (ref > 0.0) ref * 0.93 else 0.0,
                openPrice = finfoItem.open ?: close,
                highPrice = finfoItem.high ?: close,
                lowPrice = finfoItem.low ?: close,
                avgPrice = finfoItem.average ?: close,
                change = change,
                changePercent = pct,
                lastVolume = 0L,
                totalVolume = finfoItem.nmVolume ?: 0L,
                foreignBuyVol = 0L,
                foreignSellVol = 0L,
                bids = emptyList(),
                asks = emptyList(),
                sparkline = sparkline,
                lastUpdatedMillis = nowMillis,
                sourceLabel = "VNDirect",
                tickDirection = tickDirection,
            )
        }

        // Try Entrade latest daily/intraday candle
        val nowSec = nowMillis / 1000L
        val entradeCandle = runCatching {
            entradeApi.getStockOhlc(
                fromSec = nowSec - 10L * 86_400L,
                toSec = nowSec,
                symbol = symbol,
                resolution = "1D",
            )
        }.getOrNull()

        val closes = entradeCandle?.c.orEmpty()
        if (closes.isNotEmpty()) {
            val lastClose = closes.last()
            val prevClose = if (closes.size >= 2) closes[closes.size - 2] else lastClose
            val change = lastClose - prevClose
            val pct = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0
            return StockQuote(
                symbol = symbol,
                companyName = meta.companyName,
                shortDescription = meta.shortDescription,
                exchange = meta.exchange,
                securityType = meta.securityType,
                sector = meta.sector,
                underlyingSymbol = meta.underlyingSymbol,
                issuer = meta.issuer,
                price = lastClose,
                refPrice = prevClose,
                ceilingPrice = if (prevClose > 0.0) prevClose * 1.07 else 0.0,
                floorPrice = if (prevClose > 0.0) prevClose * 0.93 else 0.0,
                openPrice = entradeCandle?.o?.lastOrNull() ?: lastClose,
                highPrice = entradeCandle?.h?.lastOrNull() ?: lastClose,
                lowPrice = entradeCandle?.l?.lastOrNull() ?: lastClose,
                avgPrice = lastClose,
                change = change,
                changePercent = pct,
                lastVolume = 0L,
                totalVolume = entradeCandle?.v?.lastOrNull() ?: 0L,
                foreignBuyVol = 0L,
                foreignSellVol = 0L,
                bids = emptyList(),
                asks = emptyList(),
                sparkline = sparkline.ifEmpty { closes },
                lastUpdatedMillis = nowMillis,
                sourceLabel = "DNSE Feed",
                tickDirection = PriceTickDirection.NONE,
            )
        }

        return null
    }

    private fun parseOrderLevel(raw: String?): OrderBookLevel? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("|")
        if (parts.size < 2) return null
        val priceRaw = parts[0].trim()
        val price = priceRaw.toDoubleOrNull() ?: 0.0
        val vol = parts[1].trim().toLongOrNull() ?: 0L
        if (price <= 0.0 && vol <= 0L) return null
        return OrderBookLevel(price = price, volume = vol * 10L)
    }

    private fun keepLatestTradingDayCandles(candles: List<OhlcCandle>): List<OhlcCandle> {
        if (candles.isEmpty()) return emptyList()
        val vnTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = vnTz }
        val lastDay = dayFmt.format(Date(candles.last().timestampSec * 1000L))
        return candles.filter { dayFmt.format(Date(it.timestampSec * 1000L)) == lastDay }
            .ifEmpty { candles.takeLast(60) }
    }

    fun currentVietnamSessionInfo(nowMillis: Long = System.currentTimeMillis()): MarketSessionInfo {
        val vnTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val cal = Calendar.getInstance(vnTz).apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = vnTz }
        val vnTime = timeFmt.format(Date(nowMillis))

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return MarketSessionInfo(
                labelVi = "CUỐI TUẦN",
                isLiveTrading = false,
                vietnamTimeFormatted = vnTime,
                detailVi = "Thị trường nghỉ giao dịch (Hiển thị giá đóng cửa gần nhất)",
            )
        }

        return when {
            totalMinutes < 9 * 60 -> MarketSessionInfo(
                labelVi = "TRƯỚC GIỜ",
                isLiveTrading = false,
                vietnamTimeFormatted = vnTime,
                detailVi = "Chờ mở cửa phiên ATO lúc 09:00 (GMT+7)",
            )
            totalMinutes in (9 * 60) until (9 * 60 + 15) -> MarketSessionInfo(
                labelVi = "PHIÊN ATO",
                isLiveTrading = true,
                vietnamTimeFormatted = vnTime,
                detailVi = "Định kỳ mở cửa HOSE (09:00 - 09:15)",
            )
            totalMinutes in (9 * 60 + 15) until (11 * 60 + 30) -> MarketSessionInfo(
                labelVi = "ĐANG GIAO DỊCH",
                isLiveTrading = true,
                vietnamTimeFormatted = vnTime,
                detailVi = "Khớp lệnh liên tục phiên sáng (09:15 - 11:30)",
            )
            totalMinutes in (11 * 60 + 30) until (13 * 60) -> MarketSessionInfo(
                labelVi = "NGHỈ TRƯA",
                isLiveTrading = false,
                vietnamTimeFormatted = vnTime,
                detailVi = "Nghỉ giữa phiên (11:30 - 13:00)",
            )
            totalMinutes in (13 * 60) until (14 * 60 + 30) -> MarketSessionInfo(
                labelVi = "ĐANG GIAO DỊCH",
                isLiveTrading = true,
                vietnamTimeFormatted = vnTime,
                detailVi = "Khớp lệnh liên tục phiên chiều (13:00 - 14:30)",
            )
            totalMinutes in (14 * 60 + 30) until (14 * 60 + 45) -> MarketSessionInfo(
                labelVi = "PHIÊN ATC",
                isLiveTrading = true,
                vietnamTimeFormatted = vnTime,
                detailVi = "Định kỳ đóng cửa (14:30 - 14:45)",
            )
            totalMinutes in (14 * 60 + 45) until (15 * 60) -> MarketSessionInfo(
                labelVi = "THỎA THUẬN",
                isLiveTrading = false,
                vietnamTimeFormatted = vnTime,
                detailVi = "Giao dịch thỏa thuận sau giờ (14:45 - 15:00)",
            )
            else -> MarketSessionInfo(
                labelVi = "ĐÃ ĐÓNG CỬA",
                isLiveTrading = false,
                vietnamTimeFormatted = vnTime,
                detailVi = "Phiên giao dịch đã kết thúc · Giá đóng cửa gần nhất",
            )
        }
    }
}
