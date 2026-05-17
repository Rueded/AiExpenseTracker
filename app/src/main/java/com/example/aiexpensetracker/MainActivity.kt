package com.example.aiexpensetracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import com.example.aiexpensetracker.ui.HomeScreen
import com.example.aiexpensetracker.ui.theme.AiExpenseTrackerTheme
import com.example.aiexpensetracker.viewmodel.HomeViewModel
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // 检查是否是从通知栏点进来的
        val expenseIdFromNotif = intent.getIntExtra("EDIT_EXPENSE_ID", -1)
        if (expenseIdFromNotif != -1) {
            viewModel.triggerEditFromNotification(expenseIdFromNotif)
        }

        val prefs = getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        val savedDarkMode = prefs.getBoolean("dark_mode", false)

        setContent {
            var darkTheme by remember { mutableStateOf(savedDarkMode) }
            val toggleTheme = { isDark: Boolean ->
                darkTheme = isDark
                prefs.edit().putBoolean("dark_mode", isDark).apply()
            }

            AiExpenseTrackerTheme(darkTheme = darkTheme) {
                val context = LocalContext.current

                // 🟢 1. 准备权限请求器 (用于发送通知)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(context, "Notifications are needed to track expenses", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                // 🟢 2. 一启动 APP，就立刻检查并申请“发送通知”权限 (针对 Android 13+)
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPostPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPostPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // 监听“读取通知”权限状态
                var hasListenerPermission by remember { mutableStateOf(isNotificationServiceEnabled()) }
                var isUnlocked by remember { mutableStateOf(false) }

                LifecycleEffect(onResume = { hasListenerPermission = isNotificationServiceEnabled() })

                val canAuth = remember { checkBiometricSupport() }
                val showHomeScreen = isUnlocked || !canAuth || expenseIdFromNotif != -1

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // 只有当获得了读取权限，才显示主页或锁屏；否则显示索要读取权限的页面
                    if (!hasListenerPermission) {
                        PermissionScreen()
                    } else {
                        if (showHomeScreen) {
                            HomeScreen(
                                viewModel = viewModel,
                                isDarkTheme = darkTheme,
                                onToggleTheme = toggleTheme,
                                onLanguageChange = { langCode ->
                                    val appLocale = LocaleListCompat.forLanguageTags(langCode)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            )
                        } else {
                            LockScreen(onAuthenticate = { authenticateUser { success -> if (success) isUnlocked = true } })
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun checkBiometricSupport(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }

    private fun authenticateUser(onResult: (Boolean) -> Unit) {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { super.onAuthenticationSucceeded(result); onResult(true) }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { super.onAuthenticationError(errorCode, errString); onResult(false) }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle("Locked / 已锁定")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun LifecycleEffect(onResume: () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) onResume() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun LockScreen(onAuthenticate: () -> Unit) {
    LaunchedEffect(Unit) { onAuthenticate() }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "App Locked", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onAuthenticate, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)) { Text("Unlock") }
    }
}

// 🟢 这里的权限请求是指“读取通知”的权限 (Notification Listener)
@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Permission Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "To track expenses automatically,\nplease enable Notification Access.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (e: Exception) {} }, modifier = Modifier.fillMaxWidth()) { Text("Go to Settings") }
    }
}