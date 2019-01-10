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

  private val uid = FirebaseAuth.getInstance().uid!!
  private val db = FirebaseFirestore.getInstance()

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

    if (savedInstanceState == null) {
      val recyclerViewFragment = RecyclerViewFragment()
      val bundle = Bundle()
      bundle.putParcelable(MainActivity.USER_KEY, toUser)
      recyclerViewFragment.arguments = bundle
      supportFragmentManager.beginTransaction().run {
        replace(R.id.fragment_recycler_view_direct_messages_log, recyclerViewFragment)
        commit()
      }
    }

    functions = FirebaseFunctions.getInstance()

    supportActionBar!!.title = toUser.displayName
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    direct_messages_log_send_button.setOnClickListener {
      sendMessage(toUser)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private fun sendMessage(toUser: User) {
    val fromId = uid
    val toId = toUser.uid
    if (direct_messages_log_edit_text.text.trim().isEmpty()) return
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
