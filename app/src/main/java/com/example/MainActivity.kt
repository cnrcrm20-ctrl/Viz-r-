package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LanguageManager
import com.example.model.ThemeSettingsManager
import com.example.model.ChatEngine
import com.example.ui.VizorApp
import com.example.util.PermissionsHelper

class MainActivity : ComponentActivity() {

    // Permission launcher request callbacks
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val cameraGranted = result[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = result[android.Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && audioGranted) {
            Toast.makeText(this, "Tüm izinler başarıyla verildi! / All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bazı izinler reddedildi! / Some permissions denied!", Toast.LENGTH_LONG).show()
        }
        // Force state recomposition
        permissionsCheckTrigger++
    }

    private var permissionsCheckTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Load localized preferences
        LanguageManager.loadLanguage(this)

        // Initialize Firebase SDK smoothly
        ChatEngine.initializeFirebase(this)

        // 2. Load custom themes
        ThemeSettingsManager.loadTheme(this)

        // Support full bleed edge-to-edge content
        enableEdgeToEdge()

        // Create notification channels
        com.example.util.NotificationManagerHelper.createChannels(this)

        setContent {
            // Re-execute check whenever permissionsCheckTrigger increments
            val key = permissionsCheckTrigger
            val isPermissionsGranted = remember(key) {
                PermissionsHelper.areAllPermissionsGranted(this)
            }

            val themeState by ThemeSettingsManager.themeFlow.collectAsState()
            val primaryColor = themeState.getPrimary()
            val canvasBgColor = themeState.getCanvasBg()
            val textColor = themeState.getTextColor()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = canvasBgColor
            ) {
                if (isPermissionsGranted) {
                    // Start main app
                    VizorApp(context = this)
                } else {
                    // Show a custom permission helper screen
                    PermissionsExplanationScreen(
                        primaryColor = primaryColor,
                        canvasBgColor = canvasBgColor,
                        textColor = textColor,
                        onGrantClick = {
                            permissionLauncher.launch(PermissionsHelper.requiredPermissions)
                        },
                        onSettingsClick = {
                            PermissionsHelper.openAppSettings(this)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionsExplanationScreen(
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = "Security Guard",
            tint = primaryColor,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = LanguageManager.getString("permissions"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = canvasBgColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = LanguageManager.getString("perm_explain"),
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Detail bullets
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "bullet", tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = LanguageManager.getString("camera_perm"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "bullet", tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = LanguageManager.getString("audio_perm"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "bullet", tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Network & Media Access Permissions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = LanguageManager.getString("grant_perm"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSettingsClick) {
            Text(
                text = LanguageManager.getString("go_to_settings"),
                fontSize = 14.sp,
                color = primaryColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
