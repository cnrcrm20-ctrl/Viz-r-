package com.example.games

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageManager
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class SnakeDirection {
    UP, DOWN, LEFT, RIGHT
}

data class Point(val x: Int, val y: Int)

@Composable
fun SnakeGameScreen(
    context: Context,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    val gridSize = 20
    var score by remember { mutableStateOf(0) }
    var highScore by remember {
        val prefs = context.getSharedPreferences("vizor_snake_prefs", Context.MODE_PRIVATE)
        mutableStateOf(prefs.getInt("high_score", 0))
    }

    var snake by remember { mutableStateOf(listOf(Point(10, 10), Point(10, 11), Point(10, 12))) }
    var direction by remember { mutableStateOf(SnakeDirection.UP) }
    var food by remember { mutableStateOf(Point(5, 5)) }

    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    // Snake game loop timer
    LaunchedEffect(isPlaying, isGameOver) {
        if (isPlaying && !isGameOver) {
            score = 0
            snake = listOf(Point(10, 10), Point(10, 11), Point(10, 12))
            direction = SnakeDirection.UP
            generateNewFood(gridSize, snake) { food = it }

            while (isPlaying && !isGameOver) {
                // Determine tick speed (gets faster as score grows)
                val delayTime = (250 - (score * 4)).coerceAtLeast(90).toLong()
                delay(delayTime)

                // Move head
                val head = snake.first()
                val newHead = when (direction) {
                    SnakeDirection.UP -> Point(head.x, (head.y - 1 + gridSize) % gridSize)
                    SnakeDirection.DOWN -> Point(head.x, (head.y + 1) % gridSize)
                    SnakeDirection.LEFT -> Point((head.x - 1 + gridSize) % gridSize, head.y)
                    SnakeDirection.RIGHT -> Point((head.x + 1) % gridSize, head.y)
                }

                // Check self-collision
                if (snake.contains(newHead)) {
                    isGameOver = true
                    isPlaying = false
                    if (score > highScore) {
                        highScore = score
                        val prefs = context.getSharedPreferences("vizor_snake_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putInt("high_score", score).apply()
                    }
                    break
                }

                val newSnake = mutableListOf(newHead)
                newSnake.addAll(snake)

                // Eat food check
                if (newHead == food) {
                    score += 10
                    generateNewFood(gridSize, snake) { food = it }
                } else {
                    newSnake.removeAt(newSnake.size - 1)
                }

                snake = newSnake
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper stats ticker
        Text(
            text = LanguageManager.getString("retro_snake"),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${LanguageManager.getString("snake_score")} $score",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
            Text(
                text = "BEST: $highScore",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid Box with canvas rendering
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(Color(18, 20, 26))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSizeX = size.width / gridSize
                val cellSizeY = size.height / gridSize

                // Draw background grid lines (extremely subtle)
                for (i in 0..gridSize) {
                    drawLine(Color(0xFF222634), Offset(i * cellSizeX, 0f), Offset(i * cellSizeX, size.height))
                    drawLine(Color(0xFF222634), Offset(0f, i * cellSizeY), Offset(size.width, i * cellSizeY))
                }

                // Draw Food
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(food.x * cellSizeX + 2f, food.y * cellSizeY + 2f),
                    size = Size(cellSizeX - 4f, cellSizeY - 4f)
                )

                // Draw Snake (Head has different accent)
                snake.forEachIndexed { index, part ->
                    val color = if (index == 0) primaryColor else Color(0xFF4CAF50)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(part.x * cellSizeX + 2f, part.y * cellSizeY + 2f),
                        size = Size(cellSizeX - 4f, cellSizeY - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }

            if (!isPlaying) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isGameOver) "GAME OVER" else "SNAKE RETRO",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isGameOver) Color.Red else primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isGameOver = false
                            isPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGameOver) "Restart" else "Start Engine", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation D-Pad controller (beautiful circle joystick)
        Box(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // UP
                IconButton(
                    onClick = { if (direction != SnakeDirection.DOWN) direction = SnakeDirection.UP },
                    enabled = isPlaying,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(46, 52, 64), CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = primaryColor)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT
                    IconButton(
                        onClick = { if (direction != SnakeDirection.RIGHT) direction = SnakeDirection.LEFT },
                        enabled = isPlaying,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(46, 52, 64), CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = primaryColor)
                    }

                    // Middle dot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(primaryColor.copy(alpha = 0.35f), CircleShape)
                    )

                    // RIGHT
                    IconButton(
                        onClick = { if (direction != SnakeDirection.LEFT) direction = SnakeDirection.RIGHT },
                        enabled = isPlaying,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(46, 52, 64), CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = primaryColor)
                    }
                }

                // DOWN
                IconButton(
                    onClick = { if (direction != SnakeDirection.UP) direction = SnakeDirection.DOWN },
                    enabled = isPlaying,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(46, 52, 64), CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = primaryColor)
                }
            }
        }
    }
}

private fun generateNewFood(gridSize: Int, snake: List<Point>, onGenerate: (Point) -> Unit) {
    var newFood = Point(Random.nextInt(gridSize), Random.nextInt(gridSize))
    while (snake.contains(newFood)) {
        newFood = Point(Random.nextInt(gridSize), Random.nextInt(gridSize))
    }
    onGenerate(newFood)
}
