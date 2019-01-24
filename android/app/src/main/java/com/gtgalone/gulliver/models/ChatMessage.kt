package com.gtgalone.gulliver.models

class ChatMessage(val id: String, val fromId: String, val toId: String, val timestamp: Long, val body: String? = null, val messageType: Int? = null) {
  constructor() : this("", "", "", -1, "",-1)
}