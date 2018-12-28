package com.gtgalone.gulliver.views

import android.os.Build
import androidx.core.content.ContextCompat
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.City
import com.gtgalone.gulliver.models.User
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_cities_row.view.*

class CitiesRow(val city: City, val user: User? = null) : Item<ViewHolder>() {
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

    if (user == null) return
    if (city.id == user.currentCity) {
      viewHolder.itemView.apply {
        activity_main_cities_row_locality
          .setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorWhite))
        activity_main_cities_row_admin_country
          .setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorWhite))
      }
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        viewHolder.itemView.activity_main_cities_row_layout
          .setBackgroundDrawable(ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.background))
      } else {
        viewHolder.itemView.activity_main_cities_row_layout
          .background = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.background)
      }
    }
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_cities_row
  }
}