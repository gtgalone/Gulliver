package com.gtgalone.gulliver.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.gtgalone.gulliver.CustomAdapter
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.helper.CompareHelper
import com.gtgalone.gulliver.models.AdapterItemMessage
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.models.User
import org.jetbrains.anko.collections.forEachReversedByIndex

class RecyclerViewFragment : Fragment() {
  private lateinit var currentLayoutManagerType: LayoutManagerType
  private lateinit var recyclerView: RecyclerView
  private lateinit var layoutManager: RecyclerView.LayoutManager
  private lateinit var chatMessageRef: CollectionReference

  private val dataset: ArrayList<AdapterItemMessage> = arrayListOf()
  private val adapter = CustomAdapter(dataset)
  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()
  private val messagePerPage = 21L
  private var isInit = true
  private var isLoading = false

  enum class LayoutManagerType { LINEAR_LAYOUT_MANAGER }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_recycler_view, container, false).apply {
      tag = TAG
    }

    recyclerView = rootView.findViewById(R.id.recycler_view_fragment)

    layoutManager = LinearLayoutManager(activity)
    (layoutManager as LinearLayoutManager).stackFromEnd = true
    recyclerView.layoutManager = layoutManager

    currentLayoutManagerType =
        LayoutManagerType.LINEAR_LAYOUT_MANAGER

    if (savedInstanceState != null) {
      currentLayoutManagerType = savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER) as LayoutManagerType
    }

    recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
          RecyclerView.SCROLL_STATE_IDLE -> {
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {

              if (
                isLoading or
                (dataset.size == 0) or
                (dataset[0].type == 0)
              ) return

              dataset.add(0, AdapterItemMessage(AdapterItemMessage.TYPE_MESSAGE_LOADER))
              adapter.notifyItemInserted(0)
              recyclerView.scrollToPosition(0)

              isLoading = true

              val lastTopItem = dataset[2]

              chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastTopItem.message!!.timestamp).limit(messagePerPage).get()
                .addOnSuccessListener {
                  if (it.documents.isEmpty()) {
                    dataset.removeAt(0)
                    adapter.notifyItemRemoved(0)
                    return@addOnSuccessListener
                  }
                  dataset.removeAt(0)
                  adapter.notifyItemRemoved(0)

                  var lastItem: AdapterItemMessage? = null

                  val items = mutableListOf<AdapterItemMessage>()
                  it.documents.forEachReversedByIndex { docSnapshot ->
                    val chatMessage = docSnapshot.toObject(ChatMessage::class.java) ?: return@forEachReversedByIndex

                    if (lastItem == null && it.documents.count() < messagePerPage) {
                      lastItem = AdapterItemMessage(chatMessage.messageType!!, uid, true, chatMessage)
                      items.add(lastItem!!)

                      return@forEachReversedByIndex
                    } else if (lastItem == null) {
                      lastItem = AdapterItemMessage(chatMessage.messageType!!, message = chatMessage)

                      return@forEachReversedByIndex
                    }

                    var isPhoto = !CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)
                      .and(lastItem!!.message!!.fromId == chatMessage.fromId)

                    if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
                      items.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
                      isPhoto = true
                    }

                    lastItem = AdapterItemMessage(chatMessage.messageType!!, uid, isPhoto, chatMessage)
                    items.add(lastItem!!)
                  }

                  if (dataset.isNotEmpty()) {
                    val currentTopMessage = items.first()
                    dataset[0] = AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = currentTopMessage.message)
                    adapter.notifyItemChanged(0)
                  }

                  dataset.addAll(1, items)
                  adapter.notifyItemRangeInserted(1, items.count())
                  isLoading = false
                  recyclerView.apply {
                    setItemViewCacheSize(adapter!!.itemCount)
                  }
                }
            }
          }
        }
      }
    })

    val chatType = arguments!!.getInt(MainActivity.CHAT_TYPE)

    if (chatType == MainActivity.CHAT_TYPE_MAIN) {
      val currentUser = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return rootView
      chatMessageRef = db.collection("cities").document(currentUser.currentCity!!)
        .collection("channels").document(currentUser.currentChannel!!)
        .collection("chatMessages")
    } else {
      val toUser = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return rootView
      chatMessageRef = db.collection("directMessagesLog").document(uid).collection(toUser.uid)
    }

    chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(messagePerPage)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        var lastItem: AdapterItemMessage? = null

        querySnapshot!!.documentChanges.forEachReversedByIndex { it ->
          val chatMessage = it.document.toObject(ChatMessage::class.java)

          var isPhoto: Boolean

          if (isInit) {
            if (lastItem == null && querySnapshot.documentChanges.count() < messagePerPage) {
              lastItem = AdapterItemMessage(chatMessage.messageType!!, uid, true, chatMessage)
              dataset.add(lastItem!!)
              adapter.notifyItemInserted(dataset.lastIndex)
              return@forEachReversedByIndex
            } else if (lastItem == null) {
              lastItem = AdapterItemMessage(chatMessage.messageType!!, message = chatMessage)
              return@forEachReversedByIndex
            }

            isPhoto = !CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)
              .and(lastItem!!.message!!.fromId == chatMessage.fromId)

            if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
              dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
              adapter.notifyItemInserted(dataset.lastIndex)
              isPhoto = true
            }

            lastItem = AdapterItemMessage(chatMessage.messageType!!, uid, isPhoto, chatMessage)

            dataset.add(lastItem!!)
          } else {
            if (it.type == DocumentChange.Type.ADDED) {
              if (dataset.size > 0) {
                if (dataset[dataset.size - 1].type != chatMessage.messageType!!) return@forEachReversedByIndex
                if (lastItem == null) lastItem = dataset[dataset.size - 1]

                isPhoto = !CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)
                  .and(lastItem!!.message!!.fromId == chatMessage.fromId)

                if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
                  dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
                  adapter.notifyItemInserted(dataset.lastIndex)
                }
              } else {
                dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
                adapter.notifyItemInserted(dataset.lastIndex)
                isPhoto = true
              }

              lastItem = AdapterItemMessage(chatMessage.messageType!!, uid, isPhoto, chatMessage)

              dataset.add(lastItem!!)
              adapter.notifyItemInserted(dataset.lastIndex)

              recyclerView.apply {
                val count = adapter!!.itemCount
                setItemViewCacheSize(count)
                scrollToPosition(count - 1)
              }
            }
          }
        }
        if (isInit) {
          if (dataset.isNotEmpty()) {
            dataset.add(0, AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = dataset.first().message))
          }
          isInit = false
          recyclerView.apply {
            setHasFixedSize(true)
            adapter = this@RecyclerViewFragment.adapter
            val count = dataset.count()
            setItemViewCacheSize(count)
            scrollToPosition(count - 1)
          }
        }
      }

    return rootView
  }

  companion object {
    private const val TAG = "RecyclerViewFragment"
    private const val KEY_LAYOUT_MANAGER = "layoutManager"
  }
}