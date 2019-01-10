package com.gtgalone.gulliver

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.gtgalone.gulliver.models.AdapterItemMessage
import com.gtgalone.gulliver.models.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.text_message.view.*
import java.text.SimpleDateFormat

class CustomAdapter(private val dataset: ArrayList<AdapterItemMessage>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
  val uid = FirebaseAuth.getInstance().uid

  class ViewHolder(v: View, viewType: Int) : RecyclerView.ViewHolder(v) {
    var textViewMessage: TextView? = null
    var textViewDate: TextView? = null
    var textViewDateDivider: TextView? = null

    init {
      v.setOnClickListener { Log.d(TAG, "Clicked $adapterPosition") }
      when (viewType) {
        AdapterItemMessage.TYPE_DATE_DIVIDER -> textViewDateDivider = v.findViewById(R.id.date_divider)
        else -> {
          textViewMessage = v.findViewById(R.id.text_message_message)
          textViewDate = v.findViewById(R.id.text_message_date)
        }
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    Log.d(TAG, viewType.toString())

    val layout = when (viewType) {
      AdapterItemMessage.TYPE_MESSAGE_LOADER -> R.layout.message_loader
      AdapterItemMessage.TYPE_DATE_DIVIDER -> R.layout.date_divider
      else -> R.layout.text_message
    }

    val v = LayoutInflater.from(parent.context)
      .inflate(layout, parent,false)

    return ViewHolder(v, viewType)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    Log.d(TAG, "Element $position set ${dataset[position]}.")
    val item = dataset[position]
    when (item.type) {
      AdapterItemMessage.TYPE_DATE_DIVIDER -> {
        holder.textViewDateDivider!!.text = SimpleDateFormat.getDateInstance().format(item.message!!.timestamp)
      }
      else -> {
        val message = item.message

        holder.itemView.apply {
          holder.textViewMessage!!.text = message!!.text
          if (item.isTimestamp) {
            holder.textViewDate!!.text = SimpleDateFormat.getTimeInstance().format(message.timestamp)
            holder.textViewDate!!.visibility = View.VISIBLE
          }

          val constraintSet = ConstraintSet()
          constraintSet.clone(text_message_constraint_layout)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_message, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_date, ConstraintSet.START)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.END)
          constraintSet.clear(R.id.text_message_message, ConstraintSet.END)
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
              R.id.text_message_message,
              ConstraintSet.START,
              R.id.text_message_photo,
              ConstraintSet.END,
              24
            )
            constraintSet.connect(
              R.id.text_message_date,
              ConstraintSet.START,
              R.id.text_message_message,
              ConstraintSet.START
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              text_message_message.setBackgroundDrawable(
                ContextCompat.getDrawable(
                  context,
                  R.drawable.rounded_message_from
                )
              )
            } else {
              text_message_message.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_from)
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
              R.id.text_message_message,
              ConstraintSet.END,
              R.id.text_message_photo,
              ConstraintSet.START,
              24
            )
            constraintSet.connect(
              R.id.text_message_date,
              ConstraintSet.END,
              R.id.text_message_message,
              ConstraintSet.END
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              text_message_message.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_message_to))
            } else {
              text_message_message.background = ContextCompat.getDrawable(context, R.drawable.rounded_message_to)
            }
          }
          constraintSet.applyTo(text_message_constraint_layout)
        }

        if (item.isPhoto!!) {
          val userRef = FirebaseDatabase.getInstance().getReference("/users/${message!!.fromId}")
          userRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
              holder.itemView.apply {
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
    }

  }

  override fun getItemViewType(position: Int) = dataset[position].type
  override fun getItemCount() = dataset.size

  companion object {
    private const val TAG = "CustomAdapter"
  }
}