package com.gtgalone.gulliver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_splash)

    try {
      Thread.sleep(2000)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }

    val uid = FirebaseAuth.getInstance().uid

    val intent: Intent

    if (uid == null) {
      intent = Intent(this, SignInActivity::class.java)
    } else {
      intent = Intent(this, MainActivity::class.java)
    }

    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

    startActivity(intent)
  }
}
