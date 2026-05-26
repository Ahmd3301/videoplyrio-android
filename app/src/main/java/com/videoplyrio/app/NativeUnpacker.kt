package com.videoplyrio.app

import java.net.URL
import java.net.HttpURLConnection

object NativeUnpacker {

    private val PACKER_PATTERN = Regex(
        "eval\\(function\\(p,a,c,k,e,[dr]\\)\\{.*?\\}\\('(.*?)',(\\d+),(\\d+),'(.*?)'\\.split\\('\\|'\\)\\)\\)",
        RegexOption.DOT_MATCHES_ALL
    )
    private val STREAM_PATTERN = Regex("file:\"([^\"]+)\"")
    private val SRC_PATTERN = Regex("src\\s*:\\s*[\"']([^\"']+\\.m3u8[^\"']*)[\"']")

    fun tryExtract(html: String): String? {
        val match = PACKER_PATTERN.find(html) ?: return null
        val unpacked = unpack(
            p = match.groupValues[1],
            a = match.groupValues[2].toInt(),
            c = match.groupValues[3].toInt(),
            k = match.groupValues[4].split("|")
        )
        return STREAM_PATTERN.find(unpacked)?.groupValues?.get(1)
            ?: SRC_PATTERN.find(unpacked)?.groupValues?.get(1)
    }

    fun tryExtractFromUrl(targetUrl: String, userAgent: String): String? {
        return try {
            val conn = URL(targetUrl).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", userAgent)
            }
            if (conn.responseCode != 200) return null
            val sb = StringBuilder()
            val buffer = CharArray(4096)
            val reader = conn.inputStream.bufferedReader()
            var bytesRead: Int
            while (reader.read(buffer).also { bytesRead = it } != -1) {
                sb.append(buffer, 0, bytesRead)
                val result = tryExtract(sb.toString())
                if (result != null) return result
            }
            null
        } catch (e: Exception) { null }
    }

    private fun unpack(p: String, a: Int, c: Int, k: List<String>): String {
        var result = p
        for (i in c - 1 downTo 0) {
            if (i < k.size && k[i].isNotEmpty()) {
                val word36 = Integer.toString(i, 36)
                val safe = java.util.regex.Matcher.quoteReplacement(k[i])
                result = result.replace(Regex("\\b$word36\\b"), safe)
            }
        }
        return result
    }
}
