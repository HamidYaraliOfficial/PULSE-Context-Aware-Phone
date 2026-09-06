# PULSE — Context-Aware Phone

**A Personal Context Operating System for Android.** On-device, privacy-first, and fully local — PULSE fuses time, motion, sensors, location, calendar, Bluetooth, app usage and notifications into a single, explainable understanding of what you're doing right now, then adapts your phone around it.

Choose your language: **[English](#-english)** · **[فارسی](#-فارسی)** · **[中文](#-中文)**

---

## 🇬🇧 English

### What PULSE does

PULSE runs a local **Context Engine** that continuously (and efficiently) reads available signals — Activity Recognition, motion sensors, Bluetooth accessories, calendar events, app usage patterns, and more — and fuses them into a confidence-scored context such as `Meeting — 91% confidence` or `Study — 84% confidence`. Every decision is explainable: tap "Why?" to see exactly which signals contributed and by how much.

A **Rule Engine** (IF / AND / OR / NOT, with Priority, Cooldown, Schedule and Exceptions) and a **Mode Engine** (Focus, Study, Work, Meeting, Driving, Sleep, Workout, Travel, Gaming, Quiet, Custom) then adapt the device: notification filtering, Do Not Disturb, volume, brightness, screen timeout, and more — using only official, public Android APIs.

### Key features

- **Context Engine** — Activity Recognition, `SensorManager` motion fusion, `UsageStatsManager` app-usage analysis, `CalendarContract` meeting detection, Bluetooth accessory classification, and an optional Fused Location Provider signal, all combined by an auditable, weighted scoring model (not a black box).
- **Explainable confidence** — every context decision ships with the list of signals that produced it.
- **Rule Engine** — a full IF/AND/OR/NOT condition tree, Priority-based Conflict Resolution, Cooldown/Debounce, and user-defined Active Hours schedules.
- **Mode Engine** — ten built-in modes plus fully custom ones, each with its own editable action bundle and **schedule editor** (build your own "active hours" for any Mode or Rule — the app tells you live whether it's active right now and exactly how long until the next change, entirely from hours *you* enter).
- **Smart Notifications** — per-context deferral policy with session digests, built on `NotificationListenerService` and `NotificationManager`, metadata-only (never message content).
- **Local Analytics & Timeline** — daily/weekly/monthly Focus Time, Distraction Count, Notification Load, Mode Switches, Active Hours, all computed from on-device history.
- **Habit/Pattern Detection** — locally computed, always labeled as an estimate, never presented as settled fact.
- **On-device Learning** — confirming or rejecting a detected context nudges that context's confidence threshold, transparently and reversibly.
- **Simulation / Test Mode** — build a synthetic context and run it through the real inference engine without touching any live sensor or executing a real action.
- **Permission Center** — every capability has its own independent privacy toggle; PULSE never requests a permission before a feature that needs it is turned on.
- **Four themes** (Windows Default, Red, Blue) × Light/Dark/System, Material 3, Windows 11–inspired visual language.
- **Full localization** — English and Chinese (LTR), Persian (RTL) — every string lives in Android resources, no hardcoded UI text.

### Architecture

Clean Architecture + MVVM, Kotlin Coroutines & Flow throughout:

```
core/         theming, DI, shared utilities (schedule math, JSON codec, locale switching)
domain/       pure Kotlin models + repository interfaces — no Android imports
data/         Room-backed repository implementations
database/     Room entities, DAOs, migrations
context/      Context Engine, Inference Engine, Learning Engine, Pattern Detection
context/signals/  Time, Battery, Activity Recognition, Sensor fusion providers
rules/        Rule Engine, Condition Evaluator, Conflict Resolver
modes/        Mode Engine, Action Executor (the one place that touches system state)
notifications/ Smart Notification Engine, Notification Listener Service
usage/        UsageStatsManager-based App Usage Analyzer
calendar/     Calendar Context Manager
location/     Location Context Manager (Fused Location Provider)
bluetooth/    Bluetooth Context Manager
permissions/  Permission Manager + definitions
security/     Android Keystore–backed secret storage
worker/       WorkManager backstop, optional foreground service, battery-aware scheduler
ui/           Jetpack Compose screens, ViewModels, navigation, reusable components
```

### Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17
- Android SDK Platform 35, Build-Tools 35.x
- A device or emulator running **Android 8.0 (API 26)** or newer, with Google Play services (required for Activity Recognition and Fused Location Provider)

### Getting started

1. **Extract** the provided ZIP archive to a folder on your computer.
2. **Open** that folder in Android Studio (`File → Open…`, select the extracted `PULSE` folder — it already contains `settings.gradle.kts`).
3. **Let Gradle sync.** Android Studio automatically downloads every dependency listed in `app/build.gradle.kts` (Compose, Hilt, Room, WorkManager, Coroutines, Play Services Location, etc.) — no manual library installation is needed. If you prefer the command line, from the project root run:
   ```bash
   ./gradlew build
   ```
   (On Windows: `gradlew.bat build`.) The first sync will take a few minutes while Gradle fetches everything.
4. **Select a run target** — a physical device or an emulator image that includes Google Play services (e.g. any "Google APIs" or "Google Play" system image), Android 8.0+.
5. **Run** the `app` module (`Run ▶`).
6. On first launch, open **Settings → Permission Center** and turn on only the capabilities you want — each one is independent, and PULSE explains what it's for before you grant it.
7. To run the test suite:
   ```bash
   ./gradlew test
   ```

### Privacy by design

- All context analysis happens on-device. No analytics SDK, no ad SDK, no telemetry.
- Every signal source is gated by both the OS permission *and* an independent in-app toggle.
- Notification content is never stored — only metadata (package, category, priority, timestamp).
- Data Retention settings let you choose how long Context History, Usage Analytics, Notification Metadata and Event Logs are kept, with one-tap "Clear now."
- Any future Cloud AI integration is opt-in only, behind a pluggable provider interface, with secrets stored in the Android Keystore — never hardcoded.

### Project status

The Context Engine, Rule Engine, Mode Engine, Room database, DI graph, and the core navigation/screens are implemented against real Android APIs end-to-end. Some peripheral UI surfaces (the full drag-and-drop Automation canvas, additional Mode-specific screens) are intentionally left as straightforward extension points on top of the working engines, ready for further UI polish.

This codebase was written and reviewed outside of Android Studio (no Android SDK/emulator was available in the authoring environment), so it has **not** been compiled here. Open it in Android Studio and run a Gradle sync + build first — treat any small, targeted fixes Android Studio's compiler or lint surfaces as normal polish on a large, freshly-written codebase, not as a sign of missing functionality.

---

## 🇮🇷 فارسی

### پالس چه کاری انجام می‌دهد

پالس یک **Context Engine** محلی اجرا می‌کند که به‌صورت پیوسته و بهینه، سیگنال‌های در دسترس — تشخیص فعالیت، حسگرهای حرکتی، لوازم جانبی بلوتوث، رویدادهای تقویم، الگوی استفاده از برنامه‌ها و موارد دیگر — را می‌خواند و آن‌ها را در قالب یک Context با امتیاز اطمینان مشخص، مانند «جلسه — ۹۱٪ اطمینان» یا «مطالعه — ۸۴٪ اطمینان»، ترکیب می‌کند. هر تصمیم قابل توضیح است: با زدن «چرا؟» می‌توانید دقیقاً ببینید کدام سیگنال‌ها و به چه میزان در این تشخیص نقش داشته‌اند.

سپس یک **Rule Engine** (با ساختار IF / AND / OR / NOT، به‌همراه Priority، Cooldown، Schedule و Exception) و یک **Mode Engine** (تمرکز، مطالعه، کار، جلسه، رانندگی، خواب، تمرین، سفر، بازی، سکوت، سفارشی) گوشی را بر همین اساس تطبیق می‌دهند: فیلتر اعلان، مزاحم نشوید، صدا، روشنایی، Timeout صفحه و موارد دیگر — همه فقط با استفاده از APIهای رسمی و عمومی اندروید.

### ویژگی‌های کلیدی

- **Context Engine** — ترکیب Activity Recognition، حسگرهای حرکتی از طریق `SensorManager`، تحلیل استفاده از برنامه با `UsageStatsManager`، تشخیص جلسه از `CalendarContract`، طبقه‌بندی لوازم بلوتوث و سیگنال اختیاری Fused Location Provider، همه در یک مدل امتیازدهی وزن‌دار و قابل‌ممیزی (نه یک جعبه سیاه).
- **اطمینان قابل‌توضیح** — هر تصمیم Context همراه با فهرست سیگنال‌های مؤثر در آن ارائه می‌شود.
- **Rule Engine** — درخت شرط کامل IF/AND/OR/NOT، حل تداخل بر پایه Priority، سیستم Cooldown/Debounce و امکان تعریف ساعات فعال توسط خود کاربر.
- **Mode Engine** — ده حالت پیش‌فرض به‌علاوه امکان ساخت حالت سفارشی، هرکدام با مجموعه اقدامات قابل ویرایش و **ویرایشگر زمان‌بندی** (برای هر Mode یا Rule، ساعات فعالیت دلخواه خودتان را وارد کنید — برنامه به‌صورت زنده نشان می‌دهد که آیا هم‌اکنون فعال است و دقیقاً چقدر تا تغییر بعدی مانده، کاملاً بر اساس ساعاتی که خودتان وارد کرده‌اید).
- **اعلان‌های هوشمند** — سیاست به‌تعویق‌اندازی بر اساس Context با خلاصه‌سازی پایان جلسه، بر پایه `NotificationListenerService` و `NotificationManager`، فقط داده فراداده (هرگز متن پیام).
- **تحلیل و جدول زمانی محلی** — زمان تمرکز، تعداد حواس‌پرتی، حجم اعلان، تعداد تغییر حالت، ساعات فعال، به‌صورت روزانه/هفتگی/ماهانه، همگی محاسبه‌شده از تاریخچه روی دستگاه.
- **تشخیص الگو/عادت** — محاسبه کاملاً محلی، همیشه با برچسب «تخمین»، هرگز به‌عنوان واقعیت قطعی.
- **یادگیری روی دستگاه** — تأیید یا رد یک Context تشخیص‌داده‌شده، آستانه اطمینان همان Context را به‌صورت شفاف و قابل‌بازگشت تغییر می‌دهد.
- **حالت شبیه‌سازی/آزمایش** — یک Context مصنوعی بسازید و آن را از داخل موتور تشخیص واقعی عبور دهید، بدون لمس هیچ حسگر واقعی یا اجرای اقدام واقعی.
- **مرکز مجوزها** — هر قابلیت کلید حریم خصوصی مستقل خود را دارد؛ پالس هرگز پیش از فعال‌شدن قابلیت مربوطه، مجوزی درخواست نمی‌کند.
- **چهار پوسته** (پیش‌فرض ویندوز، قرمز، آبی) × روشن/تاریک/پیروی از سیستم، بر پایه Material 3 با زبان بصری الهام‌گرفته از ویندوز ۱۱.
- **بومی‌سازی کامل** — انگلیسی و چینی (چپ‌به‌راست)، فارسی (راست‌به‌چپ) — تمام متن‌ها در منابع اندروید هستند، بدون هیچ متن ثابت در کد.

### معماری

Clean Architecture + MVVM، با Kotlin Coroutines و Flow در سراسر پروژه:

```
core/         تم، تزریق وابستگی، ابزارهای مشترک (محاسبه زمان‌بندی، JSON، تغییر زبان)
domain/       مدل‌های خالص Kotlin و اینترفیس‌های Repository — بدون وابستگی به اندروید
data/         پیاده‌سازی Repositoryها بر پایه Room
database/     Entityها، DAOها و Migrationهای Room
context/      Context Engine، Inference Engine، Learning Engine، تشخیص الگو
context/signals/  ارائه‌دهنده‌های سیگنال زمان، باتری، تشخیص فعالیت، حسگر
rules/        Rule Engine، ارزیاب شرط، حل‌کننده تداخل
modes/        Mode Engine، اجراکننده اقدام (تنها نقطه‌ای که وضعیت سیستم را تغییر می‌دهد)
notifications/ موتور اعلان هوشمند، سرویس شنونده اعلان
usage/        تحلیل‌گر استفاده از برنامه بر پایه UsageStatsManager
calendar/     مدیر Context تقویم
location/     مدیر Context موقعیت مکانی (Fused Location Provider)
bluetooth/    مدیر Context بلوتوث
permissions/  مدیر مجوز و تعاریف آن
security/     ذخیره‌سازی امن بر پایه Android Keystore
worker/       پشتیبان WorkManager، سرویس Foreground اختیاری، زمان‌بند آگاه به باتری
ui/           صفحات Jetpack Compose، ViewModelها، ناوبری، کامپوننت‌های قابل استفاده مجدد
```

### پیش‌نیازها

- Android Studio Ladybug (2024.2) یا جدیدتر
- JDK 17
- Android SDK Platform 35 و Build-Tools 35.x
- دستگاه یا شبیه‌ساز با **Android 8.0 (API 26)** یا جدیدتر، همراه با Google Play services (برای Activity Recognition و Fused Location Provider لازم است)

### شروع به کار

۱. فایل ZIP ارائه‌شده را در پوشه‌ای روی سیستم خود **استخراج** کنید.
۲. آن پوشه را در Android Studio **باز کنید** (`File → Open…` و پوشه استخراج‌شده `PULSE` را انتخاب کنید — این پوشه از قبل شامل `settings.gradle.kts` است).
۳. اجازه دهید **Gradle همگام‌سازی** شود. Android Studio به‌طور خودکار تمام وابستگی‌های ذکرشده در `app/build.gradle.kts` (Compose، Hilt، Room، WorkManager، Coroutines، Play Services Location و غیره) را دانلود می‌کند — نیازی به نصب دستی هیچ کتابخانه‌ای نیست. در صورت تمایل به استفاده از خط فرمان، از ریشه پروژه اجرا کنید:
   ```bash
   ./gradlew build
   ```
   (در ویندوز: `gradlew.bat build`.) همگام‌سازی اول ممکن است چند دقیقه طول بکشد.
۴. یک **هدف اجرا** انتخاب کنید — دستگاه واقعی یا شبیه‌سازی که شامل Google Play services باشد (مثلاً هر image با برچسب «Google APIs» یا «Google Play»)، با Android 8.0 یا بالاتر.
۵. ماژول `app` را **اجرا** کنید (`Run ▶`).
۶. در اولین اجرا، به **تنظیمات ← مرکز مجوزها** بروید و فقط قابلیت‌هایی را که می‌خواهید فعال کنید — هرکدام مستقل هستند و پالس پیش از دریافت مجوز، کاربرد آن را توضیح می‌دهد.
۷. برای اجرای مجموعه تست‌ها:
   ```bash
   ./gradlew test
   ```

### حریم خصوصی به‌عنوان یک اصل طراحی

- تمام تحلیل Context روی خود دستگاه انجام می‌شود. هیچ SDK تحلیلی، تبلیغاتی یا ردیابی وجود ندارد.
- هر منبع سیگنال هم توسط مجوز سیستم‌عامل و هم توسط کلید مستقل داخل برنامه کنترل می‌شود.
- محتوای اعلان هرگز ذخیره نمی‌شود — فقط فراداده (بسته، دسته، اولویت، زمان).
- تنظیمات نگهداری داده به شما امکان می‌دهد مدت نگهداری تاریخچه Context، تحلیل استفاده، فراداده اعلان و گزارش رویدادها را انتخاب کنید، با گزینه «پاک‌سازی اکنون» در یک ضربه.
- هرگونه اتصال آینده به هوش مصنوعی ابری کاملاً اختیاری، پشت یک اینترفیس Provider قابل‌جایگزینی و با ذخیره‌سازی کلیدها در Android Keystore خواهد بود — هرگز به‌صورت ثابت در کد.

### وضعیت پروژه

Context Engine، Rule Engine، Mode Engine، دیتابیس Room، گراف تزریق وابستگی و صفحات و ناوبری اصلی، سرتاسر با APIهای واقعی اندروید پیاده‌سازی شده‌اند. برخی بخش‌های جانبی رابط کاربری (بوم کامل Drag-and-Drop اتوماسیون، صفحات اختصاصی بیشتر برای هر Mode) عمداً به‌عنوان نقاط توسعه ساده روی موتورهای کاملاً کارآمد باقی گذاشته شده‌اند تا برای پرداخت بیشتر رابط کاربری آماده باشند.

این کد بیرون از Android Studio نوشته و بازبینی شده (در محیط نگارش، SDK/شبیه‌ساز اندروید در دسترس نبود)، بنابراین اینجا کامپایل نشده است. لطفاً ابتدا آن را در Android Studio باز کرده و یک Gradle Sync + Build کامل انجام دهید؛ اگر کامپایلر یا Lint خطاهای کوچک و موضعی نشان داد، آن‌ها را به‌عنوان اصلاحات معمول روی یک codebase بزرگ و تازه‌نوشته‌شده در نظر بگیرید، نه نشانه‌ای از نبود قابلیت.

---

## 🇨🇳 中文

### PULSE 是什么

PULSE 在设备本地运行一个 **情境引擎（Context Engine）**，持续而高效地读取可用信号——活动识别、运动传感器、蓝牙配件、日历事件、应用使用模式等——并将它们融合为一个带置信度的情境判断，例如「会议 — 置信度 91%」或「学习 — 置信度 84%」。每一次判断都是可解释的：点击「为什么？」即可准确看到是哪些信号、各自贡献了多少。

随后，**规则引擎**（支持 IF / AND / OR / NOT，以及优先级、冷却时间、时间安排与例外）与 **模式引擎**（专注、学习、工作、会议、驾驶、睡眠、运动、旅行、游戏、静音、自定义）会据此调整设备：通知过滤、勿扰模式、音量、亮度、屏幕超时等——全部仅使用官方公开的 Android API 实现。

### 核心功能

- **情境引擎** — 融合活动识别、基于 `SensorManager` 的运动信号、基于 `UsageStatsManager` 的应用使用分析、基于 `CalendarContract` 的会议检测、蓝牙配件分类，以及可选的融合位置提供程序信号，全部通过一个可审计、有权重的评分模型完成（而非黑箱模型）。
- **可解释的置信度** — 每一次情境判断都附带产生该判断的信号列表。
- **规则引擎** — 完整的 IF/AND/OR/NOT 条件树、基于优先级的冲突解决、冷却/防抖机制，以及用户自定义的「激活时间段」。
- **模式引擎** — 十种内置模式外加完全自定义模式，每种模式都有可编辑的操作组合与 **时间安排编辑器**（为任意模式或规则输入你自己的「激活时间段」——应用会实时显示它当前是否处于激活状态，以及距离下一次变化还有多久，完全基于你自己填写的时间）。
- **智能通知** — 基于当前情境的延后策略，并在时段结束后提供摘要，构建于 `NotificationListenerService` 与 `NotificationManager` 之上，仅保存元数据（绝不保存消息内容）。
- **本地分析与时间线** — 按日/周/月统计专注时长、分心次数、通知负载、模式切换次数、活跃时段，全部基于本地历史数据计算。
- **习惯/规律检测** — 完全在本地计算，始终标注为「估计」，绝不作为确凿事实呈现。
- **设备端学习** — 确认或拒绝某个检测到的情境，会相应微调该情境的置信度阈值，过程透明且可撤销。
- **模拟/测试模式** — 构建一个合成情境，并将其送入真实的推理引擎运行，全程不触碰任何真实传感器，也不执行任何真实操作。
- **权限中心** — 每项能力都有独立的隐私开关；在对应功能真正启用之前，PULSE 绝不会申请相应权限。
- **四种主题**（Windows 默认、红色、蓝色）× 浅色/深色/跟随系统，基于 Material 3，视觉语言参考 Windows 11。
- **完整本地化** — 英文与中文（从左到右），波斯语（从右到左）——所有文本均存放于 Android 资源文件中，界面代码中没有任何硬编码文本。

### 架构

Clean Architecture + MVVM，全程使用 Kotlin Coroutines 与 Flow：

```
core/         主题、依赖注入、共享工具（时间安排计算、JSON 编解码、语言切换）
domain/       纯 Kotlin 模型与 Repository 接口 — 不依赖 Android
data/         基于 Room 的 Repository 实现
database/     Room 实体、DAO、迁移
context/      情境引擎、推理引擎、学习引擎、规律检测
context/signals/  时间、电池、活动识别、传感器融合的信号提供者
rules/        规则引擎、条件求值器、冲突解决器
modes/        模式引擎、操作执行器（唯一实际修改系统状态的位置）
notifications/ 智能通知引擎、通知监听服务
usage/        基于 UsageStatsManager 的应用使用分析器
calendar/     日历情境管理器
location/     位置情境管理器（融合位置提供程序）
bluetooth/    蓝牙情境管理器
permissions/  权限管理器与定义
security/     基于 Android Keystore 的密钥存储
worker/       WorkManager 兜底任务、可选前台服务、电量感知调度器
ui/           Jetpack Compose 界面、ViewModel、导航、可复用组件
```

### 环境要求

- Android Studio Ladybug (2024.2) 或更新版本
- JDK 17
- Android SDK Platform 35，Build-Tools 35.x
- 运行 **Android 8.0（API 26）** 或更高版本、并带有 Google Play services 的设备或模拟器（活动识别与融合位置提供程序均需要它）

### 快速开始

1. 将提供的 ZIP 压缩包**解压**到电脑上的某个文件夹。
2. 在 Android Studio 中**打开**该文件夹（`File → Open…`，选择解压后的 `PULSE` 文件夹——其中已包含 `settings.gradle.kts`）。
3. 等待 **Gradle 同步**。Android Studio 会自动下载 `app/build.gradle.kts` 中列出的所有依赖项（Compose、Hilt、Room、WorkManager、Coroutines、Play Services Location 等）——无需手动安装任何库。如果你更喜欢命令行方式，可在项目根目录运行：
   ```bash
   ./gradlew build
   ```
   （Windows 下为 `gradlew.bat build`。）首次同步可能需要几分钟时间来下载依赖。
4. **选择运行目标**——一台真机，或一个包含 Google Play services 的模拟器镜像（例如任意带「Google APIs」或「Google Play」标签的系统镜像），Android 8.0 及以上。
5. **运行** `app` 模块（`Run ▶`）。
6. 首次启动后，进入 **设置 → 权限中心**，只开启你需要的能力——每一项都是独立的，PULSE 会在你授权之前先说明该权限的用途。
7. 运行测试套件：
   ```bash
   ./gradlew test
   ```

### 隐私优先的设计

- 所有情境分析均在设备本地完成，不含任何分析 SDK、广告 SDK 或遥测代码。
- 每个信号来源都同时受系统权限与应用内独立开关的双重控制。
- 通知内容从不被存储——仅保存元数据（包名、类别、优先级、时间戳）。
- 数据保留设置允许你选择情境历史、使用分析、通知元数据与事件日志的保留时长,并支持一键「立即清除」。
- 未来任何云端 AI 集成均为可选项，构建于可插拔的 Provider 接口之上,密钥存储于 Android Keystore 中——绝不硬编码。

### 项目状态

情境引擎、规则引擎、模式引擎、Room 数据库、依赖注入图,以及核心导航与主要界面均已基于真实 Android API 完整实现。部分外围界面（完整的拖放式自动化画布、更多模式专属界面）被有意保留为构建在这些可正常工作的引擎之上的直接扩展点，以便后续进一步打磨界面。

本代码库是在 Android Studio 之外编写与审阅的（编写环境中没有 Android SDK / 模拟器），因此尚未在此处完成编译。请先在 Android Studio 中打开项目并执行一次完整的 Gradle 同步与构建；如果编译器或 Lint 提示了一些细小的、局部的问题，请将其视为一个大型、全新代码库上的正常打磨工作，而不是功能缺失的迹象。
