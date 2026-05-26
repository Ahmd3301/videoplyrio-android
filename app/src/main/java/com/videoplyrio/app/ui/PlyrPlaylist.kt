package com.videoplyrio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoplyrio.app.PlayerViewModel
import com.videoplyrio.app.ui.theme.PlyrColors

object PlyrDimensions {
    val ControlsHeight = 48.dp
    val ControlsPaddingHorizontal = 20.dp
    val ControlsPaddingBottom = 10.dp
    val TitleBottomOffset = (48 + 16).dp
    val TitleHeight = 34.dp
    val TitlePaddingHorizontal = 14.dp
    val TitlePaddingVertical = 6.dp
    val TitleFontSize = 15.sp
    val TitleBorderRadius = 4.dp
    val PlaylistBottomOffset = (48 + 16 + 34 + 16).dp
    val PlaylistItemWidth = 240.dp
    val PlaylistItemHeight = 135.dp
    val PlaylistItemBorderRadius = 4.dp
    val PlaylistGap = 16.dp
    val PlaylistPaddingHorizontal = 20.dp
    val ControlBorderRadius = 4.dp
    val IconSize = 18.dp
    val MenuBorderRadius = 4.dp
}

object PlyrAnimations {
    val ControlsSpec = androidx.compose.animation.core.tween<Float>(
        durationMillis = 300,
        easing = androidx.compose.animation.core.FastOutSlowInEasing
    )

    val PlaylistSpec = androidx.compose.animation.core.tween<Float>(
        durationMillis = 500,
        easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    )

    val ControlsOffsetY = 10.dp
    val PlaylistOffsetY = 20.dp
    val PlaylistScaleHidden = 0.98f
}

@Composable
fun PlyrPlaylist(viewModel: PlayerViewModel) {
    val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlyrDimensions.PlaylistPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(PlyrDimensions.PlaylistGap)
    ) {
        itemsIndexed(playlist) { index, item ->
            PlaylistItem(
                item = item,
                isActive = index == currentIndex,
                onClick = { viewModel.playAt(index) }
            )
        }
    }
}

@Composable
fun PlaylistItem(item: PlayerViewModel.PlaylistEntry, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(PlyrDimensions.PlaylistItemWidth)
            .height(PlyrDimensions.PlaylistItemHeight)
            .clip(RoundedCornerShape(PlyrDimensions.PlaylistItemBorderRadius))
            .background(
                if (isActive) PlyrColors.GlassActiveBg else PlyrColors.GlassBg
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(10.dp)
        )
    }
}
