package com.aegis.sfe.data.api

import com.aegis.sfe.UCOBankApplication
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ApiClientFactory {
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
        .setLenient()
        .create()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (android.util.Log.isLoggable("UCOBankApp", android.util.Log.DEBUG)) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(SignedRequestInterceptor())
        .addInterceptor(AuthErrorInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("${UCOBankApplication.BANK_API_BASE_URL}/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val bankApiService: BankApiService = retrofit.create(BankApiService::class.java)
}