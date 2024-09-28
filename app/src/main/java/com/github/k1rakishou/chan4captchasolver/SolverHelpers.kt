package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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

object SolverHelpers {
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
    val scale = th / height.toFloat()

    val cw = (width * scale + pw * 2).toInt()

    val canvasHeight = th
    val canvasWidth = if (cw >= 300) 300 else cw

    val bitmap = Bitmap.createBitmap(canvasHeight, imgWidth, Bitmap.Config.ARGB_8888)
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

    val fBitmap = Bitmap.createBitmap(80, 300, Bitmap.Config.ARGB_8888)
    val fcanvas = Canvas(fBitmap)

    val src = Rect(0, 0, bitmap.width, bitmap.height)
    val dst = Rect(0, 0, fBitmap.width, fBitmap.height)
    fcanvas.drawBitmap(bitmap, src, dst, paint)

    val resultPixels = IntArray(fBitmap.width * fBitmap.height)
    fBitmap.getPixels(resultPixels, 0, fBitmap.width, 0, 0, fBitmap.width, fBitmap.height)

    val adjustedScroll = if (customOffset == null && offset != null && bgBitmap != null) {
      val slideWidth = bgBitmap.width - imgBitmap.width
      Math.abs(offset) / Math.abs(slideWidth.toFloat())
    } else {
      null
    }

    val resultImageData = ResultImageData(
      offset = offset,
      adjustedScroll = adjustedScroll,
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
    this.scale(-1f, 1f)
    this.rotate(90f)

    // Fill the whole canvas with the captcha bg color (0xFFEEEEEE)
    this.drawRect(0f, 0f, this.height.toFloat(), this.width.toFloat(), paint)

    if (bg != null) {
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

  private fun pxlBlackOrWhite(r: Int, g: Int, b: Int): Int {
    return if (r + g + b > 384) 0 else 1
  }

  private fun getBoundaries(bitmap: Bitmap): List<Triple<Int, Int, Int>> {
    val width = bitmap.width
    val height = bitmap.height
    val chkArray = mutableListOf<Triple<Int, Int, Int>>()  // Store the boundary data
    var i = width * height * 4 - 1  // Similar to JavaScript, going backward through pixel data
    var opq = true  // Tracks opaque/transparent state

    while (i > 0) {
      // Get the x and y coordinates for the pixel
      val x = (i / 4) % width
      val y = (i / 4) / width

      // Get the pixel's alpha value
      val pixel = bitmap.getPixel(x, y)
      val alpha = Color.alpha(pixel)
      val isOpaque = alpha > 128  // Same logic as in JavaScript

      // Compare with the previous state (opq)
      if (isOpaque != opq) {
        // Avoid 1-width areas by checking neighboring pixel
        if ((i - 4) >= 0) {
          val neighborX = ((i - 4) / 4) % width
          val neighborY = ((i - 4) / 4) / width
          val neighborPixel = bitmap.getPixel(neighborX, neighborY)
          val neighborAlpha = Color.alpha(neighborPixel)
          if ((neighborAlpha > 128) == opq) {
            i -= 4  // Move to the next pixel
            continue
          }
        }

        // Determine if the transition was from transparent to opaque or vice versa
        if (isOpaque) {
          // Transition from transparent to opaque, check the next pixel
          val nextPixel = bitmap.getPixel(x, y)
          val clr = pxlBlackOrWhite(Color.red(nextPixel), Color.green(nextPixel), Color.blue(nextPixel))
          chkArray.add(Triple(x, y, clr))
        } else {
          // Transition from opaque to transparent, check the previous pixel
          val prevX = ((i - 3) / 4) % width
          val prevY = ((i - 3) / 4) / width
          val prevPixel = bitmap.getPixel(prevX, prevY)
          val clr = pxlBlackOrWhite(Color.red(prevPixel), Color.green(prevPixel), Color.blue(prevPixel))
          chkArray.add(Triple(prevX, prevY, clr))
        }

        // Update the opaque state
        opq = isOpaque
      }

      // Move to the previous pixel (step back by 4 to match the RGBA structure)
      i -= 4
    }

    return chkArray
  }

  private fun getBestPos(bg: Bitmap, chkArray: List<Triple<Int, Int, Int>>, slideWidth: Int): Float {
    val width = bg.width
    val height = bg.height
    var bestSimilarity = 0
    var bestPos = 0

    // Iterate over all possible slide positions (from 0 to slideWidth)
    for (s in 0..slideWidth) {
      var similarity = 0
      val amount = chkArray.size

      // Check each pixel in chkArray
      for (p in 0 until amount) {
        val chk = chkArray[p]
        val x = chk.first + s
        val y = chk.second
        val clr = chk.third

        // Check bounds to avoid accessing out-of-bounds pixels
        if (x >= width || y >= height || x < 0 || y < 0) continue

        // Get the background pixel at (x, y)
        val pixel = bg.getPixel(x, y)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // Determine if the background pixel is black or white
        val bgclr = pxlBlackOrWhite(r, g, b)

        // Compare the background color with the chkArray color
        if (bgclr == clr) {
          similarity += 1
        }
      }

      // Update the best position if current similarity is higher
      if (similarity > bestSimilarity) {
        bestSimilarity = similarity
        bestPos = s
      }
    }

    // Return the best position as a percentage
    return bestPos.toFloat() / slideWidth * 100
  }

  private fun slideCaptcha(imgBitmap: Bitmap, bgBitmap: Bitmap): Float {
    val imgBitmapPostProcessed = removeNoise(bitmap = imgBitmap, noiseThreshold = 1)
    val bgBitmapPostProcessed = removeNoise(bitmap = bgBitmap, noiseThreshold = 1)

    val chkArray = getBoundaries(imgBitmapPostProcessed)
    val slideWidth = bgBitmapPostProcessed.width - imgBitmapPostProcessed.width
    val sliderPos = getBestPos(bgBitmapPostProcessed, chkArray, slideWidth)
    return sliderPos / 2
  }

  private fun removeNoise(bitmap: Bitmap, noiseThreshold: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    // Function to mark clusters and decide whether to remove them
    fun markCluster(x: Int, y: Int, marked: Array<BooleanArray>, clusterSize: Int) {
      val stack = mutableListOf(Pair(x, y))
      val cluster = mutableListOf<Pair<Int, Int>>()

      while (stack.isNotEmpty()) {
        val (cx, cy) = stack.removeAt(stack.size - 1)

        if (cx < 0 || cy < 0 || cx >= width || cy >= height || marked[cy][cx]) continue

        val pixel = mutableBitmap.getPixel(cx, cy)
        val alpha = Color.alpha(pixel)

        // Check if it's an opaque pixel
        if (alpha >= 128) {
          cluster.add(Pair(cx, cy))
          marked[cy][cx] = true

          // Add neighbors to stack
          stack.add(Pair(cx - 1, cy))
          stack.add(Pair(cx + 1, cy))
          stack.add(Pair(cx, cy - 1))
          stack.add(Pair(cx, cy + 1))
        }
      }

      if (cluster.size <= clusterSize) {
        // If the cluster is small, mark it for removal
        for ((cx, cy) in cluster) {
          mutableBitmap.setPixel(cx, cy, Color.TRANSPARENT)
        }
      }
    }

    // Create an array to mark the processed pixels
    val marked = Array(height) { BooleanArray(width) }

    // Iterate over the pixels and process them
    for (y in 0 until height) {
      for (x in 0 until width) {
        if (!marked[y][x]) {
          markCluster(x, y, marked, noiseThreshold)
        }
      }
    }

    return mutableBitmap
  }

  class ResultImageData(
    val offset: Float?,
    val adjustedScroll: Float?,
    val width: Int,
    val height: Int,
    val bestImagePixels: IntArray
  )

}