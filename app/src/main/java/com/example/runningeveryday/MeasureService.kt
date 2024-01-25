package com.example.runningeveryday

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.CoroutineContext

const val SERVICE_COMMAND = "ServiceCommand"

const val TIMER_ACTION = "timer_action"
const val DISTANCE_ACTION = "distance_action"

const val NOTIFICATION_TIME = "NotificationTime"
const val NOTIFICATION_DISTANCE = "NotificationDistance"

class MeasureService : Service(), CoroutineScope{

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

//    private val collectionReference =
//        fireStore.collection("users").document(auth.uid!!)
//        .collection("record")
//        .document("${calendar.get(Calendar.YEAR)}${calendar.get((Calendar.MONTH))}")
//        .collection(targetDistance.toString())

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager}
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

                record()
                measureStop()
                helper.completeNotification(currentTime)
            }
        }
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RunningEveryday::MeasureWakelock")
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        wakeLock.acquire(60*60*1000L /*1 hour*/)
        intent?.extras?.apply {
            targetDistance = getFloat(SELECT_DISTANCE, 0f)
            when(getSerializable(SERVICE_COMMAND, MeasureState::class.java)) {
                MeasureState.START -> measureStart()
                MeasureState.STOP -> measureStop()
                else -> return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        helper.notificationCancel()
        if(wakeLock.isHeld) {
            wakeLock.release()
        }

        job.cancel()
    }

    private fun measureStart() {
        serviceState = MeasureState.START
        helper.setTargetDistance(targetDistance)
        startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification(), FOREGROUND_SERVICE_TYPE_LOCATION)
        broadcastTimeUpdate()

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
        serviceState = MeasureState.STOP
        targetDistance = 0f
        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        stopForeground(STOP_FOREGROUND_DETACH)
        helper.notificationCancel()
        wakeLock.release()
    }

    private fun broadcastTimeUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, currentTime))
            helper.updateNotification(totalDistance, currentTime)
        }
        else {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, 0))
            helper.notificationCancel()
        }
    }

    private fun broadcastDistanceUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, totalDistance))
        }
        else {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, 0f))
            helper.notificationCancel()
        }
    }

    private fun getGrade() {

    }

    private fun record() {

        val documentReference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("record")
                .document(SimpleDateFormat("YYYYMM", Locale.KOREA).format(calendar.time).toString())

        val timeData = hashMapOf(
            "time" to currentTime
        )

        documentReference.collection(calendar.get(Calendar.DAY_OF_MONTH).toString())
            .document(targetDistance.toString()).set(timeData as Map<String, Any>)

        val dateData = hashMapOf(
            calendar.get(Calendar.DAY_OF_MONTH).toString() to "ok"
        )
        documentReference.update(dateData as Map<String, Any>)


        val top10Reference =
            fireStore.collection("users").document(auth.uid!!)
                .collection("top10").document(targetDistance.toString())


        top10Reference.get().addOnSuccessListener { task ->

            val top10List =
                task?.data?.entries?.sortedByDescending { it.value as Long }?.toMutableList()!!

            val timeFormat = SimpleDateFormat("yyyy년 M월 dd일", Locale.KOREA)


            if(top10List.size in 0..9) {
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
        }

    }

}