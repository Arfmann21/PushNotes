package com.arfmann.pushnotes

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_list_of_notes.*

class ListOfNotes : BottomSheetDialogFragment() {

    private var values = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        loadData()

        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setContentView(R.layout.fragment_list_of_notes)

        adapter = ArrayAdapter(context!!, R.layout.listview_text_color, values)
        dialog.notes_listView.adapter = adapter

        if (adapter.isEmpty) {
            dialog.no_notes_textView.visibility = View.VISIBLE
            dialog.delete_notes_button.visibility = View.INVISIBLE
        }

        dialog.setOnShowListener {
            val bottomSheet =
                (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }

        dialog.close_list_button.setOnClickListener {
            dialog.dismiss()
        }

        dialog.delete_notes_button.setOnClickListener {
            Toast.makeText(context!!, resources.getString(R.string.fullDeleted), Toast.LENGTH_LONG)
                .show()

            (activity as MainActivity).deleteData()

            dialog.dismiss()
        }

        dialog.notes_listView.setOnItemClickListener { _, view, i, _ ->

            onListItemClick(view, i, dialog)
        }

        return dialog
    }

    private fun onListItemClick(view: View, i: Int, dialog: BottomSheetDialog) {
        val popupMenu = PopupMenu(context, view)
        val itemList = adapter.getItem(i)

        var title = ""
        var content = ""
        var j = 0

        while (itemList.toString()[j] != '-') {
            title += itemList.toString()[j]
            j++
        }

        val itemLength = itemList.toString().length

        j += 3

        while ((++j) < itemLength) {
            content += itemList.toString()[j]
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.resend_popup -> {
                    if (title != (resources.getString(R.string.no_title) + "  "))
                        (activity as MainActivity).resendTextInputTitle(title)

                    if (content != resources.getString(R.string.no_content))
                        (activity as MainActivity).resendTextInputContent(content)

                    dialog.dismiss()

                    true
                }

                R.id.copy_popup -> {
                    val myClipboard: ClipboardManager =
                        context!!.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                    val myClip = ClipData.newPlainText("text", itemList)
                    myClipboard.setPrimaryClip(myClip)

                    Toast.makeText(
                        context,
                        resources.getString(R.string.clipboardNote),
                        Toast.LENGTH_LONG
                    ).show()

                    true
                }

                R.id.delete_popup -> {
                    adapter.remove(itemList)
                    values.remove(itemList)

                    if (values.isEmpty())
                        dialog.dismiss()
                    saveData()
                    (activity as MainActivity).loadData()

                    Toast.makeText(context!!, getString(R.string.note_deleted), Toast.LENGTH_LONG)
                        .show()

                    true
                }

                else -> false
            }

        }

        popupMenu.inflate(R.menu.popup_menu)

        try {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return null
    }

    private fun saveData() {
        val sharedPreferences = context!!.getSharedPreferences(
            "shared preferences",
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json =
            gson.toJson(values) //convert ArrayList to JSON (shared preferences can't handle ArrayList)
        editor.putString("noteList", json) //save the new JSON with values
        editor.apply() //apply new changes
    }

    private fun loadData() {
        val sharedPreferences = context!!.getSharedPreferences(
            "shared preferences",
            AppCompatActivity.MODE_PRIVATE
        )
        val gson = Gson()
        val json = sharedPreferences.getString("noteList", null)

        val type = object : TypeToken<ArrayList<String>>() {
        }.type

        values = if(json == null) ArrayList() else gson.fromJson(json, type)
    }
}