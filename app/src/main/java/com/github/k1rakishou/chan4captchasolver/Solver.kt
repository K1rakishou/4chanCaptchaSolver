package com.github.k1rakishou.chan4captchasolver

import android.content.Context
import android.graphics.Bitmap
import java.io.FileInputStream
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class Solver(
  private val context: Context
) {
  private val charset = arrayOf("", "0", "2", "4", "8", "A", "D", "G", "H", "J", "K", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "X", "Y")
  private val modelFile by lazy { loadModelFile() }

  fun solve(height: Int, pixels: IntArray) {
    val shape = intArrayOf(1, height, 80, 1)
    val byteBuffer = convertBitmapToByteBuffer(pixels, shape)

    Interpreter(modelFile, Interpreter.Options()).use { interpreter ->
      val inputIndex = 0
      val outputIndex = 0

      interpreter.resizeInput(inputIndex, shape)
      interpreter.run(byteBuffer, null)

      val tensor = interpreter.getOutputTensor(outputIndex)
      val buffer = tensor.asReadOnlyBuffer()
      val count = tensor.shape().reduce { acc, i -> acc * i }

      val results = (0 until count)
        .map { index -> buffer.getFloat(index * 4) }
        .chunked(charset.size)
        .mapNotNull { probabilities ->
          val sequence = mutableListOf<Pair<String, Float>>()
          val max = probabilities.maxBy { it }

          probabilities.forEachIndexed { charIndex, probability ->
            val prob = probability / max

            if (prob > 0.05) {
              val char = charset.getOrNull(charIndex + 1) ?: ""
              sequence += Pair(char, prob)
            }
          }

          if (sequence.isEmpty()) {
            return@mapNotNull null
          }

          return@mapNotNull sequence
        }
        .map { sequence ->
          sequence.joinToString { (char, prob) -> "\'$char\': ${prob}" }
        }

      println("results=${results}")
    }
  }

  private fun convertBitmapToByteBuffer(pixels: IntArray, shape: IntArray): ByteBuffer {
    val tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
    val floatArray = FloatArray(pixels.size)

    for (index in pixels.indices) {
      val pixelValue = pixels[index]

      // pixelValue is in ARGB format and we need to extract R
      val r = ((pixelValue.toUInt() shr 16) and 0xffu).toFloat()
      val convertedPixelValue = (r * (-1f / 238f)) + 1f

      floatArray[index] = convertedPixelValue.coerceIn(0f, 1f)
    }

    tensorBuffer.loadArray(floatArray, shape)
    return tensorBuffer.buffer
  }

  private fun loadModelFile(): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd("chan4_captcha_solver_model.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength

    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
  }

}