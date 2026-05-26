package com.videoplyrio.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.videoplyrio.app.ui.PlayerScreen
import com.videoplyrio.app.ui.theme.PlyrTheme

class PlayerActivity : ComponentActivity() {

    private lateinit var viewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUi()

        viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        handleIntent(intent)

        setContent {
            PlyrTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isFullscreen by viewModel.isFullscreen.collectAsState()
                    if (isFullscreen) {
                        hideSystemUi()
                    } else {
                        WindowInsetsControllerCompat(window, window.decorView).apply {
                            show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                    PlayerScreen(viewModel, this@PlayerActivity)
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val dataString = intent?.dataString
        if (dataString != null && dataString.startsWith("videoplyrio://open")) {
            val uri = Uri.parse(dataString)
            val base64Data = uri.getQueryParameter("data") ?: ""
            viewModel.loadFromBase64(base64Data)
            return
        }

        val url = intent?.getStringExtra("url")
            ?: "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4"
        val title = intent?.getStringExtra("title") ?: "Video"
        viewModel.loadFromIntent(url, title)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            viewModel.player.release()
        }
    }
}
