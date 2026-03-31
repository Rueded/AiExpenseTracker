package com.example.aiexpensetracker.network

import com.example.aiexpensetracker.data.GeminiRequest
import com.example.aiexpensetracker.data.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {
    // 使用 Gemini 1.5 Flash 模型 (速度快，省Token)
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}