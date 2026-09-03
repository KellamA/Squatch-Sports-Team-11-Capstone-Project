package com.squatchsports.training.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.squatchsports.training.R
import kotlinx.coroutines.delay

@Composable
fun SquatchSplashScreen() {
    var showLogo by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showLogo = true
        delay(600)
        showLoading = true
    }
    val logoAlpha by animateFloatAsState(if (showLogo) 1f else 0f, tween(350), label = "logo")
    val loadingAlpha by animateFloatAsState(if (showLoading) 1f else 0f, tween(350), label = "loading")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(R.drawable.squatch_sports_logo),
                contentDescription = "Squatch Sports",
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier.alpha(loadingAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DotLoader()
                Text("Loading…", color = Color.Black.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DotLoader() {
    val transition = rememberInfiniteTransition(label = "loading dots")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(3) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150, StartOffsetType.Delay),
                ),
                label = "dot $index",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .scale(scale)
                    .background(Color.Black.copy(alpha = 0.85f), CircleShape),
            )
        }
    }
}
