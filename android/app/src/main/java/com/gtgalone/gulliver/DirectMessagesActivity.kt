package com.gtgalone.gulliver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.gtgalone.gulliver.views.DirectMessagesRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gtgalone.gulliver.models.ChatMessage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.app_bar_direct_messages.*
import kotlinx.android.synthetic.main.content_direct_messages.*
import org.jetbrains.anko.collections.forEachWithIndex

class DirectMessagesActivity : AppCompatActivity() {
  private val adapter = GroupAdapter<ViewHolder>()
  private val directMessages = mutableListOf<Item>()
  private val db = FirebaseFirestore.getInstance()

  private lateinit var directMessageEventListener: ListenerRegistration

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
      intent.putExtra(MainActivity.USER_KEY, directMessagesRow.user)
      startActivity(intent)
    }

    listenForMessages()
  }

  override fun onStop() {
    super.onStop()
    directMessageEventListener.remove()
  }

  override fun onRestart() {
    super.onRestart()
    listenForMessages()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private fun refreshRecyclerViewMessage() {
    val messageSection = Section(directMessages)
    adapter.clear()
    adapter.add(messageSection)
  }

  private fun listenForMessages() {
    val uid = FirebaseAuth.getInstance().uid
    directMessageEventListener = db.collection("directMessages").document(uid!!)
      .collection("directMessage").orderBy("timestamp", Query.Direction.DESCENDING)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        directMessages.clear()
        querySnapshot!!.documents.forEach {
          val directMessage = it.toObject(ChatMessage::class.java) ?: return@forEach
          directMessages.add(DirectMessagesRow(directMessage))
        }
        refreshRecyclerViewMessage()
      }
  }
}
