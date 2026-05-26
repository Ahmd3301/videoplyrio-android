package com.videoplyrio.app.ui

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoplyrio.app.PlayerActivity
import com.videoplyrio.app.PlayerViewModel
import com.videoplyrio.app.R
import com.videoplyrio.app.ui.theme.PlyrColors
import java.util.concurrent.TimeUnit

@Composable
fun PlyrControlsOverlay(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    activity: PlayerActivity
) {
    val controlsVisible = viewModel.controlsVisible
    val playlistOpen = viewModel.playlistOpen
    val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val controlsOffsetYPx = with(density) { PlyrAnimations.ControlsOffsetY.roundToPx() }
    val playlistOffsetYPx = with(density) { PlyrAnimations.PlaylistOffsetY.roundToPx() }
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { viewModel.toggleControls() }
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = PlyrAnimations.ControlsSpec) +
                    slideInVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetY = { controlsOffsetYPx }
                    ),
            exit = fadeOut(animationSpec = PlyrAnimations.ControlsSpec) +
                   slideOutVertically(
                       animationSpec = tween(300, easing = FastOutSlowInEasing),
                       targetOffsetY = { controlsOffsetYPx }
                   )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = PlyrDimensions.ControlsPaddingBottom),
                verticalArrangement = Arrangement.Bottom
            ) {
                AnimatedVisibility(
                    visible = controlsVisible && playlistOpen,
                    enter = fadeIn(animationSpec = PlyrAnimations.PlaylistSpec) +
                            scaleIn(
                                initialScale = PlyrAnimations.PlaylistScaleHidden,
                                animationSpec = PlyrAnimations.PlaylistSpec
                            ) +
                            slideInVertically(
                                initialOffsetY = { playlistOffsetYPx },
                                animationSpec = tween<IntOffset>(500, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                            ),
                    exit = fadeOut(animationSpec = PlyrAnimations.PlaylistSpec) +
                           scaleOut(
                               targetScale = PlyrAnimations.PlaylistScaleHidden,
                               animationSpec = PlyrAnimations.PlaylistSpec
                           ) +
                           slideOutVertically(
                               targetOffsetY = { playlistOffsetYPx },
                               animationSpec = tween<IntOffset>(500, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                           )
                ) {
                    PlyrPlaylist(viewModel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlyrButton(onClick = { viewModel.seekTo(viewModel.currentPosition - 10000) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plyr_rewind),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val isPlaying = viewModel.player.isPlaying
                    PlyrButton(onClick = {
                        if (isPlaying) viewModel.player.pause() else viewModel.player.play()
                    }) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_plyr_pause else R.drawable.ic_plyr_play
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    PlyrButton(onClick = { viewModel.seekTo(viewModel.currentPosition + 30000) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plyr_fast_forward),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    PlyrSeekBar(
                        position = viewModel.currentPosition,
                        duration = viewModel.duration,
                        buffered = viewModel.bufferedPosition,
                        onSeek = { viewModel.seekTo(it) },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = "${formatTime(viewModel.currentPosition)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    PlyrButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            painter = painterResource(
                                if (viewModel.isMuted) R.drawable.ic_plyr_muted else R.drawable.ic_plyr_volume
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Slider(
                        value = viewModel.volume,
                        onValueChange = { viewModel.updateVolume(it) },
                        modifier = Modifier.width(60.dp).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = PlyrColors.SeekBarActive,
                            inactiveTrackColor = Color(0xFF555555)
                        )
                    )

                    PlyrButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plyr_settings),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                    ) {
                        PlyrButton(onClick = {
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            activity.enterPictureInPictureMode(params)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_pip),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    PlyrButton(onClick = { viewModel.toggleFullscreen() }) {
                        Icon(
                            painter = painterResource(
                                if (isFullscreen) R.drawable.ic_plyr_exit_fullscreen
                                else R.drawable.ic_plyr_enter_fullscreen
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (showSettings) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 60.dp)
            ) {
                PlyrSettingsMenu(
                    speeds = viewModel.getAvailableSpeeds(),
                    currentSpeed = viewModel.getCurrentSpeed(),
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

@Composable
fun PlyrButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PlyrSeekBar(
    position: Long,
    duration: Long,
    buffered: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) buffered.toFloat() / duration else 0f

    Box(modifier = modifier.height(24.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).height(4.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(
                    Color(0xFF555555),
                    RoundedCornerShape(2.dp)
                ))
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufferedProgress)
                    .background(PlyrColors.SeekBarBuffer, RoundedCornerShape(2.dp))
                )
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(PlyrColors.SeekBarActive, RoundedCornerShape(2.dp))
                )
            }
        }
        Slider(
            value = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatTime(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis)
    val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
