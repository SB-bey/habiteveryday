package com.example.habitcheckin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 桌面打卡小组件（AppWidgetProvider）：
 * 1. 显示当前选中习惯的名称、目标进度（如 3/7 天）与连续打卡天数
 * 2. 显示本周一至周日 7 天迷你打卡状态格（✓ 已打卡 / × 未签 / 今 待打卡 / · 未来）
 * 3. 支持在桌面直接点击「今日打卡」按钮完成当日打卡，以及点击「切换习惯 ↻」在多个习惯间快速切换
 */
class HabitWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_CHECK_IN = "com.example.habitcheckin.ACTION_WIDGET_CHECK_IN"
        const val ACTION_WIDGET_SWITCH_HABIT = "com.example.habitcheckin.ACTION_WIDGET_SWITCH_HABIT"

        private val DAY_STATUS_VIEW_IDS = intArrayOf(
            R.id.tvWidgetDay1Status,
            R.id.tvWidgetDay2Status,
            R.id.tvWidgetDay3Status,
            R.id.tvWidgetDay4Status,
            R.id.tvWidgetDay5Status,
            R.id.tvWidgetDay6Status,
            R.id.tvWidgetDay7Status
        )

        /**
         * 供主界面或广播调用：刷新桌面上所有已添加的小组件实例
         */
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, HabitWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repository = HabitRepository(context)
            val habit = repository.getSelectedHabit()
            val views = RemoteViews(context.packageName, R.layout.widget_habit_checkin)

            // 点击标题区域打开主程序
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRootContainer, openAppPendingIntent)

            // 绑定“切换习惯”广播
            val switchIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_SWITCH_HABIT
            }
            val switchPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                switchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetSwitchHabit, switchPendingIntent)

            // 绑定“今日打卡”广播
            val checkInIntent = Intent(context, HabitWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CHECK_IN
            }
            val checkInPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                checkInIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetCheckIn, checkInPendingIntent)

            if (habit == null) {
                views.setTextViewText(R.id.tvWidgetHabitName, "暂无打卡习惯")
                views.setTextViewText(R.id.tvWidgetProgressText, "点击进入应用新增习惯")
                views.setTextViewText(R.id.btnWidgetCheckIn, "打开应用")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val today = LocalDate.now()
            val checkedToday = habit.isChecked(today.toString())
            val totalChecked = habit.totalCheckedCount()
            val streak = habit.currentStreak(today)

            views.setTextViewText(R.id.tvWidgetHabitName, "${habit.emoji} ${habit.name}")
            views.setTextViewText(
                R.id.tvWidgetProgressText,
                "周期进度 ${totalChecked}/${habit.targetDays} 天 · 连续 ${streak} 天"
            )

            if (checkedToday) {
                views.setTextViewText(R.id.btnWidgetCheckIn, "今日已打卡 ✓")
                views.setInt(R.id.btnWidgetCheckIn, "setBackgroundResource", R.drawable.bg_widget_btn_done)
            } else {
                views.setTextViewText(R.id.btnWidgetCheckIn, "打卡今日")
                views.setInt(R.id.btnWidgetCheckIn, "setBackgroundResource", R.drawable.bg_widget_btn_active)
            }

            // 计算本周一至周日 7 天状态（周一为第 1 列，周日为第 7 列）
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val habitStart = runCatching { LocalDate.parse(habit.startDate) }.getOrDefault(today)

            for (i in 0 until 7) {
                val dayDate = monday.plusDays(i.toLong())
                val statusViewId = DAY_STATUS_VIEW_IDS[i]
                val isChecked = habit.isChecked(dayDate.toString())

                when {
                    isChecked -> {
                        views.setTextViewText(statusViewId, "✓")
                        views.setTextColor(statusViewId, Color.WHITE)
                        views.setInt(statusViewId, "setBackgroundResource", R.drawable.bg_day_checked)
                    }
                    dayDate.isEqual(today) -> {
                        views.setTextViewText(statusViewId, "今")
                        views.setTextColor(statusViewId, Color.parseColor("#0D9488"))
                        views.setInt(statusViewId, "setBackgroundResource", R.drawable.bg_day_today_unchecked)
                    }
                    dayDate.isBefore(today) && !dayDate.isBefore(habitStart) -> {
                        views.setTextViewText(statusViewId, "×")
                        views.setTextColor(statusViewId, Color.parseColor("#EF4444"))
                        views.setInt(statusViewId, "setBackgroundResource", R.drawable.bg_day_missed)
                    }
                    else -> {
                        views.setTextViewText(statusViewId, "·")
                        views.setTextColor(statusViewId, Color.parseColor("#94A3B8"))
                        views.setInt(statusViewId, "setBackgroundResource", R.drawable.bg_day_neutral)
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = HabitRepository(context)

        when (intent.action) {
            ACTION_WIDGET_CHECK_IN -> {
                val habit = repository.getSelectedHabit()
                if (habit != null) {
                    when (repository.attemptCheckIn(habit.id, LocalDate.now())) {
                        CheckInResult.CHECKED_TODAY -> {
                            Toast.makeText(context, "🎉 已完成今日「${habit.name}」打卡！", Toast.LENGTH_SHORT).show()
                        }
                        CheckInResult.ALREADY_CHECKED_TODAY -> {
                            Toast.makeText(context, "今日「${habit.name}」已打卡", Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                    updateAllWidgets(context)
                }
            }
            ACTION_WIDGET_SWITCH_HABIT -> {
                val nextHabit = repository.switchToNextHabit()
                if (nextHabit != null) {
                    Toast.makeText(context, "已切换小组件习惯：${nextHabit.emoji} ${nextHabit.name}", Toast.LENGTH_SHORT).show()
                    updateAllWidgets(context)
                }
            }
        }
    }
}
