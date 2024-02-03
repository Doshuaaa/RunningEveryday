package com.example.runningeveryday

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.runningeveryday.databinding.DialogCountDownBinding
import com.example.runningeveryday.databinding.FragmentMeasureBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

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

    private val distanceArray = arrayOf("1.50 km", "3.00 km")
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
    private var tempDistance = 0f
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
        if(!CheckNetwork.checkNetworkState(mainActivity)) {
            CheckNetwork.showNetworkLostDialog(binding.root)
            //loadingDialog.dismiss()
        }
        CheckNetwork.registerFragmentNetworkCallback(this,  binding.root)
        binding.distanceSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, distanceArray)
        binding.distanceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                binding.maxDistanceTextView.text = distanceArray[position]
                selectPosition = position
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }

        }

        when(MeasureService.targetDistance) {
            1500f ->  {
                binding.distanceSpinner.setSelection(0)
                binding.distanceSpinner.isEnabled = false
            }
            3000f -> {
                binding.distanceSpinner.setSelection(1)
                binding.distanceSpinner.isEnabled = false
            }
        }

        binding.measureStartButton.setOnClickListener {

            if(MeasureService.targetDistance == 0f) {
                if(ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    //mainViewModel.isForegroundServiceRunning = true
                    binding.distanceSpinner.isEnabled = false
                    //NotificationHelper.isRunning = true
                    CountDownDialog().show()
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
        }
        binding.measureStopButton.setOnClickListener {
            if(MeasureService.targetDistance != 0f) {
                val stopDlg = AlertDialog.Builder(requireContext())
                stopDlg.apply {
                    setMessage("측정하던 기록을 중단할까요?\n중단시 기록은 저장되지 않아요.")
                    setPositiveButton("중단히기", DialogInterface.OnClickListener { _, _ ->
                        //mainViewModel.isForegroundServiceRunning = false
                        binding.distanceSpinner.isEnabled = true
                        sendCommandToForegroundService(MeasureState.STOP)
                        record()
                    })
                    setNegativeButton("측정 계속하기",  null)
                }
                stopDlg.show()
            }

        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(!mainViewModel.isReceiverRegistered) {
            context?.registerReceiver(measureReceiver, IntentFilter(TIMER_ACTION), Context.RECEIVER_EXPORTED)
            context?.registerReceiver(measureReceiver, IntentFilter(DISTANCE_ACTION), Context.RECEIVER_EXPORTED)
            mainViewModel.isReceiverRegistered = true
        }

        binding.timeTextView.text = timeFormat(tempTime)
        binding.currentDistanceTextView.text = String.format("%.2f", tempDistance / 1000.0)
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
        binding.timeTextView.text = timeFormat(time)
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
            }
            else if (intent.action == DISTANCE_ACTION) {
                val distance = intent.getFloatExtra(NOTIFICATION_DISTANCE, 0f)
                tempDistance = distance
                updateDistanceUi(distance)
            }
        }
    }

    private fun timeFormat(time: Int) : String{

        val minute = time / 60
        val second = time % 60
        return String.format(Locale.KOREA, "%02d : %02d", minute, second)
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

        val dateData = hashMapOf(
            calendar.get(Calendar.DAY_OF_MONTH).toString() to "ok"
        )

        documentReference.get().addOnSuccessListener {
            if(it.data == null) {
                documentReference.set(dateData as Map<String, String>)
            } else {
                documentReference.update(dateData as Map<String, String>)
            }
        }

        val timeData = hashMapOf(
            "time" to tempTime
        )


        documentReference.collection(calendar.get(Calendar.DAY_OF_MONTH).toString())
            .document(targetDistance.toString()).set(timeData as Map<String, Any>)


        val top10Reference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("top10").document(targetDistance.toString())


        top10Reference.get().addOnSuccessListener { task ->

            val top10List : MutableList<MutableMap.MutableEntry<String, Any>> = try {
                task?.data?.entries?.sortedByDescending { it.value as Long }?.toMutableList()!!
            } catch (e: NullPointerException) {
                mutableListOf()
            }


            val timeFormat = SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA)


            if(top10List.size in 0..9) {
                if(top10List.size == 0) {
                    top10Reference.set(
                        hashMapOf(
                            timeFormat.format(calendar.time).toString() to tempTime
                        ) as Map<String, Any>
                    )
                }
                else {
                    top10Reference.update(
                        hashMapOf(
                            timeFormat.format(calendar.time).toString() to tempTime
                        ) as Map<String, Any>
                    )
                }
            }
            else {
                if (tempTime < top10List[0].value as Long) {
                    top10List.removeAt(0)
                    val map = top10List.associate { it.key to it.value }.toMutableMap()
                    map[timeFormat.format(calendar.time).toString()] = tempTime
                    top10Reference.update(map)
                }
            }
        }


        ///////

//        val documentReference =
//            fireStore.collection("users").document(auth.uid!!)
//                .collection("record")
//                .document(SimpleDateFormat("YYYYMM", Locale.KOREA).format(calendar.time).toString())
////// 추가될 내용
//        val top10Reference =
//            fireStore.collection("users").document(auth.uid!!)
//                .collection("top10").document(targetDistance.toString())
//
//
//        top10Reference.get().addOnSuccessListener { task ->
//
//            val top10List =
//                task?.data?.entries?.sortedByDescending { it.value as Long }?.toMutableList()!!
//
//            val timeFormat = SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA)
//
//            if(top10List.size in 0..9) {
//                top10Reference.update(
//                    hashMapOf(
//                        timeFormat.format(calendar.time).toString() to tempTime
//                    ) as Map<String, Any>
//                )
//            }
//            else {
//                if (tempTime < top10List[0].value as Long) {
//                    top10List.removeAt(0)
//                    val map = top10List.associate { it.key to it.value }.toMutableMap()
//                    map[timeFormat.format(calendar.time).toString()] = tempTime
//                    top10Reference.set(map)
//                }
//            }
//        }
//
//        //////
//        val timeData = hashMapOf(
//            "time" to tempTime
//        )
//
//        documentReference.collection(calendar.get(Calendar.DAY_OF_MONTH).toString())
//            .document(targetDistance.toString()).set(timeData as Map<String, Any>)
//
//        val dateData = hashMapOf(
//            calendar.get(Calendar.DAY_OF_MONTH).toString() to "ok"
//        )
//        documentReference.update(dateData as Map<String, Any>)


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

    inner class CountDownDialog : Dialog(requireContext()) {



        private var countDownDialogViewBinding: DialogCountDownBinding? = null
        private val countDownBinding get() = countDownDialogViewBinding!!
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            countDownDialogViewBinding = DialogCountDownBinding.inflate(layoutInflater)
            setContentView(countDownBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            countDown()
        }

        private fun countDown() {
             var countDown = 3
            val timer = Timer()
            val timerTask = object : TimerTask() {
                override fun run() {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        if(countDown <= 0) {
                            dismiss()
                            timer.cancel()
                            sendCommandToForegroundService(MeasureState.START)
                        }
                        countDownBinding.countDownTextView.text = countDown.toString()
                        countDown--
                    }
                }
            }
            timer.schedule(timerTask, 0, 1000)
        }
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