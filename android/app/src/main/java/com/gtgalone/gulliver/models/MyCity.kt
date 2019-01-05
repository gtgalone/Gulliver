package com.gtgalone.gulliver.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class MyCity(val id: String, val countryCode: String, val adminArea: String, val locality: String, val timestamp: Long) : Parcelable {
  constructor() : this("", "", "", "", -1)
}