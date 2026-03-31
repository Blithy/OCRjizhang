package com.example.ocrjizhang.data.remote.service

import com.example.ocrjizhang.data.remote.response.BaiduTokenResponse
import com.example.ocrjizhang.data.remote.response.OcrReceiptResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface OcrService {

    @FormUrlEncoded
    @POST("oauth/2.0/token")
    suspend fun getAccessToken(
        @Query("grant_type") grantType: String = "client_credentials",
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
    ): BaiduTokenResponse

    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/receipt")
    suspend fun recognizeReceipt(
        @Query("access_token") accessToken: String,
        @Field("image") imageBase64: String,
    ): OcrReceiptResponse
}
