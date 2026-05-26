package com.videoplyrio.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoplyrio.app.OverlayPipService
import com.videoplyrio.app.PlayerViewModel
import com.videoplyrio.app.R
import com.videoplyrio.app.ui.theme.PlyrColors
import java.util.concurrent.TimeUnit

@Composable
fun PlyrControlsOverlay(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val controlsVisible = viewModel.controlsVisible
    val playlistOpen = viewModel.playlistOpen
    val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val context = LocalContext.current
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
                        initialOffsetY = { PlyrAnimations.ControlsOffsetY.roundToPx() }
                    ),
            exit = fadeOut(animationSpec = PlyrAnimations.ControlsSpec) +
                   slideOutVertically(
                       animationSpec = tween(300, easing = FastOutSlowInEasing),
                       targetOffsetY = { PlyrAnimations.ControlsOffsetY.roundToPx() }
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
                                initialOffsetY = { PlyrAnimations.PlaylistOffsetY.roundToPx() },
                                animationSpec = PlyrAnimations.PlaylistSpec
                            ),
                    exit = fadeOut(animationSpec = PlyrAnimations.PlaylistSpec) +
                           scaleOut(
                               targetScale = PlyrAnimations.PlaylistScaleHidden,
                               animationSpec = PlyrAnimations.PlaylistSpec
                           ) +
                           slideOutVertically(
                               targetOffsetY = { PlyrAnimations.PlaylistOffsetY.roundToPx() },
                               animationSpec = PlyrAnimations.PlaylistSpec
                           )
                ) {
                    PlyrPlaylist(viewModel)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.seekTo(viewModel.currentPosition - 10000) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_rewind),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        val isPlaying = viewModel.player.isPlaying
                        IconButton(onClick = {
                            if (isPlaying) viewModel.player.pause() else viewModel.player.play()
                        }) {
                            Icon(
                                painter = painterResource(
                                    if (isPlaying) R.drawable.ic_plyr_pause else R.drawable.ic_plyr_play
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.seekTo(viewModel.currentPosition + 30000) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_fast_forward),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${formatTime(viewModel.currentPosition)}/${formatTime(viewModel.duration)}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_volume),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_settings),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                val intent = Intent(context, OverlayPipService::class.java)
                                intent.putExtra("stream_url", playlist.getOrNull(currentIndex)?.src ?: "")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_pip),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plyr_enter_fullscreen),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                PlyrSeekBar(
                    position = viewModel.currentPosition,
                    duration = viewModel.duration,
                    buffered = viewModel.bufferedPosition,
                    onSeek = { viewModel.seekTo(it) }
                )
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
fun PlyrSeekBar(
    position: Long,
    duration: Long,
    buffered: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) buffered.toFloat() / duration else 0f
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(
            PlyrColors.SeekBarTrack,
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
        val thumbOffset = with(density) { (progress * 200f - 6.5f).dp }
        Box(
            modifier = Modifier
                .size(13.dp)
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .background(Color.White, CircleShape)
        )
    }
    Slider(
        value = progress,
        onValueChange = { onSeek((it * duration).toLong()) },
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    )
}

private fun formatTime(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis)
    val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
