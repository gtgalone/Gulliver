package com.example.jehun.gulliver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.jehun.gulliver.models.DirectMessageLog
import com.example.jehun.gulliver.views.DirectMessagesLogFrom
import com.example.jehun.gulliver.views.DirectMessagesLogTo
import com.example.jehun.gulliver.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_log.*

class DirectMessagesLogActivity : AppCompatActivity() {

  val adapter = GroupAdapter<ViewHolder>()

  val currentUser = MainActivity.currentUser!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages_log)
    recycler_view_direct_messages_log.adapter = adapter

    val toUser = intent.getParcelableExtra<User>(PeopleActivity.USER_KEY)

    supportActionBar!!.title = toUser.displayName

    listenForMessages(toUser)


    direct_messages_log_send_button.setOnClickListener {
      sendMessage(toUser)
    }

  }

  private fun listenForMessages(toUser: User) {
    val ref = FirebaseDatabase.getInstance().getReference("/direct-messages-log/${currentUser.uid}/${toUser.uid}")

    ref.addChildEventListener(object: ChildEventListener {
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {

        val directMessage = p0.getValue(DirectMessageLog::class.java)
        if (directMessage != null) {
          when (directMessage.toId) {
            directMessage.fromId -> adapter.add(
              DirectMessagesLogTo(
                directMessage.text,
                currentUser
              )
            )
            currentUser.uid -> adapter.add(
              DirectMessagesLogFrom(
                directMessage.text,
                toUser
              )
            )
            else -> adapter.add(DirectMessagesLogTo(directMessage.text, currentUser))
          }

          recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount)
          recycler_view_direct_messages_log.adapter = adapter
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

    if (direct_messages_log_edit_text.text.isEmpty()) return

    Log.d("test", "$fromId, $toId")
    val fromLogRef = FirebaseDatabase.getInstance().getReference("/direct-messages-log/$fromId/$toId").push()
    val toLogRef = FirebaseDatabase.getInstance().getReference("/direct-messages-log/$toId/$fromId").push()

    val directMessage = DirectMessageLog(fromLogRef.key!!, direct_messages_log_edit_text.text.toString(), fromId, toId, System.currentTimeMillis() / 1000)

    fromLogRef.setValue(directMessage).addOnSuccessListener {
      recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount)
      recycler_view_direct_messages_log.adapter = adapter
    }
    if (fromId != toId) toLogRef.setValue(directMessage).addOnSuccessListener {
      recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount)
      recycler_view_direct_messages_log.adapter = adapter
    }

    val fromRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$fromId/$toId")
    val toRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$toId/$fromId")

    fromRef.setValue(directMessage)
    if (fromId != toId) toRef.setValue(directMessage)

    direct_messages_log_edit_text.text.clear()

  }

}