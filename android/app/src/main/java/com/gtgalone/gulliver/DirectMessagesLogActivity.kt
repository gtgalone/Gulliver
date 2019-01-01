package com.gtgalone.gulliver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.gtgalone.gulliver.views.TextMessage
import com.gtgalone.gulliver.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.views.MessageLoading
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.app_bar_direct_messages_log.*
import kotlinx.android.synthetic.main.content_direct_messages_log.*
import org.jetbrains.anko.collections.forEachReversedByIndex

class DirectMessagesLogActivity : AppCompatActivity() {

  private val adapter = GroupAdapter<ViewHolder>()
  private val currentUser = MainActivity.currentUser!!
  private val db = FirebaseFirestore.getInstance()

  private lateinit var chatMessageRef: CollectionReference
  private lateinit var messageSection: Section
  private lateinit var functions: FirebaseFunctions
  private var isInit = true

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

    recycler_view_direct_messages_log.addOnScrollListener(object: RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
          RecyclerView.SCROLL_STATE_IDLE -> {
            Log.d("test", "idle ${(recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()}")
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
              adapter.add(0, MessageLoading())
              recycler_view_direct_messages_log.scrollToPosition(0)

              chatMessageRef.orderBy("timeStamp", Query.Direction.DESCENDING)
                .startAfter((adapter.getItem(1) as TextMessage).message.timeStamp).limit(10).get()
                .addOnSuccessListener {
                  Log.d("test", "load more")
                  if (it.documents.isEmpty()) {
                    adapter.removeGroup(0)
                    Log.d("test", "empty")
                    return@addOnSuccessListener
                  }
                  adapter.removeGroup(0)
                  val items = mutableListOf<Item>()
                  it.documents.forEachReversedByIndex { docSnapshot ->
                    Log.d("test", "add")
                    items.add(TextMessage(docSnapshot.toObject(ChatMessage::class.java)!!, currentUser!!))
                  }
                  messageSection = Section(items)
                  adapter.add(0, messageSection)
                }
            }
          }
        }
      }
    })
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
    chatMessageRef = db.collection("directMessagesLog").document(currentUser.uid).collection(toUser.uid)

    chatMessageRef.orderBy("timeStamp", Query.Direction.DESCENDING).limit(10)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        val items = mutableListOf<Item>()
        querySnapshot!!.documentChanges.forEachReversedByIndex {
          val chatMessage = it.document.toObject(ChatMessage::class.java)
          if (isInit) {
            items.add(TextMessage(chatMessage, currentUser))
          } else {
            if (it.type == DocumentChange.Type.ADDED) {
              adapter.add(TextMessage(chatMessage, currentUser))
              recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount - 1)
            }
          }
        }

        if (isInit) {
          messageSection = Section(items)
          adapter.add(messageSection)
          isInit = false
          recycler_view_direct_messages_log.scrollToPosition(adapter.itemCount - 1)
        }
      }
  }

  private fun sendMessage(toUser: User) {
    val fromId = currentUser.uid
    val toId = toUser.uid
    if (direct_messages_log_edit_text.text.isEmpty()) return
    val body = direct_messages_log_edit_text.text.toString()

    val fromLogRef = db.collection("directMessagesLog").document(fromId).collection(toId)
    val fromLogKey = fromLogRef.document().id

    fromLogRef.document(fromLogKey)
      .set(ChatMessage(fromLogKey, body, fromId, toId, System.currentTimeMillis()))

    val fromRef = db.collection("directMessages").document(fromId).collection(toId)
    val fromKey = fromRef.document().id

    fromRef.document(fromKey)
      .set(ChatMessage(fromKey, body, fromId, toId, System.currentTimeMillis()))

    if (fromId != toId) {
      val toLogRef = db.collection("directMessagesLog").document(toId).collection(fromId)
      val toLogKey = toLogRef.document().id

      toLogRef.document(toLogKey)
        .set(ChatMessage(toLogKey, body, fromId, toId, System.currentTimeMillis()))

      val toRef = db.collection("directMessages").document(toId).collection(fromId)
      val toKey = toRef.document().id

      toRef.document(toKey).set(ChatMessage(toKey, body, fromId, toId, System.currentTimeMillis()))

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
    direct_messages_log_edit_text.text.clear()
  }
}
