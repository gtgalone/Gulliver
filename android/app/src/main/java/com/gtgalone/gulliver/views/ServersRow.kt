package com.gtgalone.gulliver.views

import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.FavoriteServer
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_servers_row.view.*

class ServersRow(private val server: FavoriteServer) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.activity_main_servers_row_text_view.text = server.serverDisplayName
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_servers_row
  }
}