package com.gtgalone.gulliver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.models.City
import com.gtgalone.gulliver.views.CitiesRow
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.app_bar_add_city.*
import kotlinx.android.synthetic.main.content_add_city.*
import kotlinx.android.synthetic.main.custom_view_search.*

class AddCityActivity : AppCompatActivity() {

  private var menuAddCity: Menu? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_city)

    setSupportActionBar(toolbar_add_city)
    supportActionBar?.setDisplayShowTitleEnabled(false)
    supportActionBar?.setDisplayShowCustomEnabled(true)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setCustomView(R.layout.custom_view_search)
    custom_view_search.hint = getString(R.string.search_city)

    fetchcities()

  }

  private fun fetchcities() {
    FirebaseDatabase.getInstance().getReference("/cities/")
      .addListenerForSingleValueEvent(object: ValueEventListener {

        override fun onDataChange(p0: DataSnapshot) {
          val adapter = GroupAdapter<ViewHolder>()

          p0.children.forEach {
            val city = it.getValue(City::class.java) ?: return
            adapter.add(CitiesRow(city))
          }
          recycler_view_add_city.adapter = adapter

        }
        override fun onCancelled(p0: DatabaseError) {}
      })

  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private val textWatcher = object: TextWatcher {
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      if (s.isNullOrBlank()) {
        menuAddCity?.setGroupVisible(0, false)
      } else {
        menuAddCity?.setGroupVisible(0, true)
      }
    }
    override fun afterTextChanged(s: Editable?) {}
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_search_city, menu)
    menuAddCity = menu
    menu?.setGroupVisible(0, false)
    custom_view_search.addTextChangedListener(textWatcher)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      R.id.menu_search_city_delete -> custom_view_search.text.clear()
    }
    return super.onOptionsItemSelected(item)
  }
}
