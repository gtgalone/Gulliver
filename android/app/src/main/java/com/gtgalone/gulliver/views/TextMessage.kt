package com.gtgalone.gulliver.views

import android.os.Build
import android.util.Log
import android.view.View
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

class TextMessage(
  val message: ChatMessage,
  val uid: String,
  var isPhoto: Boolean = false,
  var isTimestamp: Boolean = false
) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.apply {
      text_message_content.text = message.text
      if (isTimestamp) {
        text_message_date.text = SimpleDateFormat.getTimeInstance().format(message.timestamp)
        text_message_date.visibility = View.VISIBLE
      }

      val constraintSet = ConstraintSet()
      constraintSet.clone(text_message_constraint_layout)

      constraintSet.clear(R.id.text_message_photo, ConstraintSet.START)
      constraintSet.clear(R.id.text_message_content, ConstraintSet.START)
      constraintSet.clear(R.id.text_message_date, ConstraintSet.START)

      constraintSet.clear(R.id.text_message_photo, ConstraintSet.END)
      constraintSet.clear(R.id.text_message_content, ConstraintSet.END)
      constraintSet.clear(R.id.text_message_date, ConstraintSet.END)
      if (message.fromId != uid) {
        constraintSet.connect(
          R.id.text_message_photo,
          ConstraintSet.START,
          ConstraintSet.PARENT_ID,
          ConstraintSet.START,
          24
        )
        constraintSet.connect(
          R.id.text_message_content,
          ConstraintSet.START,
          R.id.text_message_photo,
          ConstraintSet.END,
          24
        )
        constraintSet.connect(
          R.id.text_message_date,
          ConstraintSet.START,
          R.id.text_message_content,
          ConstraintSet.START
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          text_message_content.setBackgroundDrawable(
            ContextCompat.getDrawable(
              context,
              R.drawable.rounded_message_from
            )
          )
        } else {
          text_message_content.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_from)
        }
      } else {
        constraintSet.connect(
          R.id.text_message_photo,
          ConstraintSet.END,
          ConstraintSet.PARENT_ID,
          ConstraintSet.END,
          24
        )
        constraintSet.connect(
          R.id.text_message_content,
          ConstraintSet.END,
          R.id.text_message_photo,
          ConstraintSet.START,
          24
        )
        constraintSet.connect(
          R.id.text_message_date,
          ConstraintSet.END,
          R.id.text_message_content,
          ConstraintSet.END
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          text_message_content.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_to))
        } else {
          text_message_content.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_to)
        }
      }
      constraintSet.applyTo(text_message_constraint_layout)
    }

    if (isPhoto) {
      val userRef = FirebaseDatabase.getInstance().getReference("/users/${message.fromId}")
      userRef.addListenerForSingleValueEvent(object: ValueEventListener {
        override fun onDataChange(p0: DataSnapshot) {
          viewHolder.itemView.apply {
            Picasso.get().load(p0.getValue(User::class.java)!!.photoUrl)
              .placeholder(resources.getDrawable(R.drawable.background))
              .fit().centerCrop()
              .into(text_message_photo)
          }
        }
        override fun onCancelled(p0: DatabaseError) {}
      })
    }
  }

  override fun getLayout(): Int {
    return R.layout.text_message
  }

  fun setIsTimestamp(isTimestamp: Boolean) {
    if (this.isTimestamp) {
      this.isTimestamp = isTimestamp
    }
  }
}
