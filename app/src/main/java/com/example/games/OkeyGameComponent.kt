package com.example.games

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class OkeyTile(
    val number: Int,
    val color: String, // "RED", "BLUE", "BLACK", "YELLOW"
    var isJoker: Boolean = false
) {
    fun getComposeColor(): Color {
        return when (color) {
            "RED" -> Color(0xFFEF5350)
            "BLUE" -> Color(0xFF29B6F6)
            "BLACK" -> Color(0xFF2E3440)
            "YELLOW" -> Color(0xFFFFCA28)
            else -> Color.Gray
        }
    }
}

@Composable
fun OkeyGameScreen(
    context: Context,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Players and turns
    // Turn 0: User, Turn 1: Left Bot, Turn 2: Top Bot, Turn 3: Right Bot
    var activeTurn by remember { mutableStateOf(0) }
    var gameStatusText by remember { mutableStateOf(LanguageManager.getString("okey_user_turn")) }
    var userWinningState by remember { mutableStateOf(false) }

    // Game stats
    var userHasDrawn by remember { mutableStateOf(false) }
    var wallSize by remember { mutableStateOf(106) }
    var selectedHandIndex by remember { mutableStateOf(-1) }
    var tableDiscardPile by remember { mutableStateOf<OkeyTile?>(OkeyTile(8, "BLUE")) }

    // User's Hand (Starts with 14 tiles)
    var userHand by remember { mutableStateOf<List<OkeyTile>>(emptyList()) }

    // Reset loop
    fun startNewOkeyGame() {
        wallSize = 106
        selectedHandIndex = -1
        activeTurn = 0
        userHasDrawn = false
        userWinningState = false
        tableDiscardPile = OkeyTile(Random.nextInt(1, 14), listOf("RED", "BLUE", "BLACK", "YELLOW").random())
        gameStatusText = LanguageManager.getString("okey_user_turn")

        // Draw 14 random tiles
        val colors = listOf("RED", "BLUE", "BLACK", "YELLOW")
        val initialHand = mutableListOf<OkeyTile>()
        for (i in 1..14) {
            initialHand.add(OkeyTile(Random.nextInt(1, 14), colors.random()))
        }
        // Easy sort
        userHand = initialHand.sortedWith(compareBy({ it.color }, { it.number }))
    }

    // Initialize on start
    LaunchedEffect(Unit) {
        startNewOkeyGame()
    }

    // Simulated Bot actions
    LaunchedEffect(activeTurn) {
        if (activeTurn > 0) {
            val botName = when (activeTurn) {
                1 -> "Sol Bot (Left)"
                2 -> "Üst Bot (Top)"
                else -> "Sağ Bot (Right)"
            }
            gameStatusText = "$botName ${LanguageManager.getString("okey_bot_turn")}"
            
            // Bots takes 1.5 seconds to calculate turn P2P locally ($0 charge)
            delay(1500)
            
            // Simulating bot actions
            wallSize = (wallSize - 1).coerceAtLeast(0)
            
            // Discard pile is replaced by Bot's discard
            tableDiscardPile = OkeyTile(Random.nextInt(1, 14), listOf("RED", "BLUE", "BLACK", "YELLOW").random())
            
            // Next turn loop
            activeTurn = (activeTurn + 1) % 4
            if (activeTurn == 0) {
                userHasDrawn = false
                gameStatusText = LanguageManager.getString("okey_user_turn")
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
        Text(
            text = LanguageManager.getString("offline_okey"),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // The Table Layout (Virtual representation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F3E22)) // Cozy Green Felt Table Cover
                .border(3.dp, Color(0xFF4E342E), RoundedCornerShape(16.dp)) // Rich wooden outline
                .padding(12.dp)
        ) {
            // Top Bot
            Text(
                text = "ÜST BOT (Bot 2)",
                color = if (activeTurn == 2) Color.Yellow else Color.White.copy(alpha = 0.7f),
                fontWeight = if (activeTurn == 2) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Left Bot
            Text(
                text = "SOL BOT\n(Bot 1)",
                color = if (activeTurn == 1) Color.Yellow else Color.White.copy(alpha = 0.7f),
                fontWeight = if (activeTurn == 1) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            // Right Bot
            Text(
                text = "SAĞ BOT\n(Bot 3)",
                color = if (activeTurn == 3) Color.Yellow else Color.White.copy(alpha = 0.7f),
                fontWeight = if (activeTurn == 3) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // Center Pile & Game state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WALLS: $wallSize tiles",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Show Discard Pile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LAST DISCARD: ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    tableDiscardPile?.let { tile ->
                        Box(
                            modifier = Modifier
                                .size(30.dp, 40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF3E0))
                                .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "${tile.number}",
                                color = tile.getComposeColor(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // User at bottom
            Text(
                text = "SİZ // YOU (Player)",
                color = if (activeTurn == 0) Color.Yellow else Color.White.copy(alpha = 0.7f),
                fontWeight = if (activeTurn == 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions helper
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = gameStatusText,
                    fontSize = 13.sp,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { startNewOkeyGame() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = primaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Draw / Pile buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (activeTurn == 0 && !userHasDrawn) {
                        wallSize = (wallSize - 1).coerceAtLeast(0)
                        val colors = listOf("RED", "BLUE", "BLACK", "YELLOW")
                        val drawnTile = OkeyTile(Random.nextInt(1, 14), colors.random())
                        val updated = userHand.toMutableList()
                        updated.add(drawnTile)
                        userHand = updated
                        userHasDrawn = true
                        gameStatusText = "Taş çektiniz! Atılacak taşı seçip 'Taş At' butonuna dokunun."
                    }
                },
                enabled = activeTurn == 0 && !userHasDrawn,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(LanguageManager.getString("okey_draw"))
            }

            Button(
                onClick = {
                    if (activeTurn == 0 && userHasDrawn && selectedHandIndex != -1) {
                        val discarded = userHand[selectedHandIndex]
                        tableDiscardPile = discarded

                        val updated = userHand.toMutableList()
                        updated.removeAt(selectedHandIndex)
                        userHand = updated

                        selectedHandIndex = -1
                        userHasDrawn = false

                        // Check Win conditions (Simulation verification)
                        val randWin = Random.nextFloat() < 0.15f
                        if (randWin || userHand.size <= 13) {
                            userWinningState = true
                        }

                        // Hand off to Bot 1
                        activeTurn = 1
                    }
                },
                enabled = activeTurn == 0 && userHasDrawn && selectedHandIndex != -1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(LanguageManager.getString("okey_discard"))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Hand (Racks / Istaka)
        Text(
            text = "ISTAKANIZ // YOUR RACK",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Hand tiles list rendered on double-deck rack style
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(userHand) { index, tile ->
                val isSelected = selectedHandIndex == index
                Box(
                    modifier = Modifier
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor else Color(0xFFFFF7EC))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color(0xFFD7CCC8),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedHandIndex = if (isSelected) -1 else index
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${tile.number}",
                            color = if (isSelected) Color.White else tile.getComposeColor(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tile.color,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.DarkGray,
                            fontWeight = FontWeight.Normal,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (userWinningState) {
            AlertDialog(
                onDismissRequest = { userWinningState = false },
                title = { Text(text = "Eliniz Okey!") },
                text = { Text(text = LanguageManager.getString("okey_win")) },
                confirmButton = {
                    TextButton(onClick = {
                        userWinningState = false
                        startNewOkeyGame()
                    }) {
                        Text(LanguageManager.getString("okey_restart"))
                    }
                }
            )
        }
    }
}
