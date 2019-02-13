package com.gtgalone.gulliver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_image)
    setSupportActionBar(toolbar_image)
    supportActionBar!!.title = "Image"
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    Glide.with(this).load(intent.getByteArrayExtra(CustomAdapter.IMAGE_BYTE_ARRAY)).into(image)
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return super.onSupportNavigateUp()
  }
}