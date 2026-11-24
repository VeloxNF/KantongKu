package com.example.data.currency

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class ExchangeRateResponse(
    @Json(name = "result") val result: String? = null,
    @Json(name = "base_code") val baseCode: String? = null,
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @Json(name = "rates") val rates: Map<String, Double>? = null,
)

interface ExchangeRateApiService {
    @GET("v6/latest/IDR")
    suspend fun getLatestRatesFromIdr(): ExchangeRateResponse
}

data class FetchedRatesSnapshot(
    val rates: Map<String, Double>,
    val updatedAtMillis: Long,
)

object ExchangeRateClient {

    private const val BASE_URL = "https://open.er-api.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val service: ExchangeRateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateApiService::class.java)
    }

    suspend fun fetchLiveRates(): Result<FetchedRatesSnapshot> = withContext(Dispatchers.IO) {
        try {
            val response = service.getLatestRatesFromIdr()
            val ratesMap = response.rates?.filterValues { it > 0.0 }.orEmpty()
            if (response.result == "success" && ratesMap.isNotEmpty()) {
                val merged = CurrencyCatalog.seedRatesPerIdr.toMutableMap().apply {
                    putAll(ratesMap)
                    put("IDR", 1.0)
                }
                val tsMillis = response.timeLastUpdateUnix?.let { it * 1000L } ?: System.currentTimeMillis()
                Result.success(
                    FetchedRatesSnapshot(
                        rates = merged,
                        updatedAtMillis = System.currentTimeMillis().coerceAtLeast(tsMillis),
                    )
                )
            } else {
                Result.failure(IllegalStateException("Respons kurs mata uang tidak lengkap."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun encodeRatesToJson(rates: Map<String, Double>): String {
        val obj = JSONObject()
        rates.forEach { (code, value) ->
            if (value > 0.0) obj.put(code, value)
        }
        return obj.toString()
    }

    fun decodeRatesFromJson(raw: String): Map<String, Double> {
        val base = CurrencyCatalog.seedRatesPerIdr.toMutableMap()
        if (raw.isBlank()) return base
        return try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val v = obj.optDouble(key, -1.0)
                if (v > 0.0) base[key.uppercase()] = v
            }
            base["IDR"] = 1.0
            base
        } catch (_: Exception) {
            base
        }
    }
}
