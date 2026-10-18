package com.example.habitcheckin

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 单个习惯的数据模型
 * @param id 唯一标识符
 * @param name 习惯名称（如“每日戒糖”）
 * @param emoji 习惯图标 Emoji（如“🍵”）
 * @param targetDays 挑战目标天数（如 7 天一周、21 天、30 天）
 * @param startDate 习惯创建日期（yyyy-MM-dd），此日期之前不计入漏签
 * @param checkedDates 已打卡日期集合（格式 yyyy-MM-dd）
 */
data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val targetDays: Int,
    val startDate: String,
    val checkedDates: MutableSet<String>
) {
    /**
     * 判断指定日期是否已打卡
     */
    fun isChecked(dateStr: String): Boolean = checkedDates.contains(dateStr)

    /**
     * 已累计打卡总天数
     */
    fun totalCheckedCount(): Int = checkedDates.size

    /**
     * 目标完成进度百分比 (0..100)
     */
    fun progressPercent(): Int {
        if (targetDays <= 0) return 0
        return ((checkedDates.size * 100) / targetDays).coerceAtMost(100)
    }

    /**
     * 是否已达成目标周期
     */
    fun isGoalCompleted(): Boolean = checkedDates.size >= targetDays

    /**
     * 计算当前连续打卡天数（从今日或昨日往前推算）
     */
    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        var streak = 0
        var cursor = today
        val todayStr = today.toString()
        if (!checkedDates.contains(todayStr)) {
            cursor = today.minusDays(1)
        }
        while (checkedDates.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /**
     * 计算从创建日起到昨天为止漏打卡的天数
     */
    fun missedDaysCount(today: LocalDate = LocalDate.now()): Int {
        val start = runCatching { LocalDate.parse(startDate) }.getOrDefault(today)
        if (!start.isBefore(today)) return 0
        val totalPastDays = ChronoUnit.DAYS.between(start, today).toInt()
        var missed = 0
        for (i in 0 until totalPastDays) {
            val d = start.plusDays(i.toLong()).toString()
            if (!checkedDates.contains(d)) {
                missed++
            }
        }
        return missed
    }
}

/**
 * 打卡操作结果枚举（严格不支持补签）
 */
enum class CheckInResult {
    CHECKED_TODAY,          // 今日打卡成功
    ALREADY_CHECKED_TODAY,  // 今日已经打过卡
    PAST_DATE_FORBIDDEN,    // 过去日期禁止补签
    FUTURE_DATE_FORBIDDEN   // 未来日期禁止提前打卡
}

/**
 * 日历单元格状态枚举
 */
enum class DayStatus {
    EMPTY,            // 月初/月末空白占位格
    BEFORE_START,     // 习惯创建之前的历史日期
    PAST_CHECKED,     // 过去已打卡
    PAST_MISSED,      // 过去未打卡（漏卡，不可补签）
    TODAY_UNCHECKED,  // 今日待打卡
    TODAY_CHECKED,    // 今日已打卡
    FUTURE            // 未来日期
}

/**
 * 日历网格单项数据
 */
data class CalendarDayCell(
    val date: LocalDate?,
    val dayNumber: Int,
    val status: DayStatus
)
