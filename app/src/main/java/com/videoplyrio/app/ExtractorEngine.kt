package com.videoplyrio.app

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslError
import android.webkit.WebSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class ExtractorEngine(
    private val context: Context,
    private val onStreamFound: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isExtracting = false
    private var extractionAttemptCount = 0
    private var lastAttemptedUrl: String? = null
    private val networkExecutor = Executors.newSingleThreadExecutor()

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

        private val INTERCEPTOR_JS = """
            (function() {
                if (window.__plyr_injected__) return;
                window.__plyr_injected__ = true;

                var origOpen = XMLHttpRequest.prototype.open;
                XMLHttpRequest.prototype.open = function(method, url) {
                    if (url && typeof url === 'string' && url.indexOf('.m3u8') !== -1
                        && url.indexOf('seg') === -1 && url.indexOf('chunk') === -1) {
                        window.ExtractorBridge.onStreamFound(url);
                    }
                    return origOpen.apply(this, arguments);
                };

                var origFetch = window.fetch;
                window.fetch = function(resource, init) {
                    var url = typeof resource === 'string' ? resource : resource.url;
                    if (url && url.indexOf('.m3u8') !== -1
                        && url.indexOf('seg') === -1 && url.indexOf('chunk') === -1) {
                        window.ExtractorBridge.onStreamFound(url);
                    }
                    return origFetch.apply(this, arguments);
                };

                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.tagName === 'VIDEO' && node.src && node.src.indexOf('.m3u8') !== -1) {
                                window.ExtractorBridge.onStreamFound(node.src);
                            }
                            if (node.tagName === 'SOURCE' && node.src && node.src.indexOf('.m3u8') !== -1) {
                                window.ExtractorBridge.onStreamFound(node.src);
                            }
                        });
                    });
                });
                observer.observe(document.documentElement, { childList: true, subtree: true });

                var vid = document.querySelector('video[src*=".m3u8"]');
                if (vid) { window.ExtractorBridge.onStreamFound(vid.src); return; }
                var src = document.querySelector('source[src*=".m3u8"]');
                if (src) { window.ExtractorBridge.onStreamFound(src.src); return; }

                var scripts = document.querySelectorAll('script:not([src])');
                for (var i = 0; i < scripts.length; i++) {
                    var match = scripts[i].textContent.match(/["'](https?:\/\/[^"']+\.m3u8[^"']*?)["']/);
                    if (match) { window.ExtractorBridge.onStreamFound(match[1]); return; }
                }

                var btns = document.querySelectorAll('button[data-url*=".m3u8"], button.hd_btn');
                for (var j = 0; j < btns.length; j++) {
                    var u = btns[j].getAttribute('data-url') || btns[j].getAttribute('data-src');
                    if (u && u.indexOf('.m3u8') !== -1) { window.ExtractorBridge.onStreamFound(u); return; }
                }
            })();
        """.trimIndent()
    }

    private fun getTimeout(url: String): Long {
        if (url == lastAttemptedUrl) extractionAttemptCount++
        else { extractionAttemptCount = 1; lastAttemptedUrl = url }
        return when (extractionAttemptCount) { 1 -> 5000L; 2 -> 10000L; else -> 15000L }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun extract(url: String) {
        if (isExtracting) stop()
        isExtracting = true

        val timeout = getTimeout(url)

        networkExecutor.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    connectTimeout = 4000; readTimeout = 4000
                    setRequestProperty("User-Agent", DESKTOP_UA)
                }
                val targetUrl = if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val iframeRegex = Regex(
                        "player_iframe.*?location.*?['\"]([^'\"]+)['\"]",
                        RegexOption.IGNORE_CASE
                    )
                    iframeRegex.find(html)?.groupValues?.get(1) ?: url
                } else url

                Handler(Looper.getMainLooper()).post {
                    if (!isExtracting) return@post
                    setupWebView(targetUrl, url)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    if (!isExtracting) return@post
                    setupWebView(url, url)
                }
            }
        }

        handler.postDelayed({ if (isExtracting) onError("انتهت مهلة استخراج الرابط") }, timeout)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(targetUrl: String, referer: String) {
        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = DESKTOP_UA
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                loadsImagesAutomatically = false
                blockNetworkImage = true
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onStreamFound(url: String) {
                    if (isExtracting) {
                        Handler(Looper.getMainLooper()).post {
                            stop()
                            this@ExtractorEngine.onStreamFound(url)
                        }
                    }
                }
            }, "ExtractorBridge")

            webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val urlStr = request?.url?.toString() ?: ""
                    if (urlStr.contains(".m3u8", ignoreCase = true) && isExtracting) {
                        if (!urlStr.contains("seg") && !urlStr.contains("chunk")) {
                            Handler(Looper.getMainLooper()).post { stop(); this@ExtractorEngine.onStreamFound(urlStr) }
                        }
                    }
                    if (AdBlocker.shouldBlock(urlStr)) return AdBlocker.emptyResponse()
                    return null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(INTERCEPTOR_JS, null)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 30) {
                        view?.evaluateJavascript(INTERCEPTOR_JS, null)
                    }
                }
            }
        }

        val headers = hashMapOf("Referer" to deriveSiteReferer(referer))
        webView?.loadUrl(targetUrl, headers)
    }

    private fun deriveSiteReferer(url: String): String {
        return try {
            val uri = Uri.parse(url)
            "${uri.scheme}://${uri.host}/"
        } catch (e: Exception) { url }
    }

    fun stop() {
        isExtracting = false
        handler.removeCallbacksAndMessages(null)
        webView?.apply { stopLoading(); destroy() }
        webView = null
    }
}
