package com.gtgalone.gulliver.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.models.User
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File


class SendMessageFragment : Fragment() {
  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()

  private lateinit var functions: FirebaseFunctions
  private lateinit var sendButton: ImageView
  private lateinit var editText: EditText
  private lateinit var insertImage: ImageView

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_send_message, container, false).apply {
      tag = TAG
    }

    functions = FirebaseFunctions.getInstance()

    sendButton = rootView.findViewById(R.id.send_button)
    editText = rootView.findViewById(R.id.edit_text)
    insertImage = rootView.findViewById(R.id.insert_message)

    val toUser = arguments!!.getParcelable<User>(MainActivity.USER_KEY) ?: return rootView
    val chatType = arguments!!.getInt(MainActivity.CHAT_TYPE)

    sendButton.setOnClickListener {
      if (chatType == 0) {
        sendMessage(toUser)
      } else {
        sendDirectMessage(toUser)
      }
    }

    insertImage.setOnClickListener {
      Log.d("test", MediaStore.Images.Media.query())
    }

    return rootView
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == 0) {
      Log.d("test", data!!.data.toString())
    }

  }

  override fun onStop() {
    editText.clearFocus()
    super.onStop()
  }

  private fun sendMessage(user: User) {
    if (editText.text.trim().isEmpty()) return

    val body = editText.text.toString()

    val chatMessageRef = db.collection("cities").document(user.currentCity!!)
      .collection("channels").document(user.currentChannel!!)
      .collection("chatMessages")

    val key = chatMessageRef.document().id
    chatMessageRef.document(key)
      .set(ChatMessage(key, user.uid, user.currentChannel, System.currentTimeMillis(), body))

    editText.text.clear()
  }

  private fun sendDirectMessage(user: User) {
    val fromId = uid
    val toId = user.uid
    if (editText.text.trim().isEmpty()) return
    val body = editText.text.toString()

    val fromLogRef = db.collection("directMessagesLog").document(fromId).collection(toId)
    val fromLogKey = fromLogRef.document().id

    fromLogRef.document(fromLogKey)
      .set(ChatMessage(fromLogKey, fromId, toId, System.currentTimeMillis(), body))

    val fromRef = db.collection("directMessages").document(fromId).collection("directMessage")

    fromRef.document(toId)
      .set(ChatMessage(toId, fromId, toId, System.currentTimeMillis(), body))

    if (fromId != toId) {
      val toLogRef = db.collection("directMessagesLog").document(toId).collection(fromId)
      val toLogKey = toLogRef.document().id

      toLogRef.document(toLogKey)
        .set(ChatMessage(toLogKey, fromId, toId, System.currentTimeMillis(), body))

      val toRef = db.collection("directMessages").document(toId).collection("directMessage")

      toRef.document(fromId).set(ChatMessage(fromId, fromId, toId, System.currentTimeMillis(), body))

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
    editText.text.clear()
  }

  companion object {
    const val TAG = "Send Message"
  }
}