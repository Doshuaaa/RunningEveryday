package com.run.runningeveryday.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.run.runningeveryday.R
import com.run.runningeveryday.model.Record
import com.run.runningeveryday.model.StandardRecord

class StandardAdapter(private val list: ArrayList<StandardRecord>) : RecyclerView.Adapter<StandardAdapter.ViewHolder>() {

    val record = Record()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val standardGradeTextView: TextView = itemView.findViewById(R.id.standard_grade_text_view)
        val standardStartTextView: TextView = itemView.findViewById(R.id.standard_start_range_text_view)
        val standardEndTextView: TextView = itemView.findViewById(R.id.standard_end_range_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_standard, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size + 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if(position < list.size) {

            holder.standardGradeTextView.apply {
                text = when(list[position].grade) {

                    0 -> "특급"
                    1 -> "1급"
                    2 -> "2급"
                    3 -> "3급"
                    else -> "불합격"
                }
            }

            holder.standardStartTextView.apply {
                when(list[position].startRange) {
                    0 -> {
                        visibility = View.INVISIBLE
                    }
                    else -> text = record.timeFormat(list[position].startRange.toLong())
                }
            }

            holder.standardEndTextView.text = record.timeFormat(list[position].endRange.toLong())
        }

        else {
            holder.apply {
                standardGradeTextView.text = "불합격"
                standardStartTextView.text = record.timeFormat(list[position - 1].endRange.toLong() + 1)
                standardEndTextView.visibility = View.INVISIBLE
            }
        }
    }
}