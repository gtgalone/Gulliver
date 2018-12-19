package com.gtgalone.gulliver.models

class Server(val id: String, val name: String, val displayName: String, val countryCode: String, val adminArea: String, val locality: String) {
  constructor() : this("","", "","","", "")
}