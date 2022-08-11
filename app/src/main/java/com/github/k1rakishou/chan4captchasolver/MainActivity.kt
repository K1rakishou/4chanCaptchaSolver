package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfo
import com.github.k1rakishou.chan4captchasolver.data.CaptchaInfoRaw
import com.github.k1rakishou.chan4captchasolver.ui.compose.KurobaComposeSnappingSlider
import com.github.k1rakishou.chan4captchasolver.ui.theme.Chan4CaptchaSolverTheme
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : ComponentActivity() {
  private val testCaptchaJson = "{\"challenge\":\"wfOZwZWK6U37JUB8.e4e9bab8adc0b3b1ba5152ba56fabc526fbcc2cd1a1133c9193535807cde1fc1\",\"ttl\":120,\"cd\":26,\"img\":\"iVBORw0KGgoAAAANSUhEUgAAAOkAAABQAgMAAACBnLrNAAAACVBMVEUO9Q7u7u4AAAAhH2ZmAAAAAXRSTlMAQObYZgAABgRJREFUWIWdl1+L3DYQwLXmNtB9Okp9NH3KQVNafYpNIJD6ac9Y4tZPLXSh1ae4wLnQvhWiQPZpt8SLNZ+yM6M\\/ln231xCx3MmWfh7NP2kk1Be3tTg7tH2Ks0+wulfuS+Vq\\/+kvYh+0RzT4bLb1y0Bd0seYtf\\/D6VGuTHONZ13+vUeaiZ0GoIx9sEnuTPZNJlLtwkMNGWvO6tt2\\/G\\/yRUIhPW3PswP\\/zdkKyTvIXuQszyTdfrWqhV5NHSMBDq2Qc1b7ftLN2KqGaVihleBobsUq04TZydKIRbXkhG1OAJ3abURBTy5jTXBQs0xsqUd2Uyp9YgOvhXgo1z9tFpbX3QLYZmQFvt6XamQf0Xc0HIBrhrReIWLgCCHKGZsExBH0ZAddXLIQP01YWDxkUcBSNbugcQqDNb2OE6iLQ0M5Y1HAQrleNT27JGf9en5LLA3aOauRxbBoVocisggsXJnkagisi6x5xwJE4\\/oGjphqQgR2xyy7aENdTdzrXC4q2BGLbm1QLiArOw5KEiYcJZBltuZcgndeX+vZI00qS3WLhgAkVpAMJE4YzVc9sYXPQxKeyfWsUn+E5QWFCRA\\/bDGLrGfvZuxr\\/JqjSde0IXjjioz9nlLQ0ktx4LeJXQH8jTCNyNO1Ua\\/YuCHfmP2WppfMWk61wHIc7NHfNLI6OVM5z149D+4VgnpBrs3lErswI7vF1H1L\\/aVL7M+cwRkrA0sBltj9oJDl\\/tKeZV95uQ35ccr26gX1FypnNQwja8iga2EoYXTOoq1n7GLBrBAxCQ1tZ4FtMd0PNFLsh2TcRQoNz7qcRcGBJXs9zhrqXWQs5eMb4\\/X1cveJBYVbBmtGrq\\/Z6xeCeiOL6j4f9a0jawANP7KSlbwohhlbqsTiikZWjmwLHXUuCxhaH\\/DE3pgQt0ZSZ8K6yDboamYXEmBkK7j3HjBmzv6oIlvh2jyLgXR86dktvu48u5uzZQwIgTuIZnaNdpZH5VlHmngWFX+\\/spFdQMZi0LY2slUfWOtZre6EuqFjYMJWgcUpLHdBrq6GMuhr4H4Z5Ko5W0Fk4zZJLIZhYnuV2KGCDyNrTWSHKcvuEi\\/feA\\/FPad9mr3wrPWsgU\\/lhLWBfQbw0XSeLXJ2l7HhaIvsnyMLr0IuFJDLlWdYPmKGKDfmURH3nEsKprOsydhObQKrchbg8o7e9k+yDZQ+4yO7DqffI+x2xlaBXX4Gi6VKYr9DtoWypf41TdB3xC7OsVgi8f5OI18D9FjTjaw8YGgIeWgydhj9q9WMxc+Rg4+853gW\\/s1Y6iZb6QkbNrviyM47oHuF5LOeIqDwLE0LsZHYq8DS+Xvk+nMgtkmsoNSJ7Gu\\/xUq\\/JE0vqw8UTpjsDQTWFy8Hdhyme2Qxei0l8yqxNXzoq7XAssOAk4ChUViGPUuV7kYGtpEUTKp5gdoge8SsquGXAllMZs+y1diLS18Y6xPry+vvVKiRmC1roCJCU8UDdxySSh\\/lkAIGixQ7YRtfPR2x3q87z6otsusATFjvI2QH\\/4YOKhmKbmYx4CQc1qHMlOSJcsoexoebEt5fJ9Ynp4uFr3z3gB3bpnDWoDDdJRblupBQ6tH62bcdVpHO3gIWhZjCgUV93SXLsudZS3VV69DVtqYUDiwqeSrJK3yn2OAS\\/N3l98TuOJdwwidVD3i+dDpnLUUDIw3tzhyz5hhZR0P4q\\/HT6EnEh4zNrkloKl7AGzp5o9ysbZvMzhsIG3lQMVwuyIRTO+PkMt46A1v58t2ovGnKwxkL3TP0DlYSS+WYbTDM\\/lJqfuvmpBpmLJw6VWFK9QoPFuiNzm+eSbGGWPeW2KBPR+weLdxrSn\\/e\\/fjWGtebFNcHgHtHdo43bjx3a9jjYUq\\/sINRcxBuWfEb1TeUO7dU1rbh8kZT5EeKx6ZXGftVXHXUOeSdyeXSPjHq7tmuBi5380bsfe\\/v3UEN9EZn\\/hnZPVxROlY4oEyZsVgCVrFWiQ13Hhlvs3jj0Jj6pf8o\\/aKpncZtkZw4YckhI4vZoY9hQXhCeXs5wpFWnFaT+\\/6UVbVfKTvKO2jn\\/4RVrP8DDHoxSvhcA0sAAAAASUVORK5CYII=\",\"img_width\":233,\"img_height\":80,\"bg\":\"iVBORw0KGgoAAAANSUhEUgAAARoAAABQAQMAAAD8yShHAAAABlBMVEUAAADu7u6BVFV4AAAE5ElEQVRIiX3WT2\\/cRBQA8DdyG0cirMOpOSzrIA4c6TGoS51Tr3wEcuPaiAOBuJlp98CBQz4RrNuVWC4oHwCJdbQSPQBaR0Fdlx3P473549hpxEjZ7No\\/v3nzxzMDeGfRkj9X7kcBd6M648+Ld5F5J9Kkj9btDSoNP3CFU5z30VUn0tJ+btqcakYrHymUVTc5shtG87tzvymMJv9z3wQ0x\\/NbbeugDBfoclrcerT3c0Io7V2U+vR29nNC0+7DRppK2m\\/T9trKVkf1hqJTrN23bgUW9fJsfL2dYP0BrlF2Hkhv0No1wnAE3U3acKSmE6kdW8TEDYG7UI1lixpG\\/nJih8A3phLsHaoJmcReVhxJS+0SqoBu+NbVuQyNZoS1DJHgpjqsvwktLhLq3b2YKm96\\/UQ5meP2QvIAf4c4NIzKIkS6dJOOKuFICqKAaJYqjwz4LpCySNCoXUaC61+bTCVuqiiHeHzoLarVHqFCnLs3wqF5BTuMmsr100btAdYQP3eD7dCkhOEGy5A4TtWBoMYnStq01PuMZoUaHmMZcSQDCWVx8AirQ0JsTpVgtEzV8CMN8GXZWIRwYEMSmi5wqWKLpBoKQmlBo1PQaMFn9LpZdD7HFee0ob7wiCJRnyw40qVHi5D4EoshbAPE3LpC\\/EZI\\/u3QD9hDVEQ1ZTS7qL6lHrKokAG99uiDKma0TGtJ98oEMvwFb6NH1fmaUJlWGU2bckDoV5oN\\/eqeVYtKFuLfwcNYenRBs6yPDvbwDUUyIztTPAqRZoRGhHa3beu0sW+QR9hBKYdyiHodHZrqxKLDgERFyiLDaG0j1RYNIKDnHoHQueI1lJDgSNdtpJ30rUMa7hvtIm05NGtz2klpAo0BzXhfoHY5bZ2IMsHlqy6qGWF+g548BUhwXUCLEotqrPcj1MKin08IneML1UMjmjYVR7Lo8g2hdIpRH8U0zYt9SGvBL\\/nRH4Syhb6F0or60SJeZOAJI5r6DimPZEU1lZC+ELSmyaOtkyNCNKyM1oxiSahEE5cwimd4bWS5JSuOlEMbaZDZSIx2k5cYeZT2UMKRtEV08QAee2T2HarbnHLUcXmPXl2AUYh06FBDaJCkGvIcq9iFh9GxQxI61cU\\/animsYjXmt5hQqcWDcaEtj3Ssc45J5q4mv5eK6ruO0LJU0IfErpSONNxoy3KCPFzI3lpshKSCvJtm3iuI80RpJbwgP6L2qjRmTCP+aWGPFoQokkW6a9Wuq3uPYo6AmHG8BNHiuyayUhiQH\\/RJDIOCdNFX3jU2Eg0Z49G9hsjeelQZDegQmpqWVMz0iPBD599Wu1Kt9g3YZcyDfCKymjgF267b9h13ASEcN8jYRF2kM54l7Io9ghotzBxF9XZP24f82hqa9roRKedSGcfp13Eq1NGI\\/GJybo5fT2125FDw8y25C39kDfIHZb4xGITN26baVQvJ1fmHOmepNU5bKIxGkZNQBO70W8oEv5pN1FjkX4oJF4FNA+JZ\\/jSfqtpK+BI0TUvB+6uP+SAyNoj3VxlvOdHNqfOwQEhmrbHu8n3yIkLi\\/yGPOXljSNxkSGDCqL+OZOq4Lc\\/lNWSeq6Hrty\\/7R2P3ClzgpWaMaKoK326dmqce1Svzedc4\\/XcImrX3Pg+p\\/nuW7FZcl50bzX31XWOqO3Zoz3C0jgU\\/wEE3jJFBLk88wAAAABJRU5ErkJggg==\",\"bg_width\":282}"
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
          ) {
            BuildCaptchaWindow()
          }
        }
      }
    }
  }

  @Composable
  private fun BuildCaptchaWindow() {
    val captchaInfo = remember { getCaptchaInfo() }

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

    var currentInputValue by captchaInfo.currentInputValue
    val scrollValueState = captchaInfo.sliderValue
    val scrollValue by scrollValueState

    var bitmapPainterMut by remember { mutableStateOf<BitmapPainter?>(null) }
    val bitmapPainter = bitmapPainterMut

    TextField(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(horizontal = 16.dp),
      value = currentInputValue,
      onValueChange = { newValue -> currentInputValue = newValue.uppercase(Locale.ENGLISH) },
      keyboardOptions = KeyboardOptions(
        autoCorrect = false,
        keyboardType = KeyboardType.Password
      ),
      maxLines = 1,
      singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = "Offset=${scrollValue * SLIDE_STEPS}")

    Spacer(modifier = Modifier.height(8.dp))

    if (captchaInfo.needSlider()) {
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        KurobaComposeSnappingSlider(
          slideOffsetState = scrollValueState,
          slideSteps = SLIDE_STEPS,
          modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          onValueChange = { newValue -> scrollValueState.value = newValue }
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LaunchedEffect(
      key1 = scrollValue,
      block = {
        delay(500)

        withContext(Dispatchers.IO) {
          val resultImageData = Helpers.combineBgWithFgWithBestDisorder(captchaInfo, (scrollValue * 50f).toInt())

          val width = resultImageData.width
          val height = resultImageData.height

          val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          bitmap.setPixels(resultImageData.bestImagePixels, 0, width, 0, 0, width, height)
          bitmapPainterMut = BitmapPainter(bitmap.asImageBitmap())

          solver.solve(height, resultImageData.bestImagePixels)
        }
      }
    )

    if (bitmapPainter != null) {
      Image(
        modifier = Modifier.wrapContentSize(),
        painter = bitmapPainter,
        contentScale = Scale(3f),
        contentDescription = null
      )
    }
  }

  @Composable
  private fun BuildCaptchaWindowImageOrText(captchaInfo: CaptchaInfo?) {
    var height by remember { mutableStateOf(160.dp) }

    BoxWithConstraints(
      modifier = Modifier
        .wrapContentHeight()
        .height(height)
    ) {
      val size = with(LocalDensity.current) {
        remember(key1 = maxWidth, key2 = maxHeight) {
          IntSize(maxWidth.toPx().toInt(), maxHeight.toPx().toInt())
        }
      }

      if (size != IntSize.Zero) {
        if (captchaInfo != null) {
          if (captchaInfo.isNoopChallenge()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .align(Alignment.Center)
            ) {
              // TODO(KurobaEx):
//              Text(
//                text = stringResource(id = R.string.chan4_captcha_layout_verification_not_required),
//                textAlign = TextAlign.Center,
//                modifier = Modifier
//                  .fillMaxWidth()
//              )
            }
          } else {
            height = 160.dp
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
    val fgBitmapPainter = captchaInfo.fgBitmapPainter!!

    val scale = Math.min(
      size.width.toFloat() / fgBitmapPainter.intrinsicSize.width,
      size.height.toFloat() / fgBitmapPainter.intrinsicSize.height
    )

    val contentScale = Scale(scale)
    var scrollValue by captchaInfo.sliderValue

    if (captchaInfo.bgBitmapPainter != null) {
      val bgBitmapPainter = captchaInfo.bgBitmapPainter
      val offset = remember(key1 = scrollValue) {
        val xOffset = (captchaInfo.bgInitialOffset + MIN_OFFSET + (scrollValue * MAX_OFFSET * -1f)).toInt()
        IntOffset(x = xOffset, y = 0)
      }

      Image(
        modifier = Modifier
          .fillMaxSize()
          .offset { offset },
        painter = bgBitmapPainter,
        contentScale = contentScale,
        contentDescription = null,
      )
    }

    Image(
      modifier = Modifier
        .fillMaxSize(),
      painter = fgBitmapPainter,
      contentScale = contentScale,
      contentDescription = null
    )
  }

  private fun getCaptchaInfo(): CaptchaInfo? {
    val captchaInfoRawAdapter = moshi.adapter(CaptchaInfoRaw::class.java)
    val testCaptchaInfoRaw = captchaInfoRawAdapter.fromJson(testCaptchaJson)!!

    val (bgBitmapPainter, bgPixels) = testCaptchaInfoRaw.bg?.let { bgBase64Img ->
      val bgByteArray = Base64.decode(bgBase64Img, Base64.DEFAULT)
      val bitmap = BitmapFactory.decodeByteArray(bgByteArray, 0, bgByteArray.size)

      val pixels = IntArray(bitmap.width * bitmap.height)
      bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

      return@let BitmapPainter(bitmap.asImageBitmap()) to pixels
    } ?: (null to null)

    val (fgBitmapPainter, imgPixels) = testCaptchaInfoRaw.img?.let { imgBase64Img ->
      val imgByteArray = Base64.decode(imgBase64Img, Base64.DEFAULT)
      val bitmap = BitmapFactory.decodeByteArray(imgByteArray, 0, imgByteArray.size)

      val pixels = IntArray(bitmap.width * bitmap.height)
      bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

      return@let BitmapPainter(bitmap.asImageBitmap()) to pixels
    } ?: (null to null)

    val bgInitialOffset = if (testCaptchaInfoRaw.bgWidth != null && testCaptchaInfoRaw.imgWidth != null) {
      if (testCaptchaInfoRaw.bgWidth > testCaptchaInfoRaw.imgWidth) {
        testCaptchaInfoRaw.bgWidth - testCaptchaInfoRaw.imgWidth
      } else {
        testCaptchaInfoRaw.imgWidth - testCaptchaInfoRaw.bgWidth
      }
    } else {
      0
    }

    return CaptchaInfo(
      boardCode = "vg",
      threadNo = 394579078L,
      bgBitmapPainter = bgBitmapPainter,
      fgBitmapPainter = fgBitmapPainter!!,
      bgPixelsArgb = bgPixels,
      fgPixelsArgb = imgPixels,
      challenge = testCaptchaInfoRaw.challenge!!,
      startedAt = System.currentTimeMillis(),
      ttlSeconds = testCaptchaInfoRaw.ttl!!,
      bgInitialOffset = bgInitialOffset.toFloat(),
      imgWidth = testCaptchaInfoRaw.imgWidth,
      bgWidth = testCaptchaInfoRaw.bgWidth
    )
  }

  class Scale(
    private val scale: Float
  ) : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
      return ScaleFactor(scale, scale)
    }
  }

  companion object {
    private const val MIN_OFFSET = 100f
    private const val MAX_OFFSET = 400f

    private const val SLIDE_STEPS = 50
    private const val PIXELS_PER_STEP = 50
  }

}
