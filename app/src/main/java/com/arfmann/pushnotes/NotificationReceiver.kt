package com.arfmann.pushnotes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

class NotificationReceiver : BroadcastReceiver(){

    private var arrayOfNotes = ArrayList<String>()
    private lateinit var notificationChannel: NotificationChannel

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInputReply: Bundle ? = RemoteInput.getResultsFromIntent(intent)

        val randomId = Random.nextInt()

        if (remoteInputReply != null) {
            val name: CharSequence? = remoteInputReply.getCharSequence("NotificationReply")
            val deleteIntent = Intent() //intent to click on notification without opening app
            val pendingIntentDelete = PendingIntent.getBroadcast(context,0,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            var notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            var builder = NotificationCompat.Builder(
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

            addNoteToList(context, name)

            // Recreate notification to avoid stuck on loading

            val channelId = "FastNotes"
            val description = "Create a note from notification"

            val title = context.resources.getString(R.string.reply_title)

            val helpPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                Intent(context, NotificationReceiver::class.java)
                    .putExtra("keyintenthelp", 101),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val remoteInput : RemoteInput = RemoteInput.Builder("NotificationReply")
                .setLabel(context.resources.getString(R.string.add))
                .build()

            val replyAction = NotificationCompat.Action.Builder(R.drawable.logo, context.resources.getString(R.string.add), helpPendingIntent)
                .addRemoteInput(remoteInput)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //check if API is 25 (Android 8) or upper
                notificationChannel = NotificationChannel(
                    channelId,
                    description,
                    NotificationManager.IMPORTANCE_MIN
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }

            builder = NotificationCompat.Builder(context, channelId) //build notification

            builder.setSmallIcon(R.drawable.logo)
                .setContentIntent(helpPendingIntent) //set visibility to public to show notification on lock screen
                .setStyle(NotificationCompat.BigTextStyle()) //set big text style to enable multiline notification
                .setAutoCancel(false) //set auto cancel to delete notification when click on it
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .addAction(replyAction)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.click_on_add))
                .setOngoing(true)

            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(560, builder.build())

        }
    }

    private fun addNoteToList(context: Context, note: CharSequence?){
        val sharedPreferences = context.getSharedPreferences("shared preferences",
            AppCompatActivity.MODE_PRIVATE
        )
        val gson = Gson()
        val json = sharedPreferences.getString("noteList", null)

        val type = object: TypeToken<ArrayList<String>>() {
        }.type

        arrayOfNotes = if(json == null) ArrayList() else gson.fromJson(json, type)
        arrayOfNotes.add("Nota rapida  -  " + note.toString())

        val editor = sharedPreferences.edit()

        val toJson = gson.toJson(arrayOfNotes) //convert ArrayList to JSON (shared preferences can't handle ArrayList)
        editor.putString("noteList", toJson)

        editor.apply()
    }
}