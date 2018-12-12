package com.gtgalone.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gtgalone.gulliver.models.DirectMessageLog
import com.gtgalone.gulliver.views.DirectMessagesLogFrom
import com.gtgalone.gulliver.views.DirectMessagesLogTo
import com.gtgalone.gulliver.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_log.*

class DirectMessagesLogActivity : AppCompatActivity() {

  val adapter = GroupAdapter<ViewHolder>()

  val currentUser = MainActivity.currentUser!!

  private lateinit var functions: FirebaseFunctions

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages_log)
    recycler_view_direct_messages_log.adapter = adapter

    functions = FirebaseFunctions.getInstance()

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

    val fromLogRef = FirebaseDatabase.getInstance().getReference("/direct-messages-log/$fromId/$toId").push()
    val toLogRef = FirebaseDatabase.getInstance().getReference("/direct-messages-log/$toId/$fromId").push()

    val body = direct_messages_log_edit_text.text.toString()

    val directMessage = DirectMessageLog(fromLogRef.key!!, body, fromId, toId, System.currentTimeMillis() / 1000)

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

    if (fromId == toId) return

    val data = hashMapOf(
      "fromId" to fromId,
      "toId" to toId,
      "body" to body
    )

    functions
      .getHttpsCallable("sendMessage")
      .call(data)
      .continueWith { task ->
        // This continuation runs on either success or failure, but if the task
        // has failed then result will throw an Exception which will be
        // propagated down.
        val result = task.result?.data as String
        result
      }
  }
}
