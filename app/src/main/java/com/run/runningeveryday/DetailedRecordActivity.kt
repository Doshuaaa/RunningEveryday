package com.run.runningeveryday

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.run.runningeveryday.databinding.ActivityDetailedRecordBinding
import com.run.runningeveryday.model.Record
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetailedRecordActivity : AppCompatActivity() {

    private var viewBinding: ActivityDetailedRecordBinding? = null
    private val binding get() = viewBinding!!
    private lateinit var calendar: Calendar
    private val recordCollectionRef by lazy {
        FirebaseFirestore.getInstance().collection("users")
            .document(FirebaseAuth.getInstance().uid!!).collection("record")
            .document(SimpleDateFormat("yyyyMM", Locale.KOREA).format(calendar.time))
            .collection(calendar.get(Calendar.DAY_OF_MONTH).toString())
    }
    private val record = Record()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityDetailedRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendar = Calendar.getInstance()
        val date = Date()
        date.time = intent.getLongExtra("date", 0)
        calendar.time = date

        binding.detailedDateTextView.text =
            SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA).format(calendar.time)

        recordCollectionRef.get().addOnSuccessListener {

            for (document in it.documents) {

                when (document.id) {

                    "1500" -> {
                        set1500View(document.get("time") as Long)
                    }

                    "3000" -> {
                        set3000View(document.get("time") as Long)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(NotificationHelper.vibrator != null) {
            NotificationHelper.vibrator?.cancel()
        }
    }

    private fun set1500View(time: Long) {

        binding.detailed1500TimeTextView.text = record.timeFormat(time)

        binding.detailed1500GradeImageView.apply {
            when(record.getGrade(MainActivity.sex, MainActivity.age, 1500, time)) {

                1 -> {
                    setImageResource(R.drawable.first)
                    binding.detailed1500GradeTextView.text = "1급"
                }
                2 -> {
                    setImageResource(R.drawable.second)
                    binding.detailed1500GradeTextView.text = "2급"
                }
                3 -> {
                    setImageResource(R.drawable.third)
                    binding.detailed1500GradeTextView.text = "3급"
                }
                4 -> {
                    setImageResource(R.drawable.ore)
                    binding.detailed1500GradeTextView.text = "노력이 필요해"
                }
            }
        }

        binding.detailed1500LinearLayout.visibility = View.VISIBLE
    }

    private fun set3000View(time: Long) {

        binding.detailed3000TimeTextView.text = record.timeFormat(time)

        binding.detailed3000GradeImageView.apply {
            when(record.getGrade(MainActivity.sex, MainActivity.age, 3000, time)) {

                0 ->  {
                    setImageResource(R.drawable.diamond)
                    binding.detailed3000GradeTextView.text = "특급"
                }
                1 -> {
                    setImageResource(R.drawable.first)
                    binding.detailed3000GradeTextView.text = "1급"
                }
                2 -> {
                    setImageResource(R.drawable.second)
                    binding.detailed3000GradeTextView.text = "2급"
                }
                3 -> {
                    setImageResource(R.drawable.third)
                    binding.detailed3000GradeTextView.text = "3급"
                }
                4 -> {
                    setImageResource(R.drawable.ore)
                    binding.detailed3000GradeTextView.text = "노력이 필요해"
                }
            }
        }

        binding.detailed3000LinearLayout.visibility = View.VISIBLE
    }

}