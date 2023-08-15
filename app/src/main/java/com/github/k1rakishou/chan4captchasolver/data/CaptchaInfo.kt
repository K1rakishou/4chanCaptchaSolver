package com.github.k1rakishou.chan4captchasolver.data

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfoRaw.Companion.NOOP_CHALLENGE


class CaptchaInfo(
  val bgBitmapPainter: BitmapPainter?,
  val imgBitmapPainter: BitmapPainter?,
  val bgBitmap: Bitmap?,
  val imgBitmap: Bitmap?,
  val bgPixelsArgb: IntArray?,
  val imgPixelsArgb: IntArray?,
  val challenge: String,
  val startedAt: Long,
  val ttlSeconds: Int,
  val imgWidth: Int?,
  val bgWidth: Int?
) {
  var currentInputValue = mutableStateOf<String>("")
  var sliderValue = mutableStateOf(0f)

  fun widthDiff(): Int {
    if (imgWidth == null || bgWidth == null) {
      return 0
    }

//    return abs(imgWidth - bgWidth)

    // Hardcode it as 50 because that's how it is in the script and using "imgWidth - bgWidth"
    // (which is always 49) causes the NN model to fail.
    return 50
  }

  fun reset() {
    currentInputValue.value = ""
    sliderValue.value = 0f
  }

  fun needSlider(): Boolean = bgBitmapPainter != null

  fun ttlMillis(): Long {
    val ttlMillis = ttlSeconds * 1000L

    return ttlMillis - (System.currentTimeMillis() - startedAt)
  }

  fun isNoopChallenge(): Boolean {
    return challenge.equals(NOOP_CHALLENGE, ignoreCase = true)
  }

}
