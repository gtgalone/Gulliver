package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

class MessageLoading : Item() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
  }

  override fun getLayout(): Int {
    return R.layout.message_loader
  }
}