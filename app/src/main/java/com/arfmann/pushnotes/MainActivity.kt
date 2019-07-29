package com.arfmann.pushnotes

import android.Manifest
import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_autocancel.view.*
import kotlinx.android.synthetic.main.sheet_advise.view.*
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private var i = 0
    private var oneTimeAdviseInt = 0
    private var constant = 0
    private var values = ArrayList<String>()

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var builder : NotificationCompat.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        loadData()

        if(oneTimeAdviseInt == 0)
            oneTimeAdvise()

        title_editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        content_editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        title_editText.requestFocus()

        val adapter = ArrayAdapter(this, R.layout.listview_text_color, values)

        gitHub_link_textView.text = HtmlCompat.fromHtml("<a href='https://github.com/Arfmann21/PushNotes'>GitHub</a>", HtmlCompat.FROM_HTML_MODE_LEGACY) //Add link to textView
        gitHub_link_textView.movementMethod = LinkMovementMethod.getInstance()

        done_fab.setOnClickListener {
            doneClick()
        }

        listImageView.setOnClickListener {
            listOfNotes(adapter)
        }


        settingsImageView.setOnClickListener {
            showSettings()
        }

        delete_fab.setOnClickListener{
            cancelAllNotifications(notificationManager)
        }

        checkUpdate()

    }

    private fun oneTimeAdvise(){

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.sheet_advise, null)

            dialog.setContentView(view)
            dialog.show()
            oneTimeAdviseInt = 1

            view.closeSheetAdvise.setOnClickListener {
                dialog.cancel()
            }

            saveData()
        }

    }


    private fun doneClick(){

        if(content_editText.text!!.isEmpty() && title_editText.text!!.isEmpty()) {
            Toast.makeText(this, R.string.no_title_content, Toast.LENGTH_LONG).show()
            title_editText.requestFocus()
        }

        else {

            if(autodelete_notification_switch.isChecked)
                autoDeleteChecked()
            else {
                addNotesToList()
                notificationFunction(0)
                copyToClipboard()
            }

            saveData()
        }
    }


    private fun autoDeleteChecked(){ //function to handle auto-delete notifications

        var hourMilli: Long
        var minuteMilli: Long
        var totalMilli: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //Check if API is 26 (Android 8.0) or upper

            val inflater = LayoutInflater.from(applicationContext)
            val dialogView = inflater.inflate(R.layout.alertdialog_autocancel, null)

            val alertDialogHour = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle) //build AlertDialog

            alertDialogHour.setView(dialogView) //set inflated layout
            alertDialogHour.setTitle(R.string.alertdialog_title)

            val setTime = dialogView.setTimeLayout

            alertDialogHour.setPositiveButton(R.string.send_alertDialog) { _, _ ->

                val hourEditText = dialogView.hour_editText as TextInputEditText //declare the two editText
                val minuteEditText = dialogView.minute_editText as TextInputEditText

                fun TextInputEditText.longValue() = text.toString().toLongOrNull() ?: 0//function to convert editText's value from string to Long

                hourMilli = hourEditText.longValue() * 3600000 //convert from hours to milliseconds
                minuteMilli = minuteEditText.longValue() * 60000 //convert from minutes to milliseconds
                totalMilli = hourMilli + minuteMilli

                if(hourEditText.text!!.isEmpty() && minuteEditText.text!!.isNotEmpty())
                    Toast.makeText(this, resources.getString(R.string.willBeDeletedIn) + " " + resources.getString(R.string.inString) + " 0 " + resources.getString(R.string.hours) + " " +  resources.getString(R.string.and) + " " + minuteEditText.text!!.toString() + " " + resources.getString(R.string.minutes), Toast.LENGTH_LONG).show()
                else if(minuteEditText.text!!.isEmpty() && hourEditText.text!!.isNotEmpty())
                    Toast.makeText(this, resources.getString(R.string.willBeDeletedIn) + " " + resources.getString(R.string.inString) + " " + hourEditText.text!!.toString()  + " " + resources.getString(R.string.hours) + " " + resources.getString(R.string.and) + " 0 " + resources.getString(R.string.minutes), Toast.LENGTH_LONG).show()
                else if(hourEditText.text.isNullOrEmpty() && minuteEditText.text.isNullOrEmpty())
                    Toast.makeText(this, resources.getString(R.string.willNotBeDeleted), Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(this, resources.getString(R.string.willBeDeletedIn) + " " + resources.getString(R.string.inString) + " " + hourEditText.text!!.toString() +  " " + resources.getString(R.string.hours) + " " + resources.getString(R.string.and) + " " + minuteEditText.text!!.toString() + " " + resources.getString(R.string.minutes), Toast.LENGTH_LONG).show()

                addNotesToList()
                copyToClipboard()
                notificationFunction(totalMilli)
            }

            alertDialogHour.setNegativeButton(R.string.cancel_alertDialog) { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialogClose = alertDialogHour.create()

            setTime.setOnClickListener {
                timePicker()
                alertDialogClose.dismiss()
            }

            alertDialogClose.show()

        } else { //if not, this feature will not work because API 24 and below doesn't supports it
            Toast.makeText(this, R.string.version_not_supported, Toast.LENGTH_LONG).show()
            autodelete_notification_switch.isChecked = false
        }
    }


    private fun timePicker(){

        var totalMilli: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this,TimePickerDialog.OnTimeSetListener(function = { _, h, m ->

                if(h < hour)
                    Toast.makeText(this,  R.string.invalidTime, Toast.LENGTH_LONG).show()
                else{

                    Toast.makeText(this, resources.getString(R.string.willBeDeletedIn) + " "  + resources.getString(R.string.at) + " " + h.toString() + ":" + m.toString(), Toast.LENGTH_LONG).show()
                    totalMilli = ((h.toLong() - hour.toLong()) * 3600000) + ((m.toLong() - minute.toLong()) * 60000)
                    addNotesToList()
                    notificationFunction(totalMilli)
                }

            }),hour,minute,true)

            tpd.show()

        }
    }


    private fun notificationFunction(totalMilli: Long){ //function to handle notification

        val channelId = "com.arfmann.notificationnotes"
        val description = "Notes"
        val groupKey = "com.arfmann.notificationnotes"

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val howtoDelete = resources.getString(R.string.howto_delete) //declare string with R.string value

        val deleteIntent = Intent() //intent to click on notification without opening app
        val pendingIntentDelete = PendingIntent.getBroadcast(this,0,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //check if API is 25 (Android 8) or upper
            notificationChannel = NotificationChannel(channelId,description,NotificationManager.IMPORTANCE_DEFAULT) //set default importance
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
                .setContentIntent(pendingIntentDelete)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setTimeoutAfter(totalMilli)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //set visibility to public to show notification on lock screen
                .setAutoCancel(true) //set auto cancel to delete notification when click on it
                .setStyle(NotificationCompat.BigTextStyle()) //set big text style to enable multiline notification

            if(persistent_notfication_switch.isChecked || autodelete_notification_switch.isChecked){
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
                .setContentIntent(pendingIntentDelete)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle())

            if(persistent_notfication_switch.isChecked || autodelete_notification_switch.isChecked) {
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
        dont_save_switch.isChecked = false
        copy_to_clipboard_switch.isChecked = false
    }


    private fun cancelAllNotifications(notificationManager: NotificationManager){

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


    private fun addNotesToList(){

        if(!dont_save_switch.isChecked) {

            if (title_editText.text!!.isEmpty())
                values.add(resources.getString(R.string.no_title) + "  -  " + content_editText.text!!.toString())
            else if (content_editText.text!!.isEmpty())
                values.add(title_editText.text!!.toString() + "  -  " + resources.getString(R.string.no_content))
            else
                values.add(title_editText.text!!.toString() + "  -  " + content_editText.text!!.toString())

        }

    }


    private fun copyToClipboard(){

        val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val myClip: ClipData

        if(copy_to_clipboard_switch.isChecked) {


            if (title_editText.text!!.isEmpty())
                myClip = ClipData.newPlainText(
                    "text",
                    resources.getString(R.string.no_title) + " - " + content_editText.text!!.toString()
                )
            else if (content_editText.text!!.isEmpty())
                myClip = ClipData.newPlainText(
                    "text",
                    title_editText.text!!.toString() + " - " + resources.getString(R.string.no_content)
                )
            else
                myClip = ClipData.newPlainText(
                    "text",
                    title_editText.text!!.toString() + "  -  " + content_editText.text!!.toString()
                )

            myClipboard.setPrimaryClip(myClip)

            Toast.makeText(this, resources.getString(R.string.clipboardNote), Toast.LENGTH_LONG).show()

        }

    }


    private fun listOfNotes(adapter: ArrayAdapter<String>) {


        val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        var myClip: ClipData

        adapter.notifyDataSetChanged()

        val alertDialogList = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)

        alertDialogList.setAdapter(adapter) { _, which ->
            val item = adapter.getItem(which)
            myClip = ClipData.newPlainText("text", item)
            myClipboard.setPrimaryClip(myClip)

            Toast.makeText(this, resources.getString(R.string.clipboardNote), Toast.LENGTH_LONG).show()
        }

        alertDialogList.setTitle(resources.getString(R.string.notes))

        alertDialogList.setPositiveButton(R.string.close) { dialog, _ ->
            dialog.dismiss()
        }

        if (!adapter.isEmpty) {
            alertDialogList.setNegativeButton(R.string.deleteNotes) { _, _ ->
                deleteData(adapter)
                Toast.makeText(this, resources.getString(R.string.fullDeleted), Toast.LENGTH_LONG).show()
            }

        } else
            alertDialogList.setMessage(resources.getString(R.string.noNotes))

        alertDialogList.show()

    }


    private fun showSettings(){

        val slideStart = AnimationUtils.loadAnimation(this, R.anim.visibility_anim_start)
        val infoStart = AnimationUtils.loadAnimation(this, R.anim.info_anim_start)
        val infoEnd = AnimationUtils.loadAnimation(this, R.anim.info_anim_end)
        val visibilityEnd = AnimationUtils.loadAnimation(this, R.anim.visibility_anim_end)

        if(settingsGridLayout.visibility == View.GONE) {
            settingsGridLayout.animation = slideStart
            info_gridLayout.animation = infoStart
            settingsGridLayout.visibility = View.VISIBLE
            settingsImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_settings_pressed_icon))
        }

        else{
            settingsGridLayout.animation = visibilityEnd
            info_gridLayout.animation = infoEnd
            settingsImageView.setImageDrawable(resources.getDrawable(R.drawable.ic_settings_icon))
            settingsGridLayout.visibility = View.GONE

        }

    }


    private fun checkUpdate(){

        val queue = Volley.newRequestQueue(this)

        val jsonLink = "https://api.github.com/repos/arfmann21/pushnotes/releases/latest"

        val stringReq = StringRequest(Request.Method.GET, jsonLink, //request to get JSON
            Response.Listener<String> { response ->

                val strResp = response.toString()
                val jsonObj = JSONObject(strResp)

                val jsonObjTagName: String = jsonObj.getString("tag_name") //get GitHub release version tag

                val versionName = packageManager.getPackageInfo(packageName, 0).versionName //get installed version
                val jsonArray: JSONArray = jsonObj.getJSONArray("assets")

                var jsonUrlDownload = ""
                val jsonUrlInfo = jsonObj.getString("html_url")

                for (i in 0 until jsonArray.length()) {
                    val jsonInner: JSONObject = jsonArray.getJSONObject(i)
                    jsonUrlDownload = jsonInner.getString("browser_download_url")
                }

                if(versionName < jsonObjTagName){

                    update_fab.show()

                    update_fab.setOnClickListener{

                        checkPermission(Uri.parse(jsonUrlDownload), Uri.parse(jsonUrlInfo))

                    }

                }
            },
            Response.ErrorListener { "Errore durante la ricerca dell'aggiornamento"}) //if update check go fail
        queue.add(stringReq) //add request to queue

    }


    private fun checkPermission(jsonUrlDownload: Uri, jsonUrlInfo: Uri){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            val alertDialogPermission = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            val inflater = LayoutInflater.from(applicationContext)

            val dialogView = inflater.inflate(R.layout.alertdialog_permission, null)

            alertDialogPermission.setTitle(R.string.noPermissionAlertTitle)
            alertDialogPermission.setView(dialogView)

            alertDialogPermission.setPositiveButton(R.string.yes){ _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), constant)

            }

            alertDialogPermission.setNegativeButton(R.string.no){
                    dialog, _ -> dialog.dismiss()
            }

            alertDialogPermission.show()
        }
        else
            downloadUpdate(jsonUrlDownload, jsonUrlInfo)

    }


    private fun downloadUpdate(jsonUrlDownload: Uri, jsonUrlInfo: Uri){

        val downloadIntent: Intent = jsonUrlInfo.let { webpage -> //create intent to release URL
            Intent(Intent.ACTION_VIEW, webpage)
        }
        val chooser = Intent.createChooser(downloadIntent, "Browser")

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(jsonUrlDownload)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setVisibleInDownloadsUi(true)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PushNotes" + ".apk")

        val alertDialogUpdateAvaible = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)

        alertDialogUpdateAvaible.setMessage(resources.getString(R.string.update_avaible))

        alertDialogUpdateAvaible.setPositiveButton(R.string.yes) { _, _ ->
            downloadManager.enqueue(request)

            Toast.makeText(this, resources.getString(R.string.downloadPath), Toast.LENGTH_LONG).show()

            update_fab.hide()
        }

        alertDialogUpdateAvaible.setNeutralButton("Info"){
                _, _ ->  startActivity(chooser)
        }

        alertDialogUpdateAvaible.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }

        alertDialogUpdateAvaible.show()
    }


    private fun saveData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(values) //convert ArrayList to JSON (shared preferences can't handle ArrayList)
        editor.putString("noteList", json) //save the new JSON with values
        editor.putInt("oneTime", oneTimeAdviseInt)
        editor.apply() //apply new changes
    }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("noteList", null)
        val oneTimeAdvise = sharedPreferences.getInt("oneTime", 0)
        val type = object: TypeToken<ArrayList<String>>() {
        }.type

        when(json){
            null -> values = ArrayList() //if json is null, so empty, values is just an empty ArrayList
            else-> values = gson.fromJson(json, type) //got JSON values and convert them back to ArrayList

        }

        if(oneTimeAdvise >= 1)
            oneTimeAdviseInt = 1

    }


    private fun deleteData(adapter: ArrayAdapter<String>){
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() //delete everything from shared preferences
        editor.apply() //apply new changes

        adapter.clear() //clear everything from the adapter

    }

}



