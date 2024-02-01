package com.github.k1rakishou.chan4captchasolver.updater

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.k1rakishou.chan4captchasolver.Dependencies
import com.github.k1rakishou.chan4captchasolver.NotificationHelper
import com.github.k1rakishou.chan4captchasolver.suspendCall
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import logcat.logcat
import okhttp3.Request


class UpdateCheckerWorker(
  private val appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {
  private val sharedPreferences by Dependencies.sharedPreferences
  private val okHttpClient by Dependencies.okHttpClient
  private val moshi by Dependencies.moshi

  override suspend fun doWork(): Result {
    Dependencies.init(appContext)

    logcat { "Starting updater" }
    val result = runCatching { doWorkInternal() }
    logcat { "Updater finished. Result is success: ${result.isSuccess}" }

    if (result.isFailure) {
      return Result.failure()
    }

    return Result.success()
  }

  private suspend fun doWorkInternal() {
    val lastCheckedVersion = sharedPreferences.getFloat(lastCheckedVersionKey, 0.0f)
    val currentVersion = Dependencies.appVersionCode

    logcat { "doWorkInternal() lastCheckedVersion (${lastCheckedVersion}), currentVersion (${currentVersion})" }

    if (lastCheckedVersion >= currentVersion) {
      logcat { "doWorkInternal() skipping because lastCheckedVersion (${lastCheckedVersion}) >= currentVersion (${currentVersion})" }
      return
    }

    val request = Request.Builder()
      .url("https://api.github.com/repos/K1rakishou/4chanCaptchaSolver/releases/latest")
      .build()

    val response = okHttpClient.suspendCall(request)
    if (!response.isSuccessful) {
      logcat { "doWorkInternal() response is not successful: ${response.code}" }
      return
    }

    val responseBody = response.body
    if (responseBody == null) {
      logcat { "doWorkInternal() responseBody is null" }
      return
    }

    val githubLatestRelease = moshi.adapter<GithubLatestRelease>(GithubLatestRelease::class.java)
      .fromJson(responseBody.string())

    if (githubLatestRelease == null) {
      logcat { "doWorkInternal() githubLatestRelease is null" }
      return
    }

    val latestVersion = githubLatestRelease.latestVersion()
    if (latestVersion == null || latestVersion <= 0.0f) {
      logcat { "doWorkInternal() bad latestVersion from response: ${latestVersion}" }
      return
    }

    val htmlUrl = githubLatestRelease.htmlUrl

    logcat { "doWorkInternal() latestVersion (${latestVersion}) lastCheckedVersion (${lastCheckedVersion})" }

    if (latestVersion <= lastCheckedVersion) {
      logcat { "doWorkInternal() skipping because latestVersion (${latestVersion}) <= lastCheckedVersion (${lastCheckedVersion})" }
      return
    }

    logcat { "doWorkInternal() displaying notification for new latest version ${latestVersion} with url `${htmlUrl}`" }
    NotificationHelper.showUpdateNotification(appContext, latestVersion, htmlUrl)

    logcat { "doWorkInternal() updating last checked version with ${latestVersion}" }
    sharedPreferences.edit {
      putFloat(lastCheckedVersionKey, latestVersion)
    }.commit()

    logcat { "doWorkInternal() done" }
  }

  @JsonClass(generateAdapter = true)
  data class GithubLatestRelease(
    @Json(name = "tag_name")
    val tagName: String?,
    @Json(name = "html_url")
    val htmlUrl: String?
  ) {
    fun latestVersion(): Float? {
      val localTagName = tagName
      if (localTagName.isNullOrBlank()) {
        return null
      }

      return localTagName.removePrefix("v").toFloatOrNull()
    }
  }

  companion object {
    private const val lastCheckedVersionKey = "last_checked_version"
  }

}