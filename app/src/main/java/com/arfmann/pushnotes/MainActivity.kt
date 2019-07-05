package com.arfmann.pushnotes

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.text.HtmlCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_autocancel.view.*
import kotlinx.android.synthetic.main.alertdialog_list.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    var i = 0
    private var values = ArrayList<String>()

    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    private lateinit var builder : NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        loadData()

        title_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES //Set first letter in CAP
        content_editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        title_editText.requestFocus()

        val adapter = ArrayAdapter(this, R.layout.listview_text_color, values)

        gitHub_link_textView.text = HtmlCompat.fromHtml("<a href='https://github.com/Arfmann21/PushNotes'>GitHub</a>", HtmlCompat.FROM_HTML_MODE_LEGACY) //Add link to textView
        gitHub_link_textView.movementMethod = LinkMovementMethod.getInstance()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        done_fab.setOnClickListener {


            if(content_editText.text!!.isEmpty() && title_editText.text!!.isEmpty()) {
                Toast.makeText(this, R.string.no_title_content, Toast.LENGTH_LONG).show()
                title_editText.requestFocus()
            }
            else {
                addNotesToList()

                if(autodelete_notification_switch.isChecked) {
                    autoDeleteChecked()
                }
                else
                    notificationFunction(0, notificationManager)

                saveData()
            }
        }

        listImageView.setOnClickListener {
            listOfNotes(adapter)
        }

        cancelAllNotifications(notificationManager)
        checkUpdate()

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //check if API is 25 (Android 8) or upper
                notificationChannel = NotificationChannel(channelId,description,NotificationManager.IMPORTANCE_DEFAULT) //set default importance
                /*  notificationChannel.enableLights(true) //enable LED
                  notificationChannel.lightColor = Color.GREEN //set LED color to green*/
                notificationChannel.enableVibration(true)
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

            } else {

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
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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


    private fun cancelAllNotifications(notificationManager: NotificationManager){

        delete_fab.setOnClickListener{

            val inflater = LayoutInflater.from(applicationContext)
            val dialogView = inflater.inflate(R.layout.alertdialog_cancel_all, null)

            val alertDialogCancelAll = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            alertDialogCancelAll.setView(dialogView)

            alertDialogCancelAll.setTitle(R.string.delete_question)

            alertDialogCancelAll.setPositiveButton(R.string.yes){
                    _, _ -> notificationManager.cancelAll()
            }

            alertDialogCancelAll.setNegativeButton(R.string.no){
                    dialog, _ -> dialog.dismiss()
            }

            alertDialogCancelAll.show()
        }

    }


    private fun addNotesToList(){

        if(title_editText.text!!.isEmpty())
            values.add(resources.getString(R.string.no_title) +  "  -  " + content_editText.text!!.toString())

        else if(content_editText.text!!.isEmpty())
            values.add(title_editText.text!!.toString() + "  -  " + resources.getString(R.string.no_content))

        else
            values.add(title_editText.text!!.toString() + "  -  " + content_editText.text!!.toString())

    }


    private fun listOfNotes(adapter: ArrayAdapter<String>){

        val inflater = LayoutInflater.from(applicationContext)
        val dialogView = inflater.inflate(R.layout.alertdialog_list, null)

        listView?.adapter = adapter
        adapter.notifyDataSetChanged()

        val alertDialogList = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
        alertDialogList.setView(dialogView)

        alertDialogList.setAdapter(adapter, null)
        alertDialogList.setTitle(resources.getString(R.string.notes))

        alertDialogList.setPositiveButton(R.string.close){
                dialog, _ -> dialog.dismiss()
        }

        if(!adapter.isEmpty()) {
            alertDialogList.setNegativeButton(R.string.deleteNotes) { _, _ ->
                deleteData(adapter)
            }
        }
        else
            alertDialogList.setMessage("Nessuna nota")

        alertDialogList.show()

    }


    private fun checkUpdate(){

        val queue = Volley.newRequestQueue(this)

        val jsonLink = "https://api.github.com/repos/arfmann21/pushnotes/releases/latest"

        val stringReq = StringRequest(Request.Method.GET, jsonLink,
            Response.Listener<String> { response ->

                val strResp = response.toString()
                val jsonObj = JSONObject(strResp)

                val jsonObjTagName: String = jsonObj.getString("tag_name")

                val jsonUrl = jsonObj.getString("html_url")

                val versionName = packageManager.getPackageInfo(packageName, 0).versionName

                if(versionName < jsonObjTagName){

                    val downloadIntent: Intent = Uri.parse(jsonUrl ).let { webpage ->
                        Intent(Intent.ACTION_VIEW, webpage)
                    }
                    val chooser = Intent.createChooser(downloadIntent, "Browser")

                    val inflater = LayoutInflater.from(applicationContext)
                    val dialogView = inflater.inflate(R.layout.alertdialog_update, null)

                    val alertDialogUpdateAvaible = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                    alertDialogUpdateAvaible.setView(dialogView)


                    alertDialogUpdateAvaible.setPositiveButton(R.string.yes){
                            _, _ ->  startActivity(chooser)

                    }

                    alertDialogUpdateAvaible.setNegativeButton(R.string.no){
                            dialog, _ -> dialog.dismiss()
                    }

                    alertDialogUpdateAvaible.show()
                }
            },
            Response.ErrorListener { "Errore durante la ricerca dell'aggiornamento"})
        queue.add(stringReq)

    }


    private fun saveData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(values)
        editor.putString("noteList", json)
        editor.apply()
    }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("noteList", null)
        val type = object: TypeToken<ArrayList<String>>() {
        }.type

        if(json != null)
            values = gson.fromJson(json, type)
        else
            values = ArrayList()

    }


    private fun deleteData(adapter: ArrayAdapter<String>){
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        adapter.clear()

    }

}



