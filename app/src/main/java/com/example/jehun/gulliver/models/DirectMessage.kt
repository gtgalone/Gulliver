package com.example.jehun.gulliver.models

class DirectMessage(val id: String, val text: String, val fromId: String, val toId: String, val timeStamp: Long) {
  constructor() : this("", "", "", "" ,-1)
}