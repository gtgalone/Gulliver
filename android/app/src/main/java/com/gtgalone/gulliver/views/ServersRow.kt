package com.gtgalone.gulliver.views

import androidx.core.content.ContextCompat
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.FavoriteServer
import com.gtgalone.gulliver.models.User
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_servers_row.view.*

class ServersRow(val server: FavoriteServer, val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.activity_main_servers_row_text_view.text = server.serverDisplayName

    if (server.serverId == user.currentServer) {

      viewHolder.itemView.activity_main_servers_row_text_view
        .setBackgroundColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorPrimary))
      viewHolder.itemView.activity_main_servers_row_text_view
        .setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorWhite))
    }
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_servers_row
  }
}