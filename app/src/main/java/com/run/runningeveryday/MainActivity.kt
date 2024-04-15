package com.run.runningeveryday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.run.runningeveryday.databinding.ActivityMainBinding
import com.run.runningeveryday.databinding.DialogInformationBinding
import com.run.runningeveryday.fragment.HomeFragment
import com.run.runningeveryday.fragment.MeasureFragment
import com.run.runningeveryday.fragment.NeedSettingFragment
import com.run.runningeveryday.fragment.StatsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.run.runningeveryday.fragment.SettingFragment
import com.run.runningeveryday.service.MeasureService
import com.run.runningeveryday.state.MeasureState
import java.util.Calendar

object ServiceStateObj {
    var serviceState = MeasureState.STOP
}
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
                 setReorderingAllowed(true)
            }
        }
    }

    private val onBackPressedCallBack: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(ServiceStateObj.serviceState == MeasureState.START) {
                moveTaskToBack(true)
            } else {
                finish()
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

    private val homeDrawable: Drawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.baseline_home_24, null)!! }
    private val measureDrawable: Drawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.baseline_directions_run_24, null)!!  }
    private val statsDrawable: Drawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.baseline_query_stats_24, null)!!  }
    private val settingDrawable: Drawable by lazy { ResourcesCompat.getDrawable(resources, R.drawable.baseline_settings_24, null)!!  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        supportManager = supportFragmentManager

        setContentView(binding.root)
        //onBackPressedDispatcher.addCallback(onBackPressedCallBack)

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

                    setHomeFragmentTint("#03A9F4")
                    setMeasureFragmentTint("#666363")
                    setStatsFragmentTint("#666363")
                    setSettingFragmentTint("#666363")
                    setFragment(HomeFragment())
                }

                measureButton.setOnClickListener {

                    setHomeFragmentTint("#666363")
                    setMeasureFragmentTint("#03A9F4")
                    setStatsFragmentTint("#666363")
                    setSettingFragmentTint("#666363")
                    setFragment(MeasureFragment())
                }

                statsButton.setOnClickListener{
                    setHomeFragmentTint("#666363")
                    setMeasureFragmentTint("#666363")
                    setStatsFragmentTint("#03A9F4")
                    setSettingFragmentTint("#666363")
                    setFragment(StatsFragment())
                }

                settingButton.setOnClickListener{
                    setHomeFragmentTint("#666363")
                    setMeasureFragmentTint("#666363")
                    setStatsFragmentTint("#666363")
                    setSettingFragmentTint("#03A9F4")
                    setFragment(SettingFragment())
                }
            }
            setHomeFragmentTint("#03A9F4")
            setFragment(HomeFragment())
        }
    }

    private fun setHomeFragmentTint(color: String) {
        DrawableCompat.setTint(homeDrawable, Color.parseColor(color))
        binding.homeImageAndText.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, homeDrawable, null, null)
            setTextColor(Color.parseColor(color))
        }
    }

    private fun setMeasureFragmentTint(color: String) {
        DrawableCompat.setTint(measureDrawable, Color.parseColor(color))
        binding.measureImageAndText.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, measureDrawable, null, null)
            setTextColor(Color.parseColor(color))
        }
    }

    private fun setSettingFragmentTint(color: String) {
        DrawableCompat.setTint(settingDrawable, Color.parseColor(color))
        binding.settingImageText.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, settingDrawable, null, null)
            setTextColor(Color.parseColor(color))
        }
    }

    override fun onResume() {
        super.onResume()
        if(NotificationHelper.vibrator != null) {
            NotificationHelper.vibrator?.cancel()
        }
    }

    private fun checkLocationPermission(permissions: Array<String>) : Boolean {

        for(permission in permissions) {
            if(permission != Manifest.permission.POST_NOTIFICATIONS) {
                if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                    return false
                }
            }
        }
        return true
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

    private fun setStatsFragmentTint(color: String) {
        DrawableCompat.setTint(statsDrawable, Color.parseColor(color))
        binding.statsImageText.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, statsDrawable, null, null)
            setTextColor(Color.parseColor(color))
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