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
  private val testCaptchaJson = "{\"challenge\":\"XiFO8V3FJyFk9hCf.d8ac5d4d06af4834f2e0a728de0864ff0fcb0b4306cccaaddf48f8a3ddd8af30\",\"ttl\":120,\"cd\":4,\"img\":\"iVBORw0KGgoAAAANSUhEUgAAAR8AAABQAgMAAABdQJnTAAAACVBMVEUL9wvu7u4AAAB7lQvzAAAAAXRSTlMAQObYZgAABy1JREFUWIWFmF+L3DYQwLXHXWnv6QrnQPPUFA4u\\/hROIYX6abNYItZTAlkI\\/hQJxKXpUyino9zTbYgXez5l548kS969iyG5tTz6aTQzGo2k9GOPhaHNGgxAf1SyUY+C7gCmJQjuw8s2Nk\\/fAZnRRJB1AQRQhO4z8jsaOez43EP5P3z\\/Cn56BhLJJehqqVM+N4sQ14A0mvDFHYLssADVOcjsEaT1OcBS2IO2uuO\\/EL\\/xTHQJC2vfMkhvqLE7AtK6lfFd1A1\\/Nall41QzER2ig0DTLLWLPXC4DXPGbKopyOhELQLFcOiCG7Y8XAfkogy0ahPQrJbOjb0RCznWsSIu9CYDNcp2+VS9LXNQOYZmiq8zTeYxfkXIh7V6ercAzWPMSkJfnYWXtVpp0oViJczFukqd7wm0mF4OatDP2Ns\\/leIPTTnNzjBaqZOJ7Fg+BqqheKfim1IKR54s0s0wCysF9jsg9CvOh3qHLjjNLYHi1AyD9vjrOmOYDFTClubjjbQWEOWjpMM9tpZ5WtFrLSoHkKXVg3JspJZBP1NzmsUMtZYF9Z0flMg0IhBpzq\\/kHqUu5MucvUhjde6shG0VOuY2ItD7AGr1DAqmfutBP\\/o01HgrWL+sZtCo3ygVrE2\\/qiJaEp+uENCZbjtdDxxoXoNiCVofA4Wn20nrStv\\/aKX6QCOQE1AbQTSgV9iD6gSEAwtoTUt+G8acNeqOgMjsK3VWJ2bt0BYquPWFHoNoR6Au10hFQQFJQsWXHwbpQN9PHJuNHHNGKaQjoTa3kfKCM4gdgmbFNtrPWMDp2knErlrtQdHY2wQkfZU6VScyezEr5UoWuGWjrIPyAnrlQROBOB7FFwnIgJgVYMdxpnBRftQvqyAqINsnGnnQsNBoCqDhdQDB3SAgE0HjbKPJg0YPuvCgd+IfBHGcKdoQ9jv+WdgIgm8HIMqtKaiJINEIRlNOg4AwMDtO6jUaMHotgMYZ5Lx2J\\/2sUQm0Lr1GTsMBaBI5FOw5NPFfAjIJCPftbwIypOmUge7ARdB4BPQbbk0BBLcwCUgHUIdBxhzaYyMId5ME9CuBqJbxoKsNgkb22tkhSIMJIFwYJLZSAqo\\/BFd5la8M3MB48RBoa8PUnmCfDxG0gSMgZ8fTJSi4HzODBxUpqPGTYNAbATW93oyragY5D2p5C4waedApg0oYqqVG2HszqosIopVdIuj19F6XtBUcAf0Dh6Bn6Jp+5O85yA51X4Z1sQBBCnpy70HNXxg1Kw+qeedjEJyTfAT98hCo+MLfn2sqmjg8OEUyCOCTslyXgU7i6OsHxTbIQV7gqoE9gS4FZCJISnD09KwRdQ6g3REQa4RzO3OzRn8rqoDhExSJ1zIQL5EcJMmykqmVOwbdKM6ifX075KDqIdD1Jb0oGoozcQkugGo6Eiw1ehBUggedho1toDr6BncFK7EQcrYkjMp3j6AxAf1OoJXflTEIGQRspGIBWgU9dtRMARJSKJ8FCKTWAqr75pJAE59UpodAUwQNEbQT0GkAGUwyl2rL1kaLPQDSfjqT86ARteSc4CvXbsKtDkG0BtkEHtQw6PQQ5AVgKj3IJw5KdB6kMVbR9iy3uslBQNWbzDGArouGt+9QS9MwVva1bv817Ayrka1xGixDXkfiiLvQJxbon1FNTqCTkMqA60YBUdkioFsGXURfoQj+mGrMKfT95CMvMArtnyJo6AJoojqb532GNuozkK7YZRKFqB2FHMT6iJPr0AQQhnadgDSDnnpQw+m0jqB6asCXFnzKoiObExBX2VL\\/FAKqFiD+n0Ej6t+w+ryPaS58rQfRynP2ADRaRhgpE0XlczTIZKjqigcg\\/RLzvgdhLAz6mmODQRWBbG95Z6\\/lzNZ40I41GtFESS0fS7\\/O+cwmoBe8PB2DQt0qeyWC\\/iR\\/j1V6E8Eg5hoalIOMQa\\/YrKJRF0ANzf0KC25svh8PbyJmbkUufRjEtUWBHq5BDk3Ss8s04ofPa3yxI0veLUDUKmd4AUnPNrNREAw3RGSinuYwNhBvA94H0K45uETK70bIpQLCWezkTiW5QmjRhgJaYpagSjbhz\\/aLHH4EVCRGRIE1gsjJj4H4seA2d6M\\/iUJ2FbbmZVEj6HZ5rZPdjfiHQBCWTphG61h6EOOmGtkpgLYLkHUbf3GxmQ3E5XRzMsi4LxNxOWMevRpzcgPShlWmsTB3k8fNZ\\/5wuHTHQYZidouHaYy0cDNid15v+IyFX1Mm4uEK4AiIzOuoYkKNxEBzSGJBBnLM9hdQ9fQYCA3cyeYrTfZOTEVzMH08oWsy4rQZElC3AE1jFnTGShD40ZNLOg6PZzMou0ZFV+0X0WskLN3B9SIH7NsjU3PcVu4hv1szAVwuFgf8C\\/G4nraHa4c\\/fLZOQTy55SobbMgEESSXbs73Xh+ARlaxXK4MAhUtBU28P5I\\/HC0tezwHTZtja16KNLqbGv8HYtWMWDdN1IgAAAAASUVORK5CYII=\",\"img_width\":287,\"img_height\":80,\"bg\":\"iVBORw0KGgoAAAANSUhEUgAAAVAAAABQAQMAAAB1Ub8uAAAABlBMVEUAAADu7u6BVFV4AAAFwElEQVRIiXWXMY\\/cRBTHx+eIjUS0jpQmRcCHxAdIOqQc8X0DvkCUnIREy0kUgFhufFxDt0JUVKGjoaCMBMp6dUhLF0oqPBuXoNibINlhvfP4vzdjr\\/dyWLqzZ\\/zz3\\/\\/33nhmVlF3bPormtFlh6LCX7W675y6x\\/KLaH2J6kpOi74jd+hlR31B9ewS1F76JC0srXu0BJbAcOzbegCuqGz1qkfn7jbjr8Gw0yTGo5s+Dz0gCSm3rU616fKwDXqz09x63XQd+W44ZZ9K8hmYueuk7ax1Tw5fw+i8D6SRoExvkHYqBrToGxzKhrhUU26uh+YE3dpxBQBRD8T6LKjZRfdeqa8b\\/K3lScWvWrwacnZXtRDPpVf9O99B453A+xdKstrRDhq8lt8eLSkf3nqi2gtQ2aNPiWK+Rugwbe8pl98+TdNpj37rBhUVNS1Lslb5b6dyZBvNxBejXQxlScsp91kXSqql5CYkeTWjH\\/N4JCmieZPqVjm9VmkxpgSda6A28qMTX5CJkEdLFTcrFbFqxeisyCJWjWltOX0wko6gesJoTWkYc6SnDdC1USNGZxo9KeekYQUL1YR+aPdCvjEJmrCm7Mih8kGdsqFMfAGNKDBR+Dwhexw1YUG\\/eVX5oE7Z16EawQ7Q0IZAF7hxqJtwXmapRznXKV5Ke0ATiYhGJrpxtqHHIUG1yJSK+ikjzcCcAHXBJ9+b6CXiRBbgtXwdtUBpKagBitKpmDgD2XVBOSojaHsCEboy9qiUIYZxoIlTxStXeZa9x+VPkLBxYg73Dw2XHmj9O95kkpTRX6H6xbQCyqNnk6kxmWOlVPBuwmiRhjMUHmirs7soWN5m+zWjLZjSTBht7zrVcAqTQG2Z3REPmSpEFczSo3eAhvDK44sNLFlVKiWq1KFGXbu7YjSTkcWoUYlHcR7xGeh9QZM1UKsGqOT2p0MVbegRBmYKNBE0YAOG0fwbRrNxwGiDodJQvHGoToFeEVTdZNVTQdGDCJGkoIrjVlcPBG08qhyacgmASBlwbmZvf0mNDZYFpcf\\/i3Ij2NB1TRsbzJ\\/pdB8xBPSLQ22SXR059MShyJSu6CSYO6\\/oOIBXFWrS7S3l0DZukM85o8YeKKBKUHsNI1rtRfhKv\\/YoogK6EhSDNuhQuqVG11QYW36p96r5mtHPDcaooJXzOvpMhT\\/TAJVrg4DfMRiFSnWqrRohtTwtpHsOzcmpQlDRvw5VW7Th2z4DC0HXPFYU+P0L6MaPga2qd2TVgaC3lJuhKzmlsZJ8aqEWP3r0nvoIaITZdgJVPxE7lD9Em+7naY8ewW6FGwdQNqyqPcq7AKCWG1fh74E6CvipADUK\\/fy6q4qJNb0ZYwCP0SVhiap8BToTNKQJfwWCTvgzuK+slCBkr5Td5lCSqRJ7Lc8a+xMl+5nsxiRRQVpx4pAsmjcyd2unyguXqOKDIfMJnadBJqoN0Kdt2HsFikbaodkHNvxKVEOZ5KkNPMpzfc3okXUor\\/tn3w1QmaYZfSMIqTj3KFw1MFOfPTKVuo5ECbrkmgGdhmd0\\/mIkaAXUYEUsCqorhSkwE\\/QVx\\/VQUR5o+pAeCoopkp4GbrWueOw51SpZO2NKt5+yocqhlRu+eGOl638EbeilQ59hLcAE9rhCEDzNcWqwHFb6BS1aQTeYqmWfxeJYWlugIzwiUxj6jH5JuUN5W9j4\\/QCrtscQel+Tfz8\\/UlKVOnTtd29u5W5uU23HmqIO1bhRC+q2PF6VWBXnt\\/J+AxnKDYcmHeoOy8vUk6Rvx\\/I\\/HSOorSrRH\\/Kfh5X2ezZP5lDt9uvDnSZUH\\/WiM3cCyjdrj3odnwm3b\\/Bx8xrOR7HjtY+vocFG0Ms4Vdc72LqvaLj73D6juqzKf+06LT8\\/3UW5Wmei\\/pdIJ7R9brijqzvVgnzFul8FXZx9OcQPe+Ud9HOeQv70qttfEp1qTC4D59zCvozOtAtwNvgl4VzN8Pcf1oPiA8Ctbq0AAAAASUVORK5CYII=\",\"bg_width\":336}"
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
