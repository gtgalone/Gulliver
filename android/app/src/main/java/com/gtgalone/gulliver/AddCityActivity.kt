package com.gtgalone.gulliver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.gtgalone.gulliver.models.MyCity
import com.gtgalone.gulliver.views.CitiesRow
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.app_bar_add_city.*
import kotlinx.android.synthetic.main.content_add_city.*
import kotlinx.android.synthetic.main.custom_view_search.*

class AddCityActivity : AppCompatActivity() {

  private var menuAddCity: Menu? = null
  private val db = FirebaseFirestore.getInstance()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_city)

    setSupportActionBar(toolbar_add_city)
    supportActionBar?.setDisplayShowTitleEnabled(false)
    supportActionBar?.setDisplayShowCustomEnabled(true)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setCustomView(R.layout.custom_view_search)
    custom_view_search.hint = getString(R.string.search_city)

    fetchCities()
  }

  private fun fetchCities(query: String? = null) {
    var cityRef = db.collection("/cities/").orderBy("locality")

    if (query != null) {
      var q: String? = ""
      query.split(" ").forEachIndexed { index, s ->
        if (index != 0) {
          q = q + " " + s.capitalize()
        } else {
          q += s.capitalize()
        }
      }
      cityRef = cityRef.startAt(q)
    }

    cityRef.get()
      .addOnSuccessListener {
        val adapter = GroupAdapter<ViewHolder>()
        it.documents.forEach { docSnapshot ->
          val city = docSnapshot.toObject(MyCity::class.java) ?: return@addOnSuccessListener
          adapter.add(CitiesRow(city))
          adapter.setOnItemClickListener { item, view ->
            val citiesRow = item as CitiesRow
            val currentUserRef = FirebaseDatabase.getInstance().getReference("/users/${MainActivity.currentUser?.uid}")
            currentUserRef.child("cities")
              .orderByChild("locality").equalTo(citiesRow.city.locality)
              .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(userCitiesDataSnapshot: DataSnapshot) {
                  if (!userCitiesDataSnapshot.hasChildren()) {
                    currentUserRef.child("cities").child(citiesRow.city.id!!)
                      .setValue(MyCity(
                        citiesRow.city.id,
                        citiesRow.city.countryCode,
                        citiesRow.city.adminArea,
                        citiesRow.city.locality
                      )).addOnCompleteListener {
                        finish()
                      }
                  }
                }
                override fun onCancelled(p0: DatabaseError) {}
              })
          }
          recycler_view_add_city.adapter = adapter
        }
      }
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  private val textWatcher = object: TextWatcher {
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      if (s.isNullOrBlank()) {
        menuAddCity?.setGroupVisible(0, false)
        fetchCities()
      } else {
        menuAddCity?.setGroupVisible(0, true)
        if (s.length >= 2) fetchCities(s.toString())
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
