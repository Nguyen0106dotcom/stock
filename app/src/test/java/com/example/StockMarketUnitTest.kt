package com.example

import com.example.data.model.KnownVietnamTickers
import com.example.data.model.SecurityType
import com.example.data.repository.StockMarketRepository
import com.example.ui.util.MarketFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StockMarketUnitTest {

    @Test
    fun defaultTickers_includeAllRequestedSymbolsAndVietstockLinks() {
        val defaults = KnownVietnamTickers.DEFAULT_SYMBOLS
        assertEquals(listOf("HHV", "KOS", "CHPG2625", "C4G"), defaults)

        val hhv = KnownVietnamTickers.resolve("HHV")
        assertEquals("HOSE", hhv.exchange)
        assertEquals(SecurityType.STOCK, hhv.securityType)
        assertTrue(hhv.vietstockUrl.contains("HHV-ctcp-dau-tu-ha-tang-giao-thong-deo-ca.htm"))

        val cw = KnownVietnamTickers.resolve("CHPG2625")
        assertEquals(SecurityType.WARRANT, cw.securityType)
        assertEquals("HPG", cw.underlyingSymbol)
        assertTrue(cw.vietstockUrl.contains("CHPG2625/cw-tong-quan.htm"))

        val c4g = KnownVietnamTickers.resolve("C4G")
        assertEquals("UPCOM", c4g.exchange)
        assertTrue(c4g.vietstockUrl.contains("C4G-ctcp-tap-doan-cienco4.htm"))

        val kos = KnownVietnamTickers.resolve("KOS")
        assertEquals("HOSE", kos.exchange)
        assertTrue(kos.vietstockUrl.contains("KOS-ctcp-kosy.htm"))
    }

    @Test
    fun marketFormatters_formatPricesAndVolumesCorrectly() {
        assertEquals("12.45", MarketFormatters.formatBoardPrice(12.45))
        assertEquals("12,450 ₫", MarketFormatters.formatFullVnd(12.45))
        assertEquals("+0.35", MarketFormatters.formatSignedChange(0.35))
        assertEquals("-1.20%", MarketFormatters.formatSignedPercent(-1.20))
        assertEquals("2.50M", MarketFormatters.formatVolumeCompact(2_500_000L))
    }

    @Test
    fun vietnamSessionInfo_detectsContinuousTradingHours() {
        val repo = StockMarketRepository()
        val vnTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        val cal = Calendar.getInstance(vnTz).apply {
            set(2026, Calendar.NOVEMBER, 16, 10, 0, 0) // Monday 10:00 ICT
        }
        val info = repo.currentVietnamSessionInfo(cal.timeInMillis)
        assertTrue(info.isLiveTrading)
        assertEquals("ĐANG GIAO DỊCH", info.labelVi)
    }
}
