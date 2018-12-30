package com.gtgalone.gulliver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.gtgalone.gulliver.views.TextMessage
import com.gtgalone.gulliver.models.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.gtgalone.gulliver.models.ChatMessage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.app_bar_direct_messages_log.*
import kotlinx.android.synthetic.main.content_direct_messages_log.*

class DirectMessagesLogActivity : AppCompatActivity() {

  val adapter = GroupAdapter<ViewHolder>()

  val currentUser = MainActivity.currentUser!!

  private lateinit var functions: FirebaseFunctions

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages_log)
    setSupportActionBar(toolbar_direct_messages_log)

    val toUser: User
    if (intent.getParcelableExtra<User>(MainActivity.USER_KEY) != null) {
      toUser = intent.getParcelableExtra(MainActivity.USER_KEY)
    } else {
      val bundle = intent.extras
      toUser = Gson().fromJson(bundle?.getString("toUser"), User::class.java)
    }

    recycler_view_direct_messages_log.adapter = adapter

    functions = FirebaseFunctions.getInstance()

    supportActionBar!!.title = toUser.displayName
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    listenForMessages(toUser)

    direct_messages_log_send_button.setOnClickListener {
      sendMessage(toUser)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private fun listenForMessages(toUser: User) {
    val ref = FirebaseDatabase.getInstance().getReference("/direct-messages-log/${currentUser.uid}/${toUser.uid}")

    ref.addChildEventListener(object: ChildEventListener {
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        Log.d("test", "adds")
        val chatMessage = p0.getValue(ChatMessage::class.java)
        if (chatMessage != null) {
          adapter.add(
            TextMessage(
              chatMessage,
              currentUser
            )
          )
//          adapter.notifyDataSetChanged()
          recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount - 1)
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

    val chatMessage = ChatMessage(body, fromId, toId, System.currentTimeMillis() / 1000)

    fromLogRef.setValue(chatMessage).addOnSuccessListener {
      adapter.notifyDataSetChanged()
    }
    if (fromId != toId) toLogRef.setValue(chatMessage).addOnSuccessListener {
      adapter.notifyDataSetChanged()
    }

    val fromRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$fromId/$toId")
    val toRef = FirebaseDatabase.getInstance().getReference("/direct-messages/$toId/$fromId")

    fromRef.setValue(chatMessage)
    if (fromId != toId) toRef.setValue(chatMessage)

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
