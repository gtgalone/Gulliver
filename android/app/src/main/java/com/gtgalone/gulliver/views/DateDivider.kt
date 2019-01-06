package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.date_divider.view.*
import java.text.SimpleDateFormat

class DateDivider(var timestamp: Long?) : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.apply {
      date_divider.text = SimpleDateFormat.getDateInstance().format(timestamp)
    }
  }

  override fun getLayout(): Int {
    return R.layout.date_divider
  }

  fun setTimestamp(timestamp: Long) {
    this.timestamp = timestamp
  }
}