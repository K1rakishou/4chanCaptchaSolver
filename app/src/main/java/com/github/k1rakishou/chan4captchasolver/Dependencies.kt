package com.github.k1rakishou.chan4captchasolver

import com.squareup.moshi.Moshi

object Dependencies {
  val moshi = lazy { Moshi.Builder().build() }
}