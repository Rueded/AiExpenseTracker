package com.example.aiexpensetracker.data

// --- 1. 发给 Gemini 的请求结构 ---
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

// --- 2. Gemini 返回的响应结构 (嵌套比较深) ---
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

// --- 3. 我们真正想要的记账数据 (最终提取结果) ---
data class ExpenseData(
    val valid: Boolean,      // 是不是有效的交易
    val amount: Double?,     // 金额
    val type: String?,       // "EXPENSE" or "INCOME"
    val merchant: String?,   // 商家名
    val category: String?    // 分类
)