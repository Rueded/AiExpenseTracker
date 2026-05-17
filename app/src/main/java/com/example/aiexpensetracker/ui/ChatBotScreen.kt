package com.example.aiexpensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.text.SimpleDateFormat

import com.example.aiexpensetracker.R
import com.example.aiexpensetracker.database.ExpenseDao
import com.example.aiexpensetracker.database.ExpenseEntity
import com.example.aiexpensetracker.network.AiProcessor

// 🟢 支持挂载多张小卡片
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val attachedExpenses: List<ExpenseEntity> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(
    dao: ExpenseDao,
    onBackClick: () -> Unit,
    onAddTransaction: (ExpenseEntity) -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onDeleteTransaction: (ExpenseEntity) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    // 🟢 替换为 stringResource
    val welcomeMessage = stringResource(R.string.chat_welcome)
    var messages by remember { mutableStateOf(listOf(
        ChatMessage(welcomeMessage, isUser = false)
    )) }

    val coroutineScope = rememberCoroutineScope()
    val currentLanguage = Locale.getDefault().language
    val networkErrorMsg = stringResource(id = R.string.chat_network_error)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        TopAppBar(
            // 🟢 替换为 stringResource
            title = { Text(stringResource(R.string.chat_title), fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                ChatBubble(msg, onExpenseClick, onDeleteTransaction)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                // 🟢 替换为 stringResource
                placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // 提取常量，避免在 onClick 中反复调用 stringResource
            val loadingSearchText = stringResource(R.string.chat_loading_search)
            val addSuccessText = stringResource(R.string.chat_add_success)
            val addFailAmountText = stringResource(R.string.chat_add_fail_amount)
            val loadingDetailsText = stringResource(R.string.chat_loading_details)

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userText = inputText
                        inputText = ""
                        messages = messages + ChatMessage(userText, isUser = true)

                        // 🟢 替换为 stringResource
                        messages = messages + ChatMessage(loadingSearchText, isUser = false, isLoading = true)

                        coroutineScope.launch {
                            val recentList = dao.getAllExpenses().firstOrNull()?.take(5) ?: emptyList()
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val historyStr = recentList.joinToString("\n") {
                                "- ${sdf.format(Date(it.timestamp))}: RM${it.amount} at ${it.merchant} via ${it.accountName} (Note: ${it.note})"
                            }
                            val todayStr = sdf.format(Date())

                            val intentResult = AiProcessor.analyzeChatIntent(
                                userInput = userText,
                                language = currentLanguage,
                                errorMessage = networkErrorMsg,
                                recentHistory = historyStr,
                                currentDate = todayStr
                            )

                            messages = messages.filterNot { it.isLoading }

                            if (intentResult != null) {
                                when (intentResult.intent) {
                                    "ADD" -> {
                                        val data = intentResult.addData
                                        if (data != null && data.amount != null && data.amount > 0) {
                                            val newExpense = ExpenseEntity(
                                                amount = data.amount,
                                                merchant = data.merchant ?: "Unknown",
                                                category = data.category ?: "Other",
                                                type = data.type ?: "EXPENSE",
                                                timestamp = System.currentTimeMillis(),
                                                originalText = userText,
                                                note = data.note ?: "",
                                                accountName = data.account ?: "Cash"
                                            )
                                            val generatedId = dao.insert(newExpense)
                                            val realExpenseWithId = newExpense.copy(id = generatedId.toInt())

                                            messages = messages + ChatMessage(
                                                // 🟢 替换为 stringResource
                                                text = addSuccessText,
                                                isUser = false,
                                                attachedExpenses = listOf(realExpenseWithId)
                                            )
                                        } else {
                                            // 🟢 替换为 stringResource
                                            messages = messages + ChatMessage(addFailAmountText, isUser = false)
                                        }
                                    }
                                    "QUERY" -> {
                                        val keywords = intentResult.queryData?.searchKeywords ?: emptyList()
                                        val startDateStr = intentResult.queryData?.startDate ?: "1970-01-01"
                                        val endDateStr = intentResult.queryData?.endDate ?: "2099-12-31"
                                        val needsDetails = intentResult.queryData?.needsDetails ?: false

                                        val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val startTime = try {
                                            sdfParse.parse(startDateStr)?.time ?: 0L
                                        } catch (e: Exception) { 0L }

                                        val endTime = try {
                                            val date = sdfParse.parse(endDateStr)
                                            if (date != null) {
                                                val cal = Calendar.getInstance()
                                                cal.time = date
                                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                                cal.set(Calendar.MINUTE, 59)
                                                cal.set(Calendar.SECOND, 59)
                                                cal.timeInMillis
                                            } else { System.currentTimeMillis() }
                                        } catch (e: Exception) { System.currentTimeMillis() }

                                        val timeFiltered = dao.getExpensesByTimeRange(startTime, endTime)
                                        val finalResults = if (keywords.isEmpty()) {
                                            timeFiltered
                                        } else {
                                            timeFiltered.filter { exp ->
                                                keywords.any { kw ->
                                                    exp.merchant.contains(kw, ignoreCase = true) ||
                                                            exp.category.contains(kw, ignoreCase = true) ||
                                                            exp.note.contains(kw, ignoreCase = true) ||
                                                            exp.originalText.contains(kw, ignoreCase = true)
                                                }
                                            }
                                        }

                                        val totalExpense = finalResults.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                        val totalRecords = finalResults.size

                                        val dataFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

                                        val listStr = finalResults.take(15).joinToString("\n") {
                                            val detailPart = if (it.note.contains("[Details]")) {
                                                " | 物品明细: " + it.note.substringAfter("[Details]").trim().replace("\n", ", ")
                                            } else {
                                                " | 备注: " + it.note
                                            }
                                            "[${dataFmt.format(Date(it.timestamp))}] RM ${it.amount} | 商户: ${it.merchant}$detailPart"
                                        }

                                        val safeDataStr = if (finalResults.isEmpty()) {
                                            "No matching records found from $startDateStr to $endDateStr."
                                        } else {
                                            """
                                            === 核心统计 (CRITICAL STATS - READ THIS FOR TOTALS) ===
                                            Date Range: $startDateStr to $endDateStr
                                            Total Transactions (实际总笔数): $totalRecords
                                            Actual Total Spent (实际总花费): RM ${String.format("%.2f", totalExpense)}

                                            === 明细截取 (Top 15 Samples ONLY - DO NOT SUM THESE UP) ===
                                            $listStr
                                            """.trimIndent()
                                        }

                                        // 🟢 替换为 stringResource
                                        messages = messages + ChatMessage(loadingDetailsText, isUser = false, isLoading = true)

                                        val cardsToShow = if (needsDetails && finalResults.isNotEmpty()) finalResults.take(3) else emptyList()

                                        val smartReply = AiProcessor.generateQueryResponse(
                                            userInput = userText,
                                            dbData = safeDataStr,
                                            language = currentLanguage,
                                            showCards = cardsToShow.isNotEmpty()
                                        )

                                        messages = messages.filterNot { it.isLoading }

                                        messages = messages + ChatMessage(
                                            text = smartReply,
                                            isUser = false,
                                            attachedExpenses = cardsToShow
                                        )
                                    }
                                    else -> {
                                        messages = messages + ChatMessage(intentResult.replyMessage, isUser = false)
                                    }
                                }
                            } else {
                                messages = messages + ChatMessage(networkErrorMsg, isUser = false)
                            }
                        }
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onDeleteClick: (ExpenseEntity) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (message.attachedExpenses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.attachedExpenses.forEach { expense ->
                    key(expense.id) {
                        var isDeleted by remember { mutableStateOf(false) }

                        if (!isDeleted) {
                            Box(modifier = Modifier.fillMaxWidth(0.85f)) {
                                ExpenseCard(
                                    expense = expense,
                                    onDeleteClick = {
                                        onDeleteClick(expense)
                                        isDeleted = true
                                    },
                                    onClick = { onExpenseClick(expense) },
                                    isVisible = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}