package com.example.aiexpensetracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.R
import com.example.aiexpensetracker.database.AccountEntity
import com.example.aiexpensetracker.database.AnalysisReportEntity
import com.example.aiexpensetracker.database.AppDatabase
import com.example.aiexpensetracker.database.ExpenseEntity
import com.example.aiexpensetracker.database.MerchantLearningEntity
import com.example.aiexpensetracker.database.SubscriptionEntity
import com.example.aiexpensetracker.network.AiProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryStat(val category: String, val totalAmount: Double, val percentage: Float)
data class AccountBalance(val name: String, val balance: Double)
data class SubscriptionSuggestion(val expense: ExpenseEntity, val predictedBillingDate: Int)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()
    private val learningDao = database.merchantLearningDao()
    private val accountDao = database.accountDao()
    private val subscriptionDao = database.subscriptionDao()
    private val ignoredDao = database.ignoredDao()
    private val analysisDao = database.analysisReportDao()

    private val _rawExpenses = expenseDao.getAllExpenses()
    private val _accounts = accountDao.getAllAccounts()
    val subscriptions = subscriptionDao.getAllSubscriptions()
    val ignoredLogs = ignoredDao.getRecentLogs()
    val allLearnedHabits = learningDao.getAllLearnings()

    private val _currentMonth = MutableStateFlow(System.currentTimeMillis())
    val currentMonth: StateFlow<Long> = _currentMonth

    private val _statsType = MutableStateFlow("EXPENSE")
    val statsType = _statsType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _editRequest = MutableStateFlow<Int?>(null)
    val editRequest = _editRequest.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget = _monthlyBudget.asStateFlow()

    private val _categoryBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBudgets = _categoryBudgets.asStateFlow()

    private val _subscriptionSuggestion = MutableStateFlow<SubscriptionSuggestion?>(null)
    val subscriptionSuggestion = _subscriptionSuggestion.asStateFlow()

    // 🟢 AI 报告状态
    private val _monthlyReport = MutableStateFlow<AnalysisReportEntity?>(null)
    val monthlyReport = _monthlyReport.asStateFlow()

    private var reportListeningJob: Job? = null

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport = _isGeneratingReport.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (accountDao.getCount() == 0) {
                accountDao.insert(AccountEntity(name = "Cash", initialBalance = 0.0))
            }
        }
        refreshBudgets()
        startListeningToReport()
    }

    // 🟢 手动管理 AI 报告监听
    private fun startListeningToReport() {
        reportListeningJob?.cancel()
        val currentMonthId = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(_currentMonth.value))
        reportListeningJob = viewModelScope.launch(Dispatchers.IO) {
            analysisDao.getReport(currentMonthId).collect { report ->
                _monthlyReport.value = report
            }
        }
    }

    fun prevMonth() {
        val c = Calendar.getInstance()
        c.timeInMillis = _currentMonth.value
        c.add(Calendar.MONTH, -1)
        _currentMonth.value = c.timeInMillis
        startListeningToReport()
    }

    fun nextMonth() {
        val c = Calendar.getInstance()
        c.timeInMillis = _currentMonth.value
        c.add(Calendar.MONTH, 1)
        _currentMonth.value = c.timeInMillis
        startListeningToReport()
    }

    // 🟢 生成 AI 月度报告
    fun generateMonthlyReport(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_isGeneratingReport.value) return@launch
            _isGeneratingReport.value = true
            try {
                val currentMonthId = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(_currentMonth.value))
                val expenses = filteredExpenses.first()

                if (expenses.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No data this month!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val totalSpent = expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val grouped = expenses.filter { it.type == "EXPENSE" }.groupBy { it.category }
                val topCatEntry = grouped.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                val topCategory = topCatEntry?.key ?: "None"
                val topAmount = topCatEntry?.value?.sumOf { it.amount } ?: 0.0

                val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
                val budget = prefs.getFloat("monthly_budget", 3000f).toDouble()
                val persona = prefs.getString("ai_persona", "NEUTRAL") ?: "NEUTRAL"

                val currentLocale = androidx.core.os.LocaleListCompat.getAdjustedDefault()[0]
                val lang = if (currentLocale?.language == "zh") "zh" else "en"

                val insightText = AiProcessor.generateInsight(totalSpent, budget, topCategory, topAmount, persona, lang)

                val report = AnalysisReportEntity(
                    monthId = currentMonthId,
                    totalSpent = totalSpent,
                    summaryText = insightText,
                    persona = persona
                )
                analysisDao.insert(report)

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun setAiPersona(context: Context, persona: String) {
        context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putString("ai_persona", persona).apply()
    }

    fun getAiPersona(context: Context): String {
        return context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .getString("ai_persona", "NEUTRAL") ?: "NEUTRAL"
    }

    fun refreshBudgets() {
        val prefs = getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        _monthlyBudget.value = prefs.getFloat("monthly_budget", 3000f).toDouble()
        val allPrefs = prefs.all
        val catMap = mutableMapOf<String, Double>()
        allPrefs.forEach { (key, value) ->
            if (key.startsWith("budget_cat_"))
                catMap[key.removePrefix("budget_cat_")] = (value as? Float)?.toDouble() ?: 0.0
        }
        _categoryBudgets.value = catMap
    }

    fun updateMonthlyBudget(newBudget: Double) {
        getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
        _monthlyBudget.value = newBudget
    }

    fun updateCategoryBudget(category: String, newBudget: Double) {
        getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putFloat("budget_cat_$category", newBudget.toFloat()).apply()
        refreshBudgets()
    }

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (accountDao.exists(expense.accountName) == 0)
                    accountDao.insert(AccountEntity(name = expense.accountName))
                expense.targetAccountName?.let {
                    if (accountDao.exists(it) == 0)
                        accountDao.insert(AccountEntity(name = it))
                }
            }
            expenseDao.insert(expense)
            if (expense.type == "EXPENSE") {
                if (expense.merchant.isNotBlank()) learnUserHabit(expense)
                checkLatestExpenseForSubscription(expense)
            }
        }
    }

    private suspend fun learnUserHabit(expense: ExpenseEntity) {
        withContext(Dispatchers.IO) {
            val timeSlot = getTimeSlot(expense.timestamp)
            val existing = learningDao.getLearning(expense.merchant, timeSlot)
            if (existing != null)
                learningDao.updateLearning(existing.id, expense.category, expense.note)
            else
                learningDao.insert(MerchantLearningEntity(
                    merchant = expense.merchant,
                    timeSlot = timeSlot,
                    category = expense.category,
                    note = expense.note
                ))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { expenseDao.delete(expense) }
    }

    suspend fun getExpenseById(id: Int): ExpenseEntity? =
        withContext(Dispatchers.IO) { _rawExpenses.first().find { it.id == id } }

    fun checkLatestExpenseForSubscription(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (subscriptionDao.countActiveByMerchant(expense.merchant) > 0) return@launch
            val allowedCategories = listOf("Entertainment", "Education", "Medical", "Bills", "Utilities", "Digital Services", "Communication")
            val strongKeywords = listOf("subscription", "premium", "membership", "plan", "monthly", "recurring")
            val isCat = allowedCategories.any { expense.category.contains(it, true) }
            val isKey = strongKeywords.any { expense.originalText.contains(it, true) || expense.merchant.contains(it, true) }
            if (isCat || isKey) {
                _subscriptionSuggestion.value = SubscriptionSuggestion(
                    expense,
                    Calendar.getInstance().apply { timeInMillis = expense.timestamp }.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }

    fun confirmSubscription(suggestion: SubscriptionSuggestion) {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionDao.insert(SubscriptionEntity(
                merchant = suggestion.expense.merchant,
                amount = suggestion.expense.amount,
                category = suggestion.expense.category,
                billingDate = suggestion.predictedBillingDate,
                accountName = suggestion.expense.accountName
            ))
            dismissSuggestion()
        }
    }

    fun dismissSuggestion() { _subscriptionSuggestion.value = null }
    fun deleteSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch(Dispatchers.IO) { subscriptionDao.delete(sub) }
    }
    fun deleteLearning(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { learningDao.deleteById(id) }
    }
    fun clearIgnoredLogs() {
        viewModelScope.launch(Dispatchers.IO) { ignoredDao.clearAll() }
    }
    fun addNewAccount(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank() && accountDao.exists(name) == 0)
                accountDao.insert(AccountEntity(name = name))
        }
    }
    fun deleteAccount(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name != "Cash") accountDao.deleteByName(name)
        }
    }

    fun adjustAccountBalance(accountName: String, actualBalance: Double) {
        viewModelScope.launch {
            val currentCalcBalance = accountBalances.first().find { it.name == accountName }?.balance ?: 0.0
            val diff = actualBalance - currentCalcBalance
            if (diff == 0.0) return@launch
            val type = if (diff > 0) "INCOME" else "EXPENSE"
            expenseDao.insert(ExpenseEntity(
                amount = Math.abs(diff),
                type = type,
                merchant = "System Adjustment",
                category = "Other",
                timestamp = System.currentTimeMillis(),
                originalText = "Manual Correction",
                note = "Adj: ${String.format("%.2f", currentCalcBalance)} -> ${String.format("%.2f", actualBalance)}",
                accountName = accountName
            ))
        }
    }

    val accountBalances = combine(_accounts, _rawExpenses) { accounts, expenses ->
        accounts.map { acc ->
            var bal = acc.initialBalance
            expenses.forEach { exp ->
                when (exp.type) {
                    "INCOME" -> if (exp.accountName == acc.name) bal += exp.amount
                    "EXPENSE" -> if (exp.accountName == acc.name) bal -= exp.amount
                    "TRANSFER" -> {
                        if (exp.accountName == acc.name) bal -= exp.amount
                        if (exp.targetAccountName == acc.name) bal += exp.amount
                    }
                }
            }
            AccountBalance(acc.name, bal)
        }
    }

    val filteredExpenses = combine(_rawExpenses, _currentMonth, _searchQuery) { expenses, monthTime, query ->
        val localizedExpenses = expenses.map { expense ->
            expense.copy(category = mapCategoryName(getApplication(), expense.category))
        }
        if (query.isBlank()) {
            val (start, end) = getMonthRange(monthTime)
            localizedExpenses.filter { it.timestamp in start..end }
        } else {
            localizedExpenses.filter {
                it.merchant.contains(query, true) ||
                        it.category.contains(query, true) ||
                        it.note.contains(query, true)
            }
        }
    }

    val expenseStats = combine(filteredExpenses, _statsType) { list, type ->
        val filteredList = list.filter { it.type == type }
        val totalAmount = filteredList.sumOf { it.amount }
        filteredList.groupBy { it.category }.map { (category, items) ->
            CategoryStat(
                category,
                items.sumOf { it.amount },
                if (totalAmount > 0) (items.sumOf { it.amount } / totalAmount).toFloat() else 0f
            )
        }.sortedByDescending { it.totalAmount }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun setStatsType(type: String) { _statsType.value = type }
    fun triggerEditFromNotification(id: Int) { _editRequest.value = id }
    fun clearEditRequest() { _editRequest.value = null }

    private fun mapCategoryName(context: Context, rawName: String): String {
        return when (rawName) {
            "饮食", "Food" -> context.getString(R.string.cat_food)
            "交通", "Transport" -> context.getString(R.string.cat_transport)
            "购物", "Shopping" -> context.getString(R.string.cat_shopping)
            "娱乐", "Entertainment" -> context.getString(R.string.cat_entertainment)
            "居家", "Home" -> context.getString(R.string.cat_home)
            "医疗", "Medical" -> context.getString(R.string.cat_medical)
            "其他", "Other" -> context.getString(R.string.cat_other)
            "薪水", "Salary" -> context.getString(R.string.cat_salary)
            "奖金", "Bonus" -> context.getString(R.string.cat_bonus)
            "津贴", "Allowance" -> context.getString(R.string.cat_allowance)
            "投资", "Investment" -> context.getString(R.string.cat_investment)
            "红包", "Red Packet" -> context.getString(R.string.cat_red_packet)
            else -> rawName
        }
    }

    private fun getMonthRange(timeInMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis - 1
        return Pair(start, end)
    }

    companion object {
        fun getTimeSlot(timestamp: Long): String {
            val h = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
            return when (h) {
                in 5..10 -> "breakfast"
                in 11..14 -> "lunch"
                in 17..21 -> "dinner"
                in 22..23, in 0..4 -> "supper"
                else -> "other"
            }
        }
    }

    fun exportCsvForExcel(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expenses = _rawExpenses.first()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write("\ufeffID,TIME,AMT,MERCH,NOTE,CAT,TYPE,ACC,TGT\n")
                    expenses.forEach { e ->
                        it.write("${e.id},${e.timestamp},${e.amount},${e.merchant},${e.note},${e.category},${e.type},${e.accountName},${e.targetAccountName?:""}\n")
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Exported!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun backupData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expenses = _rawExpenses.first()
                val learnings = learningDao.getAllLearnings().first()
                val accounts = _accounts.first()
                val subscriptions = subscriptionDao.getAllSubscriptions().first()
                val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)

                val root = JSONObject()
                val expArray = JSONArray()
                expenses.forEach {
                    val o = JSONObject()
                    o.put("amount", it.amount)
                    o.put("type", it.type)
                    o.put("merchant", it.merchant)
                    o.put("category", it.category)
                    o.put("timestamp", it.timestamp)
                    o.put("originalText", it.originalText)
                    o.put("note", it.note)
                    o.put("accountName", it.accountName)
                    o.put("targetAccountName", it.targetAccountName)
                    expArray.put(o)
                }
                root.put("expenses", expArray)

                val learnArray = JSONArray()
                learnings.forEach {
                    val o = JSONObject()
                    o.put("merchant", it.merchant)
                    o.put("timeSlot", it.timeSlot)
                    o.put("category", it.category)
                    o.put("note", it.note)
                    o.put("useCount", it.useCount)
                    learnArray.put(o)
                }
                root.put("memories", learnArray)

                val subArray = JSONArray()
                subscriptions.forEach {
                    val o = JSONObject()
                    o.put("merchant", it.merchant)
                    o.put("amount", it.amount)
                    o.put("category", it.category)
                    o.put("billingDate", it.billingDate)
                    o.put("accountName", it.accountName)
                    o.put("isActive", it.isActive)
                    subArray.put(o)
                }
                root.put("subscriptions", subArray)

                val accArray = JSONArray()
                accounts.forEach {
                    val o = JSONObject()
                    o.put("name", it.name)
                    o.put("initialBalance", it.initialBalance)
                    accArray.put(o)
                }
                root.put("accounts", accArray)

                val prefObj = JSONObject()
                prefObj.put("monthly_budget", prefs.getFloat("monthly_budget", 0f).toDouble())
                val customCats = prefs.getStringSet("custom_cat_expense", emptySet()) ?: emptySet()
                prefObj.put("custom_cat_expense", JSONArray(customCats))
                prefs.all.forEach { (k, v) -> if (k.startsWith("budget_cat_")) prefObj.put(k, v) }
                root.put("preferences", prefObj)

                context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(4).toByteArray()) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup Complete!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: return@launch
                val root = JSONObject(jsonString)

                if (root.has("preferences")) {
                    val prefObj = root.getJSONObject("preferences")
                    val editor = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE).edit()
                    val keys = prefObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k == "custom_cat_expense") {
                            val arr = prefObj.getJSONArray(k)
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) set.add(arr.getString(i))
                            editor.putStringSet(k, set)
                        } else if (k == "monthly_budget" || k.startsWith("budget_cat_"))
                            editor.putFloat(k, prefObj.getDouble(k).toFloat())
                    }
                    editor.apply()
                }

                if (root.has("accounts")) {
                    val arr = root.getJSONArray("accounts")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val n = o.getString("name")
                        if (accountDao.exists(n) == 0)
                            accountDao.insert(AccountEntity(name = n, initialBalance = o.optDouble("initialBalance", 0.0)))
                    }
                }

                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    val exists = _accounts.first().map { it.name }.toMutableSet()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val acc = o.getString("accountName")
                        if (acc !in exists) {
                            if (accountDao.exists(acc) == 0) accountDao.insert(AccountEntity(name = acc))
                            exists.add(acc)
                        }
                        expenseDao.insert(ExpenseEntity(
                            amount = o.getDouble("amount"),
                            type = o.getString("type"),
                            merchant = o.getString("merchant"),
                            category = o.getString("category"),
                            timestamp = o.getLong("timestamp"),
                            originalText = o.optString("originalText"),
                            note = o.optString("note"),
                            accountName = acc,
                            targetAccountName = if (o.has("targetAccountName")) o.getString("targetAccountName") else null
                        ))
                    }
                }

                if (root.has("memories")) {
                    val arr = root.getJSONArray("memories")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        learningDao.insert(MerchantLearningEntity(
                            merchant = o.getString("merchant"),
                            timeSlot = o.getString("timeSlot"),
                            category = o.getString("category"),
                            note = o.getString("note"),
                            useCount = o.optInt("useCount", 1)
                        ))
                    }
                }

                if (root.has("subscriptions")) {
                    val arr = root.getJSONArray("subscriptions")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        subscriptionDao.insert(SubscriptionEntity(
                            merchant = o.getString("merchant"),
                            amount = o.getDouble("amount"),
                            category = o.getString("category"),
                            billingDate = o.getInt("billingDate"),
                            accountName = o.getString("accountName"),
                            isActive = o.optBoolean("isActive", true)
                        ))
                    }
                }

                withContext(Dispatchers.Main) {
                    refreshBudgets()
                    Toast.makeText(context, "Restored!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}