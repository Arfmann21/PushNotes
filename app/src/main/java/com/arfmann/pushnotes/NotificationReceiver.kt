package com.arfmann.pushnotes

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlin.random.Random

class NotificationReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput: Bundle ? = RemoteInput.getResultsFromIntent(intent)

        val randomId = Random.nextInt()

        if (remoteInput != null) {
            val name: CharSequence? = remoteInput.getCharSequence("NotificationReply")
            val deleteIntent = Intent() //intent to click on notification without opening app
            val pendingIntentDelete = PendingIntent.getBroadcast(context,0,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(
                context,
                "com.arfmann.notificationnotes"
            ) //build notification
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntentDelete)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //set visibility to public to show notification on lock screen
                .setStyle(NotificationCompat.BigTextStyle()) //set big text style to enable multiline notification
                .setAutoCancel(true) //set auto cancel to delete notification when click on it
                .setContentTitle(context.resources.getString(R.string.quick_note))
                .setContentText(name)
                .setSubText(context.getString(R.string.howto_delete))
                .setOngoing(true)
                .setGroup("fast")

            notificationManager.notify(randomId, builder.build())
        }
    }
}