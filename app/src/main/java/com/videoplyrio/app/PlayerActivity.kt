package com.videoplyrio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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

        val url = intent?.dataString
            ?: intent?.getStringExtra("url")
            ?: "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4"
        val title = intent?.getStringExtra("title") ?: "Video"
        val playlistJson = intent?.getStringExtra("playlist")

        viewModel.loadFromIntent(url, title, playlistJson)

        setContent {
            PlyrTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerScreen(viewModel)
                }
            }
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
