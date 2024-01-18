package com.github.k1rakishou.chan4captchasolver

import android.content.Context
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object Dependencies {
  private lateinit var appContext: Context

  var appVersionCode: Float = 0.0f
    private set

  fun init(appContext: Context) {
    appVersionCode = BuildConfig.VERSION_NAME.toFloat()

    if (!::appContext.isInitialized) {
      this.appContext = appContext
    }
  }

  val moshi = lazy { Moshi.Builder().build() }

  val sharedPreferences = lazy { appContext.getSharedPreferences("chan4_captcha_solver", Context.MODE_PRIVATE) }

  val okHttpClient = lazy {
    OkHttpClient.Builder()
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .connectTimeout(30, TimeUnit.SECONDS)
      .callTimeout(30, TimeUnit.SECONDS)
      .build()
  }
}