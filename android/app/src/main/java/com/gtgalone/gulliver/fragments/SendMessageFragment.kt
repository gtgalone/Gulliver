package com.gtgalone.gulliver.fragments

import android.app.AlertDialog
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
import android.media.Image
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.gtgalone.gulliver.models.AdapterItemMessage
import java.util.*


class SendMessageFragment : Fragment() {
  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()

  private var chatType: Int? = null

  private lateinit var functions: FirebaseFunctions
  private lateinit var sendButton: ImageView
  private lateinit var editText: EditText
  private lateinit var insertImage: ImageView
  private lateinit var toUser: User

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_send_message, container, false).apply {
      tag = TAG
    }

    functions = FirebaseFunctions.getInstance()
    sendButton = rootView.findViewById(R.id.send_button)
    editText = rootView.findViewById(R.id.edit_text)
    insertImage = rootView.findViewById(R.id.insert_message)
    toUser = arguments!!.getParcelable(MainActivity.USER_KEY) ?: return rootView

    chatType = arguments!!.getInt(MainActivity.CHAT_TYPE)

    sendButton.setOnClickListener {
      if (chatType == MainActivity.CHAT_TYPE_MAIN) {
        sendMessage(toUser, AdapterItemMessage.TYPE_TEXT_MESSAGE, editText.text.toString())
      } else {
        sendDirectMessage(toUser, AdapterItemMessage.TYPE_TEXT_MESSAGE, editText.text.toString())
      }
    }

    insertImage.setOnClickListener {
      val intent = Intent()
      intent.type = "image/*"
      intent.action = Intent.ACTION_GET_CONTENT
      startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
    }

    return rootView
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == -1) {
      val builder = AlertDialog.Builder(context, R.style.DialogTheme)
      val imageView = ImageView(context)
      imageView.setImageURI(data!!.data)
      Log.d("test", data.data.toString())

      builder
        .setView(imageView)
        .setPositiveButton("send") { dialog, which ->
          FirebaseStorage.getInstance().reference
            .child("imageMessages/${toUser.uid}/${UUID.randomUUID()}")
            .putFile(data.data!!)
            .addOnSuccessListener {

              if (chatType == MainActivity.CHAT_TYPE_MAIN) {
                sendMessage(toUser, AdapterItemMessage.TYPE_IMAGE_MESSAGE, it.metadata!!.path)
              } else {
                sendDirectMessage(toUser, AdapterItemMessage.TYPE_IMAGE_MESSAGE, it.metadata!!.path)
              }
            }
        }
        .setNegativeButton("cancel") { dialog, which ->
          dialog.cancel()
        }
      builder.show()
    }
  }

  override fun onStop() {
    editText.clearFocus()
    super.onStop()
  }

  private fun sendMessage(user: User, messageType: Int, body: String) {
    if (messageType == AdapterItemMessage.TYPE_TEXT_MESSAGE && editText.text.trim().isEmpty()) return

    val chatMessageRef = db.collection("cities").document(user.currentCity!!)
      .collection("channels").document(user.currentChannel!!)
      .collection("chatMessages")

    val key = chatMessageRef.document().id

    chatMessageRef.document(key)
      .set(ChatMessage(key, user.uid, user.currentChannel, System.currentTimeMillis(), body, messageType))

    editText.text.clear()
  }

  private fun sendDirectMessage(user: User, messageType: Int, body: String) {
    if (messageType == AdapterItemMessage.TYPE_TEXT_MESSAGE && editText.text.trim().isEmpty()) return

    val fromId = uid
    val toId = user.uid

    val fromLogRef = db.collection("directMessagesLog").document(fromId).collection(toId)
    val fromLogKey = fromLogRef.document().id

    fromLogRef.document(fromLogKey)
      .set(ChatMessage(fromLogKey, fromId, toId, System.currentTimeMillis(), body, messageType))

    val fromRef = db.collection("directMessages").document(fromId).collection("directMessage")

    fromRef.document(toId)
      .set(ChatMessage(toId, fromId, toId, System.currentTimeMillis(), body, messageType))

    if (fromId != toId) {
      val toLogRef = db.collection("directMessagesLog").document(toId).collection(fromId)
      val toLogKey = toLogRef.document().id

      toLogRef.document(toLogKey)
        .set(ChatMessage(toLogKey, fromId, toId, System.currentTimeMillis(), body, messageType))

      val toRef = db.collection("directMessages").document(toId).collection("directMessage")

      toRef.document(fromId).set(ChatMessage(fromId, fromId, toId, System.currentTimeMillis(), body, messageType))

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