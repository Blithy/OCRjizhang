package com.example.ocrjizhang.data.remote.service

import com.example.ocrjizhang.data.remote.request.LoginRequest
import com.example.ocrjizhang.data.remote.request.RegisterRequest
import com.example.ocrjizhang.data.remote.request.UpdateCurrentUserRequest
import com.example.ocrjizhang.data.remote.response.ApiResponse
import com.example.ocrjizhang.data.remote.response.AuthPayloadDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthPayloadDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthPayloadDto>

    @GET("user/me")
    suspend fun getCurrentUser(): ApiResponse<AuthPayloadDto>

    @PUT("user/me")
    suspend fun updateCurrentUser(
        @Body request: UpdateCurrentUserRequest,
    ): ApiResponse<AuthPayloadDto>
}
