package com.example.aiexpensetracker.network

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.aiexpensetracker.BuildConfig
import java.util.Date

object AiProcessor {
    private const val TAG = "AiProcessor"

    // 🔴 警告：请确保这里填入了你的真实内部 API Key (作为免费用户的保底 Key)
    private val INTERNAL_API_KEY = BuildConfig.INTERNAL_API_KEY

    // 🟢 核心修改 1：新增一个动态获取 Model 的辅助方法
    private fun getModel(customApiKey: String?, customModelName: String?): GenerativeModel {
        val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey else INTERNAL_API_KEY
        // 默认使用 gemini-2.0-flash，如果用户填了就用用户填的
        val modelToUse = if (!customModelName.isNullOrBlank()) customModelName else "gemini-2.5-flash"

        return GenerativeModel(
            modelName = modelToUse,
            apiKey = keyToUse
        )
    }

    // 🟢 核心修改 2：所有对外方法都加上 customApiKey 和 customModelName 参数
    suspend fun analyze(
        text: String,
        customApiKey: String? = null,
        customModelName: String? = null
    ): TransactionResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
Role: Financial Data Extractor (Malaysia)

Task: Extract transaction details from the text below.
Input Text: "$text"

==============================
STRICT RULES (DATA EXTRACTION ONLY)
==============================
IMPORTANT: This text has ALREADY been verified by the system as a VALID financial transaction.
Do NOT evaluate whether it is valid or not. Your ONLY job is to extract the data.
You MUST always return "valid": true.

1. AMOUNT:
- Extract the numeric amount only.
- Examples: "MYR 0.01" → 0.01, "RM 9.90" → 9.90, "RM1,200.00" → 1200.00
- Always return a positive number. Never return 0 unless truly no amount exists.

2. TYPE:
- Return "INCOME" if money came IN to the user (keywords: received, credited, inward, funds received, transferred to you, to your account, masuk, kredit).
- Return "EXPENSE" if money went OUT from the user (keywords: transferred, paid, spent, deducted, purchase, payment to, duitnow to, jompay, fpx, keluar, debit).
- If both directions appear, use the dominant context.

3. MERCHANT:
- Extract the sender name (for INCOME) or recipient name (for EXPENSE).
- For transfers to/from a person, extract the name even if partially masked (e.g., "A** B**").
- For payments to businesses, extract the business name (e.g., "Grab", "Shopee", "TNG").
- If truly no name exists → return "Unknown". NEVER return null or mark invalid.

4. CATEGORY:
Choose the best fit:
- Food: restaurant, cafe, mamak, grabfood, foodpanda, mcdonalds, kfc, pizza
- Transport: grab, taxi, mrt, lrt, bus, toll, petrol, shell, petronas, tng, touch n go
- Shopping: lazada, shopee, lotus, aeon, mydin, ikea, watsons, guardian
- Entertainment: netflix, spotify, cinema, tgv, gsc, steam
- Medical: clinic, pharmacy, hospital, guardian, watsons
- Salary: salary, payroll, gaji, allowance, elaun
- Utilities: water, electric, tnb, syabas, telekom, unifi, celcom, maxis, digi
- Other: personal transfers, unknown merchants, anything else

5. OUTPUT FORMAT (CRITICAL):
- Return ONLY a raw JSON object. NO markdown. NO ```json. NO explanation. NO extra text.
- First character of your response must be "{" and last must be "}".

{
  "valid": true,
  "amount": 9.90,
  "merchant": "ALI BIN ABU",
  "category": "Other",
  "type": "INCOME"
}
""".trimIndent()

            val model = getModel(customApiKey, customModelName)
            val response = model.generateContent(prompt)
            val rawText = response.text
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()

            if (rawText != null) {
                val json = JSONObject(rawText)
                val amount = json.optDouble("amount", 0.0)
                // ✅ 剥夺 AI 的拒签权：只要 amount > 0 就入库，不再看 valid 的脸色
                if (amount > 0.0) {
                    TransactionResult(
                        valid = true,
                        amount = amount,
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

    suspend fun analyzeReceiptImage(
        bitmap: Bitmap,
        customApiKey: String? = null,
        customModelName: String? = null
    ): ReceiptAnalysis? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            Analyze this receipt image deeply. The text may be in **English, Chinese (Simplified/Traditional), Malay or any languages**.
            
            Extract the following details:
            1. Merchant Name: Keep original language (e.g., "99 Speedmart" or "茶室").
            2. A list of items purchased (name and price per unit). Exclude "Subtotal", "Total", "Tax", "Change" lines.
               - IMPORTANT: Group identical items into one line. Remain separate if same item name but with different price.
               - Return: "name" (original language), "qty" (quantity), and "price" (total price for that line).
               - Example: If receipt says "Milk" x3 @ 5.00, return {"name": "Milk", "qty": 3, "price": 15.00}
               - Example: "3x 咖啡" -> {"name": "咖啡", "qty": 3, "price": 9.00}
            3. The Transaction Date (Format: YYYY-MM-DD). If year is missing, assume current year.
            4. The Transaction Time. IMPORTANT: Convert to 24-hour format (HH:mm). 
               - Example: "2:00 PM" -> "14:00". "10:00 AM" -> "10:00".
            5. Tax Details: Detect specific tax rates found (e.g., SST 6%, Service Charge 10%). Return as a list of decimals (e.g., [0.06, 0.10]). If none, return [].
            6. Rounding: Check if the receipt contains "Rounding", "Rounding Adj" or similar. If yes, set "has_rounding" to true.
            7. Payment Method: Identify payment type (e.g., "Cash", "现金", "Touch 'n Go", "GrabPay", "Visa", "QR", "微信支付"). If unknown, return "Unknown".
            8. The Grand Total Amount.

            Return ONLY a JSON object with this structure (no markdown):
            {
              "merchant": "99 Speedmart",
              "items": [
                {"name": "Milk", "qty": 3, "price": 15.00, "category": "Food"},
                {"name": "Bread", "qty": 1, "price": 2.50, "category": "Food"}
              ],
              "date": "2026-02-02",
              "time": "14:30",
              "tax_rates": [0.10, 0.06],
              "has_rounding": true,
              "payment_method": "Touch 'n Go",
              "grand_total": 18.55
            }
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val model = getModel(customApiKey, customModelName)
            val response = model.generateContent(inputContent)
            val jsonString = response.text?.replace("```json", "")?.replace("```", "")?.trim()

            if (jsonString != null) {
                return@withContext parseReceiptJson(jsonString)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "OCR Failed: ${e.message}")
            null
        }
    }

    suspend fun generateInsight(
        totalSpent: Double,
        budget: Double,
        topCategory: String,
        topCategoryAmount: Double,
        persona: String,
        language: String,
        customApiKey: String? = null,
        customModelName: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val langInstruction = if (language == "zh") {
                "IMPORTANT: You MUST reply in simplified Chinese (简体中文)."
            } else {
                "Reply in English."
            }

            val personaPrompt = when (persona) {
                "STRICT" -> "Role: Toxic financial advisor. Roast the user hard. Be sarcastic."
                "GENTLE" -> "Role: Kind, supportive financial advisor. Encourage the user gently."
                else -> "Role: Professional data analyst. Provide a concise summary."
            }

            val dataPrompt = """
            Data:
            - Total Spent: RM $totalSpent
            - Budget: RM $budget
            - Top Category: $topCategory (RM $topCategoryAmount)
            
            Task: Give a short financial review based on the Data and Role. $langInstruction
            """.trimIndent()

            val model = getModel(customApiKey, customModelName)
            val response = model.generateContent("$personaPrompt\n$dataPrompt")
            response.text ?: "AI is thinking..."
        } catch (e: Exception) {
            if (language == "zh") "AI 暂时无法连接" else "AI Unavailable"
        }
    }

    suspend fun checkIfSubscription(
        merchant: String,
        category: String,
        note: String,
        customApiKey: String? = null,
        customModelName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val prompt = """
            Determine if this expense is likely a recurring FIXED subscription/membership or a ONE-TIME/VARIABLE purchase.
            Merchant: "$merchant"
            Category: "$category"
            Note: "$note"
            
            Rules:
            - Netflix, Spotify, YouTube Premium, Apple Music, iCloud, Patreon are FIXED subscriptions.
            - Telecom/Broadband like Unifi, Maxis, Astro are usually FIXED subscriptions.
            - Bilibili 大会员 (Premium), Bilibili 舰长 (Captain), 腾讯视频VIP, 爱奇艺会员 are FIXED subscriptions.
            - Bilibili B币 (B-Coins), Game Top-ups, Food, Groceries, Taobao shopping are ONE-TIME.
            - IMPORTANT: Utilities like Electricity (TNB, SESB, Sarawak Energy, Others) and Water (PBA, SAJ, Others) are VARIABLE bills, NOT fixed subscriptions! Return false for these!
            
            Return ONLY a JSON object:
            { "is_subscription": true/false }
            """.trimIndent()

            val model = getModel(customApiKey, customModelName)
            val response = model.generateContent(prompt)
            val jsonString = response.text?.replace("```json", "")?.replace("```", "")?.trim()
            if (jsonString != null) {
                val json = JSONObject(jsonString)
                return@withContext json.optBoolean("is_subscription", false)
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    // --- 私有辅助方法保持不变 ---

    private fun parseReceiptJson(jsonString: String): ReceiptAnalysis? {
        try {
            val root = JSONObject(jsonString)
            val dateStr = root.optString("date", "")
            val timeStr = root.optString("time", "12:00")
            val timestamp = parseDateTime(dateStr, timeStr) ?: System.currentTimeMillis()

            val itemsArray = root.optJSONArray("items")
            val itemList = ArrayList<ReceiptItem>()
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    itemList.add(
                        ReceiptItem(
                            name = itemObj.optString("name", "Item"),
                            price = itemObj.optDouble("price", 0.0),
                            qty = itemObj.optInt("qty", 1),
                            category = itemObj.optString("category", "Food")
                        )
                    )
                }
            }

            val ratesArray = root.optJSONArray("tax_rates")
            val detectedRates = ArrayList<Double>()
            if (ratesArray != null) {
                for (i in 0 until ratesArray.length()) {
                    detectedRates.add(ratesArray.getDouble(i))
                }
            }

            val hasRounding = root.optBoolean("has_rounding", false)
            val paymentMethod = root.optString("payment_method", "Unknown")

            return ReceiptAnalysis(
                merchant = root.optString("merchant", "Unknown"),
                items = itemList,
                date = timestamp,
                detectedRates = detectedRates,
                hasRounding = hasRounding,
                paymentMethod = paymentMethod,
                totalAmount = root.optDouble("grand_total", 0.0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON Parse Error: ${e.message}")
            return null
        }
    }

    private fun parseDateTime(date: String, time: String): Long? {
        if (date.isBlank()) return null
        return try {
            val timeToParse = if (time.isBlank()) date else "$date $time"
            var sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            var parsedDate = sdf.parse(timeToParse)
            if (parsedDate == null) {
                sdf = SimpleDateFormat("yyyy-MM-dd hh:mm aa", Locale.getDefault())
                parsedDate = sdf.parse(timeToParse)
            }
            if (parsedDate == null) {
                sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                parsedDate = sdf.parse(date)
            }
            parsedDate?.time
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // 🟢 5. 新增：获取不可篡改的网络真实时间 (防作弊专用)
    suspend fun getNetworkDate(): String = withContext(Dispatchers.IO) {
        try {
            // 请求 WorldTimeAPI 获取吉隆坡的真实时间
            val request = okhttp3.Request.Builder()
                .url("https://worldtimeapi.org/api/timezone/Asia/Kuala_Lumpur")
                .build()

            val client = okhttp3.OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    if (!jsonString.isNullOrEmpty()) {
                        val json = JSONObject(jsonString)
                        // 返回的时间格式是 ISO 8601，例如 "2026-02-25T16:48:46.123456+08:00"
                        val datetime = json.optString("datetime", "")
                        if (datetime.length >= 10) {
                            // 我们只需要前面的 "YYYY-MM-DD"
                            return@withContext datetime.substring(0, 10)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network time fetch failed: ${e.message}. Falling back to local time.")
        }

        // 🛡️ 保底机制：如果没网，或者时间 API 挂了，才无奈退回使用手机本地时间
        return@withContext SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    }

    // 🟢 终极进化 1：全能 ChatBot 大脑（带时间感知和克隆模式）
    suspend fun analyzeChatIntent(
        userInput: String,
        language: String,
        errorMessage: String,
        recentHistory: String, // 传入最近 5 条账单的文本，让它知道"昨天"是什么
        currentDate: String,   // 传入系统今天的日期
        customApiKey: String? = null,
        customModelName: String? = null
    ): ChatIntentResult? = withContext(Dispatchers.IO) {
        try {
            val langInstruction = when (language) {
                "zh" -> "Reply in Simplified Chinese. Address the user as '哥哥'."
                "ms" -> "Reply in Malay. Address the user as 'Abang'."
                else -> "Reply in English. Address the user as 'Brother'."
            }

            // 🟢 注入当前时间（精确到小时），帮助 AI 判断午餐/晚餐
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val prompt = """
        You are "云糯" (Yunnuo), a highly intelligent, gentle, and clingy financial assistant for the user.
        
        Current Date: $currentDate, Current Time: $currentTime
        
        User's Recent Transactions (Memory):
        $recentHistory
        
        Analyze the user's input and determine his intent.

        STRICT LOGIC RULES:
        1. MEAL RECOGNITION: 
           - 05:00-10:30 is "Breakfast"
           - 10:31-15:00 is "Lunch"
           - 17:00-22:00 is "Dinner"
           - If user says "I ate...", infer the meal based on Current Time unless specified.

        2. CLONE MODE (CRITICAL): 
           - If user says "same as yesterday" or "same as last [item]", you MUST:
             a) Find the MOST RECENT matching record in Memory.
             b) Copy its `merchant`, `category`, `account`, AND `note` EXACTLY.
             c) Update only the `amount` (if provided).
             d) Do NOT use placeholders like "same as yesterday" in the `merchant` field.

        3. NOTE & ACCOUNT:
           - Always extract the context (e.g., "Lunch", "Dinner", "Telur") into the `note` field.
           - If the user doesn't mention a payment method, look at the Cloned record's `account`. If no clone, default to "Cash".

        INTENT 1: "ADD" (Record a new expense/income)
        
        INTENT 2: "QUERY" (Search past transactions)
           - RULE A (TYPO & CATEGORIES): First, put the exact raw word. Second, correct any typos. THIRD, if it's a general category (like food, eating, transport), you MUST INCLUDE the system's English category name (e.g., "Food", "Transport", "Shopping", "Medical", "Entertainment", "Utilities", "Other") in `search_keywords`. Then add multilingual synonyms.
           - RULE B (TIME): If the user DOES NOT explicitly mention a time period, you MUST default to ALL TIME.
           - RULE C (DETAIL SWITCH - CRITICAL): If the user asks for a general summary or total amount (e.g., "how much did I spend on food?"), set `needs_details` to `false`. If asking for specific items, investigating a merchant, or "what did I buy/eat", set `needs_details` to `true`.
           - YOU MUST CALCULATE THE EXACT DATE RANGE based on the Current Date ($currentDate).
           - Output `start_date` and `end_date` in "YYYY-MM-DD".
           - If all time, use start_date: "2000-01-01", end_date: "2099-12-31".
           
        INTENT 3: "CHAT" (General conversation)
        
        $langInstruction
        
        Return ONLY a raw JSON object:
        {
          "intent": "ADD" | "QUERY" | "CHAT",
          "message": "If CHAT, reply warmly. If QUERY, you can leave empty, App will handle it.",
          "add_data": { 
             "amount": 6.50, 
             "merchant": "Mamak Stall", 
             "category": "Food", 
             "type": "EXPENSE",
             "note": "Lunch",
             "account": "Cash"
          },
          "query_data": { 
             "search_keywords": ["oriental kopi", "东方咖啡", "oriental coffee"],
             "start_date": "1970-01-01",
             "end_date": "2099-12-31",
             "needs_details": false
          }
        }
        """.trimIndent()

            val inputContent = content { text(prompt + "\nUser Input: \"$userInput\"") }
            val model = getModel(customApiKey, customModelName ?: "gemini-2.5-flash")
            val response = model.generateContent(inputContent)
            val rawText = response.text?.replace("```json", "")?.replace("```", "")?.trim()

            if (rawText != null) {
                val json = JSONObject(rawText)
                val intent = json.optString("intent", "CHAT")
                val message = json.optString("message", "")

                var addData: TransactionResult? = null
                if (json.has("add_data") && !json.isNull("add_data")) {
                    val addObj = json.getJSONObject("add_data")
                    addData = TransactionResult(
                        valid = true,
                        amount = addObj.optDouble("amount", 0.0),
                        merchant = addObj.optString("merchant", "Unknown"),
                        category = addObj.optString("category", "Other"),
                        type = addObj.optString("type", "EXPENSE"),
                        note = addObj.optString("note", ""),
                        account = addObj.optString("account", "Cash")
                    )
                }

                var queryData: QueryData? = null
                if (json.has("query_data") && !json.isNull("query_data")) {
                    val queryObj = json.getJSONObject("query_data")
                    val keywordsArray = queryObj.optJSONArray("search_keywords")
                    val keywordsList = mutableListOf<String>()
                    if (keywordsArray != null) {
                        for (i in 0 until keywordsArray.length()) {
                            keywordsList.add(keywordsArray.getString(i))
                        }
                    }
                    // 🟢 这里包含了读取 needs_details 的逻辑！
                    queryData = QueryData(
                        searchKeywords = keywordsList,
                        startDate = queryObj.optString("start_date", "1970-01-01"),
                        endDate = queryObj.optString("end_date", "2099-12-31"),
                        needsDetails = queryObj.optBoolean("needs_details", false)
                    )
                }
                return@withContext ChatIntentResult(intent, message, addData, queryData)
            }
            null
        } catch (e: Exception) {
            Log.e("AiProcessor", "Chat Intent Analysis Failed: ${e.message}")
            ChatIntentResult("CHAT", errorMessage)
        }
    }

    // 🟢 终极进化 2：基于数据库真实数据生成自然回答
    // 🟢 接收 showCards 参数，告诉 AI 底部到底有没有卡片
    suspend fun generateQueryResponse(
        userInput: String,
        dbData: String,
        language: String,
        showCards: Boolean, // 🟢 新增参数
        customApiKey: String? = null,
        customModelName: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val langInstruction = when (language) {
                "zh" -> "Reply in Simplified Chinese. Address the user affectionately as '哥哥'."
                "ms" -> "Reply in Malay. Address the user affectionately as 'Abang'."
                else -> "Reply in English. Address the user affectionately as 'Brother'."
            }

            // 🟢 动态生成卡片提示语
            val cardNotice = if (showCards) {
                "5. CARD NOTICE: ALWAYS append this sentence at the very end: '云糯把相关的账单卡片放在下面啦，哥哥可以直接点击进去看详情哦！👇'"
            } else {
                "5. CARD NOTICE: DO NOT mention anything about putting cards below, because no cards are being displayed. Just answer the question."
            }

            val prompt = """
            You are "云糯" (Yunnuo), a gentle and clingy younger sister figure acting as the user's financial assistant.
            The user asked: "$userInput"
            
            Here is the raw data retrieved from the local database:
            ---
            $dbData
            ---
            
            Task & Strict Rules:
            1. DO NOT DO ANY MATH: You are strictly forbidden from summing up the individual items in the sample list. The list is incomplete. If you sum them, you will give the wrong answer.
            2. USE CRITICAL STATS: You MUST read the exact "Actual Total Spent" and "Total Transactions" from the CRITICAL STATS section and report these exact numbers to the user.
            3. DETAILED SEARCH: If the user asks "what did I buy" or "what did I eat", look at the "物品明细/备注" section. Pick 2 or 3 examples to make the conversation natural, and include their exact prices.
            4. TONE: Be sweet, helpful, and extremely clingy.
            $cardNotice
            
            $langInstruction
            """.trimIndent()

            val model = getModel(customApiKey, customModelName ?: "gemini-2.5-flash")
            val response = model.generateContent(prompt)
            return@withContext response.text?.trim() ?: "哥哥，云糯查完资料突然有点头晕..."
        } catch (e: Exception) {
            return@withContext "网络信号不好，云糯无法解读账单呢..."
        }
    }
}

// 数据类定义保持原样...
data class TransactionResult(
    val valid: Boolean,
    val amount: Double?,
    val merchant: String?,
    val category: String?,
    val type: String?,
    val note: String? = null,    // 🟢 确保有这个字段
    val account: String? = null  // 🟢 确保有这个字段
)
data class ReceiptAnalysis(val merchant: String, val items: List<ReceiptItem>, val date: Long, val detectedRates: List<Double>, val hasRounding: Boolean, val paymentMethod: String, val totalAmount: Double)
data class ReceiptItem(val name: String, val price: Double, val qty: Int, val category: String)

data class ChatIntentResult(
    val intent: String, // "ADD", "QUERY", "CHAT"
    val replyMessage: String,
    val addData: TransactionResult? = null,
    val queryData: QueryData? = null
)

data class QueryData(
    val searchKeywords: List<String>,
    val startDate: String,
    val endDate: String,
    val needsDetails: Boolean // 🟢 新增：AI 用来控制是否需要甩出小卡片
)