package com.videoplyrio.app.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.videoplyrio.app.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                TextureView(context).also { textureView ->
                    viewModel.attachTextureView(textureView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        PlyrControlsOverlay(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        PlyrLoadingOverlay(viewModel)

        PlyrErrorOverlay(viewModel)
    }
}
