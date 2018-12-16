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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.models.Location
import com.gtgalone.gulliver.views.PeopleRow
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity() {
  companion object {
    const val USER_KEY = "USER_KEY"
    var currentUser: User? = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    val toggle = ActionBarDrawerToggle(
      this,
      activity_main_layout,
      toolbar,
      R.string.navigation_drawer_open,
      R.string.navigation_drawer_close
    )

    activity_main_layout.addDrawerListener(toggle)
    toggle.syncState()

    fetchCurrentUser()
    fetchUsers()

    val location = intent.getParcelableExtra<Location>(SplashActivity.CURRENT_LOCATION) ?: return

    Log.d("test", location.countryCode)
    Log.d("test", location.adminArea)
    Log.d("test", location.locality)

    val channelArea: String
    if (location.adminArea == location.locality) {
      channelArea = getString(R.string.channel_area2, location.locality, location.countryCode)
    } else {
      channelArea = getString(R.string.channel_area1, location.locality, location.adminArea, location.countryCode)
    }

    supportActionBar!!.title = channelArea
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
    Log.d("test", item?.itemId.toString() + R.id.menu_people)

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
}
