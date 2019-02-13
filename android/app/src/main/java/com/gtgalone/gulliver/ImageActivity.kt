package com.gtgalone.gulliver

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {

    val aa = intent.getByteArrayExtra("aa")
    Log.d("test", aa.toString())
    Glide.with(this).load(aa).into(image)
    super.onCreate(savedInstanceState)
  }

}