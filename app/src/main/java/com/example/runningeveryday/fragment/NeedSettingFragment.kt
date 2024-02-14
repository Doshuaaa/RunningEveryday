package com.example.runningeveryday.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.runningeveryday.MainActivity
import com.example.runningeveryday.R
import com.example.runningeveryday.databinding.FragmentNeedSettingBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NeedSettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NeedSettingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var viewBinding: FragmentNeedSettingBinding? = null
    private val binding get() = viewBinding!!

    private var locationPermission = false
    private var gpsActive = false

    private val locationManager by lazy { requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentNeedSettingBinding.inflate(layoutInflater)

        binding.goToSetPermissionButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${requireContext().packageName}"))
            startActivity(intent)
        }

        binding.goToActivateGpsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        locationPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        gpsActive = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        initView()

    }

    private fun initView() {

        if(locationPermission && gpsActive) {
            goTo()
        }

        if(locationPermission) {
            binding.backgroundLocationPermissionImageView.setImageResource(R.drawable.baseline_check_24)
            binding.goToSetPermissionButton.visibility = View.GONE
        } else {
            binding.backgroundLocationPermissionImageView.setImageResource(R.drawable.baseline_cancel_24)
            binding.goToSetPermissionButton.visibility = View.VISIBLE
        }

        if(gpsActive) {
            binding.gpsPermissionImageView.setImageResource(R.drawable.baseline_check_24)
            binding.goToActivateGpsButton.visibility = View.GONE
        } else {
            binding.gpsPermissionImageView.setImageResource(R.drawable.baseline_cancel_24)
            binding.goToActivateGpsButton.visibility = View.VISIBLE
        }
    }

    private fun goTo() {

        val intent = Intent(activity, MainActivity::class.java)
        activity?.finish()
        startActivity(intent)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment NeedSettingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            NeedSettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}