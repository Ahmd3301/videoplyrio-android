package com.videoplyrio.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.dataString
        if (url != null) {
            val playerIntent = Intent(this, PlayerActivity::class.java).apply {
                data = intent.data
                putExtra("title", intent?.getStringExtra("title") ?: "Video")
                putExtra("playlist", intent?.getStringExtra("playlist"))
            }
            startActivity(playerIntent)
            finish()
            return
        }

        setContent {
            com.videoplyrio.app.ui.theme.PlyrTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(onOpenPlayer = {
                        val demoUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4"
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                            putExtra("url", demoUrl)
                            putExtra("title", "Demo Video")
                        }
                        startActivity(intent)
                    })
                }
            }
        }
    }
}

@Composable
private fun MainScreen(onOpenPlayer: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Button(onClick = onOpenPlayer) {
            androidx.compose.material3.Text("Open Player")
        }
    }
}
