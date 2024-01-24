package com.example.runningeveryday

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.runningeveryday.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private var viewBinding: ActivitySettingBinding? = null
    private val binding get() = viewBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}