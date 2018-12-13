package com.gtgalone.gulliver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.gtgalone.gulliver.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.models.Location
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

  companion object {
    const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    var currentUser: User? = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)


    val location = intent.getParcelableExtra<Location>(SplashActivity.CURRENT_LOCATION)

    Log.d("test", location.countryCode)
    Log.d("test", location.adminArea)
    Log.d("test", location.locality)

    val channelArea: String
    if (location.adminArea == location.locality) {
      channelArea = getString(R.string.channel_area2, location.locality, location.countryCode)
    } else {
      channelArea = getString(R.string.channel_area1, location.locality, location.adminArea, location.countryCode)
    }

    location_text!!.text = channelArea
    supportActionBar?.title = channelArea


    fetchCurrentUser()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.nav_menu, menu)
    return super.onCreateOptionsMenu(menu)
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

  fun close(v: View) {
    moveTaskToBack(true)
    exitProcess(-1)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      R.id.menu_people -> {
        changeActivity(PeopleActivity::class.java, false)
      }
      R.id.menu_direct_message -> {
        changeActivity(DirectMessagesActivity::class.java, false)
      }
      R.id.menu_sign_out -> {
        FirebaseAuth.getInstance().signOut()
        changeActivity(SignInActivity::class.java)
      }
    }
    return super.onOptionsItemSelected(item)
  }
}
