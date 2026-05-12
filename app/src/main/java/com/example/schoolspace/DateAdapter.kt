package com.example.schoolspace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DateAdapter(
    private val dates: List<Date>,
    private val onDateSelected: (Date) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = -1

    init {
        // Zaznacz dzisiejszą datę domyślnie
        val today = Calendar.getInstance()
        selectedPosition = dates.indexOfFirst { 
            val cal = Calendar.getInstance().apply { time = it }
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        }
    }

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDayName: TextView = view.findViewById(R.id.txtDayName)
        val txtDayNumber: TextView = view.findViewById(R.id.txtDayNumber)
        val indicator: View = view.findViewById(R.id.indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        val cal = Calendar.getInstance().apply { time = date }
        
        holder.txtDayName.text = SimpleDateFormat("EEE", Locale("pl")).format(date)
        holder.txtDayNumber.text = cal.get(Calendar.DAY_OF_MONTH).toString()

        val isSelected = position == selectedPosition
        holder.indicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        
        val color = if (isSelected) R.color.primary else R.color.text_primary
        holder.txtDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, color))
        holder.txtDayName.alpha = if (isSelected) 1.0f else 0.6f

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onDateSelected(date)
        }
    }

    override fun getItemCount() = dates.size
}
