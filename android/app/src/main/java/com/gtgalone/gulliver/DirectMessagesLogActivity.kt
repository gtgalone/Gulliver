package com.gtgalone.gulliver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.gtgalone.gulliver.views.TextMessage
import com.gtgalone.gulliver.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.gtgalone.gulliver.helper.CompareHelper
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.views.DateDivider
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
  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()

  private lateinit var chatMessageRef: CollectionReference
  private lateinit var messageSection: Section
  private lateinit var functions: FirebaseFunctions
  private val messagePerPage = 21L
  private var isInit = true
  private var isLoading = false

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
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
              if (isLoading) return

              adapter.add(0, MessageLoading())
              recycler_view_direct_messages_log.scrollToPosition(0)

              isLoading = true
              val lastTopItem = messageSection.getItem(0) as TextMessage

              chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastTopItem.message.timestamp).limit(messagePerPage).get()
                .addOnSuccessListener {
                  if (it.documents.isEmpty()) {
                    adapter.removeGroup(0)
                    return@addOnSuccessListener
                  }
                  adapter.removeGroup(0)

                  val items = mutableListOf<Item>()
                  val headDateDivider = (adapter.getItem(0) as DateDivider)
                  var lastItem: TextMessage? = null

                  it.documents.forEachReversedByIndex { docSnapshot ->
                    val chatMessage = docSnapshot.toObject(ChatMessage::class.java) ?: return@forEachReversedByIndex

                    if (lastItem == null && it.documents.count() < messagePerPage) {
                      lastItem = TextMessage(chatMessage, uid, true, true)
                      items.add(lastItem!!)

                      return@forEachReversedByIndex
                    } else if (lastItem == null) {
                      lastItem = TextMessage(chatMessage, "")

                      return@forEachReversedByIndex
                    }

                    var isPhoto = lastItem!!.message.fromId != chatMessage.fromId

                    if (CompareHelper.isSameMinute(lastItem!!.message.timestamp, chatMessage.timestamp)) {
                      lastItem!!.setIsTimestamp(false)
                      lastItem!!.notifyChanged()
                    }

                    if (!CompareHelper.isSameDay(lastItem!!.message.timestamp, chatMessage.timestamp)) {
                      items.add(DateDivider(chatMessage.timestamp))
                      isPhoto = true
                    }

                    lastItem = TextMessage(chatMessage, uid, isPhoto, true)

                    items.add(lastItem!!)
                  }

                  if (items.isNotEmpty()) {
                    val currentTopMessage = items.first() as TextMessage
                    headDateDivider.setTimestamp(currentTopMessage.message.timestamp)
                    headDateDivider.notifyChanged()

                    val currentBottomMessage = items.last() as TextMessage
                    currentBottomMessage.setIsTimestamp(!CompareHelper.isSameMinute(lastTopItem.message.timestamp, currentBottomMessage.message.timestamp))
                    currentBottomMessage.notifyChanged()
                  }

                  messageSection.addAll(0, items)
                  isLoading = false
                  recycler_view_direct_messages_log.apply {
                    setItemViewCacheSize(adapter!!.itemCount)
                  }
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
    chatMessageRef = db.collection("directMessagesLog").document(uid).collection(toUser.uid)

    chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(messagePerPage)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        val items = mutableListOf<Item>()
        var lastItem: TextMessage? = null

        querySnapshot!!.documentChanges.forEachReversedByIndex { it ->
          val chatMessage = it.document.toObject(ChatMessage::class.java)
          var isPhoto = false

          if (isInit) {

            if (lastItem == null) {
              lastItem = TextMessage(chatMessage, "")
              return@forEachReversedByIndex
            }

            isPhoto = lastItem!!.message.fromId != chatMessage.fromId

            if (CompareHelper.isSameMinute(lastItem!!.message.timestamp, chatMessage.timestamp)) {
              lastItem!!.setIsTimestamp(false)
              lastItem!!.notifyChanged()
            }

            if (!CompareHelper.isSameDay(lastItem!!.message.timestamp, chatMessage.timestamp)) {
              items.add(DateDivider(chatMessage.timestamp))
              isPhoto = true
            }

            lastItem = TextMessage(chatMessage, uid, isPhoto, true)

            items.add(lastItem!!)
          } else {
            if (it.type == DocumentChange.Type.ADDED) {

              if (messageSection.itemCount > 0) {
                lastItem = messageSection.getItem(messageSection.itemCount - 1) as TextMessage

                isPhoto = lastItem!!.message.fromId != chatMessage.fromId

                if (CompareHelper.isSameMinute(lastItem!!.message.timestamp, chatMessage.timestamp)) {
                  lastItem!!.setIsTimestamp(false)
                  lastItem!!.notifyChanged()
                }

                if (!CompareHelper.isSameDay(lastItem!!.message.timestamp, chatMessage.timestamp)) {
                  messageSection.add(DateDivider(chatMessage.timestamp))
                }
              }

              messageSection.add(TextMessage(chatMessage, uid, isPhoto, true))
              recycler_view_direct_messages_log.apply {
                setItemViewCacheSize(adapter!!.itemCount)
                scrollToPosition(adapter!!.itemCount - 1)
              }
              return@addSnapshotListener
            }
          }
        }
        if (isInit) {
          if (items.isNotEmpty()) adapter.add(DateDivider((items.first() as TextMessage).message.timestamp))

          messageSection = Section(items)
          adapter.add(messageSection)
          isInit = false
          recycler_view_direct_messages_log.apply {
            setHasFixedSize(true)
            adapter = adapter
            setItemViewCacheSize(adapter!!.itemCount)
            scrollToPosition(adapter!!.itemCount - 1)
          }
        }
      }
  }

  private fun sendMessage(toUser: User) {
    val fromId = uid
    val toId = toUser.uid
    if (direct_messages_log_edit_text.text.isEmpty()) return
    val body = direct_messages_log_edit_text.text.toString()

    val fromLogRef = db.collection("directMessagesLog").document(fromId).collection(toId)
    val fromLogKey = fromLogRef.document().id

    fromLogRef.document(fromLogKey)
      .set(ChatMessage(fromLogKey, body, fromId, toId, System.currentTimeMillis()))

    val fromRef = db.collection("directMessages").document(fromId).collection("directMessage")

    fromRef.document(toId)
      .set(ChatMessage(toId, body, fromId, toId, System.currentTimeMillis()))

    if (fromId != toId) {
      val toLogRef = db.collection("directMessagesLog").document(toId).collection(fromId)
      val toLogKey = toLogRef.document().id

      toLogRef.document(toLogKey)
        .set(ChatMessage(toLogKey, body, fromId, toId, System.currentTimeMillis()))

      val toRef = db.collection("directMessages").document(toId).collection("directMessage")

      toRef.document(fromId).set(ChatMessage(fromId, body, fromId, toId, System.currentTimeMillis()))

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
