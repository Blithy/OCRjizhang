package com.example.ocrjizhang.data.remote.service

import com.example.ocrjizhang.data.remote.response.ApiResponse
import com.example.ocrjizhang.data.remote.response.TransactionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionService {

    @GET("transactions")
    suspend fun getTransactions(
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null,
        @Query("type") type: String? = null,
    ): ApiResponse<List<TransactionDto>>

    @POST("transactions")
    suspend fun createTransaction(@Body request: TransactionDto): ApiResponse<TransactionDto>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") transactionId: Long,
        @Body request: TransactionDto,
    ): ApiResponse<TransactionDto>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") transactionId: Long): ApiResponse<Unit>
}
