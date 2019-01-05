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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import com.gtgalone.gulliver.models.*
import com.gtgalone.gulliver.views.TextMessage
import com.gtgalone.gulliver.views.PeopleRow
import com.gtgalone.gulliver.views.CitiesRow
import com.gtgalone.gulliver.views.MessageLoading
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.ViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_left_drawer.*
import kotlinx.android.synthetic.main.activity_main_cities_row.view.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.collections.forEachReversedByIndex
import org.jetbrains.anko.doAsync

class MainActivity : AppCompatActivity() {
  private lateinit var chatMessageRef: CollectionReference
  private lateinit var chatEventListener: ListenerRegistration
  private lateinit var messageSection: Section

  private val adapter = GroupAdapter<ViewHolder>()
  private val db = FirebaseFirestore.getInstance()
  private val messagePerPage = 20L
  private var isInit = true
  private var isLoading = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    val currentCity = intent.getParcelableExtra<MyCity>(SplashActivity.CURRENT_CITY)

    setTitleForActionBar(currentCity)

    recycler_view_main_activity_log.addOnScrollListener(object: RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
          RecyclerView.SCROLL_STATE_IDLE -> {
            if ((recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
              if (isLoading) return
              adapter.add(0, MessageLoading())
              recycler_view_main_activity_log.scrollToPosition(0)

              isLoading = true
              chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter((messageSection.getItem(0) as TextMessage).message.timestamp).limit(messagePerPage).get()
                .addOnSuccessListener {
                  if (it.documents.isEmpty()) {
                    adapter.removeGroup(0)
                    return@addOnSuccessListener
                  }
                  adapter.removeGroup(0)
                  val items = mutableListOf<Item>()
                  it.documents.forEachReversedByIndex { docSnapshot ->
                    val chatMessage = docSnapshot.toObject(ChatMessage::class.java) ?: return@forEachReversedByIndex
                    val lastTopMessage = messageSection.getItem(0) as TextMessage
                    var isPhoto = true
                    var isTimestamp = true
                    var isDateDivider = true

                    if (items.isNotEmpty()) {
                      val lastTextMessage = (items.last() as TextMessage)
                      isPhoto = lastTextMessage.message.fromId != chatMessage.fromId

                      if ((lastTextMessage.message.timestamp / 60000) == (chatMessage.timestamp / 60000))
                        lastTextMessage.setIsTimestamp(false)

                      if ((lastTextMessage.message.timestamp / (1000 * 60 * 60 * 24)) == (chatMessage.timestamp / (1000 * 60 * 60 * 24))) {
                        isDateDivider = false
                      }
                    }

                    if ((lastTopMessage.message.timestamp / 60000) == (chatMessage.timestamp / 60000)) {
                      isTimestamp = false
                    }

                    items.add(TextMessage(chatMessage, currentUser!!.uid, isPhoto, isTimestamp, isDateDivider))
                  }
                  messageSection.addAll(0, items)
                  isLoading = false
                  recycler_view_main_activity_log.apply {
                    setHasFixedSize(true)
                    setItemViewCacheSize(adapter!!.itemCount)
                  }
                }
            }
          }
        }
      }
    })
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
        main_activity_log_edit_text.clearFocus()
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

  override fun onStop() {
    main_activity_log_edit_text.clearFocus()
    super.onStop()
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

  private fun sendMessage() {
    if (main_activity_log_edit_text.text.isEmpty()) return
    Log.d("test", "send message")

    val body = main_activity_log_edit_text.text.toString()

    val key = chatMessageRef.document().id
    chatMessageRef.document(key)
      .set(ChatMessage(key, body, currentUser!!.uid, currentUser!!.currentChannel!!, System.currentTimeMillis()))

    main_activity_log_edit_text.text.clear()
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

  private fun listenForMessages() {
    chatMessageRef = db.collection("cities").document(currentUser?.currentCity!!)
      .collection("channels").document(currentUser?.currentChannel!!)
      .collection("chatMessages")

    chatEventListener = chatMessageRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(messagePerPage)
      .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        Log.d("test", "listenForMessages")
        val items = mutableListOf<Item>()

        querySnapshot!!.documentChanges.forEachReversedByIndex {
          var isDateDivider = true
          val isTimestamp = true
          var isPhoto = true

          val chatMessage = it.document.toObject(ChatMessage::class.java)
          if (isInit) {
            if (items.isNotEmpty()) {
              val lastTextMessage = (items.last() as TextMessage)
              isPhoto = lastTextMessage.message.fromId != chatMessage.fromId
              if ((lastTextMessage.message.timestamp / 60000) == (chatMessage.timestamp / 60000)) {
                lastTextMessage.setIsTimestamp(false)
              }
              if ((lastTextMessage.message.timestamp / (1000 * 60 * 60 * 24)) == (chatMessage.timestamp / (1000 * 60 * 60 * 24))) {
                isDateDivider = false
              }
            }
            items.add(TextMessage(chatMessage, currentUser!!.uid, isPhoto, isTimestamp, isDateDivider))
          } else {
            if (it.type == DocumentChange.Type.ADDED) {

              if (messageSection.itemCount > 0) {
                val lastTextMessage = (messageSection.getItem(messageSection.itemCount - 1) as TextMessage)
                isPhoto = lastTextMessage.message.fromId != chatMessage.fromId

                if ((lastTextMessage.message.timestamp / 60000) == (chatMessage.timestamp / 60000)) {
                  lastTextMessage.setIsTimestamp(false)
                  lastTextMessage.notifyChanged()
                }

                if ((lastTextMessage.message.timestamp / (1000 * 60 * 60 * 24)) == (chatMessage.timestamp / (1000 * 60 * 60 * 24))) {
                  Log.d("test", "in divider")
                  isDateDivider = false
                }
              }

              messageSection.add(TextMessage(chatMessage, currentUser!!.uid, isPhoto, isTimestamp, isDateDivider))
              recycler_view_main_activity_log.apply {
                setHasFixedSize(true)
                setItemViewCacheSize(adapter!!.itemCount)
                scrollToPosition(adapter!!.itemCount - 1)
              }
              return@addSnapshotListener
            }
          }
        }
        if (isInit) {
          if (items.isNotEmpty()) (items.first() as TextMessage).setIsDateDivider(true)

          messageSection = Section(items)
          adapter.add(messageSection)
          isInit = false
          recycler_view_main_activity_log.adapter = adapter
          recycler_view_main_activity_log.scrollToPosition(adapter.itemCount - 1)
        }

      }

  }

  private fun setTitleForActionBar(currentCity: MyCity? = null) {
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

  private fun fetchCurrentUser() {
    val uid = FirebaseAuth.getInstance().uid
    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        currentUser = p0.getValue(User::class.java) ?: return
        Picasso.get().load(currentUser?.photoUrl).into(activity_main_left_drawer_circle_image_view)
        activity_main_left_drawer_text_view.text = currentUser?.displayName
        fetchCities()
        listenForMessages()
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
            val bundle = Bundle()
            bundle.putParcelable(USER_KEY, (item as PeopleRow).user)
            val peopleBottomSheetDialogFragment = PeopleBottomSheetDialogFragment()
            peopleBottomSheetDialogFragment.arguments = bundle
            peopleBottomSheetDialogFragment.show(supportFragmentManager, peopleBottomSheetDialogFragment.tag)
          }
        }
        recycler_view_people.adapter = adapter
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
          userCitiesRef.removeEventListener(this)
          val cityId = (item as CitiesRow).city.id
          chatEventListener.remove()

          if (cityId == currentUser?.currentCity) return@setOnItemClickListener
          view.activity_main_cities_row_layout.setBackgroundColor(Color.LTGRAY)
          setTitleForActionBar(item.city)

          doAsync {
            fetchUsers(cityId)

            FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentCity")
              .setValue(cityId)

            FirebaseDatabase.getInstance().getReference("/users/${currentUser?.uid}/currentChannel")
              .setValue("general")
              .addOnCompleteListener {
                adapter.clear()
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
    var currentUser: User? = null
  }

}
