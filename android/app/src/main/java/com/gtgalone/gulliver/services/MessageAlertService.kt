package com.gtgalone.gulliver.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gtgalone.gulliver.models.Notification

class MessageAlertService : FirebaseMessagingService() {
  override fun onNewToken(p0: String?) {
    super.onNewToken(p0)

    Log.d("test", "Refreshed token: $p0")
  }

  override fun onMessageReceived(p0: RemoteMessage?) {
    super.onMessageReceived(p0)

    Log.d("test", p0?.notification?.title)
    Log.d("test", p0?.notification?.body)
    Log.d("test", p0?.notification?.icon)
    sendNotification(p0?.notification!!)
  }

  private fun sendNotification(notification: RemoteMessage.Notification) {
    val intent = Intent(this, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

    val channelId = getString(R.string.default_notification_channel_id)
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(notification.title)
      .setContentText(notification.body)
      .setAutoCancel(true)
      .setSound(defaultSoundUri)
      .setContentIntent(pendingIntent)

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Since android Oreo notification channel is needed.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId,
        "Channel human readable title",
        NotificationManager.IMPORTANCE_DEFAULT)
      notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(0, notificationBuilder.build())
  }
}