package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.withRotation
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfo
import java.util.LinkedList

@OptIn(ExperimentalUnsignedTypes::class)
object Helpers {

  fun combineBgWithFgWithBestDisorder(
    captchaInfo: CaptchaInfo,
    sliderOffset: Int
  ): ResultImageData {
    if (captchaInfo.bgPixelsArgb == null) {
      val width = captchaInfo.fgBitmapPainter!!.intrinsicSize.width.toInt()
      val height = captchaInfo.fgBitmapPainter!!.intrinsicSize.height.toInt()

      return ResultImageData(
        bestDisorder = null,
        bestOffset = null,
        width = width,
        height = height,
        bestImagePixels = captchaInfo.fgPixelsArgb!!
      )
    }

    val bgPixelsArgb = captchaInfo.bgPixelsArgb
    val fgPixelsArgb = captchaInfo.fgPixelsArgb!!

    var bestDisorder = 999
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
    val fgWidthDiff = canvasHeight - width

    val resultBitmap = Bitmap.createBitmap(canvasWidth, captchaInfo.bgWidth!!, Bitmap.Config.ARGB_8888)
    val bgBitmap = Bitmap.createBitmap(captchaInfo.bgWidth!!, height, Bitmap.Config.ARGB_8888)
    val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val resultPixels = IntArray(resultBitmap.width * resultBitmap.height)

    val offset = -sliderOffset
//    for (offset in (0 downTo -50)) {
      val canvas = Canvas(resultBitmap)

      canvas.scale(-scale.toFloat(), scale.toFloat())
      canvas.rotate(90f)

//      canvas.withTranslation(x = -(bgWidthDiff / 2f)) {
        canvas.withTranslation(x = offset.toFloat()) {
          kotlin.run {
            bgBitmap.setPixels(bgPixelsArgb, 0, captchaInfo.bgWidth!!, 0, 0, captchaInfo.bgWidth!! - bgWidthDiff, height)
            canvas.drawBitmap(bgBitmap, 0f, 0f, null)
          }
        }
//      }

//      canvas.withTranslation(x = -(bgWidthDiff / 2f)) {
        kotlin.run {
          fgBitmap.setPixels(fgPixelsArgb, 0, width, 0, 0, width, height)
          canvas.drawBitmap(fgBitmap, 0f, 0f, null)
        }
//      }

      resultBitmap.getPixels(resultPixels, 0, resultBitmap.width, 0, 0, resultBitmap.width, resultBitmap.height)
      val disorder = calculateDisorder(resultPixels, width, height)

      if (disorder < bestDisorder) {
        bestDisorder = disorder
        bestImagePixels = resultPixels
        bestOffset = offset
      }
//    }

    val resultImageData = ResultImageData(
      bestDisorder = bestDisorder,
      bestOffset = bestOffset,
      width = resultBitmap.width,
      height = resultBitmap.height,
      bestImagePixels = bestImagePixels!!
    )

    resultBitmap.recycle()
    fgBitmap.recycle()
    bgBitmap.recycle()

    return resultImageData
  }

  class ResultImageData(
    val bestDisorder: Int?,
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

  private fun calculateDisorder(imagePixelsInput: IntArray, width: Int, height: Int): Int {
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

    return res / total
  }

  private fun black(x: UInt): Boolean {
    // Get the R channel of RGBA
    val r = ((x shr 24) and 0xFFu)

    return r < 64u
  }

}