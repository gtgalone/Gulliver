package com.example.jehun.gulliver.views

import com.example.jehun.gulliver.R
import com.example.jehun.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_people_row.view.*

class PeopleRow(val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.people_row_display_name_text_view.text = user.displayName
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.people_row_photo_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_people_row
  }
}
