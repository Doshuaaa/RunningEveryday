package com.run.runningeveryday.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.bumptech.glide.Glide
import com.run.runningeveryday.R
import com.run.runningeveryday.databinding.DialogProgressBinding

class LoadingDialog(private val context: Context) : Dialog(context) {

    private var viewBinding: DialogProgressBinding? = null
    private val binding get() = viewBinding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = DialogProgressBinding.inflate(layoutInflater)
        Glide.with(context).load(R.raw.load_32_128).into(binding.loadingImageView)
        setContentView(binding.root)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}