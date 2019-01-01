package com.gtgalone.gulliver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.gtgalone.gulliver.views.DirectMessagesRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gtgalone.gulliver.models.ChatMessage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.app_bar_direct_messages.*
import kotlinx.android.synthetic.main.content_direct_messages.*

class DirectMessagesActivity : AppCompatActivity() {
  private val adapter = GroupAdapter<ViewHolder>()
  private val directMessagesMap = HashMap<String, ChatMessage>()
  private val db = FirebaseFirestore.getInstance()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages)
    setSupportActionBar(toolbar_direct_messages)

    supportActionBar!!.title = "Direct Messages"
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    recycler_view_direct_messages.adapter = adapter
    recycler_view_direct_messages.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

    adapter.setOnItemClickListener { item, view ->
      val intent = Intent(view.context, DirectMessagesLogActivity::class.java)
      val directMessagesRow = item as DirectMessagesRow
      intent.putExtra(MainActivity.USER_KEY, directMessagesRow.person)
      startActivity(intent)
    }

    listenForMessages()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private fun refreshRecyclerViewMessage() {
    adapter.clear()
    directMessagesMap.values.forEach {
      adapter.add(DirectMessagesRow(it))
    }
  }

  private fun listenForMessages() {
    val uid = FirebaseAuth.getInstance().uid
//    db.collection("directMessages").document(uid!!).get().add
//      .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
//
//        val directMessage = documentSnapshot!!.toObject(ChatMessage::class.java) ?: return@addSnapshotListener
//        directMessagesMap[documentSnapshot.id] = directMessage
//        refreshRecyclerViewMessage()
//      }
    val ref = FirebaseDatabase.getInstance().getReference("/direct-messages/$uid")

    ref.addChildEventListener(object: ChildEventListener{
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val directMessage = p0.getValue(ChatMessage::class.java) ?: return

        directMessagesMap[p0.key!!] = directMessage
        refreshRecyclerViewMessage()
      }

      override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        val directMessage = p0.getValue(ChatMessage::class.java) ?: return

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
