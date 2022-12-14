package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfo
import com.github.k1rakishou.chan4captchasolver.ui.compose.KurobaComposeSnappingSlider
import com.github.k1rakishou.chan4captchasolver.ui.theme.Chan4CaptchaSolverTheme
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private val moshi by Dependencies.moshi
  private val solver by lazy { Solver(this.applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      Chan4CaptchaSolverTheme {
        // A surface container using the 'background' color from the theme
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          Column(
            modifier = Modifier
              .wrapContentHeight()
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          ) {
            BuildCaptchaWindow()
          }
        }
      }
    }
  }

  @Composable
  private fun BuildCaptchaWindow() {
    val captchaInfo = remember { Helpers.getCaptchaInfo(moshi, loadCaptcha()) }

    BuildCaptchaWindowImageOrText(captchaInfo)

    Spacer(modifier = Modifier.height(8.dp))

    BuildCaptchaWindowSliderOrInput(captchaInfo)

    Spacer(modifier = Modifier.height(8.dp))
  }

  @Composable
  private fun BuildCaptchaWindowSliderOrInput(
    captchaInfo: CaptchaInfo?,
  ) {
    if (captchaInfo == null || captchaInfo.isNoopChallenge()) {
      return
    }

    val coroutineScope = rememberCoroutineScope()

    var currentInputValue by captchaInfo.currentInputValue
    val scrollValueState = captchaInfo.sliderValue
    var scrollValue by scrollValueState

    var bitmapPainterMut by remember { mutableStateOf<BitmapPainter?>(null) }
    val bitmapPainter = bitmapPainterMut

    var solving by remember { mutableStateOf(false) }

    suspend fun solveCaptchaFunc(scroll: Float?) {
      solveCaptcha(
        captchaInfo = captchaInfo,
        scrollValue = scroll,
        onStarted = { solving = true },
        onFinished = { newSliderValue, results, resultBitmapPainter ->
          if (newSliderValue != null) {
            scrollValue = newSliderValue
          }

          if (results.isNotEmpty()) {
            currentInputValue = results.first().sequence
          }

          bitmapPainterMut = resultBitmapPainter
          solving = false
        }
      )
    }

    // Auto solve upon entering the composition
    LaunchedEffect(
      key1 = Unit,
      block = { solveCaptchaFunc(scroll = null) }
    )

    TextField(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      value = currentInputValue,
      onValueChange = { newValue -> currentInputValue = newValue.uppercase(Locale.ENGLISH) },
      keyboardOptions = KeyboardOptions(
        autoCorrect = false,
        keyboardType = KeyboardType.Password
      ),
      maxLines = 1,
      singleLine = true,
      enabled = !solving
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = "Offset=${(scrollValue * SLIDE_STEPS).toInt()}")

    Spacer(modifier = Modifier.height(8.dp))

    if (captchaInfo.needSlider()) {
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        KurobaComposeSnappingSlider(
          slideOffsetState = scrollValueState,
          slideSteps = SLIDE_STEPS,
          modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
          onValueChange = { newValue -> scrollValueState.value = newValue },
          enabled = !solving
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row {
      Button(
        onClick = { coroutineScope.launch { solveCaptchaFunc(scroll = scrollValue) } },
        enabled = !solving,
        content = {
          if (solving) {
            Text(text = "Solving...")
          } else {
            Text(text = "Solve")
          }
        }
      )

      Spacer(modifier = Modifier.width(32.dp))

      RunTestsButton()
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (bitmapPainter != null) {
      Image(
        modifier = Modifier.wrapContentSize(),
        painter = bitmapPainter,
        contentScale = Scale(2f),
        contentDescription = null
      )
    }
  }

  @Composable
  private fun RunTestsButton() {
    val coroutineScope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var currentTestIndex by remember { mutableStateOf(0) }
    var totalTestsCount by remember { mutableStateOf(0) }

    Button(
      onClick = {
        job?.cancel()
        job = coroutineScope.launch(Dispatchers.IO) {
          coroutineContext[Job.Key]!!.invokeOnCompletion {
            job = null
          }

          // Some of the captchas in the captchas_for_tests.txt file have incorrect answers but
          // those answers are the same in the browser script version
          // (see https://github.com/AUTOMATIC1111/4chan-captcha-solver),
          // meaning those are the bugs in the model itself which I can't fix (I am not a ML expert).
          // They are still considered "correct" because the tests are only used to prove that the
          // solutions received in this app are the same as the solutions from the script.
          val testCaptchas = loadTestCaptcha()

          currentTestIndex = 0
          totalTestsCount = testCaptchas.size

          var correct = 0
          var incorrect = 0

          testCaptchas.forEachIndexed { index, pair ->
            currentTestIndex = index

            val captchaJson = pair.first
            val captchaAnswer = pair.second
            val captchaInfo = Helpers.getCaptchaInfo(moshi, captchaJson)!!

            val resultImageData = Helpers.combineBgWithFgWithBestDisorder(
              captchaInfo = captchaInfo,
              customOffset = null
            )

            val height = resultImageData.height
            val results = solver.solve(height, resultImageData.bestImagePixels)

            if (results.first().sequence.equals(other = captchaAnswer, ignoreCase = true)) {
              correct++
            } else {
              incorrect++
            }
          }

          currentTestIndex = totalTestsCount

          withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Tests finished. Solved: ${correct}, failed: ${incorrect}", Toast.LENGTH_LONG).show()
          }
        }
      },
      content = {
        if (job == null) {
          Text(text = "Run tests")
        } else {
          Text(text = "Cancel (${currentTestIndex}/${totalTestsCount})")
        }
      }
    )
  }

  private suspend fun solveCaptcha(
    captchaInfo: CaptchaInfo,
    scrollValue: Float?,
    onStarted: () -> Unit,
    onFinished: (newSliderValue: Float?, results: List<Solver.RecognizedSequence>, resultBitmapPainter: BitmapPainter) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      withContext(Dispatchers.Main) {
        onStarted()
      }

      val offset = if (scrollValue == null) {
        null
      } else {
        scrollValue * captchaInfo.widthDiff()
      }

      val resultImageData = Helpers.combineBgWithFgWithBestDisorder(
        captchaInfo = captchaInfo,
        customOffset = offset
      )

      val width = resultImageData.width
      val height = resultImageData.height
      val adjustedScroll = resultImageData.adjustedScroll

      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      bitmap.setPixels(resultImageData.bestImagePixels, 0, width, 0, 0, width, height)

      val adjustedScrollValue = if (scrollValue == null && adjustedScroll != null) {
        adjustedScroll
      } else {
        null
      }

      val bitmapPainter = BitmapPainter(bitmap.asImageBitmap())
      val results = solver.solve(height, resultImageData.bestImagePixels)

      withContext(Dispatchers.Main) {
        onFinished(adjustedScrollValue, results, bitmapPainter)
      }
    }
  }

  @Composable
  private fun BuildCaptchaWindowImageOrText(captchaInfo: CaptchaInfo?) {
    BoxWithConstraints(
      modifier = Modifier.wrapContentSize()
    ) {
      val size = with(LocalDensity.current) {
        remember(key1 = maxWidth, key2 = maxHeight) {
          IntSize(maxWidth.toPx().toInt(), maxHeight.toPx().toInt())
        }
      }

      if (size != IntSize.Zero) {
        if (captchaInfo != null) {
          if (captchaInfo.isNoopChallenge()) {
            error("Not supported here")
          } else {
            BuildCaptchaImageNormal(captchaInfo, size)
          }
        }
      }
    }
  }

  @Composable
  private fun BuildCaptchaImageNormal(
    captchaInfo: CaptchaInfo,
    size: IntSize
  ) {
    val density = LocalDensity.current

    val width = captchaInfo.fgBitmapPainter!!.intrinsicSize.width.toInt()
    val height = captchaInfo.fgBitmapPainter!!.intrinsicSize.height.toInt()
    val th = 80
    val pw = 16
    val canvasScale = (th / height)
    val canvasHeight = th
    val canvasWidth = width * canvasScale + pw * 2

    val scale = Math.min(size.width.toFloat() / width, size.height.toFloat() / height)
    val canvasWidthDp = with(density) { (canvasWidth * scale).toDp() }
    val canvasHeightDp = with(density) { (canvasHeight * scale).toDp() }

    val scrollValue by captchaInfo.sliderValue

    Canvas(
      modifier = Modifier
        .size(canvasWidthDp, canvasHeightDp)
        .clipToBounds(),
      onDraw = {
        val canvas = drawContext.canvas.nativeCanvas

        canvas.withScale(x = scale, y = scale) {
          drawRect(Color(0xFFEEEEEE.toInt()))

          if (captchaInfo.bgBitmap != null) {
            canvas.withTranslation(x = (scrollValue * captchaInfo.widthDiff() * -1)) {
              canvas.drawBitmap(captchaInfo.bgBitmap, 0f, 0f, null)
            }
          }

          canvas.drawBitmap(captchaInfo.fgBitmap!!, 0f, 0f, null)
        }
      }
    )
  }

  private fun loadCaptcha(): String {
    val captchasRaw = assets.open("captchas.txt").use { stream ->
      String(stream.readBytes())
    }

    return captchasRaw.split('\n').first()
  }

  private fun loadTestCaptcha(): List<Pair<String, String>> {
    val captchasRaw = assets.open("captchas_for_tests.txt").use { stream ->
      String(stream.readBytes())
    }

    val linePairs = captchasRaw.split('\n').filter { it.isNotBlank() }.chunked(2)
    val result = mutableListOf<Pair<String, String>>()

    linePairs.forEach { linePair ->
      val captcha = linePair[0].trim()
      val answer = linePair[1].trim()

      result += Pair(captcha, answer)
    }

    return result
  }

  class Scale(
    private val scale: Float
  ) : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
      return ScaleFactor(scale, scale)
    }
  }

  companion object {
    private const val SLIDE_STEPS = 50
  }

}
