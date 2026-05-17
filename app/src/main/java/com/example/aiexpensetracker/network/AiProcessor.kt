package com.example.aiexpensetracker.network

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AiProcessor {
    private const val TAG = "AiProcessor"

    // 🔴 请替换为你自己的 API Key
    private const val API_KEY = "AIzaSyC_5D2qJi2oMbslQ-G7MtAM4L6oz085nJ0"

    // 🟢 使用 gemini-1.5-flash，速度快且稳定，适合实时记账
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = API_KEY
    )

    // 🟢 1. 交易分析 (用于 NotificationListener)
    suspend fun analyze(text: String): TransactionResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 针对马来西亚本地化的 Prompt
                val prompt = """
                Role: Strict Financial Analyzer for Malaysia context.
                Task: Extract transaction data from notification text.
                Input: "$text"

                Rules:
                1. 🛡️ FILTER: If text is ads, promo, OTP, TAC, "Log in", "You won" -> Return "valid": false.
                2. 🧹 CLEAN: Remove "DuitNow to", "Payment to". "Grab*1234" -> "Grab".
                3. 💰 TYPE: 
                   - "Sent", "Paid", "Debit", "Purchase" -> "EXPENSE"
                   - "Received", "Credit", "Inward Transfer" -> "INCOME"
                
                Output JSON only:
                {
                   "valid": boolean,
                   "amount": number (no currency symbol),
                   "merchant": string,
                   "category": string (Food, Transport, Shopping, Entertainment, Medical, Other),
                   "type": "EXPENSE" or "INCOME"
                }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                // 清洗 Markdown 标记 (```json ... ```)
                val rawText = response.text?.replace("```json", "")?.replace("```", "")?.trim()

                if (rawText != null) {
                    val json = JSONObject(rawText)
                    if (json.optBoolean("valid", false)) {
                        TransactionResult(
                            valid = true,
                            amount = json.optDouble("amount", 0.0),
                            merchant = json.optString("merchant", "Unknown"),
                            category = json.optString("category", "Other"),
                            type = json.optString("type", "EXPENSE")
                        )
                    } else null
                } else null

            } catch (e: Exception) {
                Log.e(TAG, "Analysis Failed: ${e.message}")
                null
            }
        }
    }

    // 🟢 2. 生成月度报表 (支持双语)
    suspend fun generateInsight(
        totalSpent: Double,
        budget: Double,
        topCategory: String,
        topCategoryAmount: Double,
        persona: String,
        language: String // "zh" (Chinese) or "en" (English)
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // 动态语言指令
                val langInstruction = if (language == "zh") {
                    "IMPORTANT: You MUST reply in simplified Chinese (简体中文)."
                } else {
                    "Reply in English."
                }

                // 人设指令
                val personaPrompt = when (persona) {
                    "STRICT" -> "Role: Toxic financial coach. Roast the user hard for their spending. Be sarcastic."
                    "GENTLE" -> "Role: Kind, supportive mom. Encourage the user gently and praise them."
                    else -> "Role: Professional data analyst. Provide a concise, objective summary."
                }

                val dataPrompt = """
                Data:
                - Total Spent: RM $totalSpent
                - Budget: RM $budget
                - Top Category: $topCategory (RM $topCategoryAmount)
                
                Task: Give a short financial review based on the Data and Role. $langInstruction
                """.trimIndent()

                val response = generativeModel.generateContent("$personaPrompt\n$dataPrompt")
                response.text ?: "AI is thinking..."
            } catch (e: Exception) {
                if (language == "zh") "AI 暂时无法连接 (请检查网络)" else "AI Unavailable (Check Internet)"
            }
        }
    }
}

// 简单的结果数据类
data class TransactionResult(
    val valid: Boolean,
    val amount: Double?,
    val merchant: String?,
    val category: String?,
    val type: String?
)