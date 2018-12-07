package com.example.jehun.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.jehun.gulliver.models.User
import com.google.firebase.database.FirebaseDatabase

class DirectMessageActivity : AppCompatActivity() {

//  val fromUser = intent.getParcelableExtra<User>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_direct_message)

    supportActionBar!!.title = "Direct Message"

  }

//  private fetchDirectMessages() {
//    val fromId =
//    FirebaseDatabase.getInstance().getReference("/direct-messages/$fromId/$toId")
//  }

}
