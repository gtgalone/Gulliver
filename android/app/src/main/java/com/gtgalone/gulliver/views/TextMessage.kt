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
import java.text.SimpleDateFormat

class TextMessage(val message: ChatMessage, val uid: String) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {

    val userRef = FirebaseDatabase.getInstance().getReference("/users/${message.fromId}")
    userRef.addListenerForSingleValueEvent(object: ValueEventListener {
      override fun onDataChange(p0: DataSnapshot) {
        viewHolder.itemView.apply {
          Log.d("test", "message")
          val constraintSet = ConstraintSet()
          constraintSet.clone(text_message_constraint_layout)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_message, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_date, ConstraintSet.START)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.END)
          constraintSet.clear(R.id.text_message_message, ConstraintSet.END)
          constraintSet.clear(R.id.text_message_date, ConstraintSet.END)
          if (message.fromId != uid) {
            constraintSet.connect(R.id.text_message_photo, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 24)
            constraintSet.connect(R.id.text_message_message, ConstraintSet.START, R.id.text_message_photo, ConstraintSet.END, 24)
            constraintSet.connect(R.id.text_message_date, ConstraintSet.START, R.id.text_message_message, ConstraintSet.START, 24)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              text_message_message.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_from))
            } else {
              text_message_message.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_from)
            }
          } else {
            constraintSet.connect(R.id.text_message_photo, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 24)
            constraintSet.connect(R.id.text_message_message, ConstraintSet.END, R.id.text_message_photo, ConstraintSet.START, 24)
            constraintSet.connect(R.id.text_message_date, ConstraintSet.END, R.id.text_message_message, ConstraintSet.END, 24)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              text_message_message.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_to))
            } else {
              text_message_message.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_to)
            }
          }
          constraintSet.applyTo(text_message_constraint_layout)

          text_message_message.text = message.text
          text_message_date.text = SimpleDateFormat.getInstance().format(message.timeStamp)
          Picasso.get().load(p0.getValue(User::class.java)!!.photoUrl).into(text_message_photo)
        }

        userRef.onDisconnect()
      }
      override fun onCancelled(p0: DatabaseError) {}
    })
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
