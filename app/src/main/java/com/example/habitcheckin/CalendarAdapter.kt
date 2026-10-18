package com.example.habitcheckin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 月历打卡网格适配器
 * 直观展示每日状态：已打卡(✓)、漏卡(×，不可补签)、今日待打卡(今)、未来日期
 */
class CalendarAdapter(
    private var days: List<CalendarDayCell>,
    private val onDayClick: (CalendarDayCell) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    fun submitList(newDays: List<CalendarDayCell>) {
        this.days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], onDayClick)
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cellRoot: LinearLayout = itemView.findViewById(R.id.dayCellRoot)
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val tvDayBadge: TextView = itemView.findViewById(R.id.tvDayBadge)

        fun bind(cell: CalendarDayCell, onDayClick: (CalendarDayCell) -> Unit) {
            if (cell.status == DayStatus.EMPTY || cell.date == null) {
                cellRoot.visibility = View.INVISIBLE
                cellRoot.setOnClickListener(null)
                return
            }

            cellRoot.visibility = View.VISIBLE
            tvDayNumber.text = cell.dayNumber.toString()

            when (cell.status) {
                DayStatus.TODAY_CHECKED -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_checked)
                    tvDayNumber.setTextColor(Color.WHITE)
                    tvDayBadge.text = "今日✓"
                    tvDayBadge.setTextColor(Color.WHITE)
                }
                DayStatus.PAST_CHECKED -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_checked)
                    tvDayNumber.setTextColor(Color.WHITE)
                    tvDayBadge.text = "✓"
                    tvDayBadge.setTextColor(Color.WHITE)
                }
                DayStatus.TODAY_UNCHECKED -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_today_unchecked)
                    tvDayNumber.setTextColor(Color.parseColor("#0D9488"))
                    tvDayBadge.text = "可打卡"
                    tvDayBadge.setTextColor(Color.parseColor("#0D9488"))
                }
                DayStatus.PAST_MISSED -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_missed)
                    tvDayNumber.setTextColor(Color.parseColor("#94A3B8"))
                    tvDayBadge.text = "未签"
                    tvDayBadge.setTextColor(Color.parseColor("#EF4444"))
                }
                DayStatus.BEFORE_START -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_neutral)
                    tvDayNumber.setTextColor(Color.parseColor("#CBD5E1"))
                    tvDayBadge.text = "-"
                    tvDayBadge.setTextColor(Color.parseColor("#CBD5E1"))
                }
                DayStatus.FUTURE -> {
                    cellRoot.setBackgroundResource(R.drawable.bg_day_neutral)
                    tvDayNumber.setTextColor(Color.parseColor("#94A3B8"))
                    tvDayBadge.text = ""
                }
                DayStatus.EMPTY -> Unit
            }

            cellRoot.setOnClickListener {
                onDayClick(cell)
            }
        }
    }
}
