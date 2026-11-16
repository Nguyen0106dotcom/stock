package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface VpsApiService {
    @GET("getliststockdata/{symbols}")
    suspend fun getListStockData(@Path("symbols") symbols: String): List<VpsStockDto>
}

interface EntradeChartApiService {
    @GET("v2/ohlcs/stock")
    suspend fun getStockOhlc(
        @Query("from") fromSec: Long,
        @Query("to") toSec: Long,
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
    ): EntradeOhlcResponse
}

interface VnDirectFinfoApiService {
    @GET("v4/stock_prices")
    suspend fun getStockPrices(
        @Query("sort") sort: String = "date:desc",
        @Query("q") query: String,
        @Query("size") size: Int = 5,
        @Query("page") page: Int = 1,
    ): VnDirectFinfoResponse
}

interface VnDirectDchartApiService {
    @GET("dchart/history")
    suspend fun getHistory(
        @Query("resolution") resolution: String,
        @Query("symbol") symbol: String,
        @Query("from") fromSec: Long,
        @Query("to") toSec: Long,
    ): VnDirectDchartResponse
}

object MarketNetworkModule {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val vpsApi: VpsApiService by lazy {
        buildRetrofit("https://bgapidatafeed.vps.com.vn/").create(VpsApiService::class.java)
    }

    val entradeApi: EntradeChartApiService by lazy {
        buildRetrofit("https://services.entrade.com.vn/chart-api/").create(EntradeChartApiService::class.java)
    }

    val vnDirectFinfoApi: VnDirectFinfoApiService by lazy {
        buildRetrofit("https://finfo-api.vndirect.com.vn/").create(VnDirectFinfoApiService::class.java)
    }

    val vnDirectDchartApi: VnDirectDchartApiService by lazy {
        buildRetrofit("https://dchart-api.vndirect.com.vn/").create(VnDirectDchartApiService::class.java)
    }
}
