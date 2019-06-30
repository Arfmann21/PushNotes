package com.arfmann.pushnotes

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.text.HtmlCompat
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_autocancel.view.*


class MainActivity : AppCompatActivity() {

    var i = 0

    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder : NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        var hourMilli: Long
        var minuteMilli: Long
        var totalMilli: Long

        title_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        content_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        title_editText.requestFocus()

        gitHub_link_textView.text = HtmlCompat.fromHtml("<a href='https://github.com/Arfmann21/PushNotes'>GitHub</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        gitHub_link_textView.movementMethod = LinkMovementMethod.getInstance()

        done_fab.setOnClickListener {

            if (autodelete_notification_switch.isChecked) {
                val inflater = LayoutInflater.from(applicationContext)
                val dialogView = inflater.inflate(R.layout.alertdialog_autocancel, null)

                val alertDialogHour = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                alertDialogHour.setView(dialogView)
                alertDialogHour.setTitle(R.string.alertdialog_title)

                val hourEditText = dialogView.hour_editText as EditText
                val minuteHourEditText = dialogView.minute_editText as EditText

                alertDialogHour.setPositiveButton(R.string.send_alertDialog) {
                        dialog, _ ->
                    dialog.dismiss()

                    fun EditText.longValue() = text.toString().toLongOrNull() ?: 0

                    hourMilli = hourEditText.longValue() * 3600000
                    minuteMilli = minuteHourEditText.longValue() * 60000
                    totalMilli = hourMilli + minuteMilli

                    notificationFunction(totalMilli)
                }

                alertDialogHour.setNegativeButton(R.string.cancel_alertDialog){
                        dialog, _ -> dialog.dismiss()
                }

                alertDialogHour.show()

            }
            else
                notificationFunction(0)


        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    fun notificationFunction(totalMilli: Long){


        var channelId = "com.arfmann.notificationnotes"
        val description = "Notes"
        val groupKey = "com.arfmann.notificationnotes"

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        title_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        content_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        val intent = Intent(this,MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId,description,NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = NotificationCompat.Builder(this,channelId)
                .setContentTitle(title_editText.text!!.toString())
                .setContentText(content_editText.text!!.toString())
                .setSmallIcon(R.drawable.logo)
               //Not needed for now .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.logo))
                .setContentIntent(pendingIntent)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setTimeoutAfter(totalMilli)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            Notification.VISIBILITY_PUBLIC

            if(persistent_notfication_switch.isChecked){
                builder.setOngoing(true)
            }
            else
                builder.setOngoing(false)

        }else{

            builder = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title_editText.text!!.toString())
                .setContentText(content_editText.text!!.toString())
                .setSmallIcon(R.drawable.logo)
                //Not needed for now  .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.logo))
                .setContentIntent(pendingIntent)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setTimeoutAfter(totalMilli)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            Notification.VISIBILITY_PUBLIC

            if(persistent_notfication_switch.isChecked)
                builder.setOngoing(true)
            else
                builder.setOngoing(false)
        }
        notificationManager.notify(i, builder.build())
        i++

        title_editText.text = null
        content_editText.text = null

        title_editText.requestFocus()

        persistent_notfication_switch.isChecked = false
        autodelete_notification_switch.isChecked = false

        cancelAllNotifications(notificationManager)
    }

    private fun cancelAllNotifications(notificationManager: NotificationManager){

        delete_fab.setOnClickListener{

            val inflater = LayoutInflater.from(applicationContext)
            val dialogView = inflater.inflate(R.layout.alertdialog_cancel_all, null)

            val alertDialogCancelAll = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            alertDialogCancelAll.setView(dialogView)


            alertDialogCancelAll.setPositiveButton(R.string.yes){
                    _, _ -> notificationManager.cancelAll()
            }

            alertDialogCancelAll.setNegativeButton(R.string.no){
                    dialog, _ -> dialog.dismiss()
            }

            alertDialogCancelAll.show()
        }

    }

}


