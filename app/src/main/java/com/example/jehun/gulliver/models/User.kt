package com.example.jehun.gulliver.models

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val displayName: String, val email: String, val photoUrl: String): Parcelable {
  constructor() : this("", "", "", "")
}
