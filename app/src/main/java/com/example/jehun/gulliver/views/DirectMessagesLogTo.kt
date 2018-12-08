package com.example.jehun.gulliver.views

import com.example.jehun.gulliver.R
import com.example.jehun.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_log_to.view.*

class DirectMessagesLogTo(val text: String, val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.direct_message_item_to_text_view.text = text
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.direct_message_item_to_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_messages_log_to
  }
}
