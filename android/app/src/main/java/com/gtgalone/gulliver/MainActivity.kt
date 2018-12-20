package com.gtgalone.gulliver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.gtgalone.gulliver.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.gtgalone.gulliver.models.ChatLog
import com.gtgalone.gulliver.models.FavoriteServer
import com.gtgalone.gulliver.views.DirectMessagesLogFrom
import com.gtgalone.gulliver.views.DirectMessagesLogTo
import com.gtgalone.gulliver.views.PeopleRow
import com.gtgalone.gulliver.views.ServersRow
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {
  companion object {
    const val USER_KEY = "USER_KEY"
    var currentUser: User? = null
  }

  private val adapter = GroupAdapter<ViewHolder>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    recycler_view_main_activity_log.scrollToPosition(adapter.itemCount)
    recycler_view_main_activity_log.adapter = adapter

    fetchCurrentUser()
    fetchUsers()

    supportActionBar!!.title = intent.getParcelableExtra<FavoriteServer>(SplashActivity.CURRENT_SERVER).serverDisplayName
    main_activity_log_text_view.text = getString(R.string.channel, intent.getStringArrayListExtra(SplashActivity.CURRENT_CHANNEL)[1])

    val toggle = ActionBarDrawerToggle(
      this,
      activity_main_layout,
      toolbar,
      R.string.navigation_drawer_open,
      R.string.navigation_drawer_close
    )

    activity_main_layout.addDrawerListener(toggle)
    toggle.syncState()

    main_activity_log_send_button.setOnClickListener {
      sendMessage()
    }
  }

  private fun listenForChatLog() {
    val chatLogRef = FirebaseDatabase.getInstance().getReference("/servers/${currentUser?.currentServer}/channels/${currentUser?.currentChannel}/chatLog")
    chatLogRef.addChildEventListener(object: ChildEventListener {
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val chatLog = p0.getValue(ChatLog::class.java) ?: return

        FirebaseDatabase.getInstance().getReference("/users/${chatLog.fromId}")
          .addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(userDataSnapshot: DataSnapshot) {
              val fromUser = userDataSnapshot.getValue(User::class.java)
              when(chatLog.fromId) {
                currentUser?.uid -> {
                  adapter.add(
                    DirectMessagesLogTo(
                      chatLog.text,
                      currentUser!!,
                      chatLog.timeStamp
                    )
                  )
                }
                else -> {
                  adapter.add(
                    DirectMessagesLogFrom(
                      chatLog.text,
                      fromUser!!,
                      chatLog.timeStamp
                    )
                  )
                }
              }
              recycler_view_main_activity_log.scrollToPosition(adapter.itemCount)
              recycler_view_main_activity_log.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {
            }
          })
      }

      override fun onChildChanged(p0: DataSnapshot, p1: String?) {

      }

      override fun onChildMoved(p0: DataSnapshot, p1: String?) {

      }

      override fun onChildRemoved(p0: DataSnapshot) {

      }

      override fun onCancelled(p0: DatabaseError) {

      }
    })
  }

  private fun sendMessage() {
    if (main_activity_log_edit_text.text.isEmpty()) return

    val body = main_activity_log_edit_text.text.toString()

    val chatLog = FirebaseDatabase.getInstance().getReference("/servers/${currentUser!!.currentServer}/channels/${currentUser!!.currentChannel}/chatLog").push()

    val chat = ChatLog(chatLog.key!!, body, currentUser!!.uid, currentUser!!.currentChannel, System.currentTimeMillis() / 1000)

    chatLog.setValue(chat)

    main_activity_log_edit_text.text.clear()
  }

  override fun onBackPressed() {
    if (activity_main_layout.isDrawerOpen(GravityCompat.START)) {
      activity_main_layout.closeDrawer(GravityCompat.START)
    } else if (activity_main_layout.isDrawerOpen(GravityCompat.END)) {
      activity_main_layout.closeDrawer(GravityCompat.END)
    } else {
      super.onBackPressed()
    }
  }

  private fun fetchCurrentUser() {
    val uid = FirebaseAuth.getInstance().uid
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        currentUser = p0.getValue(User::class.java)
        listenForChatLog()
        fetchServers()
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
  }

  private fun changeActivity(activity: Class<*>, reset: Boolean = true) {
    val intent = Intent(this, activity)
    if (reset) intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.nav_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.menu_people -> {
        activity_main_layout.openDrawer(GravityCompat.END)
//        changeActivity(PeopleActivity::class.java, false)
        true
      }
      R.id.menu_direct_message -> {
        changeActivity(DirectMessagesActivity::class.java, false)
        true
      }
      R.id.menu_sign_out -> {
        FirebaseAuth.getInstance().signOut()
        changeActivity(SignInActivity::class.java)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun fetchUsers() {
    val ref = FirebaseDatabase.getInstance().getReference("/users")

    ref.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        val adapter = GroupAdapter<ViewHolder>()
        p0.children.forEach {
          val user = it.getValue(User::class.java)

          if (user != null) adapter.add(PeopleRow(user))
          adapter.setOnItemClickListener { item, view ->

            val userItem = item as PeopleRow

            val intent = Intent(view.context, DirectMessagesLogActivity::class.java)
            intent.putExtra(USER_KEY, userItem.user)
            startActivity(intent)
          }
        }
        recycler_view_people.adapter = adapter
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })
  }

  private fun fetchServers() {
    Log.d("test", "fetchserver ${currentUser?.uid}")

    FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/servers")
      .addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          val adapterServer = GroupAdapter<ViewHolder>()
          recycler_view_servers.adapter = adapterServer

          p0.children.forEach {
            val server = it.getValue(FavoriteServer::class.java) ?: return
            adapterServer.add(ServersRow(server))
          }

          recycler_view_servers.adapter = adapterServer
        }

        override fun onCancelled(p0: DatabaseError) {
        }
      })
  }
}
