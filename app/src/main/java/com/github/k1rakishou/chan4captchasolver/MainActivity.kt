package com.github.k1rakishou.chan4captchasolver

import android.graphics.Bitmap
import android.os.Bundle
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
import com.github.k1rakishou.chan4captchasolver.ui.compose.KurobaComposeSnappingSlider
import com.github.k1rakishou.chan4captchasolver.ui.theme.Chan4CaptchaSolverTheme
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private val testCaptchaJson = "{\"challenge\":\"obM5pKOCtgYS5LdE.c93e8e836e749e8deb886833f36a55395a0f88f3a83325f9a0d67d42150375c1\",\"ttl\":120,\"cd\":13,\"img\":\"iVBORw0KGgoAAAANSUhEUgAAARsAAABQAgMAAABUqzmpAAAACVBMVEUL+Avu7u4AAACitRlfAAAAAXRSTlMAQObYZgAABxFJREFUWIWdmEuL20gQx9vGsyw+mWUUSI7L5hB9CgV2Lj5NhLqJdVuIQ6Y\\/hRfiPewpLFHYzGk8jIy6PuXWo1+SHSesYJi2uuunf1VXP5Xm507\\/78dZ+KIbJT9sp\\/XmGw23F38asAAFcTZvm94OyPoGZ\\/ze9BOgCxzb7qGE\\/Y\\/oMYU2ySH59w4AOuLAPZagz01f6+4s0rjcIf7XoO1Bix4s\\/j3i2DE2s93zv6SHMNAxZ0Pl6wkHHiYE\\/EOnyCZEbBswCJf+QkHFyMpS7Z9TNY44vc4jVqJTrdejw2cmHILLa18LvZnEUVNsWgxU5LgQ2Y1UwyOBvHM2GD2LnOZKag7kTaYH9i6z4a4sRZKOGcovjgK6nfG\\/PzhEQQ\\/ZDZ18hH3gPtC3MPaXPgd\\/SblSKQZfI6eGJQmmyqjnNG7M2UvaKEVKKeBrahPGF7A6qtxQZRDimjyohuIvnEYpChD5uD7wbxFQYrQOWiqTntTB2\\/Yr\\/fsF+4dDdqvUzKf0Rri+gzq0\\/VSpWc7JR7bhsM3Bf4E5VsdhEv1yGKPdSqnNSI+E9CqUHlTpu7BSSkkIxhyLduVuoQI2JHhDKR+7ZqeWQKnmOdkTODQAjZpJLxBnJyLWe3Kh8K2VmvucQkx8m3N4QlSqwkrSCoNSAV\\/F6KMyNebcnHAaym21QhNH5g\\/yOYscpUL0hfMIxSZwrGgyfeSgOTZbIGdLnB2LwMTbkzE3qglOgf5SWH55Rd9hkEl6NOuZiQsGRESdc1qCU6A7nLEiZ2BMxuGcUJW4MIgx6bn1LjR14ujEod5rTzmrWSwqnv64h9Gkhn+Y8xOHOePgMLB95HTeeDHitDh4hfMKw7KkkqJxKPXzQua7fdtFTsitmfz2Iig72XjuUIblUsbBVwA1TyW5Hjaucg5EzhG9OeXQUiqTex4fNl5xkY1nmZ4jWn+Y+rXGlW+gCehwylnknDUtlPzcgzaD53Seo\\/T6yP1uz3BmIXHxofnvAgeb3jFn7U45+PsQOOi580WbcfrU1OL+okPZI45YFNrnMHP6U866yzj3rAf6sxy4jhwvbUalBy6+Mpl0exQOyk5zWuC0IBY4Jnsf0UUJNBsyJw+BdYnjphxtI0f\\/OMdvY3BQnnJc4KwSp0icDhPsw0SP+S7HeE4b+6s3JdxP4tMeLnGqjLOOnA7H1nEfOPRsdX04Ex+3vcihec7h4Eocp3E7dIbjfOaqxHn5JsbHvgAaqrmeNQxwhvN+f8pJcebZp4UhcXibd4ZjhbOYcMxO6mm7w7si91Y4RN5HThs4YXomTpFzRA\\/v\\/ViD11Pzahs576ecFXFqeXuV\\/ILO8l4M3Bvh0K6mSJw7GZH0PRmyKywWXuUVbQdHHF37ecOgmEM+Lj4kzm9UqJgj9K51Iz3mnrb\\/ntOn8UXR+jdyHp+4uzLOxxf5\\/IN6epn5EqeKnKfIgRMOfNJhfhbONnC2zLnTu8hxXJobz1mMOIVO86rEx3MsRc64OnGApc2Ic2DO\\/Ec473Dd0OUAwnlOtZ+4Z0wnnBVzLCfqnA3P6wHJIOHQwRA+SqZgij1+n9N5Dp2pKKQr385YVkEZIPSKdhmRA27M4YMmcXhJxKXBL7a00D5UnnOIHMwVErwEOppk+42kh\\/YnfTmERZv0PK0k4lDgnD9TGUfhjDfh0POZxwVC+EzH3+NT5UJW1p52nMhZRw4GTXvOz4ljnhTPGgdK9sjBde112KQiZyGckpejotCBsxwmepDzVY85t1XcSWN3zdYoFx6oQSnHL2pawlDn\\/U6crFLW2VXc2eNp4YrPNPqEA8M859Tgphz9+zJwsO+uZQAWGLSMc03pvs44IKdyHqi1cIyDwBkq2WPosq\\/UVdmbdL4o46mXOGsvhyuNcCzEs63bLYEdLzHDihuateI5pcw58fB+y2OT1qMj8EKJG1E4unBrgUaUC6xHznG2f5lzEhTHJopjzlHzRr2lsnwIXSJnGx05+J2cE37Q+dRQ7MhWunBrjkdfxMMNBm97Ez8plxUmcuJxj8\\/LvJgcQ170+sv983hXgxzbx0\\/yf1bUdv4eILskaISz5yYmadXiUuD487tYxvNOF30taCrqfh28ZXbfIy5Z76S\\/T2hdio883k8aANAZTqjN+MjM\\/WElf\\/Ttsoh63Pi8IxwYulKkpe6oucAuvfZ3UsZ3ItVsTzmGF2tvv\\/E6G0lU2rbTC7eRhvkNTqNGt2v2GXPyu6j3vfaHeXSJNRSkkziZ6UQP72hck98CUE5Lr6FLyLmhi7ADc+wlTlGOOYYvG\\/Qb8bpYs4twuKynoXXBTa+5aG8i\\/YuHMOGEMfcNDo71woa7kPigKW1d+dTPnO13OB3lA3E+55zaydTCLnEuOHORQ0nV8WJWjHx1NS9+LIWHpmzm8lYjTkjyesQxuJVlGxsHifN3XKlZ8x+rNisRTB9dygAAAABJRU5ErkJggg==\",\"img_width\":283,\"img_height\":80,\"bg\":\"iVBORw0KGgoAAAANSUhEUgAAAUwAAABQAQMAAABIw95IAAAABlBMVEUAAADu7u6BVFV4AAAFmUlEQVRIiYWXz27cVBTGj3Gou0B12BUp1NM1EuquQURxERIseYRGYsG2VTeVKLHLSASJxbwAUvoEfQGgdhpBlmEHK+ZGg9TleFSkcbDnHr5zru2xO5G4o5nMXP383e+c+++E+MpWJxtdBW1iZ3iXu\\/H\\/oSu856L6ZEMW6PIqA9ZeZWDG2g2tTX8bqqV8O+8MtC27wmsl38b6u9xUK3uoM3vWqfZ8rPRt1eI6A\\/MWt\\/GbkbokD5M11c+kGaFxBgv70tuiFW8+wrxYq9q4RaXzdCOiRkCTOVCdbaCD5tA6ks+Naat6JloUQb\\/RpvLkYtmLYtUYOEz6nNG0LbjCZ9QOVj8gTbkNuUv9uYw55Wopn\\/OMe6qCWS9qu+BbxywVmg\\/DivBKnVtk0LzXxBTJSKdDNMOrQTHb9FC\\/4Olwzt28zbqJnaZtDmyg2bGhoXCiBmSupktFJZrjNRo7lMg7dyEVj9YG0IIObWYCaNL4TP0BOumhTpx8zjV7NQ1R9VoN0MCh02KIVpoB3RljZE7Ra3wCtDod5wN0wenHbFT1jLPVWRlZ2kpEtXrpC2rjXNAF\\/2FEdSFpX0p27hNQivNApMLcn7ItaQ60UtVYrUp6\\/BVTVEM1VTSGquGSGgOX7WzxK2vRswX0GisaJDklCy5HDXrRoBM+qnfQ8xfQSNESiaAAAxYN+hToa\\/wMbWD3SVdAHjrVr4BS0qG1B\\/Rbrsy8CDhxaOFQJBBoIOEIOiu8LLWUFAfn+TVmQX1BJfcrQXc79MJ4QVq8NS2xFCQgWV5AU3IzmVN81BowRDfoIJqsOKWtj4yi9TahRRlSCFTmyt4hLg+IrtO9ZCoCoxFtvRAUYHEDA79WdAlVbMNSngeqY6HdiOBV0fCpLP\\/UqYoBhx506FaIxfKEbgG94MVrPBRjIzXo7bRV\\/Y5oD8m3tEO3vCJecVUVgtZJg34A9BMN9jl5NWVAbwJ9FrsE0N21apzqhKD7M0HZoSOK0WkfUd+rootxxvmHgsaKRgXtHAL5YoiOgXq\\/QfXwHaDQAhobehuISVKKeuiEyPpY1Lnd8+p7AigqO0WS1UeZqJRjJ8fKKeUmywXdBlp\\/CRQz8UsPNWLIONSdvmbbA+oD9Tib9NFkjWpYsZ5tqaA4krIrVZMG\\/VtorNmUDoJMvVpF07xBoeLQRGcUGykdHX7uwrK7tG\\/9NP8aDk1VJkTzyvpTQXdCLoBSskxa1b3HYXpSFnfZLPgF3Tv\\/\\/dBDPrETPUZegNrDNln7Jkx\\/rQrZ72Y\\/yNObtkEvmFQVk92GlYd5yYrSXpzT+9iGii4YUwe07KNlkRRII9Mu0EjRkqJLQXNBNa\\/1Y0GPS9kQopp06AOH7gmatcnK4\\/FKthlQyw4tcFHdv\\/yXAuub5HHPAI7Velu8+rKBcTstC6QyuXw39Qu6JV7L6Rq1d\\/aAjhtUbgQcTT\\/Vfo4lIKio2uuC2vBh3kdZUeaX9IC+aVHmOJVzHadlkeEqe4bTVdBMUCvrRwwYReXwG+MUBnp5qqrUqZY7oaLNUfzPLuHYxAxWxqgBcqhcI9XPMVCsPIcWI9KCLDUk15m53qKy2FYsqqZB6xYlh1JzU+HE1j+IaQcPO9TvUEyu+Z60CEJErowirj894gYNFc3pTjqCyp93m3sRJn\\/Us92e4BmHxssxZOqgSBdTR+mlbEI+5K5pXm8zh7VEUB8NqrjS7\\/9S9BVyWMaidywXUqdqvQ0UbVwnE1cSoRZyBS9Om5MWQ2lkO9R2NSNmqb7PynWV2zHPshZ92hvJ8EoS0i8RA56\\/ugpdcIZCaNWWiOIGARwDPeuhUn6e\\/cCT+eDJuZiTpa0eL1x3LSFMh7X7sim2xMB83akrZNAEG3foTIVxb86G\\/+VE4lar9dVQFffmiXhdz1dnY\\/5GsR806Sn6wl2DbvEfaywzg2+EPQgAAAAASUVORK5CYII=\",\"bg_width\":332}"
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
    val captchaInfo = remember { Helpers.getCaptchaInfo(moshi, testCaptchaJson) }

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

    Spacer(modifier = Modifier.height(8.dp))

    if (bitmapPainter != null) {
      Image(
        modifier = Modifier.wrapContentSize(),
        painter = bitmapPainter,
        contentScale = Scale(3f),
        contentDescription = null
      )
    }
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
      val bestOffset = resultImageData.bestOffset

      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      bitmap.setPixels(resultImageData.bestImagePixels, 0, width, 0, 0, width, height)

      val adjustedScrollValue = if (scrollValue == null && bestOffset != null) {
        resultImageData.bestOffset.toFloat() / captchaInfo.widthDiff().toFloat()
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

    val scale = 4f

    val contentScale = remember(key1 = scale) { Scale(scale) }
    var scrollValue by captchaInfo.sliderValue

    if (captchaInfo.bgBitmapPainter != null) {
      val bgBitmapPainter = captchaInfo.bgBitmapPainter

      val offset = remember(key1 = scrollValue, key2 = scale) {
        val xOffset = scrollValue * captchaInfo.widthDiff() * scale * -1
        IntOffset(x = xOffset.toInt(), y = 0)
      }

      Image(
        modifier = Modifier
          .wrapContentSize()
          .offset { offset },
        painter = bgBitmapPainter,
        contentScale = contentScale,
        contentDescription = null,
      )
    }


    Image(
      modifier = Modifier
        .wrapContentSize(),
      painter = fgBitmapPainter,
      contentScale = contentScale,
      contentDescription = null
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
  }

}
