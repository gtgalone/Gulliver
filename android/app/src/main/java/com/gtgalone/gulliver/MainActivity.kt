package com.gtgalone.gulliver

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.gtgalone.gulliver.models.*
import com.gtgalone.gulliver.views.DirectMessagesLogFrom
import com.gtgalone.gulliver.views.DirectMessagesLogTo
import com.gtgalone.gulliver.views.PeopleRow
import com.gtgalone.gulliver.views.CitiesRow
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_left_drawer.*
import kotlinx.android.synthetic.main.activity_main_cities_row.view.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity() {
  private val adapter = GroupAdapter<ViewHolder>()
  private lateinit var chatLogRef: DatabaseReference

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    val currentCity = intent.getParcelableExtra<City>(SplashActivity.CURRENT_CITY)
    setTitleForActionBar(currentCity)

    recycler_view_main_activity_log.scrollToPosition(adapter.itemCount)
    recycler_view_main_activity_log.adapter = adapter

    fetchCurrentUser()
    fetchUsers(currentCity.id)

    val toggle = object: ActionBarDrawerToggle(
      this,
      activity_main_layout,
      toolbar,
      R.string.navigation_drawer_open,
      R.string.navigation_drawer_close
    ) {
      override fun onDrawerOpened(drawerView: View) {
        super.onDrawerOpened(drawerView)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(drawerView.windowToken, 0)
      }
    }

    activity_main_layout.addDrawerListener(toggle)
    toggle.syncState()

    main_activity_log_send_button.setOnClickListener {
      sendMessage()
    }

    activity_main_left_drawer_menu_image_view.setOnClickListener {
      val popup = PopupMenu(this@MainActivity, it)
      popup.menuInflater.inflate(R.menu.menu_account, popup.menu)
      popup.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
          R.id.account_menu_sign_out -> {
            FirebaseAuth.getInstance().signOut()
            changeActivity(SplashActivity::class.java)
            return@setOnMenuItemClickListener true
          }
          else -> return@setOnMenuItemClickListener true
        }
      }
      popup.show()
    }

    activity_main_left_drawer_add_city_layout.setOnClickListener {
      startActivity(Intent(this, AddCityActivity::class.java))
    }
  }

  private fun sendMessage() {
    if (main_activity_log_edit_text.text.isEmpty()) return
    Log.d("test", "send message")

    val body = main_activity_log_edit_text.text.toString()

    val chatLog = FirebaseDatabase.getInstance().getReference("/cities/${currentUser!!.currentCity}/channels/${currentUser!!.currentChannel}/chatLog").push()

    val chat = ChatLog(chatLog.key!!, body, currentUser!!.uid, currentUser!!.currentChannel!!, System.currentTimeMillis() / 1000)

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

  private fun changeActivity(activity: Class<*>, reset: Boolean = true) {
    val intent = Intent(this, activity)
    startActivity(intent)
    if (reset) finish()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_nav, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.menu_people -> {
        activity_main_layout.openDrawer(GravityCompat.END)
        true
      }
      R.id.menu_direct_message -> {
        changeActivity(DirectMessagesActivity::class.java, false)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private val chatLogChildEventListener = object: ChildEventListener {
    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
      Log.d("test", "child add")
      val chatLog = p0.getValue(ChatLog::class.java) ?: return

      when(chatLog.fromId) {
        currentUser?.uid -> {
          Log.d("test", "me")
          adapter.add(
            DirectMessagesLogTo(
              chatLog.text,
              currentUser!!,
              chatLog.timeStamp
            )
          )
        }
        else -> {
          Log.d("test", "others")
          adapter.add(
            DirectMessagesLogFrom(
              chatLog.text,
              chatLog.fromId,
              chatLog.timeStamp
            )
          )
        }
      }

      recycler_view_main_activity_log.scrollToPosition(adapter.itemCount)
      recycler_view_main_activity_log.adapter = adapter
    }
    override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
    override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
    override fun onChildRemoved(p0: DataSnapshot) {}
    override fun onCancelled(p0: DatabaseError) {}
  }

  private fun setTitleForActionBar(currentCity: City? = null) {
    if (currentCity != null) {
      supportActionBar?.title = currentCity.locality

      if (currentCity.locality == currentCity.adminArea) {
        supportActionBar?.subtitle = currentCity.countryCode
      } else {
        supportActionBar?.subtitle = currentCity.adminArea + ", " + currentCity.countryCode
      }
    } else {
      FirebaseDatabase.getInstance().getReference("/cities/${currentUser?.currentCity}")
        .addListenerForSingleValueEvent(object: ValueEventListener {
          override fun onDataChange(cityDataSnapshot: DataSnapshot) {
            val city = cityDataSnapshot.getValue(City::class.java) ?: return
            supportActionBar?.title = city.locality

            if (city.locality == city.adminArea) {
              supportActionBar?.subtitle = city.countryCode
            } else {
              supportActionBar?.subtitle = city.adminArea + ", " + city.countryCode
            }
          }
          override fun onCancelled(p0: DatabaseError) {}
        })
    }
  }

  private fun listenForChatLog() {
    chatLogRef = FirebaseDatabase.getInstance().getReference("/cities/${currentUser?.currentCity}/channels/${currentUser?.currentChannel}/chatLog")
    chatLogRef.removeEventListener(chatLogChildEventListener)
    chatLogRef.addChildEventListener(chatLogChildEventListener)
  }

  private fun fetchCurrentUser() {
    val uid = FirebaseAuth.getInstance().uid
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        currentUser = p0.getValue(User::class.java)
        Picasso.get().load(currentUser?.photoUrl).into(activity_main_left_drawer_circle_image_view)
        activity_main_left_drawer_text_view.text = currentUser?.displayName
        fetchCities()
        setTitleForActionBar()
        listenForChatLog()
      }
      override fun onCancelled(p0: DatabaseError) {}
    })
  }

  private fun fetchUsers(cityId: String?) {
    val ref = FirebaseDatabase.getInstance().getReference("/users").orderByChild("currentCity").equalTo(cityId)

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
      override fun onCancelled(p0: DatabaseError) {}
    })
  }

  private fun fetchCities() {
    Log.d("test", "fetchcities ${currentUser?.uid}")

    FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/cities")
      .addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          val adapterCity = GroupAdapter<ViewHolder>()
          recycler_view_cities.adapter = adapterCity
          p0.children.forEach {
            val city = it.getValue(City::class.java) ?: return
            adapterCity.add(CitiesRow(city, currentUser!!))

            adapterCity.setOnItemClickListener { item, view ->
              val cityId = (item as CitiesRow).city.id

              if (cityId == currentUser?.currentCity) return@setOnItemClickListener
              view.activity_main_cities_row_layout.setBackgroundColor(Color.LTGRAY)

              doAsync {
                fetchUsers(cityId)

                FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentCities")
                  .setValue(cityId)
              }

              FirebaseDatabase.getInstance().getReference("/cities/$cityId/channels").limitToFirst(1)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                  override fun onDataChange(channels: DataSnapshot) {
                    FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentChannel")
                      .setValue(channels.children.first().getValue(Channel::class.java)?.id)
                      .addOnCompleteListener {
                        adapter.clear()
                        fetchCurrentUser()
                      }
                  }
                  override fun onCancelled(p0: DatabaseError) {}
                })
            }

          }
          recycler_view_cities.adapter = adapterCity
          doAsync { activity_main_layout.closeDrawer(GravityCompat.START) }
        }
        override fun onCancelled(p0: DatabaseError) {}
      })
  }

  companion object {
    const val USER_KEY = "USER_KEY"
    var currentUser: User? = null
  }

}
