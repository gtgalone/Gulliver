package com.example.jehun.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class DirectMessageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direct_message)

        supportActionBar!!.title = "Direct Message"
    }
}