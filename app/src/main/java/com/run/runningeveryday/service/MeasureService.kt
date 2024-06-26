package com.run.runningeveryday.service

import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.run.runningeveryday.CheckNetwork
import com.run.runningeveryday.MainActivity
import com.run.runningeveryday.NotificationHelper
import com.run.runningeveryday.R
import com.run.runningeveryday.databinding.DialogMeasureCompleteBinding
import com.run.runningeveryday.fragment.MeasureFragment
import com.run.runningeveryday.fragment.SELECT_DISTANCE
import com.run.runningeveryday.model.Record
import com.run.runningeveryday.state.MeasureState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext

const val SERVICE_COMMAND = "ServiceCommand"

const val TIMER_ACTION = "timer_action"
const val DISTANCE_ACTION = "distance_action"

const val NOTIFICATION_TIME = "NotificationTime"
const val NOTIFICATION_DISTANCE = "NotificationDistance"

class MeasureService : Service(), CoroutineScope {

    private var serviceState: MeasureState = MeasureState.INITIALIZED
    private val helper: NotificationHelper by lazy {  NotificationHelper(this, targetDistance) }

    companion object {
        var targetDistance = 0f
    }

    private var currentTime = 0
    private var totalDistance = 0f

    private val calendar = Calendar.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fireStore = FirebaseFirestore.getInstance()

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {

            currentTime++
            broadcastTimeUpdate()
            handler.postDelayed(this, 1000)
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if(lastLocation != null) {
                totalDistance += location.distanceTo(lastLocation!!)
            }
            lastLocation = location

            broadcastDistanceUpdate()
            if(totalDistance >= targetDistance) {

                helper.notificationCancel()
                if(CheckNetwork.checkNetworkState(baseContext)) {
                    record()
                }
                else {
                    val sharedPreferences = getSharedPreferences("saved record", Context.MODE_PRIVATE)
                    sharedPreferences.edit().apply {
                        putBoolean("exist", true).apply()
                        putInt("time", currentTime).apply()
                        putFloat("target distance", targetDistance).apply()
                        putLong("date", calendar.timeInMillis).apply()
                    }
                    measureComplete()
                }
            }
        }
    }

    private fun measureComplete() {
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        MeasureCompleteDialog().show()
        serviceState = MeasureState.STOP
        totalDistance = 0f
        targetDistance = 0f
        broadcastDistanceUpdate()
        broadcastTimeUpdate()
        stopForeground(STOP_FOREGROUND_DETACH)
        helper.completeNotification(currentTime)
        currentTime = 0
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val state = intent?.extras?.getSerializable(SERVICE_COMMAND, MeasureState::class.java)
        runBlocking {
            CoroutineScope(Dispatchers.Main).launch {
                if(state == MeasureState.START) {
                    delay(3000)
                }

                targetDistance = intent?.extras?.getFloat(SELECT_DISTANCE, 0f)!!
                when(state) {
                    MeasureState.START -> measureStart()
                    MeasureState.STOP -> measureStop()
                    else -> {}
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        job.cancel()
    }

    private fun measureStart() {
        serviceState = MeasureState.START
        helper.setTargetDistance(targetDistance)
        startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification(), FOREGROUND_SERVICE_TYPE_LOCATION)

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10f, locationListener)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        launch(coroutineContext){
            handler.post(runnable)
        }

    }

    private fun measureStop() {
        helper.notificationCancelDelayed()
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        serviceState = MeasureState.STOP
        totalDistance = 0f
        targetDistance = 0f
        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        stopForeground(STOP_FOREGROUND_DETACH)
        //wakeLock.release()
        currentTime = 0

    }

    private fun broadcastTimeUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, currentTime))
            helper.updateNotification(totalDistance, currentTime)
        }
        else {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, 0))
        }
    }

    private fun broadcastDistanceUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, totalDistance))
        }
        else {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, 0f))
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
            "time" to currentTime
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
                            timeFormat.format(calendar.time).toString() to currentTime
                        ) as Map<String, Any>
                    )
                }
                else {
                    top10Reference.update(
                        hashMapOf(
                            timeFormat.format(calendar.time).toString() to currentTime
                        ) as Map<String, Any>
                    )
                }
            }
            else if(top10List.size == 10 && top10Map.contains(timeFormat.format(calendar.time))) {
                top10Reference.update(
                    hashMapOf(
                        timeFormat.format(calendar.time).toString() to currentTime
                    ) as Map<String, Any>
                )
            }
            else {
                if (currentTime < top10List[0].value as Long) {
                    top10List.removeAt(0)
                    val map = top10List.associate { it.key to it.value }.toMutableMap()
                    map[timeFormat.format(calendar.time).toString()] = currentTime
                    top10Reference.set(map)
                }
            }
            measureComplete()
        }
    }

    inner class MeasureCompleteDialog : Dialog(MeasureFragment.mContext) {

        private var dialogViewBinding: DialogMeasureCompleteBinding? = null
        private val dialogBinding get() = dialogViewBinding!!
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            dialogViewBinding = DialogMeasureCompleteBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)
            setCancelable(false)
            window?.setBackgroundDrawableResource(R.drawable.round_dialog)
            initView()
        }

        private fun initView() {

            val record = Record()

            dialogBinding.apply {
                dismissMeasureCompleteDialog.setOnClickListener {
                    if(NotificationHelper.vibrator != null) {
                        NotificationHelper.vibrator?.cancel()
                    }
                    MeasureFragment.distanceSpinner.isEnabled = true
                    dismiss()
                }
                measureCompleteTargetDistance.text = getString(R.string.complete_target_distance, String.format("%.1f", targetDistance / 1000.0))
                measureCompleteTime.text = record.timeFormat(currentTime.toLong())
                measureCompleteGrade.text = record.getGrade(
                    MainActivity.sex,
                    MainActivity.age, targetDistance.toInt(), currentTime.toLong()).toString()
            }
        }
    }
}