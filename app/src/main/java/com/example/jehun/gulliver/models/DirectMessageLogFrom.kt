package com.example.jehun.gulliver.models

import com.example.jehun.gulliver.R
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_message_log_from.view.*

class DirectMessageLogFrom(val text: String, val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.direct_message_item_from_text_view.text = text
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.direct_message_item_from_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_message_log_from
  }
}
