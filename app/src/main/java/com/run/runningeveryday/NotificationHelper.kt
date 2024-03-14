package com.run.runningeveryday

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import com.run.runningeveryday.model.Record

private const val CHANNEL_ID = "Channel"
private const val CHANNEL_NAME = "ChannelName"

class NotificationHelper(val context: Context, private var targetDistance: Float) {

    val record = Record()

    companion object {
        const val NOTIFICATION_ID = 99
        var vibrator: VibratorManager? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val pendingIntent by lazy {
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private val notificationBuilder: Notification.Builder by lazy {
        Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("RunningEveryday")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.running_everyday)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setAutoCancel(true)
            .setOngoing(false)
    }

    fun getNotification() : Notification {
        notificationManager.createNotificationChannel(createChannel())
        return notificationBuilder.build()
    }

    fun deleteNotificationChannel() {
        notificationManager.deleteNotificationChannel(CHANNEL_ID)
    }

    private fun createChannel() : NotificationChannel {

        return NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            vibrationPattern = longArrayOf(100, 200)
            enableVibration(true)
        }
    }

    fun updateNotification(curDistance: Float, curTime: Int) {


        notificationBuilder.setContentText(
            "시간: ${record.timeFormat(curTime.toLong())}\n거리: ${
                String.format(
                    "%.2f",
                    curDistance / 1000.0
                )
            } / ${String.format(
                "%.2f",
                targetDistance / 1000.0
            )}"
        )
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }

    fun completeNotification(curTime: Int) {
        handler.removeCallbacks(runnable)
        vibrator = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val timings = longArrayOf(100, 1000, 100)
        val amplitudes = intArrayOf(0, 100, 0)
        val vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, 1)
        val combinedVibration = CombinedVibration.createParallel(vibrationEffect)
        val audioAttributes = VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ALARM)
            .build()
        vibrator?.vibrate(combinedVibration, audioAttributes)
        notificationBuilder.setContentText("${String.format("%.1f", targetDistance / 1000.0)}km 측정 완료!  <걸린 시간: ${curTime / 60} : ${curTime % 60}>")

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun notificationCancel() {
        handler.post(runnable)
    }

    fun notificationCancelDelayed() {
        handler.postDelayed(runnable, 1000L)
    }


    fun setTargetDistance(targetDistance: Float) {
        this.targetDistance = targetDistance
    }
}