package com.gtgalone.gulliver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.gtgalone.gulliver.models.User
import com.google.gson.Gson
import com.gtgalone.gulliver.fragments.ChatRecyclerViewFragment
import com.gtgalone.gulliver.fragments.SendMessageFragment
import kotlinx.android.synthetic.main.app_bar_direct_messages_log.*

class DirectMessagesLogActivity : AppCompatActivity() {
  private lateinit var functions: FirebaseFunctions
  private lateinit var toUser: User

  private val uid = FirebaseAuth.getInstance().uid!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_messages_log)
    setSupportActionBar(toolbar_direct_messages_log)

    functions = FirebaseFunctions.getInstance()

    if (intent.getParcelableExtra<User>(MainActivity.USER_KEY) != null) {
      toUser = intent.getParcelableExtra(MainActivity.USER_KEY)
    } else {
      val bundle = intent.extras
      toUser = Gson().fromJson(bundle?.getString("toUser"), User::class.java)
    }

    if (savedInstanceState == null) {
      val recyclerViewFragment = ChatRecyclerViewFragment()
      val bundle = Bundle()
      bundle.putParcelable(MainActivity.USER_KEY, toUser)
      bundle.putInt(MainActivity.CHAT_TYPE, MainActivity.CHAT_TYPE_DIRECT_MESSAGE)
      recyclerViewFragment.arguments = bundle

      val sendMessageFragment = SendMessageFragment()
      sendMessageFragment.arguments = bundle
      supportFragmentManager.beginTransaction().run {
        replace(R.id.fragment_recycler_view_direct_messages_log, recyclerViewFragment)
        replace(R.id.fragment_send_message_direct_messages_log, sendMessageFragment)
        commit()
      }
    }

    supportActionBar!!.title = toUser.displayName
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_direct_messages_log, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.direct_messages_log_leave_conversation -> {

        FirebaseFirestore.getInstance().collection("directMessages").document(toUser.uid)
          .collection("directMessage").document(uid).delete()

        functions
          .getHttpsCallable("recursiveDelete")
          .call(hashMapOf("path" to "/directMessagesLog/${toUser.uid}/$uid"))
          .continueWith { task ->
            // This continuation runs on either success or failure, but if the task
            // has failed then result will throw an Exception which will be
            // propagated down.
            val result = task.result?.data as String
            result
          }

        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
