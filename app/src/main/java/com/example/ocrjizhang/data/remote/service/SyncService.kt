package com.example.ocrjizhang.data.remote.service

import com.example.ocrjizhang.data.remote.request.SyncPullRequest
import com.example.ocrjizhang.data.remote.request.SyncPushRequest
import com.example.ocrjizhang.data.remote.response.ApiResponse
import com.example.ocrjizhang.data.remote.response.SyncPullPayloadDto
import retrofit2.http.Body
import retrofit2.http.POST

interface SyncService {

    @POST("sync/push")
    suspend fun pushChanges(@Body request: SyncPushRequest): ApiResponse<Unit>

    @POST("sync/pull")
    suspend fun pullChanges(@Body request: SyncPullRequest): ApiResponse<SyncPullPayloadDto>
}
