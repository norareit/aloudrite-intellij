package com.ritenoar.aloudrite

private const val DEFAULT_PAUSE_SECONDS = 60L
private const val LONGEST_PAUSE_SECONDS = 300L
private val RATE_LIMIT_SECONDS_UNTIL_RESET = Regex(";\\s*t=(\\d+)")

// How long a download waits after an answer that the server limits the requests, from the two headers of the answer.
// Retry-After comes first, and only when it is a number of seconds. The `t` of the ratelimit header comes second, the
// seconds until the window of the limit starts again. Neither gives a minute. A window lasts up to 5 minutes, so the
// pause never is longer, whatever the server says
fun rateLimitPause(retryAfter: String?, rateLimit: String?): Long {
    val seconds = retryAfter?.trim()?.takeIf { it.all(Char::isDigit) }?.toLongOrNull()
        ?: rateLimit?.let { RATE_LIMIT_SECONDS_UNTIL_RESET.find(it)?.groupValues?.get(1)?.toLongOrNull() }
        ?: DEFAULT_PAUSE_SECONDS
    return minOf(seconds, LONGEST_PAUSE_SECONDS)
}

const val RATE_LIMITED_TEXT = "Hugging Face limits the downloads for the moment"

fun rateLimitCountdownText(secondsLeft: Long): String = "Continuing in ${secondsLeft / 60}:${"%02d".format(secondsLeft % 60)}"
