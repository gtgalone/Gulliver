package com.gtgalone.gulliver

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import kotlinx.android.synthetic.main.app_bar_add_city.*
import kotlinx.android.synthetic.main.custom_view_search.*
import org.jetbrains.anko.custom.customView

class AddCityActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_city)

    setSupportActionBar(toolbar_add_city)
//    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.customView = custom_view_search
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }

  override fun onNewIntent(intent: Intent?) {
    Log.d("test", "new")
    super.onNewIntent(intent)
  }
}
