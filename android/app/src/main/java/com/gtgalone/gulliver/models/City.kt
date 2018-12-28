package com.gtgalone.gulliver.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class City(val id: String?, val name: String, val countryCode: String, val adminArea: String, val locality: String): Parcelable {
  constructor() : this("","", "","", "")
}