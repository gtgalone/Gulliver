package com.gtgalone.gulliver.views

import android.os.Build
import androidx.core.content.ContextCompat
import com.gtgalone.gulliver.R
import com.gtgalone.gulliver.models.FavoriteServer
import com.gtgalone.gulliver.models.User
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_servers_row.view.*

class ServersRow(val favoriteServer: FavoriteServer, val user: User) : Item<ViewHolder>() {
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.apply {
      activity_main_servers_row_locality.text = favoriteServer.locality
      if (favoriteServer.locality == favoriteServer.adminArea) {
        activity_main_servers_row_admin_country.text = favoriteServer.countryCode
      } else {
        activity_main_servers_row_admin_country.text = viewHolder.itemView.resources.getString(
          R.string.servers_row_admin_country,
          favoriteServer.adminArea,
          favoriteServer.countryCode
        )
      }
    }

    if (favoriteServer.serverId == user.currentServer) {
      viewHolder.itemView.apply {
        activity_main_servers_row_locality
          .setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorWhite))
        activity_main_servers_row_admin_country
          .setTextColor(ContextCompat.getColor(viewHolder.itemView.context, R.color.colorWhite))
      }
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
        viewHolder.itemView.activity_main_servers_row_layout
          .setBackgroundDrawable(ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.background))
      } else {
        viewHolder.itemView.activity_main_servers_row_layout
          .background = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.background)
      }
    }
  }

  override fun getLayout(): Int {
    return R.layout.activity_main_servers_row
  }
}