package com.example.jehun.gulliver.models

import com.example.jehun.gulliver.R
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_people_item.view.*

class UserItem(val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.people_item_display_name_text_view.text = user.displayName
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.people_item_photo_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_people_item
  }
}
