package com.example.runningeveryday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.runningeveryday.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 1001
    }
    private val activity = this
    private var viewBinding: ActivityMainBinding? = null
    private val binding get() = viewBinding!!

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )


    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}