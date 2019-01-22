package com.gtgalone.gulliver.models

class ChatMessage(val id: String, val fromId: String, val toId: String, val timestamp: Long, val text: String? = null, val image: String? = null) {
  constructor() : this("", "", "", -1, "","")
}