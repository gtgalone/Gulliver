package com.gtgalone.gulliver.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val displayName: String, val email: String, val photoUrl: String, val currentCity: String?, val currentChannel: String?): Parcelable {
  constructor() : this("", "", "", "", "", "")
}
