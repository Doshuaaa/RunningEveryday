package com.example.runningeveryday

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

const val SERVICE_COMMAND = "ServiceCommand"

const val TIMER_ACTION = "timer_action"
const val DISTANCE_ACTION = "distance_action"

const val NOTIFICATION_TIME = "NotificationTime"
const val NOTIFICATION_DISTANCE = "NotificationDistance"

class MeasureService : Service() {

    private var serviceState: MeasureState = MeasureState.INITIALIZED
    private val helper: NotificationHelper by lazy {  NotificationHelper(this) }

    private var targetDistance = 0f
    private var currentTime = 0
    private var totalDistance = 0f

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
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if(lastLocation != null) {
                totalDistance += location.distanceTo(lastLocation!!)
            }
            lastLocation = location

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
        wakeLock.release()
    }

    private fun measureStart() {
        serviceState = MeasureState.START

        startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification())

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10f, locationListener)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        handler.post(runnable)
        broadcastTimeUpdate()
    }

    private fun measureStop() {
        serviceState = MeasureState.STOP
        broadcastTimeUpdate()
        broadcastDistanceUpdate()
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        stopForeground(STOP_FOREGROUND_DETACH)
        wakeLock.release()
    }

    private fun broadcastTimeUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, currentTime))
            helper.updateNotification(totalDistance, targetDistance, currentTime)
        }
        else {
            sendBroadcast(Intent(TIMER_ACTION).putExtra(NOTIFICATION_TIME, 0))
            helper.notificationCancel()
        }
    }

    private fun broadcastDistanceUpdate() {
        if(serviceState == MeasureState.START) {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, currentTime))
        }
        else {
            sendBroadcast(Intent(DISTANCE_ACTION).putExtra(NOTIFICATION_DISTANCE, 0))
        }
    }

}