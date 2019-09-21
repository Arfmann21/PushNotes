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
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_autodelete.view.*
import kotlinx.android.synthetic.main.alertdialog_default_model.view.*
import kotlinx.android.synthetic.main.bottomsheet_settings_layout.*
import kotlinx.android.synthetic.main.bottomsheet_settings_layout.view.*
import kotlinx.android.synthetic.main.sheet_advise.*
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

    private lateinit var adapter : ArrayAdapter<String>

    private var persistent = true
    private var autodelete = false
    private var dontSave = false
    private var copy = false

    private lateinit var jsonUrlDownloadUri: Uri
    private lateinit var jsonUrlInfoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        AsyncTasks().execute()

        if(oneTimeAdviseInt == 1)
            oneTimeAdvise()

        title_editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        content_editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        title_editText.requestFocus()

        done_fab.setOnClickListener {
            doneClick()
        }

        listImageView.setOnClickListener {
            title_editText.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(title_editText.windowToken, 0)
            listOfNotes()
        }

        settingsImageView.setOnClickListener {
            showSettings()
        }

        delete_fab.setOnClickListener{
            deleteAllNotifications(notificationManager)
        }
    }

    private fun oneTimeAdvise(){

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val view = layoutInflater.inflate(R.layout.sheet_advise, limitation_container)
            val dialog = BottomSheetDialog(this)

            dialog.setContentView(view)
            dialog.show()
            oneTimeAdviseInt = 1

            saveData()
        }
    }

    private fun doneClick(){

        if(content_editText.text!!.isEmpty() && title_editText.text!!.isEmpty()) {
            Toast.makeText(this, R.string.no_title_content, Toast.LENGTH_LONG).show()
            title_editText.requestFocus()
        }
        else {
            if(autodelete) {
                autoDeleteChecked()
            }
            else {
                addNotesToList()
                copyToClipboard()
                notificationFunction(0)
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
            val dialogView = inflater.inflate(R.layout.alertdialog_autodelete, null)

            val alertDialogHour = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle) //build AlertDialog

            alertDialogHour.setView(dialogView) //set inflated layout
            alertDialogHour.setTitle(R.string.alertdialog_title)

            val setTime = dialogView.set_time_textView

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
            autodelete
        }
    }


    private fun timePicker(){

        var totalMilli: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)

            val tpd = TimePickerDialog(this,TimePickerDialog.OnTimeSetListener(function = { _, h, m ->

                Toast.makeText(this, resources.getString(R.string.willBeDeletedIn) + " "  + resources.getString(R.string.at) + " " + h.toString() + ":" + m.toString(), Toast.LENGTH_LONG).show()
                totalMilli = ((h.toLong() - hour.toLong()) * 3600000) + ((m.toLong() - minute.toLong()) * 60000)
                addNotesToList()
                copyToClipboard()
                notificationFunction(totalMilli)

            }),hour,minute,true)

            tpd.show()

        }
    }

    private fun notificationFunction(totalMilli: Long){ //function to handle notification

        val channelId = "com.arfmann.notificationnotes"
        val description = "Notes"
        // val groupKey = "com.arfmann.notificationnotes"

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val howtoDelete = resources.getString(R.string.howto_delete) //declare string with R.string value

        val deleteIntent = Intent() //intent to click on notification without opening app
        val pendingIntentDelete = PendingIntent.getBroadcast(this,0,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //check if API is 25 (Android 8) or upper
            notificationChannel = NotificationChannel(
                channelId,
                description,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setTimeoutAfter(totalMilli)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) //set visibility to public to show notification on lock screen
            .setStyle(NotificationCompat.BigTextStyle()) //set big text style to enable multiline notification
            .setAutoCancel(true) //set auto cancel to delete notification when click on it

        if(persistent || autodelete){
            builder.setOngoing(true) //set ongoing to prevent notification from clearing (except when user clicks on it)
            builder.setSubText(howtoDelete)
        }

        notificationManager.notify(i, builder.build())
        i++

        title_editText.text = null
        content_editText.text = null

        title_editText.requestFocus()

        autodelete = false
        copy = false
        dontSave = false
    }


    private fun deleteAllNotifications(notificationManager: NotificationManager){

        val inflater = LayoutInflater.from(applicationContext)
        val dialogView = inflater.inflate(R.layout.alertdialog_default_model, null)
        dialogView.alertdialog_textView.setText(R.string.delete_iconAdvise)

        val alertDialogDeleteAll = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
        alertDialogDeleteAll.setView(dialogView)

        alertDialogDeleteAll.setTitle(R.string.delete_question)

        alertDialogDeleteAll.setPositiveButton(R.string.yes){ _, _ ->
            notificationManager.cancelAll()
            Toast.makeText(this, resources.getString(R.string.deleteFromNotification), Toast.LENGTH_LONG).show()
        }

        alertDialogDeleteAll.setNegativeButton(R.string.no){
                dialog, _ -> dialog.dismiss()
        }

        alertDialogDeleteAll.show()
    }

    private fun addNotesToList(){
        if(!dontSave) {
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

        if(copy) {
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


    private fun listOfNotes() {
        adapter.notifyDataSetChanged()

        val contentLayout = findViewById<CoordinatorLayout>(R.id.contentLayout)

        val dialog = BottomSheetBehavior.from(contentLayout)
        dialog.isHideable = false
        dialog.state = BottomSheetBehavior.STATE_EXPANDED

        close_list_button.setOnClickListener {
            dialog.state = BottomSheetBehavior.STATE_COLLAPSED

            title_editText.requestFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(title_editText, 0)
        }

        if (!adapter.isEmpty) {
            no_notes_textView.visibility = View.INVISIBLE
            delete_notes_button.visibility = View.VISIBLE

            delete_notes_button.setOnClickListener {
                deleteData()

                Toast.makeText(this, resources.getString(R.string.fullDeleted), Toast.LENGTH_LONG).show()

                dialog.state = BottomSheetBehavior.STATE_COLLAPSED
            }

        } else
            no_notes_textView.visibility = View.VISIBLE

        alertdialog_list.setOnItemClickListener { _, view, i, _ ->

            onListItemClick(view, i, dialog)
        }

    }

    private fun onListItemClick(view: View, i: Int, dialog: BottomSheetBehavior<CoordinatorLayout>){
        val popupMenu = PopupMenu(this, view)
        val itemList = adapter.getItem(i)

        var title = ""
        var content = ""
        var j = 0

        while(itemList.toString()[j] != '-') {
            title += itemList.toString()[j]
            j++
        }

        val itemLength = itemList.toString().length

        j += 3

        while(j < itemLength) {
            content += itemList.toString()[j]
            j++
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.resend_popup -> {
                    if(title != (resources.getString(R.string.no_title) + "  "))
                        title_editText.setText(title)

                    if(content != resources.getString(R.string.no_content))
                        content_editText.setText(content)

                    dontSave = true

                    true
                }

                R.id.copy_popup -> {
                    val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val myClip = ClipData.newPlainText("text", itemList)
                    myClipboard.setPrimaryClip(myClip)

                    Toast.makeText(this, resources.getString(R.string.clipboardNote), Toast.LENGTH_LONG).show()

                    true
                }

                R.id.delete_popup -> {
                    adapter.remove(itemList)
                    values.remove(itemList)

                    if(values.isEmpty())
                        dialog.state = BottomSheetBehavior.STATE_COLLAPSED
                    saveData()
                    true
                }

                else -> false
            }

        }

        popupMenu.inflate(R.menu.popup_menu)

        try{
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            Log.e("Main", "Error showing menu icons")
        } finally {
            popupMenu.show()
        }

    }

    private fun showSettings(){
        val dialogView = layoutInflater.inflate(R.layout.bottomsheet_settings_layout, bottom_sheet_container)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        if(persistent)
            dialog.persistent_notfication_switch.isChecked = true
        if(autodelete)
            dialog.autodelete_notification_switch.isChecked = true
        if(dontSave)
            dialog.dont_save_switch.isChecked = true
        if(copy)
            dialog.copy_to_clipboard_switch.isChecked = true

        dialog.show()

        dialogView.persistent_notfication_switch.setOnCheckedChangeListener { _, b ->
            persistent = b
        }
        dialogView.autodelete_notification_switch.setOnCheckedChangeListener { _, b ->
            autodelete = b

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Toast.makeText(this, resources.getString(R.string.version_not_supported), Toast.LENGTH_LONG).show()
                autodelete = false
                dialogView.autodelete_notification_switch.isChecked = false
            }
        }
        dialogView.dont_save_switch.setOnCheckedChangeListener { _, b ->
            dontSave = b
        }
        dialogView.copy_to_clipboard_switch.setOnCheckedChangeListener { _, b ->
            copy = b
        }

        dialog.setOnDismissListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(title_editText, 0)
        }

        var url: Uri

        dialogView.dev_telegram_imageView.setOnClickListener {
            url = Uri.parse("http://bit.ly/ArfmannTelegram")
            openWeb(url)
        }
        dialogView.dev_github_imageView.setOnClickListener {
            url = Uri.parse("http://bit.ly/ArfmannGitHub")
            openWeb(url)
        }
        dialogView.ui_telegram_imageView.setOnClickListener {
            url = Uri.parse("http://bit.ly/AleD219Telegram")
            openWeb(url)
        }
        dialogView.ui_github_imageView.setOnClickListener {
            url = Uri.parse("http://bit.ly/AleD219GitHub")
            openWeb(url)
        }
        dialogView.push_notes_github_imageView.setOnClickListener {
            url = Uri.parse("http://bit.ly/PsGitHub")
            openWeb(url)
        }
    }

    private fun openWeb(url: Uri){
        val webIntent = Intent(Intent.ACTION_VIEW, url)
        val chooser = Intent.createChooser(webIntent, "Info")

        startActivity(chooser)
    }

    private fun checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            val alertDialogPermission = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            val inflater = LayoutInflater.from(applicationContext)

            val dialogView = inflater.inflate(R.layout.alertdialog_default_model, null)
            dialogView.alertdialog_textView.setText(R.string.noPermissionAlert)

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
            downloadUpdateAlertDialog()

    }

    private fun downloadUpdateAlertDialog(){
        val inflater = LayoutInflater.from(applicationContext)
        val dialogView = inflater.inflate(R.layout.alertdialog_default_model, null)
        dialogView.alertdialog_textView.setText(R.string.update_download)

        val downloadIntent = Intent(Intent.ACTION_VIEW, jsonUrlInfoUri)
        val chooser = Intent.createChooser(downloadIntent, "Browser")

        val alertDialogUpdateAvaible = AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
        alertDialogUpdateAvaible.setView(dialogView)

        alertDialogUpdateAvaible.setTitle(resources.getString(R.string.update_avaible))

        alertDialogUpdateAvaible.setPositiveButton(R.string.yes) { _, _ ->
            DownloadUpdate().execute()

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
        editor.putInt("notificationId", i)
        editor.apply() //apply new changes
    }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("noteList", null)
        val oneTimeAdvise = sharedPreferences.getInt("oneTime", 0)
        val notificationIdSp = sharedPreferences.getInt("notificationId", 0)
        val type = object: TypeToken<ArrayList<String>>() {
        }.type

        when(json){
            null -> values = ArrayList() //if json is null, so empty, values is just an empty ArrayList
            else-> values = gson.fromJson(json, type) //got JSON values and convert them back to ArrayList

        }

        if(oneTimeAdvise >= 1)
            oneTimeAdviseInt = 1

        i = notificationIdSp

        adapter = ArrayAdapter(this, R.layout.listview_text_color, values)
        alertdialog_list.adapter = adapter

    }


    private fun deleteData(){
        val sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("noteList")
        editor.apply() //apply new changes

        adapter.clear() //clear everything from the adapter

    }

    inner class AsyncTasks : AsyncTask<Unit, Unit, String>(){
        override fun doInBackground(vararg p0: Unit?): String {
            loadData()

            val queue = Volley.newRequestQueue(applicationContext)

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
                            jsonUrlDownloadUri = Uri.parse(jsonUrlDownload)
                            jsonUrlInfoUri = Uri.parse(jsonUrlInfo)

                            checkPermission()
                        }

                    }
                },
                Response.ErrorListener {}) //if update check go fail
            queue.add(stringReq) //add request to queue

            return "FINISHED"
        }
    }

    inner class DownloadUpdate : AsyncTask<Unit, Unit, String>(){
        override fun doInBackground(vararg p0: Unit?): String {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(jsonUrlDownloadUri)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setVisibleInDownloadsUi(true)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PushNotes" + ".apk")

            downloadManager.enqueue(request)

            return "FINISHED"
        }
    }
}



