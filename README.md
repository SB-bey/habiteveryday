# 自律习惯打卡 (HabitCheckInApp) — Android 原生工程源码

这是一个基于 **Kotlin + Android XML + AppWidgetProvider** 编写的完整 Android 原生打卡应用程序工程。默认内置**「每日戒糖（7天挑战）」**、**「23点前早睡（21天）」**和**「有氧运动30分（14天）」**示例习惯，支持自由创建多个习惯、月历打卡视图以及手机桌面小组件。

## 核心功能特性

1. **多习惯管理与自定义周期**
   - 顶部横向标签栏支持一键切换多个习惯，长按习惯标签可删除。
   - 点击右上角「+ 新增习惯」可自由添加新目标（自定义名称、Emoji 图标，支持一键选择 7天/14天/21天/30天 或任意自定义天数）。
2. **严格禁补签机制（不支持补签）**
   - `HabitRepository.attemptCheckIn()` 在数据层严格校验系统当前日期（`LocalDate.now()`）。
   - **仅允许对今日进行打卡**：点击过去的漏签日期会提示 `⚠️ 本程序不支持补签，仅可打卡当天！`，点击未来日期也会拦截。
3. **可视化月历与统计面板**
   - 展示当前习惯的目标完成进度条、已完成天数/目标天数、当前连续打卡天数以及历史漏卡天数。
   - 月历网格区分四种核心状态：
     - **绿色（✓）**：当日已完成打卡
     - **红色边框（未签）**：习惯创建后的历史漏卡日期（不可补签）
     - **青色描边（可打卡）**：今日待打卡状态（点击日历今日格或主按钮均可完成打卡）
     - **浅灰**：未来日期或习惯创建前的日期
4. **桌面小组件（AppWidget）**
   - 支持添加 4×2 桌面小组件，实时展示当前习惯名称、目标周期进度与连续天数。
   - 内置**本周一至周日 7 天进度条**，直观呈现一周挑战完成情况。
   - 支持在手机桌面上直接点击 **「打卡今日」** 按钮完成打卡，或点击 **「切换 ↻」** 按钮在多个习惯间循环切换，无需打开 App。

## 工程目录结构

```text
HabitCheckInApp/
├── .github/workflows/build-apk.yml          # GitHub Actions 云端自动编译 APK 工作流
├── build.gradle.kts                         # 根项目 Gradle 配置
├── settings.gradle.kts                      # 工程模块声明
├── gradle.properties                        # Gradle JVM 与 AndroidX 属性
└── app/
    ├── build.gradle.kts                     # App 模块构建脚本 (minSdk 26, targetSdk 34)
    └── src/main/
        ├── AndroidManifest.xml              # 注册 MainActivity 与 HabitWidgetProvider
        ├── java/com/example/habitcheckin/
        │   ├── HabitModels.kt               # 习惯模型、连续天数/漏卡计算、状态枚举
        │   ├── HabitRepository.kt           # SharedPreferences+JSON 多习惯持久化与禁补签校验
        │   ├── MainActivity.kt              # 主界面控制、新增/删除习惯弹窗、月历渲染
        │   ├── HabitChipAdapter.kt          # 顶部多习惯横向切换栏适配器
        │   ├── CalendarAdapter.kt           # 月历网格状态适配器
        │   └── HabitWidgetProvider.kt       # 桌面小组件广播接收器与 RemoteViews 渲染
        └── res/
            ├── layout/
            │   ├── activity_main.xml        # 主界面布局
            │   ├── item_habit_chip.xml      # 习惯切换标签布局
            │   ├── item_calendar_day.xml    # 日历单日格子布局
            │   ├── dialog_add_habit.xml     # 新增习惯弹窗布局
            │   └── widget_habit_checkin.xml # 桌面小组件布局（含本周7天进度格）
            ├── xml/
            │   └── habit_widget_info.xml    # 桌面小组件尺寸与刷新元数据
            ├── drawable/                    # 圆角卡片、状态徽章与按钮背景矢量资源
            └── values/                      # 字符串、颜色与 Material 主题配置
```

## 如何编译生成 APK 安装包

### 方式一：使用 Android Studio 本地打包（推荐）
1. 解压 `HabitCheckInApp_AndroidProject.zip`。
2. 打开 **Android Studio**（建议使用 Hedgehog / Iguana 或更新版本，内置 JDK 17），选择 **Open** 并打开解压后的 `HabitCheckInApp` 文件夹。
3. 等待 Gradle 同步完成（首次打开会自动下载依赖）。
4. 在顶部菜单栏选择 **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**。
5. 编译完成后点击右下角弹窗中的 **locate**，即可在 `app/build/outputs/apk/debug/app-debug.apk` 获取安装包并安装到手机。

### 方式二：使用 GitHub Actions 免环境云端自动打包
如果你电脑上没有安装 Android Studio：
1. 在 GitHub 创建一个新仓库，将解压后的全部文件上传上去。
2. 仓库已内置 `.github/workflows/build-apk.yml`，上传后点击仓库顶部的 **Actions** 标签页。
3. 等待 `Build Android APK` 任务运行完成（约 2 分钟），即可在任务底部的 **Artifacts** 区域直接下载编译好的 `HabitCheckInApp-debug-apk.zip`（内含 `app-debug.apk`）。
