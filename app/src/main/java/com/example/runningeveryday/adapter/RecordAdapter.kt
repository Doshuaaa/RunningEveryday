package com.example.runningeveryday.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.runningeveryday.MainActivity
import com.example.runningeveryday.R
import com.example.runningeveryday.Record
import kotlin.time.Duration.Companion.seconds

class RecordAdapter(private val top10List: List<Pair<String, Any>>, private val distance: Int) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    private val record = Record()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numTextView: TextView = itemView.findViewById(R.id.record_num_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.record_date_text_view)
        val timeTextView: TextView = itemView.findViewById(R.id.record_time_text_view)
        val gradeTextView: TextView = itemView.findViewById(R.id.record_grade_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.view_holder_record, null)
        view.layoutParams = RecyclerView.LayoutParams(parent.width, 120)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return top10List.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            numTextView.text = "${position + 1}"
            dateTextView.text = top10List[position].first
            timeTextView.text = record.timeFormat(top10List[position].second as Long)
            gradeTextView.text = record.getGrade(MainActivity.sex, MainActivity.age, distance, top10List[position].second as Long).toString()
        }
    }

}