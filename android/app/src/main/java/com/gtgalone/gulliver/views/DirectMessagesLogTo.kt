package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_log_to.view.*

class DirectMessagesLogTo(val text: String, val user: User, val timeStamp: Long) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.direct_messages_log_to_text_view.text = text
    viewHolder.itemView.direct_messages_log_to_date_text_view.text = java.text.SimpleDateFormat.getInstance().format(timeStamp * 1000L)
    Picasso.get().load(user.photoUrl).into(viewHolder.itemView.direct_messages_log_to_image_view)
  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_messages_log_to
  }
}
