package com.gtgalone.gulliver

import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.gtgalone.gulliver.models.AdapterItemMessage
import com.gtgalone.gulliver.models.User
import kotlinx.android.synthetic.main.text_message.view.*
import java.text.SimpleDateFormat

class CustomAdapter(private val dataset: ArrayList<AdapterItemMessage>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
  val uid = FirebaseAuth.getInstance().uid

  class ViewHolder(v: View, viewType: Int) : RecyclerView.ViewHolder(v) {
    var imageViewPhoto: ImageView? = null
    var imageViewContent: ImageView? = null
    var textViewContent: TextView? = null
    var textViewDate: TextView? = null
    var textViewDisplayName: TextView? = null
    var textViewDateDivider: TextView? = null

    init {
      v.setOnClickListener { Log.d(TAG, "Clicked $adapterPosition") }
      when (viewType) {
        AdapterItemMessage.TYPE_DATE_DIVIDER -> textViewDateDivider = v.findViewById(R.id.date_divider)
        AdapterItemMessage.TYPE_IMAGE_MESSAGE -> {
          imageViewPhoto = v.findViewById(R.id.text_message_photo)
          imageViewContent = v.findViewById(R.id.text_message_content)
          textViewDate = v.findViewById(R.id.text_message_date)
          textViewDisplayName = v.findViewById(R.id.text_message_display_name)
        }
        else -> {
          imageViewPhoto = v.findViewById(R.id.text_message_photo)
          textViewContent = v.findViewById(R.id.text_message_content)
          textViewDate = v.findViewById(R.id.text_message_date)
          textViewDisplayName = v.findViewById(R.id.text_message_display_name)
        }
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    Log.d(TAG, viewType.toString())

    val layout = when (viewType) {
      AdapterItemMessage.TYPE_MESSAGE_LOADER -> R.layout.message_loader
      AdapterItemMessage.TYPE_DATE_DIVIDER -> R.layout.date_divider
      AdapterItemMessage.TYPE_IMAGE_MESSAGE -> R.layout.image_message
      else -> R.layout.text_message
    }

    val v = LayoutInflater.from(parent.context)
      .inflate(layout, parent,false)

    return ViewHolder(v, viewType)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = dataset[position]
    when (item.type) {
      AdapterItemMessage.TYPE_MESSAGE_LOADER -> return
      AdapterItemMessage.TYPE_DATE_DIVIDER -> {
        holder.textViewDateDivider!!.text = SimpleDateFormat.getDateInstance().format(item.message!!.timestamp)
      }
      else -> {
        val message = item.message

        holder.apply {

          val constraintSet = ConstraintSet()
          constraintSet.clone(itemView.text_message_constraint_layout)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_content, ConstraintSet.START)
          constraintSet.clear(R.id.text_message_date, ConstraintSet.START)

          constraintSet.clear(R.id.text_message_photo, ConstraintSet.END)
          constraintSet.clear(R.id.text_message_content, ConstraintSet.END)
          constraintSet.clear(R.id.text_message_date, ConstraintSet.END)

          val background: Drawable?

          if (message!!.fromId != uid) {
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
              R.id.text_message_display_name,
              ConstraintSet.END,
              16
            )
            constraintSet.connect(
              R.id.text_message_display_name,
              ConstraintSet.START,
              R.id.text_message_photo,
              ConstraintSet.END,
              24
            )

            constraintSet.applyTo(itemView.text_message_constraint_layout)

            background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_message_from)
          } else {
            constraintSet.connect(
              R.id.text_message_content,
              ConstraintSet.END,
              ConstraintSet.PARENT_ID,
              ConstraintSet.END,
              24
            )

            constraintSet.connect(
              R.id.text_message_display_name,
              ConstraintSet.END,
              R.id.text_message_content,
              ConstraintSet.END,
              0
            )

            constraintSet.connect(
              R.id.text_message_date,
              ConstraintSet.END,
              R.id.text_message_display_name,
              ConstraintSet.START,
              16
            )

            constraintSet.applyTo(itemView.text_message_constraint_layout)

            if (item.isPhoto!!) {
              textViewDisplayName!!.visibility = View.VISIBLE
              textViewDisplayName!!.text = itemView.context.getString(R.string.me)
              textViewDate!!.visibility = View.VISIBLE
              textViewDate!!.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(message.timestamp)
            }

            background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_message_to)
          }

          when (message.messageType) {
            AdapterItemMessage.TYPE_IMAGE_MESSAGE -> {

              FirebaseStorage.getInstance().reference
                .child(message.body!!).getBytes(1024*1024)
                .addOnSuccessListener {
                  Glide.with(itemView).load(it).into(holder.imageViewContent!!)
                }
            }
            else -> {
              holder.textViewContent!!.text = message.body

              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                itemView.text_message_content.setBackgroundDrawable(background)
              } else {
                itemView.text_message_content.background = background
              }
            }
          }
        }

        if (item.isPhoto!! && message!!.fromId != uid) {
          val userRef = FirebaseDatabase.getInstance().getReference("/users/${message.fromId}")
          userRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
              val user = p0.getValue(User::class.java) ?: return
              holder.apply {
                textViewDate!!.text = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(message.timestamp)
                textViewDate!!.visibility = View.VISIBLE
                textViewDisplayName!!.text = user.displayName
                textViewDisplayName!!.visibility = View.VISIBLE
                Glide.with(itemView.context).load(user.photoUrl).into(itemView.text_message_photo)
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