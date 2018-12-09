package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.MainActivity
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.DirectMessageLog
import com.gtgalone.gulliver.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_direct_messages_row.view.*

class DirectMessagesRow(private val directMessage: DirectMessageLog) : Item<ViewHolder>() {
  var person: User? = null
  override fun bind(viewHolder: ViewHolder, position: Int) {

    val personId: String

    if (directMessage.fromId == MainActivity.currentUser?.uid) {
      personId = directMessage.toId
    } else {
      personId = directMessage.fromId
    }

    val ref = FirebaseDatabase.getInstance().getReference("/users/$personId")

    ref.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        person = p0.getValue(User::class.java) ?: return

        viewHolder.itemView.direct_messages_row_display_name_text_view.text = person?.displayName
        viewHolder.itemView.direct_messages_row_latest_message_text_view.text = directMessage.text
        Picasso.get().load(person?.photoUrl).into(viewHolder.itemView.direct_messages_row_image_view)
      }

      override fun onCancelled(p0: DatabaseError) {
      }
    })

  }

  override fun getLayout(): Int {
    return R.layout.activity_direct_messages_row
  }
}
