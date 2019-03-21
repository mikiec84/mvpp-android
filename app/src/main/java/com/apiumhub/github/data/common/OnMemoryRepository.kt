package com.apiumhub.github.data.common

import java.util.*

abstract class OnMemoryRepository {
  private val expiresIn: Long = 1000 * 60 * 5
  private var timestamp: Long = 0L
  protected var isExpired = true
    get() = Calendar.getInstance().timeInMillis > (timestamp + expiresIn)

  fun refresh() {
    this.timestamp = Calendar.getInstance().timeInMillis
  }
}