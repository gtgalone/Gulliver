package com.gtgalone.gulliver

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.gtgalone.gulliver.fragments.SendMessageFragment
import java.lang.IllegalStateException

class InsertImageDialogFragment : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return SendMessageFragment.let {
      val builder = AlertDialog.Builder(context, R.style.DialogTheme)
      val uri = arguments!!.getString("aa")
      val imageView = ImageView(context)
      imageView.setImageURI(Uri.parse(uri))
      builder
        .setView(imageView)
        .setPositiveButton("send") { dialog, which ->
          Log.d("test", "click good")
        }
        .setNegativeButton("cancel") { dialog, which ->
          dialog.cancel()
        }


      builder.create()
    } ?: throw IllegalStateException("Activity cannot be null")
  }
}