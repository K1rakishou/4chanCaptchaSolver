package com.github.k1rakishou.chan4captchasolver

import android.content.Context
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Solver(
  private val context: Context
) {
  private val charset = arrayOf(' ', '0', '2', '4', '8', 'A', 'D', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'R', 'S', 'T', 'V', 'W', 'X', 'Y')
  private val modelFile by lazy { loadModelFile() }
  private val threadsCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

  @OptIn(ExperimentalTime::class)
  fun solve(width: Int, pixels: IntArray): String {
    val (result, duration) = measureTimedValue {
      return@measureTimedValue solveInternal(
        width = width,
        pixels = pixels
      )
    }

    Log.d(TAG, "solve() took ${duration}, result: '${result}'")
    return result
  }

  private fun solveInternal(
    width: Int,
    height: Int = 80,
    pixels: IntArray
  ): String {
    val options = Interpreter.Options().apply {
      Log.d(TAG, "solve() using cpu with ${threadsCount} threads")
      numThreads = threadsCount
    }

    val groups = pixels.groupBy { it }
    for ((pixel, grouped) in groups) {
      println("${pixel}: ${grouped.size}")
    }

    return Interpreter(modelFile, options).use { model ->
      val mono = convertBitmapToByteBuffer(pixels, width, height)
      model.run(mono, null)

      val tensor = model.getOutputTensor(0)
      val outputBuffer = tensor.asReadOnlyBuffer()

      // tensor.shape() is [1, 75, 23] here
      val count = tensor.shape().reduce { acc, i -> acc * i }

      val outputFloatArray = FloatArray(count) { 0f }

      (0 until count)
        .forEach { index -> outputFloatArray[index] = outputBuffer.getFloat(index * 4) }

      val prediction = outputFloatArray.argmax(tensor.shape())
      val processedSequence = processCTCDecodedSequence(prediction, charset.size + 1)
      val result = indicesToSymbols(processedSequence.toIntArray())

      // For the current hardcoded captcha (JXAPXW) this will give 'YY' prediction.
      Log.d(TAG, "solve() result: ${result}")

      return@use result
    }
  }

  private fun processCTCDecodedSequence(decodedSequence: IntArray, blankLabel: Int = 0): ArrayList<Int> {
    val result = ArrayList<Int>()
    var prevLabel = blankLabel

    for (label in decodedSequence) {
      if (label != blankLabel && label != prevLabel) {
        result.add(label)
      }
      prevLabel = label
    }

    return result
  }

  private fun indicesToSymbols(decodedIndices: IntArray): String {
    return decodedIndices.map { index -> charset.getOrNull(index - 1) ?: "" }.joinToString("").trim()
  }

  private fun FloatArray.argmax(shape: IntArray): IntArray {
    // chunkSize is 23 here
    val chunkSize = shape.last()

    return this.toList()
      .chunked(chunkSize)
      .map { chunk -> chunk.indexOf(chunk.max()) }
      .toIntArray()
  }

  private fun loadModelFile(): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd("chan4_captcha_solver_model.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength

    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
  }

  private fun convertBitmapToByteBuffer(pixels: IntArray, width: Int, height: Int): ByteBuffer {
    // inputShape1 is [80, 300, 1] here
    val inputShape1 = intArrayOf(height, width, 1)

    val tensorBuffer = TensorBuffer.createFixedSize(inputShape1, DataType.FLOAT32)
    val floatArray = FloatArray(pixels.size)

    // This loop converts an array of ARGB pixels into 0..1 floats by taking R value out of ARGB and dividing it by 255f
    for (index in pixels.indices) {
      val argb = pixels[index]

      val r = ((argb.toUInt() shr 16) and 0xffu).toFloat()
      val convertedPixelValue = r / 255.0f

      floatArray[index] = convertedPixelValue.coerceIn(0f, 1f)
    }

    // inputShape2 is [1, 300, 80, 1] here
    val inputShape2 = intArrayOf(1, width, height, 1)
    val outArray = reshape(floatArray, inputShape2)

    tensorBuffer.loadArray(outArray)
    return tensorBuffer.buffer
  }

  private fun reshape(input: FloatArray, shape: IntArray): FloatArray {
    val output = FloatArray(shape.reduce { acc, i -> acc * i })
    var inputIdx = 0

    for (i in 0 until shape[0]) {
      for (j in 0 until shape[1]) {
        for (k in 0 until shape[2]) {
          for (l in 0 until shape[3]) {
            val outputIdx = i * shape[1] * shape[2] * shape[3] + j * shape[2] * shape[3] + k * shape[3] + l
            output[outputIdx] = input[inputIdx++]
          }
        }
      }
    }

    return output
  }

  companion object {
    private const val TAG = "Solver"
  }

}