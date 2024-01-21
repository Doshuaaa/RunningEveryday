package com.example.runningeveryday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.runningeveryday.databinding.ActivityMainBinding
import com.example.runningeveryday.databinding.DialogSexBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1001
    }
    private var viewBinding: ActivityMainBinding? = null
    private val binding get() = viewBinding!!

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    val fireStore = FirebaseFirestore.getInstance()
    val sexDocRef = fireStore.collection("users").document(FirebaseAuth.getInstance().uid.toString())
    .collection("information").document("sex")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkSex()
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if(it.all { permission -> permission.value }) {

            } else {

            }
        }
        if(!checkLocationPermission(permissions)) {
            activityResultLauncher.launch(permissions)
        }


        binding.apply {
            homeButton.setOnClickListener {
                setFragment(HomeFragment())
            }

            measureButton.setOnClickListener {
                setFragment(MeasureFragment())
            }

            statsButton.setOnClickListener{

            }
        }
        setFragment(HomeFragment())
    }

    private fun setFragment(fragment: Fragment) {

        supportFragmentManager.commit {
            replace(R.id.main_frame_layout, fragment)
            // setReorderingAllowed(true)
            addToBackStack("")
        }
    }

    private fun checkLocationPermission(permissions: Array<String>) : Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {

                } else {
                    Toast.makeText(this, "거부", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkSex() {
        sexDocRef.get().addOnSuccessListener { task ->
                if(!task.exists()) {
                    SexDialog().show()
                }
            }
    }

    inner class SexDialog : Dialog(this) {

        private var viewBinding: DialogSexBinding? = null
        private val binding get() = viewBinding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewBinding = DialogSexBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setCancelable(false)

            binding.sexConfirmButton.setOnClickListener {
                var sex = ""
                when(binding.sexRadioGroup.checkedRadioButtonId) {
                    R.id.man_radio_button -> sex = "남"
                    R.id.woman_radio_button -> sex = "여"
                }
                val sexData = hashMapOf(
                    "sex" to sex
                )
                sexDocRef.set(sexData)
                dismiss()
            }
        }
    }
}