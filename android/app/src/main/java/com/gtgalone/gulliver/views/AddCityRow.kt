package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.City
import com.gtgalone.gulliver.models.User
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_main_cities_row.view.*

class AddCityRow(val city: City, val user: User? = null) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.apply {
      activity_main_cities_row_locality.text = city.locality
      if (city.locality == city.adminArea) {
        activity_main_cities_row_admin_country.text = city.countryCode
      } else {
        activity_main_cities_row_admin_country.text = viewHolder.itemView.resources.getString(
          R.string.cities_row_admin_country,
          city.adminArea,
          city.countryCode
        )
      }
    }
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_cities_row
  }
}