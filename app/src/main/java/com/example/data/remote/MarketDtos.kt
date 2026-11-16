package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VpsStockDto(
    val id: Long? = null,
    val sym: String? = null,
    val mc: String? = null,
    val c: Double? = null,
    val f: Double? = null,
    val r: Double? = null,
    val lastPrice: Double? = null,
    val lastVolume: Long? = null,
    val lot: Long? = null,
    val ot: String? = null,
    val avePrice: Double? = null,
    val highPrice: Double? = null,
    val lowPrice: Double? = null,
    val openPrice: Double? = null,
    val fBVol: Long? = null,
    val fSVolume: Long? = null,
    val g1: String? = null,
    val g2: String? = null,
    val g3: String? = null,
    val g4: String? = null,
    val g5: String? = null,
    val g6: String? = null,
    val g7: String? = null,
)

@JsonClass(generateAdapter = true)
data class EntradeOhlcResponse(
    val t: List<Long>? = null,
    val o: List<Double>? = null,
    val h: List<Double>? = null,
    val l: List<Double>? = null,
    val c: List<Double>? = null,
    val v: List<Long>? = null,
)

@JsonClass(generateAdapter = true)
data class VnDirectDchartResponse(
    val s: String? = null,
    val t: List<Long>? = null,
    val o: List<Double>? = null,
    val h: List<Double>? = null,
    val l: List<Double>? = null,
    val c: List<Double>? = null,
    val v: List<Long>? = null,
)

@JsonClass(generateAdapter = true)
data class VnDirectFinfoResponse(
    val data: List<VnDirectFinfoItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class VnDirectFinfoItemDto(
    val code: String? = null,
    val date: String? = null,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double? = null,
    val average: Double? = null,
    val admittedPrice: Double? = null,
    val nmVolume: Long? = null,
    val change: Double? = null,
    val pctChange: Double? = null,
)
