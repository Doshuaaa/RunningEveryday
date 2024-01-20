package com.example.runningeveryday

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "Channel"
private const val CHANNEL_NAME = "ChannelName"

class NotificationHelper(context: Context) {

    companion object {
        const val NOTIFICATION_ID = 99
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }

    fun getNotification() : Notification {
        notificationManager.createNotificationChannel(createChannel())
        return notificationBuilder.build()
    }

    private fun createChannel() : NotificationChannel {
        return NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            //vibrationPattern = longArrayOf(0)
        }
    }

    fun updateNotification(curDistance: Float, targetDistance: Float, curTime: Int) {
        notificationBuilder.setContentText("시간: ${curTime / 60} : ${curTime % 60}\n  거리: ${String.format("%.2f", curDistance / 1000.0)} / ${targetDistance}")
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun completeNotification(targetDistance: Float, curTime: Int) {
        notificationBuilder.setContentText("${targetDistance}m 측정 완료!  <걸린 시간: ${curTime / 60} : ${curTime % 60}>")
    }

    fun notificationCancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}