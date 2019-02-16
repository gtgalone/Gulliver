package com.gtgalone.gulliver

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.gtgalone.gulliver.fragments.PeopleBottomSheetDialogFragment
import com.gtgalone.gulliver.fragments.ChatRecyclerViewFragment
import com.gtgalone.gulliver.fragments.SendMessageFragment
import com.gtgalone.gulliver.models.*
import com.gtgalone.gulliver.views.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_left_drawer.*
import kotlinx.android.synthetic.main.activity_main_cities_row.view.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_send_message.*
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity() {
  private val uid = FirebaseAuth.getInstance().uid

  private lateinit var currentCity: MyCity
  private var isInit = true
  private var isLoading = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    currentCity = intent.getParcelableExtra(SplashActivity.CURRENT_CITY)

    setTitleForActionBar(currentCity)

    if (savedInstanceState == null) {
      fetchCurrentUser()
      fetchUsers(currentCity.id)
    }

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
        edit_text.clearFocus()
      }
      override fun onDrawerClosed(drawerView: View) {
        super.onDrawerClosed(drawerView)
        if (drawerView.id == R.id.recycler_view_people) {
          fetchUsers(currentCity.id)
        }
      }
    }

    activity_main_layout.addDrawerListener(toggle)
    toggle.syncState()

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

  private fun setTitleForActionBar(myCity: MyCity? = null) {
    if (myCity != null) {
      supportActionBar?.title = myCity.locality

      if (myCity.locality == myCity.adminArea) {
        supportActionBar?.subtitle = myCity.countryCode
      } else {
        supportActionBar?.subtitle = myCity.adminArea + ", " + myCity.countryCode
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

  private fun fetchCurrentUser() {
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        currentUser = p0.getValue(User::class.java) ?: return

        val recyclerViewFragment = ChatRecyclerViewFragment()
        val bundle = Bundle()
        bundle.putParcelable(MainActivity.USER_KEY, currentUser)
        bundle.putInt(CHAT_TYPE, CHAT_TYPE_MAIN)
        recyclerViewFragment.arguments = bundle

        val sendMessageFragment = SendMessageFragment()
        sendMessageFragment.arguments = bundle
        supportFragmentManager.beginTransaction().run {
          replace(R.id.fragment_recycler_view_main_activity_log, recyclerViewFragment)
          replace(R.id.fragment_send_message_main_activity_log, sendMessageFragment)
          commit()
        }

        Glide.with(this@MainActivity).load(currentUser?.photoUrl).into(activity_main_left_drawer_circle_image_view)
        activity_main_left_drawer_text_view.text = currentUser?.displayName
        fetchCities()
      }
      override fun onCancelled(p0: DatabaseError) {}
    })
  }

  private fun fetchUsers(cityId: String?) {
    val ref = FirebaseDatabase.getInstance().getReference("/users").orderByChild("currentCity").equalTo(cityId)

    val adapter = GroupAdapter<ViewHolder>()
    recycler_view_people.adapter = adapter

    ref.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        p0.children.forEach {
          val user = it.getValue(User::class.java) ?: return@forEach

          if (user.uid == uid) return@forEach

          FirebaseDatabase.getInstance().getReference("/users/$uid/ignoredBy/${user.uid}")
            .addListenerForSingleValueEvent(object: ValueEventListener {
              override fun onDataChange(p0: DataSnapshot) {
                if (p0.value == null) {
                  adapter.add(PeopleRow(user))
                  adapter.setOnItemClickListener { item, view ->
                    val bundle = Bundle()
                    bundle.putParcelable(USER_KEY, (item as PeopleRow).user)
                    val peopleBottomSheetDialogFragment = PeopleBottomSheetDialogFragment()
                    peopleBottomSheetDialogFragment.arguments = bundle
                    peopleBottomSheetDialogFragment.show(supportFragmentManager, peopleBottomSheetDialogFragment.tag)
                  }
                }
              }
              override fun onCancelled(p0: DatabaseError) {}
            })
        }
      }
      override fun onCancelled(p0: DatabaseError) {}
    })
  }

  private fun fetchCities() {
    val adapterCity = GroupAdapter<ViewHolder>()
    val mKeys = mutableListOf<String>()
    val userCitiesRef = FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/cities").orderByChild("timestamp")
    val childEventListener = object: ChildEventListener {
      override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val city = p0.getValue(MyCity::class.java) ?: return

        adapterCity.add(CitiesRow(city, currentUser))
        recycler_view_cities.adapter = adapterCity
        mKeys.add(p0.key!!)
        adapterCity.setOnItemClickListener { item, view ->
          isInit = true
          isLoading = false
          userCitiesRef.removeEventListener(this)
          val cityId = (item as CitiesRow).city.id

          if (cityId == currentUser?.currentCity) return@setOnItemClickListener
          view.activity_main_cities_row_layout.setBackgroundColor(Color.LTGRAY)
          setTitleForActionBar(item.city)

          doAsync {
            currentCity = item.city
            fetchUsers(cityId)

            FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentCity")
              .setValue(cityId)

            FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentChannel")
              .setValue("general")
              .addOnCompleteListener {
                fetchCurrentUser()
              }

            activity_main_layout.closeDrawer(GravityCompat.START)
          }
          adapterCity.notifyDataSetChanged()
        }

      }
      override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
      override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
      override fun onChildRemoved(p0: DataSnapshot) {
        val index = mKeys.indexOf(p0.key!!)
        adapterCity.remove(adapterCity.getItem(index))
        mKeys.removeAt(index)
      }
      override fun onCancelled(p0: DatabaseError) {}
    }

    userCitiesRef.addChildEventListener(childEventListener)
  }

  companion object {
    const val USER_KEY = "USER_KEY"
    const val CHAT_TYPE = "CHAT_TYPE"
    const val CHAT_TYPE_MAIN = 0
    const val CHAT_TYPE_DIRECT_MESSAGE = 1
    var currentUser: User? = null
  }

}
