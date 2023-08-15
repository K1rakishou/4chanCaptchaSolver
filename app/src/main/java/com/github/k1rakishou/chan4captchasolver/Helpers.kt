package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.withTranslation
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfo
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfoRaw
import com.squareup.moshi.Moshi
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlinx.coroutines.coroutineScope

object Helpers {
  private const val TAG = "Helpers"
  private val captchaBgColor = 0xFFEEEEEE.toInt()

  @OptIn(ExperimentalTime::class)
  suspend fun combineBgWithFgWithBestDisorder(
    captchaInfo: CaptchaInfo,
    customOffset: Float?,
  ): ResultImageData {
    val (resultImageData, duration) = measureTimedValue {
      coroutineScope {
        combineBgWithFgWithBestDisorderInternal(
          captchaInfo = captchaInfo,
          customOffset = customOffset
        )
      }
    }

    Log.d(TAG, "combineBgWithFgWithBestDisorder() took ${duration}")

    return resultImageData
  }

  fun getCaptchaInfo(moshi: Moshi, captchaJson: String): CaptchaInfo? {
    val captchaInfoRawAdapter = moshi.adapter(CaptchaInfoRaw::class.java)
    val testCaptchaInfoRaw = captchaInfoRawAdapter.fromJson(captchaJson)!!

    val (bgBitmapPainter, bgBitmap, bgPixels) = testCaptchaInfoRaw.bg?.let { bgBase64Img ->
      val bgByteArray = Base64.decode(bgBase64Img, Base64.DEFAULT)
      val bitmap = BitmapFactory.decodeByteArray(bgByteArray, 0, bgByteArray.size)

      val pixels = IntArray(bitmap.width * bitmap.height)
      bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

      return@let Triple(BitmapPainter(bitmap.asImageBitmap()), bitmap, pixels)
    } ?: Triple(null, null, null)

    val (fgBitmapPainter, fgBitmap, imgPixels) = testCaptchaInfoRaw.img?.let { imgBase64Img ->
      val imgByteArray = Base64.decode(imgBase64Img, Base64.DEFAULT)
      val bitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)

      val pixels = IntArray(bitmap.width * bitmap.height)
      bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

      return@let Triple(BitmapPainter(bitmap.asImageBitmap()), bitmap, pixels)
    } ?: Triple(null, null, null)

    return CaptchaInfo(
      bgBitmapPainter = bgBitmapPainter,
      imgBitmapPainter = fgBitmapPainter!!,
      bgBitmap = bgBitmap,
      imgBitmap = fgBitmap,
      bgPixelsArgb = bgPixels,
      imgPixelsArgb = imgPixels,
      challenge = testCaptchaInfoRaw.challenge!!,
      startedAt = System.currentTimeMillis(),
      ttlSeconds = testCaptchaInfoRaw.ttl!!,
      imgWidth = testCaptchaInfoRaw.imgWidth,
      bgWidth = testCaptchaInfoRaw.bgWidth
    )
  }

  private suspend fun combineBgWithFgWithBestDisorderInternal(
    captchaInfo: CaptchaInfo,
    customOffset: Float?
  ): ResultImageData {
    val bgPixelsArgb = captchaInfo.bgPixelsArgb
    val imgPixelsArgb = captchaInfo.imgPixelsArgb!!

    val imgSize = captchaInfo.imgBitmapPainter!!.intrinsicSize
    val bgSize = captchaInfo.bgBitmapPainter!!.intrinsicSize

    return imageFromCanvas(
      imgBitmap = captchaInfo.imgBitmap!!,
      bgBitmap = captchaInfo.bgBitmap,
      img = imgPixelsArgb,
      bg = bgPixelsArgb,
      imgWidth = imgSize.width.toInt(),
      imgHeight = imgSize.height.toInt(),
      bgWidth = bgSize.width.toInt(),
      bgHeight = bgSize.height.toInt(),
      customOffset = customOffset
    )
  }

  private suspend fun imageFromCanvas(
    imgBitmap: Bitmap,
    bgBitmap: Bitmap?,
    img: IntArray,
    bg: IntArray?,
    imgWidth: Int,
    imgHeight: Int,
    bgWidth: Int = 0,
    bgHeight: Int = 0,
    customOffset: Float?
  ): ResultImageData {
    val paint = Paint().apply {
      flags = Paint.ANTI_ALIAS_FLAG
      color = captchaBgColor
      style = Paint.Style.FILL
    }

    val width = imgWidth
    val height = imgHeight
    val th = 80
    val pw = 16
    val scale = th / height

    val cw = width * scale + pw * 2;

    val canvasHeight = th
    val canvasWidth = if (cw >= 300) 300 else cw

    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
    val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    var offset: Float? = customOffset

    if (customOffset == null && bgBitmap != null) {
      offset = slideCaptcha(
        imgBitmap,
        bgBitmap
      )
    }

    val canvas = Canvas(bitmap)
    canvas.drawImage(
      offset = offset ?: 0f,
      bg = bg,
      img = img,
      bgWidth = bgWidth,
      width = width,
      height = height,
      paint = paint,
      fgBitmap = fgBitmap
    )

    val fBitmap = Bitmap.createBitmap(300, 80, Bitmap.Config.ARGB_8888)
    val fcanvas = Canvas(fBitmap)

    val src = Rect(0, 0, bitmap.width, bitmap.height)
    val dst = Rect(0, 0, fBitmap.width, fBitmap.height)
    fcanvas.drawBitmap(bitmap, src, dst, paint)

    val resultPixels = IntArray(fBitmap.width * fBitmap.height)
    fBitmap.getPixels(resultPixels, 0, fBitmap.width, 0, 0, fBitmap.width, fBitmap.height)

    val resultImageData = ResultImageData(
      offset = offset,
      width = fBitmap.width,
      height = fBitmap.height,
      bestImagePixels = resultPixels
    )

    bitmap.recycle()
    fBitmap.recycle()
    fgBitmap.recycle()

    return resultImageData
  }

  private fun Canvas.drawImage(
    offset: Float,
    bg: IntArray?,
    img: IntArray,
    bgWidth: Int,
    width: Int,
    height: Int,
    paint: Paint,
    fgBitmap: Bitmap
  ) {
    // Fill the whole canvas with the captcha bg color (0xFFEEEEEE)
    this.drawRect(0f, 0f, this.width.toFloat(), this.height.toFloat(), paint)

    if (bg != null) {
      // Draw the background image in the center of the canvas with "currentOffset" px horizontal translation
      this.withTranslation(x = -offset) {
        kotlin.run {
          val bitmap = Bitmap.createBitmap(bgWidth, height, Bitmap.Config.ARGB_8888)

          bitmap.setPixels(bg, 0, bgWidth, 0, 0, bgWidth, height)
          this.drawBitmap(bitmap, 0f, 0f, null)
        }
      }
    }

    // Draw the foreground image in the center of the canvas
    kotlin.run {
      fgBitmap.setPixels(img, 0, width, 0, 0, width, height)
      this.drawBitmap(fgBitmap, 0f, 0f, null)
    }
  }

  fun pxlBlackOrWhite(r: Int, g: Int, b: Int): Int {
    return if (r + g + b > 384) 0 else 1
  }

  fun getBoundaries(img: Bitmap): List<IntArray> {
    val width = img.width
    val height = img.height
    val chkArray = mutableListOf<IntArray>()

    var opq = true
    for (y in 0 until height) {
      for (x in 0 until width) {
        val pixel = img.getPixel(x, y)
        val alpha = (pixel shr 24) and 0xff
        val red = (pixel shr 16) and 0xff
        val green = (pixel shr 8) and 0xff
        val blue = pixel and 0xff

        val a = alpha > 128
        if (a != opq) {
          val clr = pxlBlackOrWhite(red, green, blue)
          chkArray.add(intArrayOf(x, y, clr))
          opq = a
        }
      }
    }
    return chkArray
  }

  fun getBestPos(bg: Bitmap, chkArray: List<IntArray>, slideWidth: Int): Float {
    val width = bg.width
    var bestSimilarity = 0
    var bestPos = 0

    for (s in 0..slideWidth) {
      var similarity = 0
      for (chk in chkArray) {
        val x = chk[0] + s
        val y = chk[1]
        val clr = chk[2]

        val pixel = bg.getPixel(x, y)
        val red = (pixel shr 16) and 0xff
        val green = (pixel shr 8) and 0xff
        val blue = pixel and 0xff

        val bgClr = pxlBlackOrWhite(red, green, blue)
        if (bgClr == clr) {
          similarity++
        }
      }
      if (similarity > bestSimilarity) {
        bestSimilarity = similarity
        bestPos = s
      }
    }
    return (bestPos.toFloat() / slideWidth) * 100f
  }

  fun slideCaptcha(imgBitmap: Bitmap, bgBitmap: Bitmap): Float {
    val chkArray = getBoundaries(imgBitmap)
    val slideWidth = bgBitmap.width - imgBitmap.width
    val sliderPos = getBestPos(bgBitmap, chkArray, slideWidth)
    return sliderPos / 2
  }

  class ResultImageData(
    val offset: Float?,
    val width: Int,
    val height: Int,
    val bestImagePixels: IntArray
  )

}