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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.gtgalone.gulliver.DirectMessagesLogActivity
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.Report
import com.gtgalone.gulliver.models.User
import kotlinx.android.synthetic.main.fragment_people_bottom_sheet_dialog.view.*

class PeopleBottomSheetDialogFragment : BottomSheetDialogFragment() {
  private lateinit var functions: FirebaseFunctions

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    functions = FirebaseFunctions.getInstance()
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
      view.people_bottom_sheet_dialog_fragment_ignore.visibility = View.GONE
      view.people_bottom_sheet_dialog_fragment_unignore.visibility = View.GONE
      return
    }
    view.people_bottom_sheet_dialog_fragment_report.setOnClickListener {
      val editText = EditText(it.context)
      editText.hint = "Reason"
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

    FirebaseDatabase.getInstance().getReference("/users/$uid/ignoring/${user.uid}")
      .addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          if (p0.value != null) {
            view.people_bottom_sheet_dialog_fragment_ignore.visibility = View.GONE
            view.people_bottom_sheet_dialog_fragment_unignore.setOnClickListener {
              FirebaseDatabase.getInstance().apply {
                getReference("/users/$uid/ignoring").child(user.uid).removeValue()
                getReference("/users/${user.uid}/ignoredBy").child(uid!!).removeValue()
              }
              this@PeopleBottomSheetDialogFragment.dialog!!.cancel()
            }
          } else {
            view.people_bottom_sheet_dialog_fragment_unignore.visibility = View.GONE
            view.people_bottom_sheet_dialog_fragment_ignore.setOnClickListener {

              val builder = AlertDialog.Builder(it.context, R.style.DialogTheme)
              builder
                .setMessage("You cannot any communicate with this user, Are you sure?")
                .setPositiveButton(getString(R.string.ignore)) { dialog, which ->
                  FirebaseDatabase.getInstance().apply {
                    getReference("/users/$uid/ignoring/${user.uid}").setValue(true)
                    getReference("/users/${user.uid}/ignoredBy/$uid").setValue(true)
                  }
                  FirebaseFirestore.getInstance().apply {
                    collection("directMessages").document(uid!!)
                      .collection("directMessage").document(user.uid).delete()

                    functions
                      .getHttpsCallable("recursiveDelete")
                      .call(hashMapOf("path" to "/directMessagesLog/$uid/${user.uid}"))
                      .continueWith { task ->
                        // This continuation runs on either success or failure, but if the task
                        // has failed then result will throw an Exception which will be
                        // propagated down.
                        val result = task.result?.data as String
                        result
                      }

                    collection("directMessages").document(user.uid)
                      .collection("directMessage").document(uid).delete()

                    functions
                      .getHttpsCallable("recursiveDelete")
                      .call(hashMapOf("path" to "/directMessagesLog/${user.uid}/$uid"))
                      .continueWith { task ->
                        // This continuation runs on either success or failure, but if the task
                        // has failed then result will throw an Exception which will be
                        // propagated down.
                        val result = task.result?.data as String
                        result
                      }
                  }
                  this@PeopleBottomSheetDialogFragment.dialog!!.cancel()
                }
                .setNegativeButton("cancel") { dialog, which ->
                  dialog.cancel()
                }
                .show()
            }
          }
        }
        override fun onCancelled(p0: DatabaseError) {}
      })
  }
}