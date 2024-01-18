package com.github.k1rakishou.chan4captchasolver

import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import logcat.logcat

class NotificationHelper {

  companion object {
    private const val CHANNEL_ID = "app_updater_channel"
    private const val NOTIFICATION_ID = 1

    fun showUpdateNotification(context: Context, version: Float, urlToOpen: String?) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        logcat { "showUpdateNotification() creating channel" }

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, "App Update", importance).apply {
          description = "Notifications for app updates"
        }

        notificationManager.createNotificationChannel(channel)
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle("Version ${version}")
        .setContentText("New version of 4chan captcha solver is available!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .also { builder ->
          if (urlToOpen.isNullOrBlank()) {
            return@also
          }

          logcat { "showUpdateNotification() creating action button" }

          val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
          val pendingIntent = PendingIntent.getActivity(context, 0, urlIntent, PendingIntent.FLAG_IMMUTABLE)

          builder
            .addAction(0, "Go to release page", pendingIntent)
        }
        .build()

      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }
}