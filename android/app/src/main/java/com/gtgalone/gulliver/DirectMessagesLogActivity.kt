package com.gtgalone.gulliver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.gtgalone.gulliver.models.User
import com.google.gson.Gson
import com.gtgalone.gulliver.fragments.ChatRecyclerViewFragment
import com.gtgalone.gulliver.fragments.SendMessageFragment
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
      R.id.direct_messages_log_delete -> {
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
