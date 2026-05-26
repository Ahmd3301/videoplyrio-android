package com.videoplyrio.app

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val BLOCKED_EXTENSIONS = setOf(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
        ".css", ".woff", ".woff2", ".ttf", ".eot", ".ico", ".pdf"
    )

    private val BLOCKED_DOMAINS = setOf(
        "google-analytics", "doubleclick", "googlesyndication",
        "facebook.net", "connect.facebook", "twitter.com/i/jot",
        "analytics", "clickmagick", "popunder", "popads",
        "trafficjunky", "exoclick", "adnxs", "onclick",
        "mgid.com", "taboola", "outbrain"
    )

    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()
        return BLOCKED_EXTENSIONS.any { lower.split("?")[0].endsWith(it) } ||
               BLOCKED_DOMAINS.any { lower.contains(it) }
    }

    fun emptyResponse() = WebResourceResponse(
        "text/plain", "UTF-8",
        ByteArrayInputStream(ByteArray(0))
    )
}
