package com.example.runningeveryday.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.runningeveryday.R
import java.util.Date

class DayAdapter(val tempMonth: Int, val dayList: MutableList<Date>, val context: Context) : RecyclerView.Adapter<DayAdapter.ViewHolder>() {

    class ViewHolder(val layout: View) : RecyclerView.ViewHolder(layout) {
        val dayTextView: TextView = layout.findViewById(R.id.day_of_week_textView)
        val gradeTextView: AppCompatTextView = layout.findViewById(R.id.check_grade_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.view_holder_day, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 42
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.dayTextView.text = dayList[position].date.toString()

        if(tempMonth != dayList[position].month) {
            holder.dayTextView.alpha=0.4f
        }

        if(position % 7 == 6) {
            holder.dayTextView.setTextColor(ContextCompat.getColor(holder.layout.context, R.color.blue))
        } else if ( position % 7 == 0) {
            holder.dayTextView.setTextColor(ContextCompat.getColor(holder.layout.context, R.color.red))
        }
    }


}