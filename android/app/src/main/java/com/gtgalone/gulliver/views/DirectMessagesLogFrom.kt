package com.gtgalone.gulliver.views

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_log_from.view.*

class DirectMessagesLogFrom(val text: String, val uid: String, val timeStamp: Long) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.direct_messages_log_from_text_view.text = text
    viewHolder.itemView.direct_messages_log_from_date_text_view.text = java.text.SimpleDateFormat.getInstance().format(timeStamp * 1000L)

    val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
    userRef.addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          Picasso.get().load(p0.getValue(User::class.java)!!.photoUrl).into(viewHolder.itemView.direct_messages_log_from_image_view)
          userRef.onDisconnect()
        }

        override fun onCancelled(p0: DatabaseError) {
        }
      })
  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_messages_log_from
  }
}
