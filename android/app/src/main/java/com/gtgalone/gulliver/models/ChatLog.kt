package com.gtgalone.gulliver.models

class ChatLog(val text: String, val fromId: String, val toId: String, val timeStamp: Long) {
  constructor() : this("","", "", -1)
}