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
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_autocancel.view.*


class MainActivity : AppCompatActivity() {

    var i = 0

    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private lateinit var builder : NotificationCompat.Builder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        title_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES //Set first letter in CAP
        content_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        title_editText.requestFocus()

        gitHub_link_textView.text = HtmlCompat.fromHtml("<a href='https://github.com/Arfmann21/PushNotes'>GitHub</a>", HtmlCompat.FROM_HTML_MODE_LEGACY) //Add link to textView
        gitHub_link_textView.movementMethod = LinkMovementMethod.getInstance()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        done_fab.setOnClickListener {

            if(autodelete_notification_switch.isChecked)
                autoDeleteChecked()
            else
                notificationFunction(0, notificationManager)
        }

        cancelAllNotifications(notificationManager)

    }

    private fun autoDeleteChecked(){ //function to handle auto-delete notifications

        var hourMilli: Long
        var minuteMilli: Long
        var totalMilli: Long


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //Check if API is 25 (Android 8) or upper

            val inflater = LayoutInflater.from(applicationContext)
            val dialogView = inflater.inflate(R.layout.alertdialog_autocancel, null) //Inflate layout for AlertDialog

            val alertDialogHour = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle) //build AlertDialog
            alertDialogHour.setView(dialogView) //set inflated layout
            alertDialogHour.setTitle(R.string.alertdialog_title)

            val hourEditText = dialogView.hour_editText as EditText //declare the two editText
            val minuteHourEditText = dialogView.minute_editText as EditText

            alertDialogHour.setPositiveButton(R.string.send_alertDialog) { _, _ -> //if user has clicked "Send"

                fun EditText.longValue() = text.toString().toLongOrNull() ?: 0 //function to convert editText's value from string to Long

                hourMilli = hourEditText.longValue() * 3600000 //convert from hours to milliseconds
                minuteMilli = minuteHourEditText.longValue() * 60000 //convert from minutes to milliseconds
                totalMilli = hourMilli + minuteMilli

                notificationFunction(totalMilli, notificationManager)
            }

            alertDialogHour.setNegativeButton(R.string.cancel_alertDialog) { dialog, _ -> //if user has clicked "Cancel"
                dialog.dismiss()
            }

            alertDialogHour.show()

        }
        else { //if not, this feature will not work because API 24 and below doesn't supports it
            Toast.makeText(this, R.string.version_not_supported, Toast.LENGTH_LONG).show()
            autodelete_notification_switch.isChecked = false
        }
    }



    private fun notificationFunction(totalMilli: Long, notificationManager: NotificationManager){ //function to handle notification


        val channelId = "com.arfmann.notificationnotes"
        val description = "Notes"
        val groupKey = "com.arfmann.notificationnotes"


        val howtoDelete = resources.getString(R.string.howto_delete) //declare string with R.string value

        val deleteIntent = Intent() //intent to click on notification without opening app
        val pendingIntentDelete = PendingIntent.getBroadcast(this,0,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        if(content_editText.text!!.isEmpty() && title_editText.text!!.isEmpty()) {
            Toast.makeText(this, R.string.no_title_content, Toast.LENGTH_LONG).show()
            title_editText.requestFocus()
        }

        else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //check if API is 25 (Android 8) or upper
                notificationChannel = NotificationChannel(channelId,description,NotificationManager.IMPORTANCE_DEFAULT) //set default importance
                notificationChannel.enableLights(true) //enable LED
                notificationChannel.lightColor = Color.GREEN //set LED color to green
                notificationChannel.enableVibration(true) //enable vibration
                notificationManager.createNotificationChannel(notificationChannel)

                builder = NotificationCompat.Builder(this,channelId) //build notification

                if(title_editText.text!!.isEmpty())
                    builder.setContentTitle(resources.getString(R.string.no_title))
                else
                    builder.setContentTitle(title_editText.text!!.toString())

                if(content_editText.text!!.isEmpty())
                    builder.setContentText(resources.getString(R.string.no_content))
                else
                    builder.setContentText(content_editText.text!!.toString())

                builder.setSmallIcon(R.drawable.logo)
                    //Not needed for now .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.logo))
                    .setContentIntent(pendingIntentDelete)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setTimeoutAfter(totalMilli)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //set visibility to public to show notification on lock screen
                    .setAutoCancel(true) //set auto cancel to delete notification when click on it
                    .setStyle(NotificationCompat.BigTextStyle()) //set big text style to enable multiline notification

                if(persistent_notfication_switch.isChecked){
                    builder.setOngoing(true) //set ongoing to prevent notification from clearing (except when user clicks on it)
                    builder.setSubText(howtoDelete)
                }

            }else{

                builder = NotificationCompat.Builder(this, channelId) //build notification

                if(title_editText.text!!.isEmpty())
                    builder.setContentTitle(resources.getString(R.string.no_title))
                else
                    builder.setContentTitle(title_editText.text!!.toString())

                if(content_editText.text!!.isEmpty())
                    builder.setContentText(resources.getString(R.string.no_content))
                else
                    builder.setContentText(content_editText.text!!.toString())

                builder.setSmallIcon(R.drawable.logo)
                    //Not needed for now  .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.logo))
                    .setContentIntent(pendingIntentDelete)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setStyle(NotificationCompat.BigTextStyle())

                if(persistent_notfication_switch.isChecked) {
                    builder.setOngoing(true)
                    builder.setSubText(howtoDelete)
                }

            }
            notificationManager.notify(i, builder.build())
            i++

            title_editText.text = null
            content_editText.text = null

            title_editText.requestFocus()

            persistent_notfication_switch.isChecked = false
            autodelete_notification_switch.isChecked = false
        }

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


