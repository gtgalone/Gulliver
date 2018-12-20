package com.gtgalone.gulliver.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class FavoriteServer(val id: String?, val serverId: String?, val serverDisplayName: String?) : Parcelable {
  constructor() : this("", "", "")
}