package com.example.ocrjizhang.data.remote.service

import com.example.ocrjizhang.data.remote.response.ApiResponse
import com.example.ocrjizhang.data.remote.response.CategoryDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoryService {

    @GET("categories")
    suspend fun getCategories(): ApiResponse<List<CategoryDto>>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryDto): ApiResponse<CategoryDto>

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") categoryId: Long,
        @Body request: CategoryDto,
    ): ApiResponse<CategoryDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") categoryId: Long): ApiResponse<Unit>
}
