package com.example.jehun.gulliver

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.jehun.gulliver.models.DirectMessage
import com.example.jehun.gulliver.models.DirectMessageLogFrom
import com.example.jehun.gulliver.models.DirectMessageLogTo
import com.example.jehun.gulliver.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.abc_alert_dialog_material.*
import kotlinx.android.synthetic.main.activity_direct_message_log.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class DirectMessageLogActivity : AppCompatActivity() {

  val adapter = GroupAdapter<ViewHolder>()

  val currentUser = MainActivity.currentUser!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_message_log)

    val toUser = intent.getParcelableExtra<User>(PeopleActivity.USER_KEY)

    supportActionBar!!.title = toUser.displayName

    listenForMessages(toUser)

    recycler_view_direct_message_log.adapter = adapter

    direct_message_log_send_button.setOnClickListener {
      sendMessage(toUser)
    }

  }

  private fun listenForMessages(toUser: User) {
    val ref = FirebaseDatabase.getInstance().getReference("/direct-messages/${currentUser.uid}/${toUser.uid}")

    ref.addChildEventListener(object: ChildEventListener {
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {

        val directMessage = p0.getValue(DirectMessage::class.java)
        if (directMessage != null) {
          when (directMessage.toId) {
            directMessage.fromId -> adapter.add(DirectMessageLogTo(directMessage.text, currentUser))
            currentUser.uid -> adapter.add(DirectMessageLogFrom(directMessage.text, toUser))
            else -> adapter.add(DirectMessageLogTo(directMessage.text, currentUser))
          }

          recycler_view_direct_message_log.scrollToPosition(adapter.itemCount)
          recycler_view_direct_message_log.adapter = adapter
        }
      }

      override fun onChildChanged(p0: DataSnapshot, p1: String?) {
      }

      override fun onChildMoved(p0: DataSnapshot, p1: String?) {
      }

      override fun onChildRemoved(p0: DataSnapshot) {
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
  }

  private fun sendMessage(toUser: User) {
    val fromId = currentUser.uid
    val toId = toUser.uid

    if (direct_message_log_edit_text.text.isEmpty()) return

    Log.d("test", "$fromId, $toId")
    val fromRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$fromId/$toId").push()
    val toRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$toId/$fromId").push()

    val directMessage = DirectMessage(fromRef.key!!, direct_message_log_edit_text.text.toString(), fromId, toId, System.currentTimeMillis() / 1000)

    fromRef.setValue(directMessage).addOnSuccessListener {
      recycler_view_direct_message_log.scrollToPosition(adapter.itemCount)
      recycler_view_direct_message_log.adapter = adapter
    }
    if (fromId != toId) toRef.setValue(directMessage).addOnSuccessListener {
      recycler_view_direct_message_log.scrollToPosition(adapter.itemCount)
      recycler_view_direct_message_log.adapter = adapter
    }

    direct_message_log_edit_text.text.clear()

  }

}