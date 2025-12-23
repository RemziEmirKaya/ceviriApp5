package com.example.ceviriapp.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// DeepL API Free hesabı (:fx ile biter) için özel URL
private const val BASE_URL = "https://api-free.deepl.com/"

interface SmartcatService {
    @FormUrlEncoded
    @POST("v2/translate")
    suspend fun translate(
        @Header("Authorization") authHeader: String,
        @Field("text") text: String,
        @Field("target_lang") targetLang: String,
        @Field("source_lang") sourceLang: String? = null
    ): Response<DeepLResponse>
}

// DeepL Yanıt Modeli
data class DeepLResponse(
    @SerializedName("translations") val translations: List<Translation>
)

data class Translation(
    @SerializedName("detected_source_language") val detectedSourceLanguage: String,
    @SerializedName("text") val text: String
)

object SmartcatApi {
    val service: SmartcatService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartcatService::class.java)
    }
}
