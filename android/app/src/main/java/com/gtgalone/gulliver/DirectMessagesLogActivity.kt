package com.gtgalone.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.gtgalone.gulliver.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.gtgalone.gulliver.fragments.RecyclerViewFragment
import com.gtgalone.gulliver.fragments.SendMessageFragment
import com.gtgalone.gulliver.models.ChatMessage
import kotlinx.android.synthetic.main.app_bar_direct_messages_log.*

class DirectMessagesLogActivity : AppCompatActivity() {

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
      bundle.putInt(MainActivity.CHAT_TYPE, 1)
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
}
