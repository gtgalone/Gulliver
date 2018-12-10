package com.gtgalone.gulliver.models

class Notification(val title: String, val body: String, val icon: String) {
    constructor() : this("", "", "")
}