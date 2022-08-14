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
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Solver(
  private val context: Context
) {
  private val charset = arrayOf("", "0", "2", "4", "8", "A", "D", "G", "H", "J", "K", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "X", "Y")
  private val modelFile by lazy { loadModelFile() }
  private val threadsCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

  @OptIn(ExperimentalTime::class)
  fun solve(height: Int, pixels: IntArray): List<RecognizedSequence> {
    val gpuDelegate = createGpuDelegate()

    try {
      val (results, duration) = measureTimedValue {
        val recognizedSymbolsList = solveInternal(
          gpuDelegate = gpuDelegate,
          height = height,
          pixels = pixels
        )

        return@measureTimedValue postProcess(recognizedSymbolsList)
      }

      Log.d(TAG, "solve() took ${duration}, results: ${results}")
      return results
    } finally {
      gpuDelegate?.close()
    }
  }

  private fun postProcess(recognizedSymbolsList: List<List<RecognizedSymbol>>): List<RecognizedSequence> {
    data class Possibility(
      val symbol: String,
      val offset: Int,
      val confidence: Float
    )

    data class Sequence(
      val seq: List<Possibility>
    )

    val possibilitySequences = mutableListOf<Sequence>()
    possibilitySequences += Sequence(emptyList())

    recognizedSymbolsList.forEachIndexed { offset, recognizedSymbols ->
      if (recognizedSymbols.isEmpty()) {
        return@forEachIndexed
      }

      if (recognizedSymbols.size == 1 && recognizedSymbols[0].symbol == "") {
        return@forEachIndexed
      }

      val oldPossibilities = possibilitySequences.toMutableList()
      possibilitySequences.clear()

      oldPossibilities.forEach { possibility ->
        recognizedSymbols.forEach { recognizedSymbol ->
          val seq = possibility.seq.toMutableList()
          if (recognizedSymbol.symbol != "") {
            seq += Possibility(recognizedSymbol.symbol, offset, recognizedSymbol.confidence)
          }

          possibilitySequences += Sequence(seq)
        }
      }
    }

    val resultMap = mutableMapOf<String, Float>()

    possibilitySequences.forEach { sequence ->
      var line = ""
      var lastSym: String? = null
      var lastOff = -1
      var count = 0
      var prob = 0f

      sequence.seq.forEach { possibility ->
        val symbol = possibility.symbol
        val offset = possibility.offset
        val confidence = possibility.confidence

        if (symbol == lastSym && lastOff + 2 >= offset) {
          return@forEach
        }

        line += symbol
        lastSym = symbol
        lastOff = offset
        prob += confidence
        count++
      }

      if(count > 0) {
        prob /= count
      }

      if(prob > (resultMap[line] ?: -1f) || !resultMap.containsKey(line)) {
        resultMap[line] = prob
      }
    }

    return resultMap.entries
      .sortedByDescending { (_, confidence) -> confidence }
      .map { (sequence, _) -> sequence }
      .filter { sequence -> sequence.length == 5 || sequence.length == 6 }
      .mapNotNull { sequence ->
        val confidence = resultMap[sequence]
          ?: return@mapNotNull null

        return@mapNotNull RecognizedSequence(
          sequence = sequence,
          confidence = confidence
        )
      }
  }

  private fun solveInternal(
    gpuDelegate: GpuDelegate?,
    height: Int,
    pixels: IntArray
  ): List<List<RecognizedSymbol>> {
    val shape = intArrayOf(1, height, 80, 1)
    val byteBuffer = convertBitmapToByteBuffer(pixels, shape)

    val options = Interpreter.Options().apply {
      if (gpuDelegate != null) {
        Log.d(TAG, "solve() using gpu delegate")
        addDelegate(gpuDelegate)
      } else {
        Log.d(TAG, "solve() using cpu with ${threadsCount} threads")
        numThreads = threadsCount
      }
    }

    return Interpreter(modelFile, options).use { interpreter ->
      val inputIndex = 0
      val outputIndex = 0

      interpreter.resizeInput(inputIndex, shape)
      interpreter.run(byteBuffer, null)

      val tensor = interpreter.getOutputTensor(outputIndex)
      val buffer = tensor.asReadOnlyBuffer()
      val count = tensor.shape().reduce { acc, i -> acc * i }

      return@use (0 until count)
        .map { index -> buffer.getFloat(index * 4) }
        // Process results in chunks of size "charset.size" because that's how they are grouped together
        .chunked(charset.size)
        .mapNotNull { probabilities ->
          val recognizedSymbols = mutableListOf<RecognizedSymbol>()
          val max = probabilities.maxBy { it }

          probabilities.forEachIndexed { charIndex, probability ->
            val prob = probability / max

            if (prob > 0.05) {
              val symbol = charset.getOrNull(charIndex + 1) ?: ""
              recognizedSymbols += RecognizedSymbol(symbol, prob)
            }
          }

          if (recognizedSymbols.isEmpty()) {
            return@mapNotNull null
          }

          return@mapNotNull recognizedSymbols
        }
    }
  }

  data class RecognizedSequence(
    val sequence: String,
    val confidence: Float
  )

  data class RecognizedSymbol(
    val symbol: String,
    val confidence: Float
  )

  private fun createGpuDelegate(): GpuDelegate? {
    val compatList = CompatibilityList()

    compatList.use { list ->
      if (!list.isDelegateSupportedOnThisDevice) {
        return null
      }

      return GpuDelegate(list.bestOptionsForThisDevice)
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

  companion object {
    private const val TAG = "Solver"
  }

}