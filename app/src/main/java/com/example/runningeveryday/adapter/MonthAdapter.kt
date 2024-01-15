package com.example.runningeveryday.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.runningeveryday.R
import java.util.Calendar
import java.util.Date

class MonthAdapter(val context: Context) : RecyclerView.Adapter<MonthAdapter.ViewHolder>() {

    private val calendar = Calendar.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val calRecyclerView: RecyclerView = itemView.findViewById(R.id.month_adapter_recycler_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.view_holder_month, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 12
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        calendar.time = Date()
        calendar.add(Calendar.MONTH, 11 - position)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val tempMonth = calendar.get(android.icu.util.Calendar.MONTH)

        val dayList: MutableList<Date> = MutableList(6 * 7) {Date()}

        for(i in 0..5) {
            for(j in 0..6) {

                calendar.add(Calendar.DAY_OF_MONTH, (1 - calendar.get(Calendar.DAY_OF_WEEK)) + j)
                dayList[i * 7 + j] = calendar.time
            }
            calendar.add(Calendar.WEEK_OF_MONTH, 1)
        }

        holder.calRecyclerView.layoutManager = GridLayoutManager(context, 7)
        //GridLayoutManager()
        holder.calRecyclerView.adapter = DayAdapter(tempMonth, dayList, context)
    }
}