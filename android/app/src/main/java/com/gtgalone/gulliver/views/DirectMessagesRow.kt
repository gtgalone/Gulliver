package com.gtgalone.gulliver.views

import com.bumptech.glide.Glide
import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.models.ChatMessage
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_row.view.*
import java.text.SimpleDateFormat

class DirectMessagesRow(val message: ChatMessage) : Item() {
  var user: User? = null
  override fun bind(viewHolder: ViewHolder, position: Int) {

    val uid: String

    if (message.fromId == MainActivity.currentUser?.uid) {
      uid = message.toId
    } else {
      uid = message.fromId
    }

    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        user = p0.getValue(User::class.java) ?: return

        viewHolder.itemView.apply {
          direct_messages_row_display_name.text = user?.displayName
          direct_messages_row_latest_message.text = message.body
          direct_messages_row_date.text = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT).format(message.timestamp)
          Glide.with(context).load(user?.photoUrl).into(direct_messages_row_photo)
        }
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })

  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_messages_row
  }
}
