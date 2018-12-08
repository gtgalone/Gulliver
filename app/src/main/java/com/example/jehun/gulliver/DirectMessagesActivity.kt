package com.example.jehun.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.jehun.gulliver.models.DirectMessageLog
import com.example.jehun.gulliver.views.DirectMessagesRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages.*

class DirectMessagesActivity : AppCompatActivity() {
  private val adapter = GroupAdapter<ViewHolder>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages)

    supportActionBar!!.title = "Direct Message"

    recycler_view_direct_messages.adapter = adapter
    recycler_view_direct_messages.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

    listenForMessages()
  }

  val directMessagesMap = HashMap<String, DirectMessageLog>()

  private fun refreshRecyclerViewMessage() {
    adapter.clear()
    directMessagesMap.values.forEach {
      adapter.add(DirectMessagesRow(it))
    }
  }
  private fun listenForMessages() {
    val uid = FirebaseAuth.getInstance().uid
    val ref = FirebaseDatabase.getInstance().getReference("/direct-messages/$uid")

    ref.addChildEventListener(object: ChildEventListener{
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val directMessage = p0.getValue(DirectMessageLog::class.java) ?: return

        directMessagesMap[p0.key!!] = directMessage
        refreshRecyclerViewMessage()
      }

      override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        val directMessage = p0.getValue(DirectMessageLog::class.java) ?: return

        directMessagesMap[p0.key!!] = directMessage
        refreshRecyclerViewMessage()
      }

      override fun onChildMoved(p0: DataSnapshot, p1: String?) {
      }

      override fun onChildRemoved(p0: DataSnapshot) {
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
  }
}
