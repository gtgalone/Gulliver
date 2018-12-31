package com.gtgalone.gulliver.views

import android.os.Build
import android.util.Log
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.ChatMessage
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.text_message.view.*

class TextMessage(val message: ChatMessage, val user: User) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.text_message_text_view.text = message.text
    viewHolder.itemView.text_message_date_text_view.text = java.text.SimpleDateFormat.getInstance().format(message.timeStamp)

    val userRef = FirebaseDatabase.getInstance().getReference("/users/${message.fromId}")
    userRef.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        Picasso.get().load(p0.getValue(User::class.java)!!.photoUrl).into(viewHolder.itemView.text_message_image_view)

        userRef.onDisconnect()
      }

      override fun onCancelled(p0: DatabaseError) {}
    })

    viewHolder.itemView.apply {
      val constraintSet = ConstraintSet()
      constraintSet.clone(text_message_constraint_layout)
      if (message.fromId != user.uid) {
        constraintSet.connect(R.id.text_message_image_view, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(R.id.text_message_text_view, ConstraintSet.START, R.id.text_message_image_view, ConstraintSet.END)
        constraintSet.connect(R.id.text_message_date_text_view, ConstraintSet.START, R.id.text_message_text_view, ConstraintSet.START)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          text_message_text_view
            .setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_from))
        } else {
          text_message_text_view
            .background = ContextCompat.getDrawable(context, R.drawable.rounded_message_from)
        }
      } else {
        constraintSet.connect(R.id.text_message_image_view, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(R.id.text_message_text_view, ConstraintSet.END, R.id.text_message_image_view, ConstraintSet.START)
        constraintSet.connect(R.id.text_message_date_text_view, ConstraintSet.END, R.id.text_message_text_view, ConstraintSet.END)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          text_message_text_view
            .setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_to))
        } else {
          text_message_text_view
            .background = ContextCompat.getDrawable(context, R.drawable.rounded_message_to)
        }
      }
      constraintSet.applyTo(text_message_constraint_layout)
    }
  }

  override fun getLayout(): Int {
    return R.layout.text_message
  }

  override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
    if (other !is TextMessage)
      return false
    Log.d("test", "is same as ${this.message.text} ${other.message.text}")
    if (this.message.text != other.message.text)
      return false
    return true
  }


}
