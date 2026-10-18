package com.example.habitcheckin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 主界面：
 * 1. 支持多习惯切换、新增习惯（如“每日戒糖 7天”）、长按删除习惯
 * 2. 展示当前习惯的目标周期进度条、累计打卡天数、连续打卡天数、漏卡天数
 * 3. 展示完整月历视图，仅支持今日打卡，严格禁止补签过去日期
 * 4. 打卡或切换习惯后自动同步刷新桌面小组件（HabitWidgetProvider）
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repository: HabitRepository
    private lateinit var chipAdapter: HabitChipAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    private var currentYearMonth: YearMonth = YearMonth.now()

    private lateinit var rvHabitChips: RecyclerView
    private lateinit var btnAddHabit: Button
    private lateinit var tvCurrentHabitTitle: TextView
    private lateinit var tvGoalBadge: TextView
    private lateinit var progressGoal: ProgressBar
    private lateinit var tvStatProgress: TextView
    private lateinit var tvStatStreak: TextView
    private lateinit var tvStatMissed: TextView
    private lateinit var btnCheckInToday: Button
    private lateinit var tvRuleNotice: TextView

    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvMonthTitle: TextView
    private lateinit var rvCalendar: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = HabitRepository(this)
        bindViews()
        setupAdapters()
        setupListeners()
        refreshAllUI()
    }

    override fun onResume() {
        super.onResume()
        // 从桌面小组件打卡后返回应用时同步最新状态
        refreshAllUI()
    }

    private fun bindViews() {
        rvHabitChips = findViewById(R.id.rvHabitChips)
        btnAddHabit = findViewById(R.id.btnAddHabit)
        tvCurrentHabitTitle = findViewById(R.id.tvCurrentHabitTitle)
        tvGoalBadge = findViewById(R.id.tvGoalBadge)
        progressGoal = findViewById(R.id.progressGoal)
        tvStatProgress = findViewById(R.id.tvStatProgress)
        tvStatStreak = findViewById(R.id.tvStatStreak)
        tvStatMissed = findViewById(R.id.tvStatMissed)
        btnCheckInToday = findViewById(R.id.btnCheckInToday)
        tvRuleNotice = findViewById(R.id.tvRuleNotice)

        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvMonthTitle = findViewById(R.id.tvMonthTitle)
        rvCalendar = findViewById(R.id.rvCalendar)
    }

    private fun setupAdapters() {
        chipAdapter = HabitChipAdapter(
            habits = emptyList(),
            selectedHabitId = null,
            onSelectHabit = { habit ->
                repository.setSelectedHabitId(habit.id)
                refreshAllUI()
                HabitWidgetProvider.updateAllWidgets(this)
            },
            onLongClickHabit = { habit ->
                showDeleteHabitDialog(habit)
            }
        )
        rvHabitChips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvHabitChips.adapter = chipAdapter

        calendarAdapter = CalendarAdapter(emptyList()) { cell ->
            handleCalendarDayClick(cell)
        }
        rvCalendar.layoutManager = GridLayoutManager(this, 7)
        rvCalendar.adapter = calendarAdapter
    }

    private fun setupListeners() {
        btnAddHabit.setOnClickListener {
            showAddHabitDialog()
        }

        btnCheckInToday.setOnClickListener {
            val selected = repository.getSelectedHabit() ?: return@setOnClickListener
            performCheckIn(selected.id, LocalDate.now())
        }

        btnPrevMonth.setOnClickListener {
            currentYearMonth = currentYearMonth.minusMonths(1)
            refreshCalendarGrid()
        }

        btnNextMonth.setOnClickListener {
            currentYearMonth = currentYearMonth.plusMonths(1)
            refreshCalendarGrid()
        }
    }

    private fun performCheckIn(habitId: String, date: LocalDate) {
        when (repository.attemptCheckIn(habitId, date)) {
            CheckInResult.CHECKED_TODAY -> {
                Toast.makeText(this, "🎉 今日打卡成功！继续保持！", Toast.LENGTH_SHORT).show()
                refreshAllUI()
                HabitWidgetProvider.updateAllWidgets(this)
            }
            CheckInResult.ALREADY_CHECKED_TODAY -> {
                Toast.makeText(this, "今天已经完成打卡啦，明天继续加油！", Toast.LENGTH_SHORT).show()
            }
            CheckInResult.PAST_DATE_FORBIDDEN -> {
                Toast.makeText(this, "⚠️ 本程序不支持补签，仅可打卡当天！", Toast.LENGTH_SHORT).show()
            }
            CheckInResult.FUTURE_DATE_FORBIDDEN -> {
                Toast.makeText(this, "未来日期尚未到来，请当日再来打卡", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCalendarDayClick(cell: CalendarDayCell) {
        val clickedDate = cell.date ?: return
        val selected = repository.getSelectedHabit() ?: return
        performCheckIn(selected.id, clickedDate)
    }

    private fun refreshAllUI() {
        val habits = repository.getHabits()
        val selected = repository.getSelectedHabit()
        chipAdapter.updateData(habits, selected?.id)

        if (selected == null) {
            tvCurrentHabitTitle.text = "暂无习惯，请先点击右上角新增"
            tvGoalBadge.text = "无目标"
            progressGoal.progress = 0
            tvStatProgress.text = "0/0 天"
            tvStatStreak.text = "0 天"
            tvStatMissed.text = "0 天"
            btnCheckInToday.isEnabled = false
            btnCheckInToday.text = "请先添加习惯"
            calendarAdapter.submitList(emptyList())
            return
        }

        val today = LocalDate.now()
        val checkedToday = selected.isChecked(today.toString())
        val totalChecked = selected.totalCheckedCount()
        val percent = selected.progressPercent()

        tvCurrentHabitTitle.text = "${selected.emoji} ${selected.name}"
        tvGoalBadge.text = if (selected.isGoalCompleted()) {
            "🏆 已完成 ${selected.targetDays} 天目标"
        } else {
            "目标周期：${selected.targetDays} 天（${percent}%）"
        }

        progressGoal.max = 100
        progressGoal.progress = percent

        tvStatProgress.text = "${totalChecked} / ${selected.targetDays} 天"
        tvStatStreak.text = "${selected.currentStreak(today)} 天"
        tvStatMissed.text = "${selected.missedDaysCount(today)} 天"

        if (checkedToday) {
            btnCheckInToday.text = "✓ 今日已打卡（已完成 ${totalChecked}/${selected.targetDays} 天）"
            btnCheckInToday.isEnabled = false
            btnCheckInToday.alpha = 0.85f
        } else {
            btnCheckInToday.text = "立即打卡 · 完成今日「${selected.name}」"
            btnCheckInToday.isEnabled = true
            btnCheckInToday.alpha = 1.0f
        }

        tvRuleNotice.text = "严格打卡模式：仅限今日（${today}）打卡，过去漏卡日期不支持补签"
        refreshCalendarGrid()
    }

    private fun refreshCalendarGrid() {
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月")
        tvMonthTitle.text = currentYearMonth.format(formatter)

        val selected = repository.getSelectedHabit() ?: run {
            calendarAdapter.submitList(emptyList())
            return
        }

        val today = LocalDate.now()
        val habitStart = runCatching { LocalDate.parse(selected.startDate) }.getOrDefault(today)
        val firstDayOfMonth = currentYearMonth.atDay(1)
        val daysInMonth = currentYearMonth.lengthOfMonth()

        // 周日为第 0 列：Sunday -> 0, Monday -> 1 ... Saturday -> 6
        val leadingEmptyCount = firstDayOfMonth.dayOfWeek.value % 7
        val cells = mutableListOf<CalendarDayCell>()

        for (i in 0 until leadingEmptyCount) {
            cells.add(CalendarDayCell(date = null, dayNumber = 0, status = DayStatus.EMPTY))
        }

        for (day in 1..daysInMonth) {
            val date = currentYearMonth.atDay(day)
            val dateStr = date.toString()
            val isChecked = selected.isChecked(dateStr)

            val status = when {
                date.isEqual(today) -> if (isChecked) DayStatus.TODAY_CHECKED else DayStatus.TODAY_UNCHECKED
                date.isBefore(today) -> {
                    if (isChecked) {
                        DayStatus.PAST_CHECKED
                    } else if (date.isBefore(habitStart)) {
                        DayStatus.BEFORE_START
                    } else {
                        DayStatus.PAST_MISSED
                    }
                }
                else -> DayStatus.FUTURE
            }

            cells.add(CalendarDayCell(date = date, dayNumber = day, status = status))
        }

        calendarAdapter.submitList(cells)
    }

    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_habit, null)
        val etHabitName = dialogView.findViewById<EditText>(R.id.etHabitName)
        val etHabitEmoji = dialogView.findViewById<EditText>(R.id.etHabitEmoji)
        val etTargetDays = dialogView.findViewById<EditText>(R.id.etTargetDays)

        val btnPreset7 = dialogView.findViewById<Button>(R.id.btnPreset7)
        val btnPreset14 = dialogView.findViewById<Button>(R.id.btnPreset14)
        val btnPreset21 = dialogView.findViewById<Button>(R.id.btnPreset21)
        val btnPreset30 = dialogView.findViewById<Button>(R.id.btnPreset30)

        btnPreset7.setOnClickListener { etTargetDays.setText("7") }
        btnPreset14.setOnClickListener { etTargetDays.setText("14") }
        btnPreset21.setOnClickListener { etTargetDays.setText("21") }
        btnPreset30.setOnClickListener { etTargetDays.setText("30") }

        AlertDialog.Builder(this)
            .setTitle("新增打卡习惯")
            .setView(dialogView)
            .setPositiveButton("创建习惯") { _, _ ->
                val name = etHabitName.text.toString().trim()
                val emoji = etHabitEmoji.text.toString().trim().ifEmpty { "🍵" }
                val targetDays = etTargetDays.text.toString().trim().toIntOrNull() ?: 7

                if (name.isEmpty()) {
                    Toast.makeText(this, "请输入习惯名称（如：每日戒糖）", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                repository.addHabit(name, emoji, targetDays)
                refreshAllUI()
                HabitWidgetProvider.updateAllWidgets(this)
                Toast.makeText(this, "已创建「$name」($targetDays 天挑战)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteHabitDialog(habit: Habit) {
        AlertDialog.Builder(this)
            .setTitle("删除习惯")
            .setMessage("确定要删除「${habit.emoji} ${habit.name}」及其所有打卡记录吗？")
            .setPositiveButton("删除") { _, _ ->
                repository.deleteHabit(habit.id)
                refreshAllUI()
                HabitWidgetProvider.updateAllWidgets(this)
                Toast.makeText(this, "已删除「${habit.name}」", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
