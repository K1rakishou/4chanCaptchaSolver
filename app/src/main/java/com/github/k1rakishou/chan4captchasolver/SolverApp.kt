package com.github.k1rakishou.chan4captchasolver

import android.app.Application
import android.content.Context
import android.util.Log
import logcat.LogPriority
import logcat.LogcatLogger


class SolverApp : Application() {

  override fun attachBaseContext(base: Context) {
    Dependencies.init(base)
    super.attachBaseContext(base)
  }

  override fun onCreate() {
    super.onCreate()

    LogcatLogger.install(SolverLogger())
  }

  private class SolverLogger : LogcatLogger {
    override fun log(priority: LogPriority, tag: String, message: String) {
      when (priority) {
        LogPriority.VERBOSE -> Log.v("$GLOBAL_TAG | $tag", message)
        LogPriority.DEBUG -> Log.d("$GLOBAL_TAG | $tag", message)
        LogPriority.INFO -> Log.i("$GLOBAL_TAG | $tag", message)
        LogPriority.WARN -> Log.w("$GLOBAL_TAG | $tag", message)
        LogPriority.ERROR -> Log.e("$GLOBAL_TAG | $tag", message)
        LogPriority.ASSERT -> Log.e("$GLOBAL_TAG | $tag", message)
      }
    }
  }

  companion object {
    private const val GLOBAL_TAG = "4SolverApp"
  }

}