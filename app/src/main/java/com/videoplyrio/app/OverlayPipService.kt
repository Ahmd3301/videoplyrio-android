package com.videoplyrio.app

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.media3.exoplayer.ExoPlayer

class OverlayPipService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var player: ExoPlayer? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val streamUrl = intent?.getStringExtra("stream_url") ?: return START_NOT_STICKY
        setupOverlay(streamUrl)
        return START_STICKY
    }

    private fun setupOverlay(url: String) {
        player = ExoPlayer.Builder(this).build().also {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }

        val params = WindowManager.LayoutParams(
            240.dpToPx(), 135.dpToPx(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16.dpToPx(); y = 100.dpToPx()
        }

        overlayView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OverlayPipPlayer(
                    player = player,
                    onClose = { stopSelf() },
                    onExpand = {
                        val i = Intent(this@OverlayPipService, PlayerActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(i)
                        stopSelf()
                    }
                )
            }

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(v, params)
                        true
                    }
                    else -> false
                }
            }
        }

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        overlayView?.let { windowManager?.removeView(it) }
        player?.release()
        super.onDestroy()
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()
}

@Composable
fun OverlayPipPlayer(
    player: ExoPlayer?,
    onClose: () -> Unit,
    onExpand: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { player?.setVideoTextureView(it) }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xAA000000))
                    )
                )
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
            IconButton(onClick = { if (player?.isPlaying == true) player?.pause() else player?.play() }) {
                Icon(
                    painter = painterResource(
                        if (player?.isPlaying == true) R.drawable.ic_plyr_pause else R.drawable.ic_plyr_play
                    ),
                    contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            IconButton(onClick = onExpand, modifier = Modifier.size(24.dp)) {
                Icon(painter = painterResource(R.drawable.ic_plyr_enter_fullscreen), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}
