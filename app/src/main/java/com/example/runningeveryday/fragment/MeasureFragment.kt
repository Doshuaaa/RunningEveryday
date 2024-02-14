package com.example.runningeveryday.fragment

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.runningeveryday.CheckNetwork
import com.example.runningeveryday.service.DISTANCE_ACTION
import com.example.runningeveryday.service.MeasureService
import com.example.runningeveryday.service.NOTIFICATION_DISTANCE
import com.example.runningeveryday.service.NOTIFICATION_TIME
import com.example.runningeveryday.model.Record
import com.example.runningeveryday.service.SERVICE_COMMAND
import com.example.runningeveryday.service.TIMER_ACTION
import com.example.runningeveryday.databinding.DialogCountDownBinding
import com.example.runningeveryday.databinding.FragmentMeasureBinding
import com.example.runningeveryday.state.MeasureState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.Calendar
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


    override fun onAttach(context: Context) {
        super.onAttach(context)

        mContext = context
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
        if(!CheckNetwork.checkNetworkState(mContext)) {
            CheckNetwork.showNetworkLostDialog(binding.root)
        }
        CheckNetwork.registerFragmentNetworkCallback(this, binding.root)

        isExistTS()

        distanceSpinner = binding.distanceSpinner
        distanceSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, distanceArray)
        distanceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                binding.maxDistanceTextView.text = distanceArray[position]
                selectPosition = position
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }

        }

        when(MeasureService.targetDistance) {
            1500f ->  {
                distanceSpinner.setSelection(0)
                distanceSpinner.isEnabled = false
            }
            3000f -> {
                distanceSpinner.setSelection(1)
                distanceSpinner.isEnabled = false
            }
        }

        binding.measureStartButton.setOnClickListener {

            if(MeasureService.targetDistance == 0f) {

                distanceSpinner.isEnabled = false
                CountDownDialog().show()
                if(!mainViewModel.isReceiverRegistered) {
                    context?.registerReceiver(measureReceiver, IntentFilter(TIMER_ACTION), Context.RECEIVER_EXPORTED)
                    context?.registerReceiver(measureReceiver, IntentFilter(DISTANCE_ACTION), Context.RECEIVER_EXPORTED)
                    mainViewModel.isReceiverRegistered = true
                }
            }
        }
        binding.measureStopButton.setOnClickListener {
            if(MeasureService.targetDistance != 0f) {
                val stopDlg = AlertDialog.Builder(requireContext())
                stopDlg.apply {
                    setMessage("측정하던 기록을 중단할까요?\n중단시 기록은 저장되지 않아요.")
                    setPositiveButton("중단히기", DialogInterface.OnClickListener { _, _ ->
                        distanceSpinner.isEnabled = true
                        sendCommandToForegroundService(MeasureState.STOP)
                        //mContext.unregisterReceiver(measureReceiver)
                        measureReceiver.unregisterMeasureReceiver()
                        mainViewModel.isReceiverRegistered = false
                        mainViewModel.isServiceRunning = false
                        tempTime = 0
                        tempDistance = 0f
                        binding.timeTextView.text = timeFormat(tempTime)
                        binding.currentDistanceTextView.text = String.format("%.2f", tempDistance / 1000.0)
                    })
                    setNegativeButton("측정 계속하기",  null)
                }
                stopDlg.show()
            }
            //record()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(!mainViewModel.isReceiverRegistered) {
            measureReceiver.unregisterMeasureReceiver()
            context?.registerReceiver(measureReceiver, IntentFilter(TIMER_ACTION), Context.RECEIVER_EXPORTED)
            context?.registerReceiver(measureReceiver, IntentFilter(DISTANCE_ACTION), Context.RECEIVER_EXPORTED)
            mainViewModel.isReceiverRegistered = true
        }
        binding.timeTextView.text = timeFormat(tempTime)
        binding.currentDistanceTextView.text = String.format("%.2f", tempDistance / 1000.0)
    }


    private fun isExistTS() {
        val sharedPreferences = requireContext().getSharedPreferences("saved record", Context.MODE_PRIVATE)
        val timeFormat = SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA)

        if(sharedPreferences.getBoolean("exist", false)) {
            val time = sharedPreferences.getInt("time", 0)
            val distance = sharedPreferences.getFloat("target distance", 0f)
            val distanceStr = String.format("%.2f", distance / 1000.0)

            val date = sharedPreferences.getLong("date", 0L)
            val dateStr = timeFormat.format(date)

            val dialog = AlertDialog.Builder(requireContext())

            dialog.apply {
                setMessage("인터넷 문제로 인해 저장되지 못한 기록이 있어요.\n" +
                        "${distanceStr}km\n" +
                        "${dateStr}  ${Record().timeFormat(time.toLong())}\n" +
                        "저장할까요?")
            }
            dialog.setPositiveButton("저장하기", DialogInterface.OnClickListener { _, _ ->

                recordTS(date, time, distance)
                sharedPreferences.edit().putBoolean("exist", false).apply()

            })

            dialog.setNegativeButton("저장하지 않고 지우기", DialogInterface.OnClickListener { _, _ ->

                sharedPreferences.edit().putBoolean("exist", false).apply()

            })

            dialog.show()
        }
    }

    private fun sendCommandToForegroundService(state: MeasureState) {
        ContextCompat.startForegroundService(mContext.applicationContext, getServiceIntent(state))
    }

    private fun getServiceIntent(state: MeasureState) : Intent {
        return Intent(mContext.applicationContext, MeasureService::class.java).apply {
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
    }

    private fun updateDistanceUi(distance: Float) {
        binding.currentDistanceTextView.text = String.format("%.2f", distance / 1000.0)
    }

    class MainViewModel: ViewModel() {
        var isReceiverRegistered: Boolean = false
        var isServiceRunning: Boolean = false
    }

    inner class MeasureReceiver: BroadcastReceiver() {

        private var receiverContext: Context? = null
        override fun onReceive(con: Context?, intent: Intent) {

            if(receiverContext == null) {
                receiverContext = con
            }

            if(intent.action == TIMER_ACTION) {
                val time = intent.getIntExtra(NOTIFICATION_TIME, 0)
                tempTime = time
                updateTimeUi(time)

                if(time == 0) {
                    //mContext.unregisterReceiver(measureReceiver)
                    measureReceiver.unregisterMeasureReceiver()
                    mainViewModel.isReceiverRegistered = false
                }
            }
            else if (intent.action == DISTANCE_ACTION) {
                val distance = intent.getFloatExtra(NOTIFICATION_DISTANCE, 0f)
                tempDistance = distance
                updateDistanceUi(distance)
            }
        }

        fun unregisterMeasureReceiver() {
            receiverContext?.unregisterReceiver(this)
        }

    }

    private fun timeFormat(time: Int) : String {

        val minute = time / 60
        val second = time % 60
        return String.format(Locale.KOREA, "%02d : %02d", minute, second)
    }

    private fun recordTS(date: Long, time: Int, distance: Float) {

        val day = SimpleDateFormat("d", Locale.KOREA).format(date).toString()
        val documentReference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("record")
                .document(SimpleDateFormat("yyyyMM", Locale.KOREA).format(date).toString())

        val dateData = hashMapOf(
             day to "ok"
        )

        documentReference.get().addOnSuccessListener {
            if(it.data == null) {
                documentReference.set(dateData as Map<String, String>)
            } else {
                documentReference.update(dateData as Map<String, String>)
            }
        }

        val timeData = hashMapOf(
            "time" to time
        )

        documentReference.collection(day)
            .document(distance.toInt().toString()).set(timeData as Map<String, Any>)

        val top10Reference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("top10").document(distance.toInt().toString())


        top10Reference.get().addOnSuccessListener { task ->

            val top10List: MutableList<MutableMap.MutableEntry<String, Any>> = try {
                task?.data?.entries?.sortedByDescending { it.value as Long }?.toMutableList()!!
            } catch (e: NullPointerException) {
                mutableListOf()
            }


            val timeFormat = SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA)


            if (top10List.size in 0..9) {
                if (top10List.size == 0) {
                    top10Reference.set(
                        hashMapOf(
                            timeFormat.format(date).toString() to time
                        ) as Map<String, Any>
                    )
                } else {
                    top10Reference.update(
                        hashMapOf(
                            timeFormat.format(date).toString() to time
                        ) as Map<String, Any>
                    )
                }
            } else {
                if (time < top10List[0].value as Long) {
                    top10List.removeAt(0)
                    val map = top10List.associate { it.key to it.value }.toMutableMap()
                    map[timeFormat.format(date).toString()] = time
                    top10Reference.update(map)
                }
            }
        }
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
                            mainViewModel.isServiceRunning = true
                        }
                        countDownBinding.countDownTextView.text = countDown.toString()
                        countDown--
                    }
                }
            }
            timer.schedule(timerTask, 0, 1000)
        }
    }

    private fun record() {
        val calendar = Calendar.getInstance()

        val documentReference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("record")
                .document(SimpleDateFormat("yyyyMM", Locale.KOREA).format(calendar.time).toString())

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
            .document(MeasureService.targetDistance.toInt().toString()).set(timeData as Map<String, Any>)



        val top10Reference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("top10").document(MeasureService.targetDistance.toInt().toString())


        top10Reference.get().addOnSuccessListener { task ->

            val top10Map = try {
                task?.data!!
            } catch (e: NullPointerException) {
                mutableMapOf()
            }

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
            else if(top10List.size == 10 && top10Map.contains(timeFormat.format(calendar.time))) {
                top10Reference.update(
                    hashMapOf(
                        timeFormat.format(calendar.time).toString() to tempTime
                    ) as Map<String, Any>
                )
            }
            else {
                if (tempTime < top10List[0].value as Long) {
                    top10List.removeAt(0)
                    val map = top10List.associate { it.key to it.value }.toMutableMap()
                    map[timeFormat.format(calendar.time).toString()] = tempTime
                    top10Reference.set(map)
                }
            }
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
        @SuppressLint("StaticFieldLeak")
        lateinit var mContext: Context
        @SuppressLint("StaticFieldLeak")
        lateinit var distanceSpinner: Spinner

        private var tempTime = 0
        private var tempDistance = 0f
    }
}