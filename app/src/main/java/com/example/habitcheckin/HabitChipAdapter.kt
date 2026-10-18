package com.example.habitcheckin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 顶部多习惯横向切换栏适配器
 * 点击切换当前查看/打卡的习惯，长按可删除习惯
 */
class HabitChipAdapter(
    private var habits: List<Habit>,
    private var selectedHabitId: String?,
    private val onSelectHabit: (Habit) -> Unit,
    private val onLongClickHabit: (Habit) -> Unit
) : RecyclerView.Adapter<HabitChipAdapter.ChipViewHolder>() {

    fun updateData(newHabits: List<Habit>, newSelectedId: String?) {
        this.habits = newHabits
        this.selectedHabitId = newSelectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val habit = habits[position]
        val isSelected = habit.id == selectedHabitId
        holder.bind(habit, isSelected, onSelectHabit, onLongClickHabit)
    }

    override fun getItemCount(): Int = habits.size

    class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.chipContainer)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvChipEmoji)
        private val tvName: TextView = itemView.findViewById(R.id.tvChipName)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvChipProgress)

        fun bind(
            habit: Habit,
            isSelected: Boolean,
            onSelect: (Habit) -> Unit,
            onLongClick: (Habit) -> Unit
        ) {
            tvEmoji.text = habit.emoji
            tvName.text = habit.name
            tvProgress.text = "${habit.totalCheckedCount()}/${habit.targetDays}天"

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_chip_selected)
                tvName.setTextColor(Color.WHITE)
                tvProgress.setTextColor(Color.parseColor("#E0F2F1"))
            } else {
                container.setBackgroundResource(R.drawable.bg_chip_unselected)
                tvName.setTextColor(Color.parseColor("#1E293B"))
                tvProgress.setTextColor(Color.parseColor("#64748B"))
            }

            itemView.setOnClickListener { onSelect(habit) }
            itemView.setOnLongClickListener {
                onLongClick(habit)
                true
            }
        }
    }
}
