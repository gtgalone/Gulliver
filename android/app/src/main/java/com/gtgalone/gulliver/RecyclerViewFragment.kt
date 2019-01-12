package com.gtgalone.gulliver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gtgalone.gulliver.helper.CompareHelper
import com.gtgalone.gulliver.models.AdapterItemMessage
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.models.User
import org.jetbrains.anko.collections.forEachReversedByIndex

class RecyclerViewFragment : Fragment() {

  private lateinit var currentLayoutManagerType: LayoutManagerType
  private lateinit var recyclerView: RecyclerView
  private lateinit var layoutManager: RecyclerView.LayoutManager
  private lateinit var dataset: ArrayList<AdapterItemMessage>

  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()

  private lateinit var chatMessageRef: CollectionReference
  private val messagePerPage = 21L
  private var isInit = true
  private var isLoading = false

  enum class LayoutManagerType { LINEAR_LAYOUT_MANAGER }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dataset = arrayListOf()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_recycler_view, container, false).apply {
      tag = TAG
    }

    recyclerView = rootView.findViewById(R.id.recycler_view_fragment)

    layoutManager = LinearLayoutManager(activity)
    (layoutManager as LinearLayoutManager).stackFromEnd = true
    recyclerView.layoutManager = layoutManager

    currentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER

    if (savedInstanceState != null) {
      currentLayoutManagerType = savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER) as LayoutManagerType
    }

    val adapter = CustomAdapter(dataset)

    recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
          RecyclerView.SCROLL_STATE_IDLE -> {
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
              if (
                isLoading or
                (dataset.size == 0) or
                (dataset[0].type == 1)
              ) return

              dataset[0] = AdapterItemMessage(AdapterItemMessage.TYPE_MESSAGE_LOADER)

              adapter.notifyItemInserted(0)
              recyclerView.scrollToPosition(0)

              isLoading = true

              val lastTopItem = dataset[0]

              chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastTopItem.message!!.timestamp).limit(messagePerPage).get()
                .addOnSuccessListener {
                  if (it.documents.isEmpty()) {
                    dataset.drop(1)
                    adapter.notifyItemRemoved(0)
                    return@addOnSuccessListener
                  }
                  dataset.drop(1)
                  adapter.notifyItemRemoved(0)

                  val headDateDivider = dataset[0]
                  var lastItem: AdapterItemMessage? = null

                  it.documents.forEachReversedByIndex { docSnapshot ->
                    val chatMessage = docSnapshot.toObject(ChatMessage::class.java) ?: return@forEachReversedByIndex

                    if (lastItem == null && it.documents.count() < messagePerPage) {
                      lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, uid, true, true, chatMessage)
                      dataset.add(lastItem!!)

                      return@forEachReversedByIndex
                    } else if (lastItem == null) {
                      lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, message = chatMessage)

                      return@forEachReversedByIndex
                    }

                    var isPhoto = lastItem!!.message!!.fromId != chatMessage.fromId

                    if (CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
//                      lastItem!!.setIsTimestamp(false)
//                      lastItem!!.notifyChanged()
                    }

                    if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
                      val index = dataset.size - 1
                      dataset[index] = AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage)
                      adapter.notifyItemInserted(index)
                      isPhoto = true
                    }

                    lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, uid, isPhoto, true, message = chatMessage)

                    dataset.add(lastItem!!)
                  }

                  if (dataset.isNotEmpty()) {
                    val currentTopMessage = dataset.first()
//                    headDateDivider.setTimestamp(currentTopMessage.message.timestamp)
//                    headDateDivider.notifyChanged()

                    val currentBottomMessage = dataset.last()
//                    currentBottomMessage.setIsTimestamp(!CompareHelper.isSameMinute(lastTopItem.message.timestamp, currentBottomMessage.message.timestamp))
//                    currentBottomMessage.notifyChanged()
                  }

//                  dataset.addAll(0, items)
                  adapter.notifyDataSetChanged()
                  isLoading = false
                  recyclerView.apply {
                    setItemViewCacheSize(adapter.itemCount)
                  }
                }
            }
          }
        }
      }
    })

    val toUser = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return rootView

    chatMessageRef = db.collection("directMessagesLog").document(uid).collection(toUser.uid)

    chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(messagePerPage)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        var lastItem: AdapterItemMessage? = null

        querySnapshot!!.documentChanges.forEachReversedByIndex { it ->
          val chatMessage = it.document.toObject(ChatMessage::class.java)
          var isPhoto = false

          if (isInit) {
            Log.d("test", "${querySnapshot.documentChanges.count()}")
            if (lastItem == null && querySnapshot.documentChanges.count() < messagePerPage) {
              lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, uid, true, true, message = chatMessage)
              dataset.add(lastItem!!)

              return@forEachReversedByIndex
            } else if (lastItem == null) {
              lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, message = chatMessage)
              return@forEachReversedByIndex
            }

            isPhoto = lastItem!!.message!!.fromId != chatMessage.fromId

            if (CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
              lastItem!!.setIsTimestamp(false)
            }

            if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
              dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
              isPhoto = true
            }

            lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, uid, isPhoto, true, message = chatMessage)

            dataset.add(lastItem!!)
            adapter.notifyDataSetChanged()
          } else {
            if (it.type == DocumentChange.Type.ADDED) {

              if (dataset.size > 0) {
                if (dataset[dataset.size - 1].type != AdapterItemMessage.TYPE_TEXT_MESSAGE) return@forEachReversedByIndex
                if (lastItem == null) lastItem = dataset[dataset.size - 1]

                isPhoto = lastItem!!.message!!.fromId != chatMessage.fromId

                if (CompareHelper.isSameMinute(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
                  Log.d("test", "${dataset.size - 1}")

//                  dataset[dataset.size - 1] = AdapterItemMessage(
//                    AdapterItemMessage.TYPE_TEXT_MESSAGE,
//                    lastItem!!.uid,
//                    lastItem!!.isPhoto,
//                    false,
//                    lastItem!!.message)
                  adapter.notifyItemChanged(dataset.size - 1)
                }

                if (!CompareHelper.isSameDay(lastItem!!.message!!.timestamp, chatMessage.timestamp)) {
                  dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
                }
              } else {
                dataset.add(AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = chatMessage))
                isPhoto = true
              }

              lastItem = AdapterItemMessage(AdapterItemMessage.TYPE_TEXT_MESSAGE, uid, isPhoto, true, chatMessage)

              dataset.add(lastItem!!)
              adapter.notifyItemInserted(dataset.size - 1)

              recyclerView.apply {
                setItemViewCacheSize(adapter.itemCount - 1)
                scrollToPosition(adapter.itemCount - 1)
              }
              return@addSnapshotListener
            }
          }
        }
        if (isInit) {
          if (dataset.isNotEmpty()) {
            dataset[0] = AdapterItemMessage(AdapterItemMessage.TYPE_DATE_DIVIDER, message = dataset.first().message)

            adapter.notifyItemInserted(0)
          }

          adapter.notifyDataSetChanged()
          isInit = false
          recyclerView.apply {
            setHasFixedSize(true)
            this.adapter = adapter
            setItemViewCacheSize(adapter.itemCount)
            scrollToPosition(adapter.itemCount - 1)
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