package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_main_people_row.view.*

class PeopleRow(val user: User) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.people_row_display_name_text_view.text = user.displayName
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.people_row_photo_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_people_row
  }
}
