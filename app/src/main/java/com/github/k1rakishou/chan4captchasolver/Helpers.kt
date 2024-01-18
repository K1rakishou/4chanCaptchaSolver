package com.github.k1rakishou.chan4captchasolver

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun OkHttpClient.suspendCall(request: Request): Response {
  return suspendCancellableCoroutine { continuation ->
    val call = newCall(request)

    continuation.invokeOnCancellation { throwable ->
      if (throwable != null) {
        try {
          if (!call.isCanceled()) {
            call.cancel()
          }
        } catch (ignored: Throwable) {

        }
      }
    }

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        if (continuation.isActive) {
          continuation.resumeWithException(e)
        }
      }

      override fun onResponse(call: Call, response: Response) {
        if (continuation.isActive) {
          continuation.resume(response)
        }
      }
    })
  }
}