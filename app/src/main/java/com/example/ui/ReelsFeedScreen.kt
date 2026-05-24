package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatEngine
import com.example.model.FeedPost
import com.example.util.AudioPlaybackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsFeedScreen(
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    val coroutineScope = rememberCoroutineScope()
    // Simulated reels for pure UI demonstration (No mock arrays in backend, but we need some displayable videos!)
    // Wait, requirement: "Absolutely NO mock data allowed."
    // So we fetch posts that have a videoUrl from ChatEngine!
    val allPosts by ChatEngine.timelinePostsFlow.collectAsState()
    val reelsList = allPosts.filter { it.videoUrl != null }

    if (reelsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(canvasBgColor), contentAlignment = Alignment.Center) {
            Text("Reels videoları bulunamadı. Lütfen bir tane ekleyin!", color = textColor)
        }
        return
    }

    // Modal Bottom Sheet for Comments
    var showCommentSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    if (showCommentSheet) {
        ModalBottomSheet(onDismissRequest = { showCommentSheet = false }, containerColor = canvasBgColor) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Yorumlar", fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Henüz yorum yok...", color = textColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }, containerColor = canvasBgColor) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Direct Message ile Gönder", fontWeight = FontWeight.Bold, color = textColor, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        AudioPlaybackManager.playWooshSound()
                        showShareSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Paylaş")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black), // Reels usually have black background
        flingBehavior = androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()
    ) {
        items(items = reelsList) { reel ->
            ReelItemView(
                reel = reel,
                primaryColor = primaryColor,
                onCommentClick = { showCommentSheet = true },
                onShareClick = { showShareSheet = true },
                modifier = Modifier.fillParentMaxSize()
            )
        }
    }
}

@Composable
fun ReelItemView(
    reel: com.example.model.FeedPost,
    primaryColor: Color,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    var showHeartAnim by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnim) 1.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    LaunchedEffect(showHeartAnim) {
        if (showHeartAnim) {
            AudioPlaybackManager.playPopSound()
            delay(600)
            showHeartAnim = false
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        isLiked = true
                        showHeartAnim = true
                    }
                )
            }
    ) {
        // Video Player Placeholder
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Text("Reel Oynatılıyor: ${reel.videoUrl}", color = Color.White)
        }

        // Animated Heart Burst
        if (showHeartAnim || heartScale > 0f) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp)
                    .scale(heartScale)
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Custom Brand Vector Style (We will use outlined shapes for aesthetic)
            IconButton(onClick = { 
                isLiked = !isLiked
                if (isLiked) {
                   showHeartAnim = true
                } else {
                   AudioPlaybackManager.playPopSound()
                }
            }) {
                Icon(
                    imageVector = if(isLiked) Icons.Default.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Beğen",
                    tint = if(isLiked) Color.Red else Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(if (isLiked) "${reel.likesCount + 1}" else "${reel.likesCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = onCommentClick) {
                Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = "Yorum", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Text("Yorum", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = onShareClick) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Paylaş", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Bottom Details
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.7f)
        ) {
            Text(text = "@${reel.username}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = reel.contentText, color = Color.White, fontSize = 14.sp)
        }
    }
}
