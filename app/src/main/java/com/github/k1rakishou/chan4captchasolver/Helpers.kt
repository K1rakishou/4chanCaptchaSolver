package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.withTranslation
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfo
import java.util.LinkedList

@OptIn(ExperimentalUnsignedTypes::class)
object Helpers {
  private const val TAG = "Helpers"
  private val captchaBgColor = 0xFFEEEEEE.toInt()
  const val maxOffset = 50

  fun combineBgWithFgWithBestDisorder(
    captchaInfo: CaptchaInfo,
    customOffset: Float?,
  ): ResultImageData {
    val paint = Paint().apply {
      flags = Paint.ANTI_ALIAS_FLAG
      color = captchaBgColor
      style = Paint.Style.FILL
    }

    val bgPixelsArgb = captchaInfo.bgPixelsArgb
    val fgPixelsArgb = captchaInfo.fgPixelsArgb!!

    var bestDisorder = 999f
    var bestImagePixels: IntArray? = null
    var bestOffset = -1

    val width = captchaInfo.fgBitmapPainter!!.intrinsicSize.width.toInt()
    val height = captchaInfo.fgBitmapPainter!!.intrinsicSize.height.toInt()
    val th = 80
    val pw = 16
    val scale = th / height
    val canvasHeight = width * scale + pw * 2
    val canvasWidth = th

    val bgWidthDiff = canvasHeight - width
    val halfBgWidthDiff = bgWidthDiff / 2f

    val resultBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
    var bgBitmap: Bitmap? = null
    val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val resultPixels = IntArray(resultBitmap.width * resultBitmap.height)

    val offsets = if (customOffset != null) {
      IntProgression.fromClosedRange(-customOffset.toInt(), -customOffset.toInt(), 1)
    } else {
      0 downTo -maxOffset
    }

    for (offset in offsets) {
      val canvas = Canvas(resultBitmap)

      // Fill the whole canvas with the captcha bg color (0xFFEEEEEE)
      canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)

      // Flip the image horizontally and then rotate it. I don't understand why we need to do this
      // either. Probably the model was trained this way.
      canvas.scale(-scale.toFloat(), scale.toFloat())
      canvas.rotate(90f)

      if (bgPixelsArgb != null) {
        // Draw the background image in the center of the canvas
        canvas.withTranslation(x = halfBgWidthDiff) {
          canvas.withTranslation(x = offset.toFloat()) {
            kotlin.run {
              val bitmap = Bitmap.createBitmap(captchaInfo.bgWidth!!, height, Bitmap.Config.ARGB_8888)
              bgBitmap = bitmap

              bitmap.setPixels(bgPixelsArgb, 0, captchaInfo.bgWidth!!, 0, 0, captchaInfo.bgWidth!! - bgWidthDiff, height)
              canvas.drawBitmap(bitmap, 0f, 0f, null)
            }
          }
        }
      }

      // Draw the foreground image in the center of the canvas
      canvas.withTranslation(x = halfBgWidthDiff) {
        kotlin.run {
          fgBitmap.setPixels(fgPixelsArgb, 0, width, 0, 0, width, height)
          canvas.drawBitmap(fgBitmap, 0f, 0f, null)
        }
      }

      kotlin.run {
        // Fill the leftmost part of the captcha image with the captcha bg color (0xFFEEEEEE).
        // This will remove all the noise which helps with character recognition
        canvas.drawRect(
          0f,
          0f,
          halfBgWidthDiff,
          resultBitmap.width.toFloat(),
          paint
        )

        // The same but for the rightmost part of the captcha image
        canvas.drawRect(
          resultBitmap.height.toFloat() - (bgWidthDiff / 2f),
          0f,
          resultBitmap.height.toFloat(),
          resultBitmap.width.toFloat(),
          paint
        )
      }

      resultBitmap.getPixels(resultPixels, 0, resultBitmap.width, 0, 0, resultBitmap.width, resultBitmap.height)

      val disorder = calculateDisorder(resultPixels, resultBitmap.width, resultBitmap.height)
      Log.d(TAG, "offset=${offset}, disorder=${disorder}, bestOffset=${bestOffset}, bestDisorder=${bestDisorder}")

      if (disorder < bestDisorder) {
        bestDisorder = disorder
        bestImagePixels = resultPixels.clone()
        bestOffset = offset
      }
    }

    val resultImageData = ResultImageData(
      bestOffset = -bestOffset,
      width = resultBitmap.width,
      height = resultBitmap.height,
      bestImagePixels = bestImagePixels!!
    )

    resultBitmap.recycle()
    fgBitmap.recycle()
    bgBitmap?.recycle()

    return resultImageData
  }

  class ResultImageData(
    val bestOffset: Int?,
    val width: Int,
    val height: Int,
    val bestImagePixels: IntArray
  )

  private fun ubyteArrayToIntArray(
    bytes: UByteArray,
    width: Int,
    height: Int
  ): IntArray {
    val intArray = IntArray(width * height) { 0xFFEEEEEE.toInt() }
    var intArrayIndex = 0

    for (offset in (0 until (bytes.size - 4)) step 4) {
      var argb = 0u
      argb = argb or ((bytes[offset + 0] and 0xFF.toUByte()).toUInt() shl 24)
      argb = argb or ((bytes[offset + 1] and 0xFF.toUByte()).toUInt() shl 16)
      argb = argb or ((bytes[offset + 2] and 0xFF.toUByte()).toUInt() shl 8)
      argb = argb or ((bytes[offset + 3] and 0xFF.toUByte()).toUInt() shl 0)

      intArray[intArrayIndex] = argb.toInt()
      ++intArrayIndex
    }

    return intArray
  }

  private fun intArrayToArgbArray(
    pixelsArgb: IntArray,
    width: Int,
    height: Int
  ): UByteArray {
    val byteArray = UByteArray(width * height * 4)

    for ((index, argb) in pixelsArgb.withIndex()) {
      byteArray[index + 0] = (argb ushr 24).toUByte()
      byteArray[index + 1] = (argb ushr 16).toUByte()
      byteArray[index + 2] = (argb ushr 8).toUByte()
      byteArray[index + 3] = argb.toUByte()
    }

    return byteArray
  }

  private fun calculateDisorder(imagePixelsInput: IntArray, width: Int, height: Int): Float {
    val pic = hashMapOf<Int, Int>()
    val visited = hashSetOf<Int>()
    val totalCount = width * height
    val imagePixels = imagePixelsInput.toUIntArray()

    for (idx in 0 until totalCount) {
      if (visited.contains(idx)) {
        continue
      }

      if (!black(imagePixels[idx])) {
        continue
      }

      var blackCount = 0
      val items = mutableListOf<Int>()
      val toVisit = LinkedList<Int>()
      toVisit.push(idx)

      while (toVisit.isNotEmpty()) {
        val cc = toVisit.pop()

        if (!visited.add(cc)) {
          continue
        }

        if (black(imagePixels[cc])) {
          items += cc
          blackCount++

          toVisit.push(cc + 1)
          toVisit.push(cc - 1)
          toVisit.push(cc + width)
          toVisit.push(cc - width)
        }
      }

      if (blackCount >= 24) {
        items.forEach { x -> pic[x] = 1 }
      }
    }

    var res = 0
    var total = 0

    for (idx in 0 until (width * height - width)) {
      if (pic[idx] != pic[idx + width]) {
        res += 1
      }

      if (pic[idx] != 0) {
        total += 1
      }
    }

    if (total == 0) {
      total = 1
    }

    return res.toFloat() / total.toFloat()
  }

  private fun black(x: UInt): Boolean {
    // Get the R channel of ARGB
    val r = ((x shr 16) and 0xFFu)

    return r < 64u
  }

}