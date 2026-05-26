package com.videoplyrio.app

object StreamRouter {

    private val DIRECT_EXTENSIONS = listOf(".m3u8", ".mpd", ".mp4", ".mkv", ".avi",
        ".mov", ".wmv", ".flv", ".webm", ".ts", ".mts", ".m2ts")
    private val HLS_INDICATORS = listOf(".m3u8", "m3u8")
    private val DASH_INDICATORS = listOf(".mpd", "mpd")
    private val DRM_SEPARATOR = "###"

    sealed class RouteResult {
        data class DirectPlay(val url: String, val type: StreamType) : RouteResult()
        data class NeedsExtraction(val url: String, val method: ExtractionMethod) : RouteResult()
    }

    enum class StreamType { HLS, DASH, MP4, UNKNOWN }
    enum class ExtractionMethod { NATIVE_PACKER, WEBVIEW }

    fun route(url: String): RouteResult {
        val cleanUrl = url.split(DRM_SEPARATOR)[0]
        val lower = cleanUrl.split("?")[0].lowercase()

        return when {
            isDirectStream(lower, url) -> RouteResult.DirectPlay(url, detectStreamType(lower))
            isFaselTarget(url) -> RouteResult.NeedsExtraction(url, ExtractionMethod.WEBVIEW)
            else -> RouteResult.NeedsExtraction(url, ExtractionMethod.WEBVIEW)
        }
    }

    private fun isDirectStream(lower: String, original: String): Boolean {
        return DIRECT_EXTENSIONS.any { lower.endsWith(it) || lower.contains("$it/") } ||
               original.contains("##ex")
    }

    private fun detectStreamType(lower: String): StreamType = when {
        HLS_INDICATORS.any { lower.contains(it) } -> StreamType.HLS
        DASH_INDICATORS.any { lower.contains(it) } -> StreamType.DASH
        lower.endsWith(".mp4") -> StreamType.MP4
        else -> StreamType.UNKNOWN
    }

    private fun isFaselTarget(url: String): Boolean {
        if (url.contains("##ex")) return false
        val clean = url.split(DRM_SEPARATOR)[0].split("?")[0].lowercase()
        return clean.contains("fasel") &&
               !clean.contains(".m3u8") &&
               !clean.contains(".mpd") &&
               !clean.contains(".mp4")
    }
}
