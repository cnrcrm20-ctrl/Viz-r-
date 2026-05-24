package com.example.games

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageManager
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CarGameScreen(
    context: Context,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    var score by remember { mutableStateOf(0) }
    var highScore by remember {
        val prefs = context.getSharedPreferences("vizor_car_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getInt("high_score", 0))
    }
    var playerLane by remember { mutableStateOf(1) } // 0 = Left, 1 = Middle, 2 = Right
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    // Obstacles: each has lane (0,1,2) and Y coordinate (0.0 to 1.0)
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var speedMultiplier by remember { mutableStateOf(1.0f) }

    // Traffic ticking
    LaunchedEffect(isPlaying, isGameOver) {
        if (isPlaying && !isGameOver) {
            obstacles = listOf(
                Obstacle(Random.nextInt(3), -0.2f),
                Obstacle(Random.nextInt(3), -0.7f)
            )
            score = 0
            speedMultiplier = 1.0f

            while (isPlaying && !isGameOver) {
                delay(40) // ~24 FPS calculation
                score += 1

                // Slowly increase velocity
                if (score % 100 == 0) {
                    speedMultiplier += 0.15f
                }

                obstacles = obstacles.map {
                    it.copy(y = it.y + 0.02f * speedMultiplier)
                }.toMutableList().apply {
                    // Filter out off-screen obstacles and add a new one
                    val iterator = iterator()
                    while (iterator.hasNext()) {
                        val obs = iterator.next()
                        if (obs.y > 1.1f) {
                            iterator.remove()
                        }
                    }
                    if (size < 3 && Random.nextFloat() < 0.05f) {
                        add(Obstacle(Random.nextInt(3), -0.2f))
                    }
                }

                // Check collision
                // Player is situated roughly at bottom y range: 0.8f to 0.95f
                for (obs in obstacles) {
                    if (obs.lane == playerLane && obs.y in 0.8f..0.96f) {
                        isGameOver = true
                        isPlaying = false
                        if (score > highScore) {
                            highScore = score
                            val prefs = context.getSharedPreferences("vizor_car_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("high_score", score).apply()
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = LanguageManager.getString("endless_car"),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${LanguageManager.getString("car_score")} $score m",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
            Text(
                text = "BEST: $highScore m",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Play field (Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color(0xFF222630))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneWidth = size.width / 3f

                // Draw lanes dividers
                drawLine(Color(0xFF3A3F50), Offset(laneWidth, 0f), Offset(laneWidth, size.height), strokeWidth = 4f)
                drawLine(Color(0xFF3A3F50), Offset(laneWidth * 2, 0f), Offset(laneWidth * 2, size.height), strokeWidth = 4f)

                // Draw roadside gravel stripes
                for (i in 0..10) {
                    val roadStripesOffset = (System.currentTimeMillis() / 5) % size.height.toInt()
                    val stripeY = (i * (size.height / 10f) + roadStripesOffset) % size.height
                    drawRect(Color.Yellow.copy(alpha = 0.4f), Offset(0f, stripeY), Size(12f, 30f))
                    drawRect(Color.Yellow.copy(alpha = 0.4f), Offset(size.width - 12f, stripeY), Size(12f, 30f))
                }

                // Draw obstacles
                for (obs in obstacles) {
                    val obsX = obs.lane * laneWidth + (laneWidth - 50.dp.toPx()) / 2f
                    val obsY = obs.y * size.height

                    // Drawing beautiful miniature traffic cars
                    drawRect(
                        color = Color(0xFFF44336), // Enemy Red Car
                        topLeft = Offset(obsX, obsY),
                        size = Size(50.dp.toPx(), 70.dp.toPx())
                    )
                    // Wheels
                    drawRect(Color.Black, Offset(obsX - 5.dp.toPx(), obsY + 10.dp.toPx()), Size(5.dp.toPx(), 15.dp.toPx()))
                    drawRect(Color.Black, Offset(obsX + 50.dp.toPx(), obsY + 10.dp.toPx()), Size(5.dp.toPx(), 15.dp.toPx()))
                    drawRect(Color.Black, Offset(obsX - 5.dp.toPx(), obsY + 45.dp.toPx()), Size(5.dp.toPx(), 15.dp.toPx()))
                    drawRect(Color.Black, Offset(obsX + 50.dp.toPx(), obsY + 45.dp.toPx()), Size(5.dp.toPx(), 15.dp.toPx()))
                }

                // Draw Player Car
                val playerX = playerLane * laneWidth + (laneWidth - 50.dp.toPx()) / 2f
                val playerY = 0.85f * size.height

                // Custom yellow formula sports car
                drawRect(
                    color = Color(0xFFFFEB3B), // Shiny Yellow Player Car
                    topLeft = Offset(playerX, playerY),
                    size = Size(50.dp.toPx(), 72.dp.toPx())
                )
                // Spoiler
                drawRect(primaryColor, Offset(playerX - 6.dp.toPx(), playerY + 62.dp.toPx()), Size(62.dp.toPx(), 10.dp.toPx()))
                // Cabin outline
                drawRect(Color.Black.copy(alpha = 0.6f), Offset(playerX + 12.dp.toPx(), playerY + 20.dp.toPx()), Size(26.dp.toPx(), 30.dp.toPx()))
                // Lights
                drawRect(Color.White, Offset(playerX + 6.dp.toPx(), playerY), Size(8.dp.toPx(), 6.dp.toPx()))
                drawRect(Color.White, Offset(playerX + 36.dp.toPx(), playerY), Size(8.dp.toPx(), 6.dp.toPx()))
            }

            if (!isPlaying) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isGameOver) {
                        Text(
                            text = "GAME OVER",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score: $score m",
                            fontSize = 18.sp,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isGameOver = false
                            isPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isGameOver) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Restart",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isGameOver) "Restart" else "Start Engine", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // On-screen touch steering controls (D-Pad style left/right arrows)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { if (playerLane > 0) playerLane-- },
                enabled = isPlaying,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF2E3440), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Lane Left",
                    tint = if (isPlaying) primaryColor else Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = { if (playerLane < 2) playerLane++ },
                enabled = isPlaying,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF2E3440), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Lane Right",
                    tint = if (isPlaying) primaryColor else Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

data class Obstacle(val lane: Int, val y: Float)
