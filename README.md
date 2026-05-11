# 🚀 AI Expense Tracker 智能记账管家 (v5.1.0)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Latest-green.svg)](https://developer.android.com/jetpack/compose)
[![AI Powered](https://img.shields.io/badge/Powered_by-Google_Gemini-orange.svg)](https://ai.google.dev/)
[![Platform](https://img.shields.io/badge/Platform-Android_10+-brightgreen.svg)]()

**AI Expense Tracker** 是一款专为马来西亚用户打造的“零摩擦”智能财务管理应用。它不只是一个死板的记账工具，而是一个拥有大脑的 **AI 智能体（Agent）**。

通过系统级通知监听、OCR 收据解析、以及自然语言对话，我们将“手动记账”的烦恼彻底消除。

---

## 🌟 核心特性 (Core Features)

- 🤖 **AI 对话管家 (Yunnuo / 云糯)**：像和人聊天一样记账和查账。支持自然语言提取、模糊搜索、拼写纠正与数据总结。
- ⚡ **自动化防漏网监听 (Triple-Layer Filter)**：深度适配马来西亚各大银行与钱包 (TNG, GrabPay, Maybank, CIMB 等)。独创“评分制+AI鉴定+正则兜底”三层防御，精准捕捉每一笔流水，杜绝广告与验证码。
- 📸 **截图极速入库 (Share-to-Track)**：在任何 App 看到付款截图，直接通过系统 Share Intent 分享至本应用，瞬间完成解析入账。
- 🔮 **智能习惯预测 (Smart Prediction)**：每天早上买咖啡？AI 自动捕捉消费频次与时段，在首页主动推送“一键记账”卡片。
- 🔒 **隐私至上 (Privacy First)**：所有财务数据与 AI 记忆均存储于本地 Room 加密数据库，支持私有化 Google Drive 同步，绝不收集用户隐私。

---

## 🛠️ 技术栈 (Tech Stack)

- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Database**: Room Persistence Library (SQLite)
- **AI Engine**: Google Gemini API (2.5 Flash / 2.0 Pro)
- **Cloud & Backup**: Google Drive API (Encrypted Zip Sync), Firebase Realtime DB
- **Architecture**: MVVM + Coroutines + StateFlow 响应式架构
- **Localization**: 全量 i18n 支持 (中/英/马来语 自动切换)

---

## 🗺️ 进化史与更新日志 (Version History)

### 🚀 v5.1.0 (Current) - "Agent 终极进化"
- **新增**：截图秒记 (Share-to-Track)，打通系统分享底层的解析。
- **新增**：首页智能预测 (Smart Prediction) 卡片，基于时间与频次一键记账。
- **重构**：通知监听系统升级为**三层防御体系**（积分白名单 -> AI 否决权 -> Regex 兜底抢救），彻底解决 Spam 误杀与漏网问题。
- **完善**：代码全量剥离硬编码，完成国际化 (i18n) 适配。

### 🌟 v5.0.0 - "大脑升级"
- **重构**：引入强大的 AI 意图路由引擎 (Intent Engine)。
- **新增**：深度查账功能。AI 现可精准解析 `[Details]` 物品明细，并在对话框内动态下发可交互的真实账单卡片。
- **新增**：“克隆模式”，支持“和昨天一样”的自然语言快捷指令。
- **优化**：加入防呆机制，强制 AI 使用数据库绝对算术结果，杜绝大模型计算幻觉。

### 💬 v4.0.0 - "云糯诞生"
- **新增**：AI ChatBot 界面。专属数字管家“云糯”上线，支持多轮自然语言记账。
- **新增**：智能订阅检测 (Subscription Radar)，自动识别 Netflix, Spotify 等固定周期扣费并提醒。
- **优化**：增强 AI 记忆库，自动学习用户在特定时间段对特定商户的分类习惯。

### 📸 v3.0.0 - "视觉觉醒"
- **新增**：接入 Google Gemini Vision 模型，支持拍照/相册选取收据 OCR 解析。
- **优化**：自动提取多行物品清单、税费 (SST/Service Charge) 及支付方式。
- **新增**：Google Drive 云端加密备份与恢复功能。

### ⚡ v2.0.0 - "自动化时代"
- **新增**：NotificationListenerService 系统级通知监听。
- **新增**：基础的 Regex 正则表达式匹配，初步支持 TNG eWallet, Maybank, GrabPay 等大马主流应用。
- **优化**：本地应用锁 (Biometric 锁屏支持)。

### 🌱 v1.0.0 - "基础奠定"
- 初始版本发布。
- 完成 Jetpack Compose 主页布局、预算环形图表及流水列表。
- 建立 Room 基础数据库实体。

---

## 🤝 贡献与反馈 (Feedback)
本项目由独立开发者在日常生活中寻找痛点并结合 AI 辅助编程完成。如果在体验中遇到任何 Bug，或有新的功能建议，欢迎提交 **Issue** 或 **Pull Request**！

---
*Developed with ❤️ and endless coffee by **白开水**.*
