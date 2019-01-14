package com.gtgalone.gulliver.models


class AdapterItemMessage(
  val type: Int,
  val uid: String? = "",
  val isPhoto: Boolean? = false,
  val message: ChatMessage? = null) {
  companion object {
    const val TYPE_MESSAGE_LOADER = 0
    const val TYPE_DATE_DIVIDER = 1
    const val TYPE_TEXT_MESSAGE = 2
  }
}