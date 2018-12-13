package com.gtgalone.gulliver.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Location(val countryCode: String, val adminArea: String, val locality: String): Parcelable {
  constructor() : this("", "", "")
}