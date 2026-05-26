package com.videoplyrio.app.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videoplyrio.app.PlayerActivity
import com.videoplyrio.app.PlayerViewModel
import com.videoplyrio.app.ui.theme.PlyrColors

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, activity: PlayerActivity) {
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
            modifier = Modifier.fillMaxSize(),
            activity = activity
        )

        PlyrTitleOverlay(viewModel)

        PlyrLoadingOverlay(viewModel)

        PlyrErrorOverlay(viewModel)
    }
}

@Composable
fun PlyrTitleOverlay(viewModel: PlayerViewModel) {
    if (viewModel.currentTitle.isNotEmpty()) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xAA000000))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = viewModel.currentTitle,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
