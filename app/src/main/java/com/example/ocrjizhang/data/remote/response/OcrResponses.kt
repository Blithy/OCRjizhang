package com.example.ocrjizhang.data.remote.response

import com.google.gson.annotations.SerializedName

data class BaiduTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Long,
)

data class OcrReceiptResponse(
    @SerializedName("words_result_num") val wordsResultNum: Int?,
    @SerializedName("words_result") val wordsResult: List<OcrWordResultDto>?,
)

data class OcrWordResultDto(
    @SerializedName("word_name") val wordName: String?,
    @SerializedName("words") val words: String?,
)
