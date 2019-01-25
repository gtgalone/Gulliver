package com.gtgalone.gulliver.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gtgalone.gulliver.DirectMessagesLogActivity
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.Report
import com.gtgalone.gulliver.models.User
import kotlinx.android.synthetic.main.fragment_people_bottom_sheet_dialog.view.*

class PeopleBottomSheetDialogFragment : BottomSheetDialogFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    return inflater.inflate(R.layout.fragment_people_bottom_sheet_dialog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val user = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return

    view.people_bottom_sheet_dialog_fragment_display_name.text = user.displayName
    Glide.with(view).load(user.photoUrl).into(view.people_bottom_sheet_dialog_fragment_photo)
    view.people_bottom_sheet_dialog_fragment_direct_message.setOnClickListener {
      val intent = Intent(view.context, DirectMessagesLogActivity::class.java)
      intent.putExtra(MainActivity.USER_KEY, user)
      startActivity(intent)
    }

    val uid = FirebaseAuth.getInstance().uid
    if (uid == user.uid) {
      view.people_bottom_sheet_dialog_fragment_report.visibility = View.GONE
      return
    }
    view.people_bottom_sheet_dialog_fragment_report.setOnClickListener {
      val editText = EditText(it.context)
      val builder = AlertDialog.Builder(it.context, R.style.DialogTheme)
      builder
        .setTitle(getString(R.string.report))
        .setView(editText)
        .setPositiveButton("send") { dialog, which ->
          val reportRef = FirebaseFirestore.getInstance().collection("reports")
          val key = reportRef.document().id
          reportRef.document(key).set(Report(key, editText.text.toString(), uid!!, user.uid, System.currentTimeMillis()))
        }
        .setNegativeButton("cancel") { dialog, which ->
          dialog.cancel()
        }
      val dialog = builder.create()
      dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
      dialog.show()
    }
  }
}