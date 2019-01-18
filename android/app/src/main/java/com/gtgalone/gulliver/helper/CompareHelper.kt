package com.gtgalone.gulliver.helper

import android.util.Log
import java.text.SimpleDateFormat

class CompareHelper {

  companion object {
    fun isSameMinute(v1: Long, v2: Long): Boolean {
      val formulaMinute = 60000
      return (v1 / formulaMinute) == (v2 / formulaMinute)
    }
    fun isSameDay(v1: Long, v2: Long): Boolean {
      return SimpleDateFormat.getDateInstance().format(v1) == SimpleDateFormat.getDateInstance().format(v2)
    }
  }
}