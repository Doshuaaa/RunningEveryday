package com.example.runningeveryday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.example.runningeveryday.databinding.ActivityMainBinding
import com.example.runningeveryday.databinding.DialogInformationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    companion object {
        var mainActivity: MainActivity? = null

        const val REQUEST_CODE_PERMISSIONS = 1001
        lateinit var sex: String
        var age = 0L

        private lateinit var supportManager: FragmentManager
        fun setFragment(fragment: Fragment) {

            supportManager.commit {
                replace(R.id.main_frame_layout, fragment)
                // setReorderingAllowed(true)
            }
        }
    }

    private var viewBinding: ActivityMainBinding? = null
    private val binding get() = viewBinding!!

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    private val fireStore = FirebaseFirestore.getInstance()
    val informationRef = fireStore.collection("users").document(FirebaseAuth.getInstance().uid.toString())
    .collection("information")
    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        supportManager = supportFragmentManager

        setContentView(binding.root)
        mainActivity = this
        checkRegisterInformation()
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if(it.all { permission -> permission.value }) {

            } else {

            }
        }
        if(!checkLocationPermission(permissions)) {
            setFragment(NeedSettingFragment())
            activityResultLauncher.launch(permissions)
        } else if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setFragment(NeedSettingFragment())
            }
        else {
            binding.apply {
                homeButton.setOnClickListener {
                    setFragment(HomeFragment())
                }

                measureButton.setOnClickListener {
                    setFragment(MeasureFragment())
                }

                statsButton.setOnClickListener{
                    setFragment(StatsFragment())
                }
            }
            setFragment(HomeFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        if(NotificationHelper.vibrator != null) {
            NotificationHelper.vibrator?.cancel()
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

    private fun checkRegisterInformation() {
        informationRef.document("information").get().addOnSuccessListener { task ->
                if(!task.exists()) {
                    InformationDialog().show()
                } else {
                    age = task.get("age") as Long
                    sex = task.get("sex") as String
                }
            }
    }

    inner class InformationDialog : Dialog(this) {

        private var viewBinding: DialogInformationBinding? = null
        private val binding get() = viewBinding!!
        private val calendar = Calendar.getInstance()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            this.window?.setBackgroundDrawableResource(R.drawable.round_dialog)

            viewBinding = DialogInformationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.yearOfBirthNumberPicker.apply {
                maxValue = calendar.get(Calendar.YEAR) - 10
                minValue = calendar.get(Calendar.YEAR) - 20
                minValue = calendar.get(Calendar.YEAR) - 100
                wrapSelectorWheel = false
            }

            setCancelable(false)

            binding.sexConfirmButton.setOnClickListener {
                var inputSex = ""
                val inputAge = calendar.get(Calendar.YEAR) - binding.yearOfBirthNumberPicker.value
                when(binding.sexRadioGroup.checkedRadioButtonId) {
                    R.id.man_radio_button -> inputSex = "남"
                    R.id.woman_radio_button -> inputSex = "여"
                }
                val informationData : HashMap<String, Any> = hashMapOf(
                    "sex" to inputSex,
                    "age" to inputAge
                )
                sex = inputSex
                age = inputAge.toLong()
                informationRef.document("information").set(informationData)

                dismiss()
            }
        }
    }
}