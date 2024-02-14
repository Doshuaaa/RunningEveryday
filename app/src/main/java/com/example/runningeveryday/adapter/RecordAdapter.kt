package com.example.runningeveryday.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.runningeveryday.MainActivity
import com.example.runningeveryday.R
import com.example.runningeveryday.model.Record

class RecordAdapter(private val top10List: List<Pair<String, Any>>, private val distance: Int) : RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    private val record = Record()
    private lateinit var mContext: Context
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numTextView: TextView = itemView.findViewById(R.id.record_num_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.record_date_text_view)
        val timeTextView: TextView = itemView.findViewById(R.id.record_time_text_view)
        val gradeTextView: TextView = itemView.findViewById(R.id.record_grade_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context
        val view = View.inflate(mContext, R.layout.view_holder_record, null)
        view.layoutParams = RecyclerView.LayoutParams(parent.width, 120)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return top10List.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            numTextView.text = mContext.getString(R.string.record_num, position + 1)
            dateTextView.text = top10List[position].first
            timeTextView.text = record.timeFormat(top10List[position].second as Long)
            when(val grade = record.getGrade(MainActivity.sex, MainActivity.age, distance, top10List[position].second as Long).toString()) {
                "0" -> gradeTextView.text = "특급"
                "4" -> gradeTextView.text = "불합격"
                else -> gradeTextView.text = mContext.getString(R.string.measure_grade, grade)
            }
        }
    }
}