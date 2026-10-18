package com.example.habitcheckin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * 多习惯与打卡记录持久化管理仓库
 * 使用 SharedPreferences + JSON 存储，零外部依赖，支持主界面与桌面小组件实时同步。
 * 核心规则：仅允许打卡“今天”（LocalDate.now()），严格禁止任何过去日期的补签操作。
 */
class HabitRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "habit_check_in_prefs"
        private const val KEY_HABITS_JSON = "habits_json"
        private const val KEY_SELECTED_HABIT_ID = "selected_habit_id"
        private const val KEY_INITIALIZED = "initialized_default_habits"
    }

    init {
        ensureDefaultHabits()
    }

    /**
     * 首次安装时默认初始化示例习惯（如“每日戒糖 7天挑战”、“早睡打卡 21天”、“每天八杯水 7天”）
     */
    private fun ensureDefaultHabits() {
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return
        val today = LocalDate.now()
        val defaultHabits = listOf(
            Habit(
                id = "habit_sugar_free",
                name = "每日戒糖",
                emoji = "🍵",
                targetDays = 7,
                startDate = today.toString(),
                checkedDates = mutableSetOf()
            ),
            Habit(
                id = "habit_sleep_early",
                name = "23点前早睡",
                emoji = "🌙",
                targetDays = 21,
                startDate = today.toString(),
                checkedDates = mutableSetOf()
            ),
            Habit(
                id = "habit_workout",
                name = "有氧运动30分",
                emoji = "🏃",
                targetDays = 14,
                startDate = today.toString(),
                checkedDates = mutableSetOf()
            )
        )
        saveHabits(defaultHabits)
        prefs.edit()
            .putString(KEY_SELECTED_HABIT_ID, defaultHabits.first().id)
            .putBoolean(KEY_INITIALIZED, true)
            .apply()
    }

    /**
     * 获取所有习惯列表
     */
    fun getHabits(): MutableList<Habit> {
        val raw = prefs.getString(KEY_HABITS_JSON, null) ?: return mutableListOf()
        val list = mutableListOf<Habit>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val checkedSet = mutableSetOf<String>()
                val datesArray = obj.optJSONArray("checkedDates") ?: JSONArray()
                for (j in 0 until datesArray.length()) {
                    checkedSet.add(datesArray.getString(j))
                }
                list.add(
                    Habit(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        emoji = obj.optString("emoji", "🎯"),
                        targetDays = obj.optInt("targetDays", 7),
                        startDate = obj.optString("startDate", LocalDate.now().toString()),
                        checkedDates = checkedSet
                    )
                )
            }
        }
        return list
    }

    /**
     * 保存所有习惯列表
     */
    private fun saveHabits(habits: List<Habit>) {
        val array = JSONArray()
        for (h in habits) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("name", h.name)
            obj.put("emoji", h.emoji)
            obj.put("targetDays", h.targetDays)
            obj.put("startDate", h.startDate)
            val datesArr = JSONArray()
            h.checkedDates.sorted().forEach { datesArr.put(it) }
            obj.put("checkedDates", datesArr)
            array.put(obj)
        }
        prefs.edit().putString(KEY_HABITS_JSON, array.toString()).apply()
    }

    /**
     * 获取当前选中的习惯（小组件与主界面共用）
     */
    fun getSelectedHabit(): Habit? {
        val habits = getHabits()
        if (habits.isEmpty()) return null
        val selectedId = prefs.getString(KEY_SELECTED_HABIT_ID, null)
        return habits.find { it.id == selectedId } ?: habits.first().also {
            setSelectedHabitId(it.id)
        }
    }

    /**
     * 设置当前选中的习惯 ID
     */
    fun setSelectedHabitId(habitId: String) {
        prefs.edit().putString(KEY_SELECTED_HABIT_ID, habitId).apply()
    }

    /**
     * 切换到下一个习惯（供桌面小组件一键切换习惯使用）
     */
    fun switchToNextHabit(): Habit? {
        val habits = getHabits()
        if (habits.isEmpty()) return null
        val currentId = prefs.getString(KEY_SELECTED_HABIT_ID, null)
        val currentIndex = habits.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % habits.size
        val nextHabit = habits[nextIndex]
        setSelectedHabitId(nextHabit.id)
        return nextHabit
    }

    /**
     * 新增自定义习惯
     */
    fun addHabit(name: String, emoji: String, targetDays: Int): Habit {
        val habits = getHabits()
        val newHabit = Habit(
            id = "habit_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            emoji = emoji.ifBlank { "🎯" },
            targetDays = targetDays.coerceIn(1, 3650),
            startDate = LocalDate.now().toString(),
            checkedDates = mutableSetOf()
        )
        habits.add(newHabit)
        saveHabits(habits)
        setSelectedHabitId(newHabit.id)
        return newHabit
    }

    /**
     * 删除指定习惯
     */
    fun deleteHabit(habitId: String) {
        val habits = getHabits()
        habits.removeAll { it.id == habitId }
        saveHabits(habits)
        if (habits.isNotEmpty() && prefs.getString(KEY_SELECTED_HABIT_ID, null) == habitId) {
            setSelectedHabitId(habits.first().id)
        }
    }

    /**
     * 严格打卡方法：仅允许打卡今天（LocalDate.now()），严禁补签过去日期或提前打卡未来日期
     */
    fun attemptCheckIn(habitId: String, targetDate: LocalDate): CheckInResult {
        val today = LocalDate.now()
        if (targetDate.isBefore(today)) {
            return CheckInResult.PAST_DATE_FORBIDDEN
        }
        if (targetDate.isAfter(today)) {
            return CheckInResult.FUTURE_DATE_FORBIDDEN
        }

        val habits = getHabits()
        val habit = habits.find { it.id == habitId } ?: return CheckInResult.PAST_DATE_FORBIDDEN
        val todayStr = today.toString()

        if (habit.checkedDates.contains(todayStr)) {
            return CheckInResult.ALREADY_CHECKED_TODAY
        }

        habit.checkedDates.add(todayStr)
        saveHabits(habits)
        return CheckInResult.CHECKED_TODAY
    }
}
