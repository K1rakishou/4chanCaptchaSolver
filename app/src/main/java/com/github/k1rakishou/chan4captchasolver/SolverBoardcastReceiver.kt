package com.github.k1rakishou.chan4captchasolver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SolverBoardcastReceiver : BroadcastReceiver() {
  private val moshi by Dependencies.moshi
  private var solver: Solver? = null

  private val coroutineScope = MainScope()

  init {
    Log.d(TAG, "initialized()")
  }

  @Synchronized
  private fun getOrCreateSolver(context: Context): Solver {
    if (solver != null) {
      return solver!!
    }

    solver = Solver(context)
    return solver!!
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    if (intent == null || context == null) {
      Log.d(TAG, "intent == null || context == null")
      return
    }

    val action = intent.action

    if (!isActionSupported(action)) {
      Log.d(TAG, "Action is not supported: ${action}")
      return
    }

    val pendingResult = goAsync()

    coroutineScope.launch {
      try {
        when (action) {
          ACTION_GET_INFO -> {
            Log.d(TAG, "Got ACTION_GET_INFO")

            val resultBundle = bundleOf(ACTION_GET_INFO_RESULT to API_VERSION)
            pendingResult.setResultExtras(resultBundle)

            Log.d(TAG, "ACTION_GET_INFO done")
          }
          ACTION_SOLVE_CAPTCHA -> {
            Log.d(TAG, "Got ACTION_SOLVE_CAPTCHA")

            val captchaSolutionJson = solveInternal(getOrCreateSolver(context), intent)
            if (captchaSolutionJson != null) {
              val resultBundle = bundleOf(ACTION_SOLVE_CAPTCHA_RESULT to captchaSolutionJson)
              pendingResult.setResultExtras(resultBundle)
            }

            Log.d(TAG, "ACTION_SOLVE_CAPTCHA done")
          }
          else -> {
            Log.d(TAG, "Unknown action: ${action}")
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private suspend fun solveInternal(solver: Solver, intent: Intent): String? {
    val captchaInfo = intent.getStringExtra(ACTION_SOLVE_CAPTCHA_JSON)
      ?.let { captchaJson ->
        try {
          return@let Helpers.getCaptchaInfo(moshi, captchaJson)
        } catch (error: Throwable) {
          Log.e(TAG, "moshi.fromJson(CaptchaInfo) error", error)
          return@let null
        }
      }

    if (captchaInfo == null) {
      return null
    }

    val scrollValue = intent.getFloatExtra(ACTION_SOLVE_CAPTCHA_SLIDER_OFFSET, -1f)
      .takeIf { it in 0f..1f }

    val offset = if (scrollValue == null) {
      null
    } else {
      scrollValue * captchaInfo.widthDiff()
    }

    val resultImageData = Helpers.combineBgWithFgWithBestDisorder(
      captchaInfo = captchaInfo,
      customOffset = offset
    )

    val height = resultImageData.height
    val adjustedScroll = resultImageData.adjustedScroll

    val adjustedScrollValue = if (scrollValue == null && adjustedScroll != null) {
      adjustedScroll
    } else {
      null
    }

    val solutions = solver.solve(height, resultImageData.bestImagePixels)
      .map { recognizedSequence -> recognizedSequence.sequence }

    val captchaSolution = CaptchaSolution(solutions, adjustedScrollValue)
    return moshi.adapter(CaptchaSolution::class.java)
      .toJson(captchaSolution)
  }

  private fun isActionSupported(action: String?): Boolean {
    return action == ACTION_GET_INFO || action == ACTION_SOLVE_CAPTCHA
  }

  @JsonClass(generateAdapter = true)
  class CaptchaSolution(
    @Json(name = "solutions")
    val solutions: List<String>,
    @Json(name = "slider_offset")
    val sliderOffset: Float?
  )

  companion object {
    private const val TAG = "SolverBoardcastReceiver"

    // Change this when any public api of this app changes to avoid crashes
    private const val API_VERSION = 1

    private const val PACKAGE = "com.github.k1rakishou.chan4captchasolver"
    private const val ACTION_GET_INFO = "${PACKAGE}.get_info"
    private const val ACTION_GET_INFO_RESULT = "${PACKAGE}.get_info_result"

    private const val ACTION_SOLVE_CAPTCHA = "${PACKAGE}.solve_captcha"
    private const val ACTION_SOLVE_CAPTCHA_RESULT = "${PACKAGE}.solve_captcha_result"
    private const val ACTION_SOLVE_CAPTCHA_JSON = "${PACKAGE}.solve_captcha_json"
    private const val ACTION_SOLVE_CAPTCHA_SLIDER_OFFSET = "${PACKAGE}.solve_captcha_slider_offset"
  }

}