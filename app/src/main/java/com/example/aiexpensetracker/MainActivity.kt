package com.example.aiexpensetracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import com.example.aiexpensetracker.service.NotificationListener
import com.example.aiexpensetracker.ui.HomeScreen
import com.example.aiexpensetracker.ui.theme.AiExpenseTrackerTheme
import com.example.aiexpensetracker.viewmodel.HomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    // 🟢 把 viewModel 提出来，让全类都能访问
    private lateinit var viewModel: HomeViewModel

    private fun pokeNotificationService() {
        val context = this
        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        if (isEnabled) {
            val componentName = ComponentName(context, NotificationListener::class.java)
            val pm = context.packageManager

            try {
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                Log.d("MainActivity", "NotificationListener Rebound Triggered")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to poke service: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.checkGoogleLogin(this)

        // 检查是否是从通知栏点进来的
        val expenseIdFromNotif = intent.getIntExtra("EDIT_EXPENSE_ID", -1)
        if (expenseIdFromNotif != -1) {
            viewModel.triggerEditFromNotification(expenseIdFromNotif)
        }

        // 🟢 1. 处理刚启动 App 时接到的分享图片
        handleSharedIntent(intent)

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

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(context, "Notifications are needed to track expenses", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPostPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        if (!hasPostPermission) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                var hasListenerPermission by remember { mutableStateOf(isNotificationServiceEnabled()) }
                var isUnlocked by remember { mutableStateOf(false) }

                LifecycleEffect(onResume = {
                    hasListenerPermission = isNotificationServiceEnabled()
                    pokeNotificationService()
                })

                val canAuth = remember { checkBiometricSupport() }
                val showHomeScreen = isUnlocked || !canAuth || expenseIdFromNotif != -1
                val accounts by viewModel.accounts.collectAsState(initial = emptyList())

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!hasListenerPermission) {
                        PermissionScreen()
                    } else {
                        if (showHomeScreen) {
                            HomeScreen(
                                viewModel = viewModel,
                                isDarkTheme = darkTheme,
                                onToggleTheme = toggleTheme,
                                accountList = accounts.map { it.name },
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

    // 🟢 2. 处理 App 已经在后台运行时收到的分享图片
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 更新当前的 Intent
        handleSharedIntent(intent)
    }

    // 🟢 3. 解析图片的专用方法
    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {

            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }

            if (imageUri != null) {
                // 拿到图片后，立刻唤醒 ViewModel 里的 AI 开始分析！
                viewModel.scanReceipt(this, imageUri) {
                    // 分析完成后，HomeScreen 会监听到 scannedReceipt 变化并自动弹窗
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
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