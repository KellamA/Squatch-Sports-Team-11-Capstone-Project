package com.squatchsports.training.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.squatchsports.training.R
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.shared.CourtPosition
import com.squatchsports.training.shared.ShotDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val MakeGreen = Color(0xFF34C759)
private val MissRed = Color(0xFFFF3B30)
private val SwishPink = Color(0xFFFF2D55)
private val SquatchBlue = Color(0xFF007AFF)
private val WarningOrange = Color(0xFFFF9500)
private val SecondaryText = Color.White.copy(alpha = 0.62f)

@Composable
fun WearWorkoutApp(connectivity: WorkoutConnectivity) {
    val state = remember(connectivity) { WearWorkoutState(connectivity) }
    val workoutSequence = connectivity.workoutSequence
    val drillEvent = connectivity.drillEvent
    val positionEvent = connectivity.positionEvent
    val shotEvent = connectivity.shotEvent

    LaunchedEffect(workoutSequence) {
        state.handleWorkoutChange(connectivity.workoutActive)
    }
    LaunchedEffect(drillEvent?.sequence) {
        drillEvent?.let { state.handleDrill(it.name) }
    }
    LaunchedEffect(positionEvent?.sequence) {
        positionEvent?.let { state.handlePosition(it.position, connectivity.workoutActive) }
    }
    LaunchedEffect(shotEvent?.sequence) {
        shotEvent?.let { state.recordPhoneShot(it.value, connectivity.workoutActive) }
    }

    when {
        state.showSummary -> WorkoutSummaryScreen(state)
        connectivity.workoutActive -> ActiveWorkoutScreen(state, connectivity.workoutActive)
        else -> WaitingScreen()
    }
}

@Composable
private fun WaitingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PhoneSyncIcon()
            Text("Waiting for workout", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Start a session on your phone to enable shot logging.",
                color = SecondaryText,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhoneSyncIcon() {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(size.width * 0.25f, size.height * 0.08f),
            size = Size(size.width * 0.5f, size.height * 0.84f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 1.dp.toPx(),
            center = Offset(size.width / 2f, size.height * 0.82f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.65f),
            startAngle = -55f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.60f, size.height * 0.27f),
            size = Size(size.width * 0.32f, size.height * 0.46f),
            style = Stroke(stroke),
        )
    }
}

@Composable
private fun ActiveWorkoutScreen(state: WearWorkoutState, workoutActive: Boolean) {
    var highlightedDirection by remember { mutableStateOf<ShotDirection?>(null) }
    val isRound = LocalConfiguration.current.isScreenRound
    val verticalInset = if (isRound) 18.dp else 4.dp
    val sideInset = if (isRound) 12.dp else 4.dp
    val horizontalPillWidth = if (isRound) 72.dp else 80.dp
    val horizontalPillHeight = if (isRound) 25.dp else 28.dp
    val verticalPillWidth = if (isRound) 25.dp else 28.dp
    val verticalPillHeight = if (isRound) 68.dp else 80.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.currentDrill, workoutActive) {
                detectTapGestures { location ->
                    state.updateGeneralPosition(
                        CourtPosition(
                            row = 0,
                            column = 0,
                            name = null,
                            rowPercent = (location.y / size.height).toDouble(),
                            columnPercent = (location.x / size.width).toDouble(),
                        ),
                        workoutActive,
                    )
                }
            },
    ) {
        CourtView(state.selectedPosition, isRound)

        if (state.currentPositionStats != null) {
            EdgePill(
                title = "Make",
                color = MakeGreen,
                highlighted = highlightedDirection == ShotDirection.MAKE,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = verticalInset)
                    .size(width = horizontalPillWidth, height = horizontalPillHeight),
            )
            EdgePill(
                title = "Miss",
                color = MissRed,
                highlighted = highlightedDirection == ShotDirection.MISS,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = verticalInset)
                    .size(width = horizontalPillWidth, height = horizontalPillHeight),
            )
            EdgePill(
                title = "Swish",
                color = SwishPink,
                highlighted = highlightedDirection == ShotDirection.SWISH,
                vertical = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = sideInset)
                    .size(width = verticalPillWidth, height = verticalPillHeight),
            )

            val padSize = min(maxWidth.value, maxHeight.value).dp * 0.20f
            var dragTranslation by remember { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(padSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        1.5.dp,
                        state.lastShotStatus?.shotColor()?.copy(alpha = 0.9f)
                            ?: Color.White.copy(alpha = 0.18f),
                        CircleShape,
                    )
                    .pointerInput(state.currentPositionStats) {
                        detectDragGestures(
                            onDragStart = {
                                dragTranslation = Offset.Zero
                                highlightedDirection = null
                            },
                            onDragEnd = {
                                resolveDirection(dragTranslation, 24.dp.toPx())?.let(state::recordLocalShot)
                                highlightedDirection = null
                                dragTranslation = Offset.Zero
                            },
                            onDragCancel = {
                                highlightedDirection = null
                                dragTranslation = Offset.Zero
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            dragTranslation += dragAmount
                            highlightedDirection = resolveDirection(dragTranslation, 24.dp.toPx())
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("☝", color = Color.White.copy(alpha = 0.85f), fontSize = 18.sp)
            }
        }

        state.currentPositionStats?.let { stats ->
            Column(
                modifier = Modifier
                    .align(if (isRound) Alignment.CenterStart else Alignment.BottomStart)
                    .padding(
                        start = if (isRound) 28.dp else 6.dp,
                        bottom = if (isRound) 0.dp else 12.dp,
                    )
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${stats.makes + stats.swishes}",
                        color = MakeGreen,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("/", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, lineHeight = 10.sp)
                    Text(
                        "${stats.totalShots}",
                        color = Color.White,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "${state.shotsPerPosition - state.shotCount} left",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                )
                stats.position.name?.let { name ->
                    Text(
                        name,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                        maxLines = 1,
                    )
                }
                if (state.currentDrill != null && state.drillPositions.isNotEmpty()) {
                    Text(
                        "${state.currentPositionIndex + 1}/${state.drillPositions.size}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EdgePill(
    title: String,
    color: Color,
    highlighted: Boolean,
    modifier: Modifier,
    vertical: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = if (highlighted) 0.9f else 0.6f), RoundedCornerShape(10.dp))
            .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            modifier = if (vertical) Modifier.rotate(-90f) else Modifier,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CourtView(position: CourtPosition?, isRound: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFD9B385), Color(0xFFC79E73), Color(0xFFD1A87A)),
                    start = Offset.Zero,
                    end = Offset(width, height),
                ),
            )
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.White.copy(alpha = 0.08f)),
                    start = Offset.Zero,
                    end = Offset(width, height),
                ),
            )

            repeat(20) { index ->
                val x = index * width / 20f + width / 40f
                drawLine(Color.Black.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, height), 1f)
            }
            repeat(12) { index ->
                val y = index * height / 12f + height / 24f
                drawLine(Color.Black.copy(alpha = 0.06f), Offset(0f, y), Offset(width, y), 0.5f)
            }

            drawRect(MakeGreen.copy(alpha = 0.6f), size = Size(width, 4.dp.toPx()))
            drawRect(MissRed.copy(alpha = 0.6f), topLeft = Offset(0f, height - 4.dp.toPx()), size = Size(width, 4.dp.toPx()))
            drawRect(Color.White, size = Size(4.dp.toPx(), height))
            drawRect(SwishPink.copy(alpha = 0.6f), topLeft = Offset(width - 4.dp.toPx(), 0f), size = Size(4.dp.toPx(), height))

            val hoopRadius = width * 0.0475f
            drawCircle(MissRed.copy(alpha = 0.9f), hoopRadius, Offset(width / 2f, height * 0.14f))
            drawCircle(Color(0xFFFF9500), hoopRadius, Offset(width / 2f, height * 0.14f), style = Stroke(2.dp.toPx()))

            val boardSize = Size(width * 0.24f, height * 0.055f)
            val boardTopLeft = Offset(width / 2f - boardSize.width / 2f, height * 0.085f - boardSize.height / 2f)
            drawRect(Color.White.copy(alpha = 0.85f), boardTopLeft, boardSize)
            drawRect(Color.White, boardTopLeft, boardSize, style = Stroke(2.dp.toPx()))
            val targetSize = Size(width * 0.12f, height * 0.035f)
            drawRect(
                Color.White,
                Offset(width / 2f - targetSize.width / 2f, height * 0.085f - targetSize.height / 2f),
                targetSize,
                style = Stroke(1.5.dp.toPx()),
            )

            val paintSize = Size(width * 0.36f, height * 0.29f)
            drawRect(
                Color.White,
                Offset(width / 2f - paintSize.width / 2f, height * 0.26f - paintSize.height / 2f),
                paintSize,
                style = Stroke(2.dp.toPx()),
            )
            drawLine(
                Color.White,
                Offset(width / 2f - width * 0.18f, height * 0.37f),
                Offset(width / 2f + width * 0.18f, height * 0.37f),
                2.dp.toPx(),
            )

            val freeThrowDiameter = width * 0.36f
            val freeThrowRect = Offset(width / 2f - freeThrowDiameter / 2f, height * 0.37f - freeThrowDiameter / 2f)
            drawArc(
                Color.White,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = freeThrowRect,
                size = Size(freeThrowDiameter, freeThrowDiameter),
                style = Stroke(2.dp.toPx()),
            )
            val dashedRect = Offset(width / 2f - freeThrowDiameter / 2f, height * 0.13f - freeThrowDiameter / 2f)
            drawArc(
                Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = dashedRect,
                size = Size(freeThrowDiameter, freeThrowDiameter),
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
            )

            repeat(4) { index ->
                val y = height * 0.15f + index * height * 0.055f
                drawLine(Color.White, Offset(width / 2f - width * 0.18f - 8.dp.toPx(), y), Offset(width / 2f - width * 0.18f, y), 2.dp.toPx())
                drawLine(Color.White, Offset(width / 2f + width * 0.18f, y), Offset(width / 2f + width * 0.18f + 8.dp.toPx(), y), 2.dp.toPx())
            }

            val centerX = width / 2f
            val centerY = height * 0.12f
            val radius = width * 0.52f
            val threePointPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius,
                    ),
                    startAngleDegrees = 38f,
                    sweepAngleDegrees = 104f,
                    forceMoveTo = true,
                )
                val angle = Math.toRadians(38.0)
                val leftX = centerX - radius * cos(angle).toFloat()
                val leftY = centerY + radius * sin(angle).toFloat()
                val rightX = centerX + radius * cos(angle).toFloat()
                val rightY = leftY
                moveTo(leftX, leftY)
                lineTo(leftX, height * 0.06f)
                moveTo(rightX, rightY)
                lineTo(rightX, height * 0.06f)
            }
            drawPath(threePointPath, Color.White, style = Stroke(2.5.dp.toPx()))
        }

        val logoSize = maxWidth * 0.28f
        Box(
            modifier = Modifier
                .size(logoSize)
                .offset(x = maxWidth / 2f - logoSize / 2f, y = maxHeight * 0.78f - logoSize / 2f)
                .background(Color.White.copy(alpha = 0.85f), CircleShape)
                .border(2.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.mascot_bigfoot),
                contentDescription = null,
                modifier = Modifier.size(logoSize * 0.79f),
                contentScale = ContentScale.Fit,
            )
        }

        position?.let { markerPosition ->
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val outerRadius = 14.dp.toPx()
                val desiredX = size.width * markerPosition.columnPercent.toFloat()
                val desiredY = size.height * markerPosition.rowPercent.toFloat()
                val markerCenter = if (isRound) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val safeRadius = min(size.width, size.height) / 2f - outerRadius - 2.dp.toPx()
                    val safeY = desiredY.coerceIn(center.y - safeRadius, center.y + safeRadius)
                    val verticalDistance = safeY - center.y
                    val horizontalReach = sqrt(max(0f, safeRadius * safeRadius - verticalDistance * verticalDistance))
                    Offset(
                        desiredX.coerceIn(center.x - horizontalReach, center.x + horizontalReach),
                        safeY,
                    )
                } else {
                    Offset(
                        desiredX.coerceIn(outerRadius, size.width - outerRadius),
                        desiredY.coerceIn(outerRadius, size.height - outerRadius),
                    )
                }

                drawCircle(SquatchBlue.copy(alpha = 0.3f), outerRadius, markerCenter)
                drawCircle(SquatchBlue, 12.dp.toPx(), markerCenter, style = Stroke(2.5.dp.toPx()))
                drawCircle(SquatchBlue, 3.dp.toPx(), markerCenter)
            }
        }
    }
}

@Composable
private fun WorkoutSummaryScreen(state: WearWorkoutState) {
    val session = state.currentSession
    val isRound = LocalConfiguration.current.isScreenRound
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (isRound) 14.dp else 0.dp,
                end = if (isRound) 14.dp else 0.dp,
                top = if (isRound) 20.dp else 8.dp,
                bottom = if (isRound) 20.dp else 8.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(session?.drillName ?: "Workout", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Complete!", color = SecondaryText, fontSize = 10.sp)
        }

        if (session != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(if (isRound) 7.dp else 12.dp)) {
                SummaryStat("${session.totalMakes}", "Makes", MakeGreen, isRound)
                SummaryStat("${session.totalMisses}", "Misses", MissRed, isRound)
                SummaryStat("${session.totalSwishes}", "Swish", SwishPink, isRound)
                SummaryStat("${session.overallPercentage.toInt()}%", "FG%", SquatchBlue, isRound)
            }
            Text("${session.totalShots} total shots", color = SecondaryText, fontSize = 9.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))

            if (session.positionStats.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "By Position",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    session.positionStats.forEachIndexed { index, stats ->
                        CompactStatCard(stats, index + 1)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(SquatchBlue, RoundedCornerShape(6.dp))
                .clickable(onClick = state::doneWithSummary)
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(value, color = color, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SecondaryText, fontSize = 8.sp)
    }
}

@Composable
private fun CompactStatCard(stats: PositionStats, number: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                stats.position.name ?: "Spot $number",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${stats.makePercentage.toInt()}%",
                color = if (stats.makePercentage >= 60) MakeGreen else WarningOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✓ ${stats.makes}", color = MakeGreen, fontSize = 9.sp)
            Text("✦ ${stats.swishes}", color = SwishPink, fontSize = 9.sp)
            Text("× ${stats.misses}", color = MissRed, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("${stats.totalShots} shots", color = SecondaryText, fontSize = 8.sp)
        }
    }
}

private fun resolveDirection(translation: Offset, threshold: Float): ShotDirection? {
    if (abs(translation.x) < threshold && abs(translation.y) < threshold) return null
    if (abs(translation.y) >= abs(translation.x)) {
        return if (translation.y < 0) ShotDirection.MAKE else ShotDirection.MISS
    }
    return if (translation.x > 0) ShotDirection.SWISH else null
}

private fun ShotDirection.shotColor(): Color = when (this) {
    ShotDirection.MISS -> MissRed
    ShotDirection.MAKE -> MakeGreen
    ShotDirection.SWISH -> SwishPink
}
