package com.example.jehun.gulliver

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun close(v: View) {
        moveTaskToBack(true)
        exitProcess(-1)
    }
}
