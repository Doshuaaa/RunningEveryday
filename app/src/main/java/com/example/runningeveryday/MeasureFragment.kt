package com.example.runningeveryday

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.runningeveryday.databinding.FragmentMeasureBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

const val SELECT_DISTANCE = "Distance"

/**
 * A simple [Fragment] subclass.
 * Use the [MeasureFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MeasureFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var mainActivity: Context

    private var viewBinding: FragmentMeasureBinding? = null
    private val binding get() = viewBinding!!

    private val distanceArray = arrayOf("1.5km", "3.0km")
    private var selectPosition = 0

    private val mainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val measureReceiver = MeasureReceiver()

    private val fireStore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val calendar = Calendar.getInstance()

    //
    private var tempTime = 0
    //

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context
    }
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
        viewBinding = FragmentMeasureBinding.inflate(layoutInflater)

        binding.distanceSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, distanceArray)
        binding.distanceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                binding.maxDistanceTextView.text = distanceArray[position]
                selectPosition = position
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }

        }

        binding.measureStartButton.setOnClickListener {

            if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                sendCommandToForegroundService(MeasureState.START)
            } else {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("백그라운드 위치 정보 수집 권한 요청")
                    setMessage("운동 중 정확한 거리 측정을 위해 위치 엑세스 권한을 항상 허용으로 주세요")
                    setPositiveButton("권한 설정하러 가기", DialogInterface.OnClickListener { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${requireContext().packageName}"))
                        startActivity(intent)
                        //ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
                    })
                    setNegativeButton("거부하기", null)
                    show()
                }
            }

        }
        binding.measureStopButton.setOnClickListener {
            sendCommandToForegroundService(MeasureState.STOP)
            //record()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(!mainViewModel.isReceiverRegistered) {
            context?.registerReceiver(measureReceiver, IntentFilter(TIMER_ACTION), Context.RECEIVER_EXPORTED)
            mainViewModel.isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if(mainViewModel.isReceiverRegistered) {
            context?.unregisterReceiver(measureReceiver)
            mainViewModel.isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun sendCommandToForegroundService(state: MeasureState) {
        ContextCompat.startForegroundService(mainActivity.applicationContext, getServiceIntent(state))
    }

    private fun getServiceIntent(state: MeasureState) : Intent {
        return Intent(mainActivity.applicationContext, MeasureService::class.java).apply {
            putExtra(SERVICE_COMMAND, state)

            if(selectPosition == 0) {
                putExtra(SELECT_DISTANCE, 1500f)
            } else {
                putExtra(SELECT_DISTANCE, 3000f)
            }
        }
    }

    private fun updateTimeUi(time: Int) {
        binding.timeTextView.text = "$time"
        //sendCommandToForegroundService(TimerState.STOP)
    }

    private fun updateDistanceUi(distance: Float) {
        binding.currentDistanceTextView.text = String.format("%.2f", distance / 1000.0)
    }

    class MainViewModel: ViewModel() {
        var isReceiverRegistered: Boolean = false
        var isForegroundServiceRunning: Boolean = false
    }

    inner class MeasureReceiver: BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent) {

            if(intent.action == TIMER_ACTION) {
                val time = intent.getIntExtra(NOTIFICATION_TIME, 0)
                tempTime = time
                updateTimeUi(time)
            } else if (intent.action == DISTANCE_ACTION) {
                val distance = intent.getFloatExtra(NOTIFICATION_DISTANCE, 0f)
                updateDistanceUi(distance)
            }
        }
    }
    ///////
    private fun record() {
        val targetDistance =
            when(selectPosition) {
                0 -> 1500
                else -> 3000
        }

        val documentReference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("record")
                .document(SimpleDateFormat("YYYYMM", Locale.KOREA).format(calendar.time).toString())



        val timeData = hashMapOf(
            "time" to tempTime
        )

        documentReference.collection(calendar.get(Calendar.DAY_OF_MONTH).toString())
            .document(targetDistance.toString()).set(timeData as Map<String, Any>)

        val dateData = hashMapOf(
            calendar.get(Calendar.DAY_OF_MONTH).toString() to "ok"
        )
        documentReference.update(dateData as Map<String, Any>)
//        documentReference.get().addOnSuccessListener {  task ->
//
//            if(task.exists()) {
//                count = task.get("count") as Int
//            }
//        }


//        collectionReference
//            .collection("${calendar.get(Calendar.DAY_OF_MONTH)}")
//            .document(targetDistance.toString()).set(data)

    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MeasureFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MeasureFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}