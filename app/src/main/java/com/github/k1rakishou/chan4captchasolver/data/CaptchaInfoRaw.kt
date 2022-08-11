package com.github.k1rakishou.chan4captchasolver.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class CaptchaInfoRaw(
  @Json(name = "error")
  val error: String?,
  @Json(name = "cd")
  val cooldown: Int?,

  // For Slider captcha
  @Json(name = "bg")
  val bg: String?,
  @Json(name = "bg_width")
  val bgWidth: Int?,

  @Json(name = "cd_until")
  val cooldownUntil: Long?,
  @Json(name = "challenge")
  val challenge: String?,
  @Json(name = "img")
  val img: String?,
  @Json(name = "img_width")
  val imgWidth: Int?,
  @Json(name = "img_height")
  val imgHeight: Int?,
  @Json(name = "valid_until")
  val validUntil: Long?,
  @Json(name = "ttl")
  val ttl: Int?
) {
  fun ttlSeconds(): Int {
    return ttl ?: 120
  }

  fun isNoopChallenge(): Boolean {
    return challenge?.equals(NOOP_CHALLENGE, ignoreCase = true) == true
  }

  companion object {
    const val NOOP_CHALLENGE = "noop"
  }
}