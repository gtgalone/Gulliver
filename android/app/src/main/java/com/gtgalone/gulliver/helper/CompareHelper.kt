package com.gtgalone.gulliver.helper

class CompareHelper {

  companion object {
    fun isSameMinute(v1: Long, v2: Long): Boolean {
      val formulaMinute = 60000
      return (v1 / formulaMinute) == (v2 / formulaMinute)
    }

    fun isSameDay(v1: Long, v2: Long): Boolean {
      val formulaDay = 1000 * 60 * 60 * 24
      return (v1 / formulaDay) == (v2 / formulaDay)
    }
  }
}