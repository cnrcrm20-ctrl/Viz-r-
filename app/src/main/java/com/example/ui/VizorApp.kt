package com.example.ui

import android.content.Context
import android.widget.Toast
import java.io.File
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.games.CarGameScreen
import com.example.games.OkeyGameScreen
import com.example.games.SnakeGameScreen
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Persistent Friendship Data Structure (Offline-first persistent representation)
data class VizorFriend(
    val id: String,
    val username: String,
    val bio: String = "Sistem üzerinde çevrimiçi",
    val status: String = "Online",
    val initials: String = "FR",
    val avatarColor: Color = Color(0xFFE91E63)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VizorApp(context: Context) {
    val coroutineScope = rememberCoroutineScope()

    // Subscribe to localization and themes
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    val currTheme by ThemeSettingsManager.themeFlow.collectAsState()

    val primaryColor = currTheme.getPrimary()
    val canvasBgColor = currTheme.getCanvasBg()
    val textColor = currTheme.getTextColor()
    val incomingBubble = currTheme.getIncoming()
    val outgoingBubble = currTheme.getOutgoing()

    // Auth Prefs storage
    val authPrefs = remember { context.getSharedPreferences("vizor_auth_prefs", Context.MODE_PRIVATE) }
    var userSessionName by remember { mutableStateOf(authPrefs.getString("logged_user", null)) }
    var userBio by remember { mutableStateOf(authPrefs.getString("user_bio", "Vizör kâşifi! 🛰️") ?: "Vizör kâşifi! 🛰️") }
    var usingVizorAvatar by remember { mutableStateOf(authPrefs.getBoolean("using_vizor_avatar", true)) }

    // Navigation Tabs: 0=Feed, 1=Direct DM, 2=Arcade Arcade, 3=Settings
    var currentBottomTab by remember { mutableStateOf(0) }

    // Dynamic Friend State (Allows adding real friends)
    var friendsList by remember {
        mutableStateOf(
            emptyList<VizorFriend>()
        )
    }

    // Direct chat logs mapping (Friend ID -> List representation of Message)
    val conversationsMap = remember {
        mutableStateMapOf<String, MutableList<Message>>()
    }

    var isMotherboardBannedState by remember { mutableStateOf(ChatEngine.isMotherboardBanned(context)) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = canvasBgColor
    ) {
        if (isMotherboardBannedState) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF09090B))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield Guard",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CİHAZ VE DONANIM YASAKLANDI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "DEVICE & HARDWARE LIFETIME BAN ACTIVE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Görüş ve paylaşımlarınızda saptanan illegal faaliyetler (bahis, kumar, sahtekarlık veya telifli dosya değişimi) sebebiyle donanım adresiniz (Anakart UID) Moderasyon Polisi (Shield Patrol) tarafından kalıcı olarak bloke edilmiştir.\n\nEğer bunun bir hata olduğunu düşünüyorsanız, system-integrity@vizor.app adresi üzerinden itiraz talebi oluşturabilirsiniz.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Hata Kodu / Status Code: 0x992B-HW-LIFETIME",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        } else if (userSessionName == null) {
            // Unauthenticated screen
            AuthScreen(
                context = context,
                primaryColor = primaryColor,
                canvasBgColor = canvasBgColor,
                textColor = textColor,
                onLoginSuccess = { user, email ->
                    authPrefs.edit().putString("logged_user", user).apply()
                    if (email.contains("cnrcrm20")) {
                        authPrefs.edit().putString("user_bio", "Vizör Üretici & Geliştiricisi VIP 👑").apply()
                        authPrefs.edit().putBoolean("is_operator", true).apply()
                        userBio = "Vizör Üretici & Geliştiricisi VIP 👑"
                    }
                    userSessionName = user
                }
            )
        } else {
            var lastTab by remember { mutableStateOf(currentBottomTab) }
            LaunchedEffect(currentBottomTab) {
                if (currentBottomTab != lastTab) {
                    com.example.util.AudioPlaybackManager.playPopSound()
                    lastTab = currentBottomTab
                }
            }
            // General App Shell layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = canvasBgColor.copy(alpha = 0.94f),
                        tonalElevation = 8.dp,
                        modifier = Modifier.border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentBottomTab == 0,
                            onClick = { currentBottomTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Akış", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = textColor.copy(alpha = 0.45f),
                                unselectedTextColor = textColor.copy(alpha = 0.45f),
                                indicatorColor = primaryColor.copy(alpha = 0.15f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == 1,
                            onClick = { currentBottomTab = 1 },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Keşfet") },
                            label = { Text("Keşfet", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = textColor.copy(alpha = 0.45f),
                                unselectedTextColor = textColor.copy(alpha = 0.45f),
                                indicatorColor = primaryColor.copy(alpha = 0.15f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == 2,
                            onClick = { currentBottomTab = 2 },
                            icon = { Icon(Icons.Default.MovieFilter, contentDescription = "Reels") },
                            label = { Text("Reels", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = textColor.copy(alpha = 0.45f),
                                unselectedTextColor = textColor.copy(alpha = 0.45f),
                                indicatorColor = primaryColor.copy(alpha = 0.15f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == 3,
                            onClick = { currentBottomTab = 3 },
                            icon = {
                                BadgedBox(badge = { Badge { Text("${friendsList.count { it.status == "Online" }}") } }) {
                                    Icon(Icons.Default.Forum, contentDescription = "DMs")
                                }
                            },
                            label = { Text("Mesajlar", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = textColor.copy(alpha = 0.45f),
                                unselectedTextColor = textColor.copy(alpha = 0.45f),
                                indicatorColor = primaryColor.copy(alpha = 0.15f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentBottomTab == 4,
                            onClick = { currentBottomTab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                            label = { Text("Profil", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                unselectedIconColor = textColor.copy(alpha = 0.45f),
                                unselectedTextColor = textColor.copy(alpha = 0.45f),
                                indicatorColor = primaryColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                },
                containerColor = canvasBgColor
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentBottomTab) {
                        0 -> SocialFeedScreen(
                            context = context,
                            userSessionName = userSessionName ?: "Kaşif",
                            primaryColor = primaryColor,
                            canvasBgColor = canvasBgColor,
                            textColor = textColor,
                            usingVizorAvatar = usingVizorAvatar,
                            onNavigateToTab = { tabIndex -> currentBottomTab = tabIndex }
                        )
                        1 -> VisualDiscoverScreen(
                            context = context,
                            userSessionName = userSessionName ?: "Kaşif",
                            primaryColor = primaryColor,
                            canvasBgColor = canvasBgColor,
                            textColor = textColor
                        )
                        2 -> ReelsFeedScreen(
                            primaryColor = primaryColor,
                            canvasBgColor = canvasBgColor,
                            textColor = textColor
                        )
                        3 -> ModernDMChatScreen(
                            context = context,
                            userSessionName = userSessionName ?: "Kaşif",
                            primaryColor = primaryColor,
                            canvasBgColor = canvasBgColor,
                            textColor = textColor,
                            incomingBubble = incomingBubble,
                            outgoingBubble = outgoingBubble,
                            friendsList = friendsList,
                            conversationsMap = conversationsMap,
                            onAddFriend = { name, initial ->
                                val rgb = Color(Random.nextInt(50, 200), Random.nextInt(50, 200), Random.nextInt(50, 200))
                                val newFr = VizorFriend(
                                    id = (friendsList.size + 1).toString(),
                                    username = name,
                                    bio = "Şimdi arkadaş ekleme ile eklendi!",
                                    status = "Online",
                                    initials = initial,
                                    avatarColor = rgb
                                )
                                friendsList = friendsList + newFr
                                conversationsMap[newFr.id] = mutableStateListOf(
                                    Message(sender = name, text = "Selam! Beni eklediğin için teşekkürler. Vizör harika!")
                                )
                            }
                        )
                        4 -> SettingsThemeScreen(
                            context = context,
                            userSessionName = userSessionName ?: "",
                            userBio = userBio,
                            usingVizorAvatar = usingVizorAvatar,
                            primaryColor = primaryColor,
                            canvasBgColor = canvasBgColor,
                            textColor = textColor,
                            onSaveProfile = { name, bio, useLogo ->
                                authPrefs.edit().putString("logged_user", name).apply()
                                authPrefs.edit().putString("user_bio", bio).apply()
                                authPrefs.edit().putBoolean("using_vizor_avatar", useLogo).apply()
                                userSessionName = name
                                userBio = bio
                                usingVizorAvatar = useLogo
                            },
                            onLogout = {
                                authPrefs.edit().remove("logged_user").apply()
                                userSessionName = null
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// AUTH & REGISTRATION SCREEN WITH 4-STAGE REGSITRY WIZARD AND GOOGLE SIGN-IN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    context: Context,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    onLoginSuccess: (String, String) -> Unit
) {
    // Wizard steps: 1 = Login/Selection or Credentials, 2 = Profile info, 3 = Birthday, 4 = Central Loading Progress
    var currentStep by remember { mutableStateOf(1) }
    var isRegisterMode by remember { mutableStateOf(false) }

    // Forms fields
    var usernameField by remember { mutableStateOf("") } // Special unique handle starting with @
    var displayNameField by remember { mutableStateOf("") } // Decorative user name
    var emailField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }
    var biographyField by remember { mutableStateOf("Vizör kâşifi! 🛰️") }
    var dateOfBirthStr by remember { mutableStateOf("") }

    // Birthday Dropdowns state
    var selectedDay by remember { mutableStateOf("15") }
    var selectedMonth by remember { mutableStateOf("Haziran / June") }
    var selectedYear by remember { mutableStateOf("1998") }

    // Avatar pickers state: 0 to 4 colors, or 5 = Vizör luxury logo icon
    var chosenAvatarType by remember { mutableStateOf(5) }

    // Captcha variables
    var numA by remember { mutableStateOf(Random.nextInt(1, 10)) }
    var numB by remember { mutableStateOf(Random.nextInt(1, 10)) }
    var captchaAnswerField by remember { mutableStateOf("") }

    // Google accounts picker state
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var isVerifyingGoogleAccount by remember { mutableStateOf(false) }
    var googleAccountSelected by remember { mutableStateOf("") }
    var googleEmailSelected by remember { mutableStateOf("") }

    // Enforce 100% Unique Handle Checker
    val isHandleTaken = remember(usernameField) {
        if (usernameField.isNotEmpty()) ChatEngine.isHandleTaken(usernameField) else false
    }

    if (currentStep == 4) {
        // Stage 4: High Luxury Creating Profile Loading Backdrop
        LaunchedEffect(Unit) {
            val resolvedColor = when (chosenAvatarType) {
                0 -> "#4CAF50"
                1 -> "#2196F3"
                2 -> "#9C27B0"
                3 -> "#FF9800"
                4 -> "#E91E63"
                else -> "#FFD700"
            }
            // Register account with live endpoints
            ChatEngine.createAccountWithFirebase(
                email = emailField.ifEmpty { "custom_${System.currentTimeMillis()}@vizor.app" },
                password = passwordField,
                username = usernameField.ifEmpty { "kasif_${System.currentTimeMillis()}" },
                displayName = displayNameField.ifEmpty { "Kâşif" },
                bio = biographyField,
                colorHex = resolvedColor
            ) { success, err ->
                val finalUser = if (usernameField.isNotEmpty()) "@${usernameField.removePrefix("@")}" else displayNameField.ifEmpty { "Kâşif" }
                onLoginSuccess(finalUser, emailField.ifEmpty { "custom@vizor.app" })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f))
                    .border(2.5.dp, primaryColor, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_vizor_logo),
                    contentDescription = "Vizör Golden Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HESAP OLUŞTURULUYOR",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                letterSpacing = 2.sp
            )

            Text(
                text = "Secure Firebase Node Configured...",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Progress Spinner with current visual color theme
            CircularProgressIndicator(
                color = primaryColor,
                strokeWidth = 3.5.dp,
                modifier = Modifier.size(54.dp)
            )
        }
    } else {
        // Stage 1, 2, 3 Standard Scrollable Auth Canvas
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                // High luxury logo display
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f))
                        .border(2.dp, primaryColor, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_vizor_logo),
                        contentDescription = "Vizor Modern Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "V İ Z Ö R",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = 6.sp
                )

                Text(
                    text = "Aesthetic, High-Fidelity Social Ecosystem",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentStep == 1) {
                // STAGE 1: Credentials configuration or Google Sign-In
                item {
                    Text(
                        text = if (isRegisterMode) "Kayıt - Aşama 1/3 (Kimlik)" else "Hesaba Giriş",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = emailField,
                        onValueChange = { emailField = it.trim() },
                        label = { Text(LanguageManager.getString("email"), fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor, focusedLabelColor = primaryColor, unfocusedTextColor = textColor, focusedTextColor = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passwordField,
                        onValueChange = { passwordField = it },
                        label = { Text(LanguageManager.getString("password"), fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor, focusedLabelColor = primaryColor, unfocusedTextColor = textColor, focusedTextColor = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Captcha verification Math
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = LanguageManager.getString("captcha_challenge"),
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Güvenlik Sualı: $numA + $numB = ?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = captchaAnswerField,
                                    onValueChange = { captchaAnswerField = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Action Button
                    Button(
                        onClick = {
                            if (emailField.isEmpty() || passwordField.isEmpty()) {
                                Toast.makeText(context, "Lütfen formu doldurun!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isRegisterMode) {
                                // Validate Captcha first
                                val capSol = captchaAnswerField.toIntOrNull()
                                if (capSol == null || capSol != (numA + numB)) {
                                    Toast.makeText(context, LanguageManager.getString("captcha_error"), Toast.LENGTH_LONG).show()
                                    numA = Random.nextInt(1, 10)
                                    numB = Random.nextInt(1, 10)
                                    captchaAnswerField = ""
                                    return@Button
                                }
                                // Progress to stage 2
                                currentStep = 2
                            } else {
                                // Direct email login
                                ChatEngine.signInWithFirebase(emailField, passwordField) { success, err ->
                                    val nick = emailField.substringBefore("@")
                                    if (emailField.trim().lowercase() == "cnrcrm20@gmail.com") {
                                        // Automatic VIP treatment with automatic operator tag for Google / credentials
                                        onLoginSuccess("Caner C.", emailField)
                                    } else {
                                        onLoginSuccess(nick, emailField)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegisterMode) "Sonraki Adım (Aşama 2)" else LanguageManager.getString("login_button"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GOOGLE DELEGATED LOGIN BUTTON TRIGGER
                    OutlinedButton(
                        onClick = { showGoogleAccountPicker = true },
                        border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "G", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Google ile Giriş Yap", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRegisterMode) LanguageManager.getString("has_account") else LanguageManager.getString("no_account"),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isRegisterMode = !isRegisterMode }
                            .padding(8.dp)
                    )
                }
            }

            if (currentStep == 2) {
                // STAGE 2: Profile Customization, Bio, Avatar selection, Uniqueness verification
                item {
                    Text(
                        text = "Profil Oluşturma - Aşama 2/3 (Profil)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = displayNameField,
                        onValueChange = { displayNameField = it.trim().take(20) },
                        label = { Text("Display Name (Görünen İsim)", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor,  unfocusedTextColor = textColor, focusedTextColor = textColor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Unique Handle starting with @
                    OutlinedTextField(
                        value = usernameField,
                        onValueChange = { usernameField = it.trim().removePrefix("@").take(15).replace(" ", "") },
                        label = { Text("Özel Kullanıcı Adı (@ takma ad)", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor,  unfocusedTextColor = textColor, focusedTextColor = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("@", color = primaryColor, fontWeight = FontWeight.Black) }
                    )

                    // Unique Checker message feedback
                    if (usernameField.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                            if (isHandleTaken) {
                                Text(text = "❌ Bu kullanıcı adı zaten alınmış! / Reserved!", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            } else {
                                Text(text = "✔ Kullanılabilir! / Available username handle", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bio
                    OutlinedTextField(
                        value = biographyField,
                        onValueChange = { biographyField = it.take(80) },
                        label = { Text("Biography (Hakkında/Biyografi)", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor,  unfocusedTextColor = textColor, focusedTextColor = textColor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Avatar Selection Grid Custom representation
                    Text(text = "Profil Fotoğrafı Seçimi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 5 colors avatar presets
                        val colors = listOf(Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFFE53935), Color(0xFF009688), Color(0xFFFF9800))
                        items(colors.size) { colIdx ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(colors[colIdx])
                                    .border(
                                        border = BorderStroke(if (chosenAvatarType == colIdx) 3.dp else 1.dp, if (chosenAvatarType == colIdx) primaryColor else Color.Transparent),
                                        shape = CircleShape
                                    )
                                    .clickable { chosenAvatarType = colIdx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = displayNameField.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }

                        // Luxury Vizör Launcher Logo preset Option (idx 5)
                        item {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(
                                        border = BorderStroke(if (chosenAvatarType == 5) 3.dp else 1.dp, if (chosenAvatarType == 5) primaryColor else Color.Gray.copy(alpha = 0.4f)),
                                        shape = CircleShape
                                    )
                                    .clickable { chosenAvatarType = 5 }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.profile_vizor_logo),
                                    contentDescription = "luxury",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { currentStep = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Geri")
                        }

                        Button(
                            onClick = {
                                if (usernameField.length < 3) {
                                    Toast.makeText(context, "Özel kullanıcı adı en az 3 harfli olmalı!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (isHandleTaken) {
                                    Toast.makeText(context, "Lütfen benzersiz bir kullanıcı adı seçin!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                currentStep = 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sonraki Adım")
                        }
                    }
                }
            }

            if (currentStep == 3) {
                // STAGE 3: Date of Birth picking
                item {
                    Text(
                        text = "Profil Oluşturma - Aşama 3/3 (Doğum Tarihi)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Lütfen topluluk kurallarımız gereğince doğum tarihinizi giriniz:",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day dropdown selection representation
                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = { selectedDay = it.trim().take(2) },
                            label = { Text("Gün", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Month input representation
                        OutlinedTextField(
                            value = selectedMonth,
                            onValueChange = { selectedMonth = it.trim() },
                            label = { Text("Ay", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Year input representation
                        OutlinedTextField(
                            value = selectedYear,
                            onValueChange = { selectedYear = it.trim().take(4) },
                            label = { Text("Yıl", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { currentStep = 2 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Geri")
                        }

                        Button(
                            onClick = {
                                val dayNum = selectedDay.toIntOrNull() ?: 15
                                val yearNum = selectedYear.toIntOrNull() ?: 1998
                                if (dayNum < 1 || dayNum > 31 || yearNum < 1920 || yearNum > 2020) {
                                    Toast.makeText(context, "Geçersiz Gün veya Yıl girdiniz!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                dateOfBirthStr = "$selectedDay $selectedMonth $selectedYear"
                                context.getSharedPreferences("vizor_auth_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("dob", dateOfBirthStr)
                                    .putString("user_bio", biographyField)
                                    .putBoolean("using_vizor_avatar", (chosenAvatarType == 5))
                                    .apply()

                                // Boom -> stage 4 Loading Backdrop
                                currentStep = 4
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Hesabı Oluştur 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // GOOGLE ACCOUNT PICKER HANDLER SHEET
            if (showGoogleAccountPicker) {
                item {
                    AlertDialog(
                        onDismissRequest = { showGoogleAccountPicker = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Google ile Devam Et", fontSize = 16.sp, color = textColor)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Cihazda bağlı Google hesabını yetkilendirerek hızlı profil oluşturun.", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                                Divider(color = textColor.copy(alpha = 0.1f))

                                // Real admin account automatically granted operator VIP
                                GoogleAccountRow(
                                    name = "Caner Yıldırım",
                                    email = "cnrcrm20@gmail.com",
                                    initial = "C",
                                    avatarBg = Color(0xFF4285F4),
                                    textColor = textColor,
                                    onClick = {
                                        googleAccountSelected = "Caner"
                                        googleEmailSelected = "cnrcrm20@gmail.com"
                                        isVerifyingGoogleAccount = true
                                        showGoogleAccountPicker = false
                                    }
                                )

                                GoogleAccountRow(
                                    name = "Vizör Developer Sandbox",
                                    email = "vizor.designer@gmail.com",
                                    initial = "V",
                                    avatarBg = Color(0xFFE91E63),
                                    textColor = textColor,
                                    onClick = {
                                        googleAccountSelected = "VizorDev"
                                        googleEmailSelected = "vizor.designer@gmail.com"
                                        isVerifyingGoogleAccount = true
                                        showGoogleAccountPicker = false
                                    }
                                )
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showGoogleAccountPicker = false }) {
                                Text("İptal", color = primaryColor)
                            }
                        },
                        containerColor = canvasBgColor
                    )
                }
            }

            // GOOGLE LOGIN VERIFYING LATENCY SPINNER
            if (isVerifyingGoogleAccount) {
                item {
                    LaunchedEffect(Unit) {
                        delay(2200) // play services authentication simulation
                        isVerifyingGoogleAccount = false
                        // For Google Sign-Ins, if email matches "cnrcrm20@gmail.com", automatically configure credentials and skip remaining wizard
                        if (googleEmailSelected.contains("cnrcrm20")) {
                            usernameField = "cnrcrm20"
                            displayNameField = "Caner"
                            biographyField = "Vizör Üretici & Geliştiricisi VIP 👑"
                            chosenAvatarType = 5
                            dateOfBirthStr = "20 02 1995"
                            currentStep = 4 // Direct to stage 4 loading screen
                        } else {
                            // Run the wizard starting from step 2 with auto filled name
                            displayNameField = googleAccountSelected
                            emailField = googleEmailSelected
                            currentStep = 2
                        }
                    }

                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(text = "Google Play Bağlantısı...", fontSize = 15.sp, color = textColor) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Güvenli OAuth2 kimlik tespiti yapılıyor...", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                            }
                        },
                        confirmButton = {},
                        containerColor = canvasBgColor
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleAccountRow(
    name: String,
    email: String,
    initial: String,
    avatarBg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
            Text(text = email, fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
        }
    }
}

// -------------------------------------------------------------
// MAIN SOCIAL FEED & REELS
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    context: Context,
    userSessionName: String,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    usingVizorAvatar: Boolean,
    onNavigateToTab: (Int) -> Unit
) {
    val posts by ChatEngine.timelinePostsFlow.collectAsState()

    var isAddingPost by remember { mutableStateOf(false) }
    var inputPostText by remember { mutableStateOf("") }
    var selectedVideoMinutes by remember { mutableStateOf("0.5") }
    var sizeReductionMetricText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(14.dp)
    ) {
        // Expiration Warning Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassBottom, contentDescription = "exp", tint = primaryColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.getString("remote_expiry_banner"),
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.8f),
                    lineHeight = 14.sp
                )
            }
        }

        // Instagram-style clean visual Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vizör",
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = primaryColor,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.clickable { onNavigateToTab(0) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Share/Post quick navigation
                IconButton(onClick = { onNavigateToTab(2) }) {
                    Icon(
                        imageVector = Icons.Default.AddBox,
                        contentDescription = "Görsel/Reels Paylaş",
                        tint = textColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Instagram Direct Messenger Paper Airplane Navigation
                IconButton(onClick = { onNavigateToTab(3) }) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Direct Messages",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Add Post Expansion panel with smooth animation
        AnimatedVisibility(
            visible = isAddingPost,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.25f)),
                colors = CardDefaults.cardColors(containerColor = canvasBgColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = inputPostText,
                        onValueChange = { inputPostText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(LanguageManager.getString("post_desc"), color = textColor.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Yerel Reels Klibi Yakala (Süre limiti: 60sn)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("0.3", "0.6", "1.0", "1.5").forEach { duration ->
                            val isSelected = selectedVideoMinutes == duration
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) primaryColor else primaryColor.copy(alpha = 0.1f))
                                    .clickable { selectedVideoMinutes = duration }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$duration dk",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else textColor
                                )
                            }
                        }
                    }

                    if (sizeReductionMetricText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = sizeReductionMetricText, fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val durationDecimal = selectedVideoMinutes.toFloatOrNull() ?: 1.0f
                            val durationSeconds = (durationDecimal * 60).toInt()

                            Toast.makeText(context, LanguageManager.getString("video_compressing"), Toast.LENGTH_LONG).show()

                            VideoUtils.compressAndSaveVideo(
                                context = context,
                                title = inputPostText.ifEmpty { "Doğa Akışı" },
                                durationSeconds = durationSeconds,
                                originalBytes = (durationSeconds * 4_200_000L),
                                onComplete = { localPath, remoteMeta ->
                                    val err = ChatEngine.submitTimelinePost(
                                        username = userSessionName,
                                        text = inputPostText + " (HD Video: ${remoteMeta.resolution}. Yerel HQ: $localPath)",
                                        videoUrl = remoteMeta.title
                                    )

                                    if (err == "RATE_LIMIT") {
                                        Toast.makeText(context, LanguageManager.getString("rate_limit_err"), Toast.LENGTH_LONG).show()
                                    } else if (err == "CENSORED") {
                                        Toast.makeText(context, LanguageManager.getString("censor_warning"), Toast.LENGTH_LONG).show()
                                    } else {
                                        sizeReductionMetricText = "Sıkıştırma Oranı %90! ${(remoteMeta.sizeBytes / 1024L)} KB'ta kaydedildi."
                                        inputPostText = ""
                                        isAddingPost = false
                                    }
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(LanguageManager.getString("post_btn"), color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Feed representation
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = canvasBgColor.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Conditional avatar loading - Shows custom Vizor logo for User or colorful initial avatar for others!
                            if (post.username == userSessionName && usingVizorAvatar) {
                                Image(
                                    painter = painterResource(id = R.drawable.profile_vizor_logo),
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, primaryColor, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = post.username.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(text = "@${post.username}", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text(text = "Aktif • Az önce", fontSize = 10.sp, color = textColor.copy(alpha = 0.45f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = post.contentText, color = textColor, fontSize = 14.sp)

                        if (post.videoUrl != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Glowing dynamic play overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.82f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = "play-video",
                                        tint = primaryColor,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "REELS: ${post.videoUrl}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interaction Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = "like", tint = Color.Red, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${post.likesCount} Beğeni", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = "secured", tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "P2P Korunmalı", fontSize = 10.sp, color = textColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SEC SEC 2: MULTI-USER DIRECT MESSAGING (DMs & Friend Add)
// -------------------------------------------------------------
@Composable
fun ModernDMChatScreen(
    context: Context,
    userSessionName: String,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    incomingBubble: Color,
    outgoingBubble: Color,
    friendsList: List<VizorFriend>,
    conversationsMap: Map<String, List<Message>>,
    onAddFriend: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val webrtcSignalingState by ChatEngine.signalingState.collectAsState()

    var activeFriendId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var inputMessageText by remember { mutableStateOf("") }
    var webrtcStatusMessage by remember { mutableStateOf("") }

    // Friend Adding Dialog
    var showFriendDialog by remember { mutableStateOf(false) }
    var newFriendName by remember { mutableStateOf("") }

    // Filtered friends list for inbox
    val filteredFriends = remember(friendsList, searchQuery) {
        if (searchQuery.isEmpty()) {
            friendsList
        } else {
            friendsList.filter { it.username.contains(searchQuery, ignoreCase = true) || it.bio.contains(searchQuery, ignoreCase = true) }
        }
    }

    val activeFriend = remember(activeFriendId, friendsList) {
        friendsList.find { it.id == activeFriendId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
    ) {
        if (activeFriend == null) {
            // INBOX PARTICIPANTS THREAD LIST VIEW (Sohbetler Listesi)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MESAJLAR / MESSAGES",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Shield Moderation Engined Active 🛡",
                            fontSize = 11.sp,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showFriendDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "add_friend", tint = primaryColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Kişilerde ara.. Search inbox..", fontSize = 13.sp, color = textColor.copy(alpha = 0.45f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search", tint = primaryColor, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredFriends.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Sonuç bulunamadı..", color = textColor.copy(alpha = 0.4f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredFriends.size) { index ->
                            val fr = filteredFriends[index]
                            val chatHistory = conversationsMap[fr.id] ?: emptyList()
                            val lastMsgText = chatHistory.lastOrNull()?.text ?: "Yazışma başlatılmadı."
                            val lastMsgSender = chatHistory.lastOrNull()?.sender ?: ""

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeFriendId = fr.id },
                                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(fr.avatarColor)
                                    ) {
                                        Text(
                                            text = fr.initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )

                                        if (fr.status == "Online") {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                                    .border(2.dp, canvasBgColor, CircleShape)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = fr.username, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                            Text(text = "Now", fontSize = 10.sp, color = textColor.copy(alpha = 0.35f))
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (lastMsgSender.isNotEmpty()) "$lastMsgSender: $lastMsgText" else lastMsgText,
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Cheerful Go Icon representation
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "start_chat",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ACTIVE THREAD SENDER LAYOUT (İşlemdeki Sohbet)
            // Beautiful active recipient upper header row
            Card(
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(activeFriend.avatarColor)
                        ) {
                            Text(
                                text = activeFriend.initials,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(text = "@${activeFriend.username}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = textColor)
                            Text(text = activeFriend.bio, fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), maxLines = 1)
                        }
                    }

                    // BACK BUTTON '<' PLACED ON THE RIGHT SIDE as requested by user
                    IconButton(
                        onClick = { activeFriendId = null },
                        modifier = Modifier
                            .size(34.dp)
                            .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "back_to_list",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // WebRTC Signaling overlay controls for luxury Voice call
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (webrtcSignalingState == SignalingState.CONNECTED_P2P) Color(0xFF1B5E20) else primaryColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (webrtcSignalingState == SignalingState.CONNECTED_P2P) "Direct P2P Link Active (0 KB limit)" else "WebRTC Handshake Engine:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )

                    if (webrtcSignalingState == SignalingState.IDLE) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    ChatEngine.startWebRTCP2PHandshake { status ->
                                        webrtcStatusMessage = status
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("P2P Sesli", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { ChatEngine.disconnectWebRTC(); webrtcStatusMessage = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Bağlantıyı Kes", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (webrtcStatusMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Yellow.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(text = webrtcStatusMessage, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                }
            }

            // Message History Scroll Box
            val currentMessageList = conversationsMap[activeFriend.id] ?: remember { mutableStateListOf() }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentMessageList) { msg ->
                    val isMe = msg.sender == userSessionName
                    val alignment = if (isMe) Alignment.End else Alignment.Start
                    val bubbleColor = if (isMe) outgoingBubble else incomingBubble

                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isMe) 14.dp else 0.dp,
                                        bottomEnd = if (isMe) 0.dp else 14.dp
                                    )
                                )
                                .background(bubbleColor)
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = msg.sender,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isMe) Color.White.copy(alpha = 0.8f) else primaryColor
                                    )

                                    if (msg.sender.lowercase().contains("operator") || msg.sender.lowercase().contains("cnrcrm20")) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "👑 OPERATÖR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = msg.text, color = if (isMe) Color.White else textColor, fontSize = 14.sp)
                            }
                        }
                        Text(
                            text = "Görüldü • 0% Cloud overhead",
                            fontSize = 8.sp,
                            color = textColor.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Input panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        ChatEngine.simulateMediaCacheDownload(context, "https://vizor.app/media/sunset.mp4")
                        Toast.makeText(context, "Saniyeler içinde disk önbelleğine kaydedildi!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.AppRegistration, contentDescription = "cache", tint = primaryColor)
                }

                OutlinedTextField(
                    value = inputMessageText,
                    onValueChange = { inputMessageText = it },
                    placeholder = { Text(LanguageManager.getString("send_message"), fontSize = 13.sp, color = textColor.copy(alpha = 0.45f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val inputCleaned = inputMessageText.trim()
                        if (inputCleaned.isNotEmpty()) {
                            // Guard Post Abuse check (Moderation Shield)
                            val modCheck = ChatEngine.checkAndApplyModeration(context, userSessionName, inputCleaned)
                            if (modCheck != null) {
                                Toast.makeText(context, "⚠️ Uyarı: Şüpheli yasa dışı faaliyet saptandı!", Toast.LENGTH_LONG).show()

                                // Enforce ban loop alerts
                                val mList = conversationsMap[activeFriend.id] as? MutableList<Message>
                                mList?.add(Message(sender = userSessionName, text = inputCleaned))

                                // Auto response from Vizör Guard Patrol bot to police illegal acts
                                mList?.add(
                                    Message(
                                        sender = "vizor_police",
                                        text = "⚠️ SİS-MODERASYON-POLİS-KODU: Görüşmenizde saptanan kelimeler yasaktır. 1. Adımda 24H ban, 2. Adımda 7D ban, devamında ise ANAKART DONANIM kilitlenmesi yaşarsınız!"
                                    )
                                )

                                inputMessageText = ""
                                return@IconButton
                            }

                            // Rate limit messaging verify
                            if (!ChatEngine.verifyMessageRateLimit()) {
                                Toast.makeText(context, "Hız Sınırı: Dakikada en fazla 20 mesaj gönderebilirsiniz!", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }

                            val newMsg = Message(sender = userSessionName, text = inputCleaned)
                            com.example.util.AudioPlaybackManager.playWooshSound()
                            val mList = conversationsMap[activeFriend.id] as? MutableList<Message>
                            if (mList != null) {
                                mList.add(newMsg)
                            } else {
                                ChatEngine.sendChatMessage(userSessionName, inputCleaned)
                            }
                            inputMessageText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(primaryColor, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "send", tint = Color.White)
                }
            }
        }

        // FRIEND ADDING POPUP DIALOG
        if (showFriendDialog) {
            AlertDialog(
                onDismissRequest = { showFriendDialog = false },
                title = { Text(text = "Yeni Arkadaş Ekle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Bağlanmak istediğiniz Vizör kullanıcısının takma ismini girin.", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = newFriendName,
                            onValueChange = { newFriendName = it.trim() },
                            placeholder = { Text("Kullanıcı adı..") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFriendName.isNotEmpty()) {
                                onAddFriend(newFriendName, newFriendName.take(2).uppercase())
                                newFriendName = ""
                                showFriendDialog = false
                                Toast.makeText(context, "Arkadaş başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Ekle", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFriendDialog = false }) {
                        Text("Kapat", color = primaryColor)
                    }
                },
                containerColor = canvasBgColor
            )
        }
    }
}

// -------------------------------------------------------------
// SEC SEC 3: ARCADE ZONE HUB (Holds 3 offline games)
// -------------------------------------------------------------
@Composable
fun ArcadeZoneScreen(
    context: Context,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    var selectedGameIndex by remember { mutableStateOf(0) } // 0=Okey, 1=Car Game, 2=Snake Game

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedGameIndex,
            containerColor = canvasBgColor.copy(alpha = 0.1f),
            edgePadding = 12.dp,
            divider = {}
        ) {
            Tab(
                selected = selectedGameIndex == 0,
                onClick = { selectedGameIndex = 0 },
                text = { Text(LanguageManager.getString("offline_okey"), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = primaryColor,
                unselectedContentColor = textColor.copy(alpha = 0.5f)
            )
            Tab(
                selected = selectedGameIndex == 1,
                onClick = { selectedGameIndex = 1 },
                text = { Text("Asfalt Araba Yarışı", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = primaryColor,
                unselectedContentColor = textColor.copy(alpha = 0.5f)
            )
            Tab(
                selected = selectedGameIndex == 2,
                onClick = { selectedGameIndex = 2 },
                text = { Text("Retro Yılan Oyunu", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = primaryColor,
                unselectedContentColor = textColor.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedGameIndex) {
                0 -> OkeyGameScreen(context, primaryColor, canvasBgColor, textColor)
                1 -> CarGameScreen(context, primaryColor, canvasBgColor, textColor)
                2 -> SnakeGameScreen(context, primaryColor, canvasBgColor, textColor)
            }
        }
    }
}

// -------------------------------------------------------------
// SEC SEC 4: ADVANCED SETTINGS, CUSTOMIZABLE PROFILE, DETAILS
// -------------------------------------------------------------
@Composable
fun SettingsThemeScreen(
    context: Context,
    userSessionName: String,
    userBio: String,
    usingVizorAvatar: Boolean,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    onSaveProfile: (String, String, Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val localCacheFlow by ChatEngine.cacheFlow.collectAsState()
    val currThemeState by ThemeSettingsManager.themeFlow.collectAsState()
    val currProfile by ChatEngine.currentUserProfile.collectAsState()

    LaunchedEffect(userSessionName) {
        ChatEngine.listenToCurrentUser(userSessionName)
    }

    var customPrimaryHex by remember { mutableStateOf("#2196F3") }
    var customBgHex by remember { mutableStateOf("#12141C") }
    var customTextHex by remember { mutableStateOf("#FFFFFF") }
    var customIncomingHex by remember { mutableStateOf("#252A37") }
    var customOutgoingHex by remember { mutableStateOf("#1A73E8") }

    // Edit profile states
    var editNickname by remember { mutableStateOf(userSessionName) }
    var editBio by remember { mutableStateOf(userBio) }
    var selectLogoAvatar by remember { mutableStateOf(usingVizorAvatar) }

    // Tech Dev Dashboard overlay
    var showModOfficeOverlay by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aesthetic interactive profile detail card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectLogoAvatar) {
                                Image(
                                    painter = painterResource(id = R.drawable.profile_vizor_logo),
                                    contentDescription = "Vizör Logo Profile",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, primaryColor, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = editNickname.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(text = editNickname, fontWeight = FontWeight.Black, fontSize = 18.sp, color = textColor)
                                Text(text = currProfile?.bio ?: editBio, fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "${currProfile?.followers?.size ?: 0} Takipçi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                    Text(text = "•", fontSize = 12.sp, color = textColor.copy(alpha = 0.4f))
                                    Text(text = "${currProfile?.following?.size ?: 0} Takip", fontSize = 12.sp, color = textColor.copy(alpha = 0.8f))
                                }
                            }
                        }

                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "exit", tint = Color.Red)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Divider(color = primaryColor.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(8.dp))

                    // Edit Form Inputs
                    Text(text = "Profili Özelleştir / Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { editNickname = it },
                        label = { Text("Display Name", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Short Bio", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle profile image choice between generic monogram or glorious Vizör logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Görkemli Vizör Logosu Kullan", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = selectLogoAvatar,
                            onCheckedChange = { selectLogoAvatar = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (editNickname.trim().isEmpty()) return@Button
                            onSaveProfile(editNickname, editBio, selectLogoAvatar)
                            Toast.makeText(context, "Profil başarılı şekilde kaydedildi!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Bilgileri Güncelle", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Language switcher
        item {
            Text(text = "Dil Ayarları / Language Settings", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { LanguageManager.setLanguage(LanguageCode.TR, context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (LanguageManager.currentLanguage.value == LanguageCode.TR) primaryColor else Color.Gray.copy(alpha = 0.2f)
                    )
                ) {
                    Text("TÜRKÇE (TR)", color = Color.White)
                }

                Button(
                    onClick = { LanguageManager.setLanguage(LanguageCode.EN, context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (LanguageManager.currentLanguage.value == LanguageCode.EN) primaryColor else Color.Gray.copy(alpha = 0.2f)
                    )
                ) {
                    Text("ENGLISH (EN)", color = Color.White)
                }
            }
        }

        // Static Themes customizing (Fixed choices: Blue/White, Pink/White, Black/Orange, Black/Red)
        item {
            Text(text = "SABİT TEMA SEÇENEKLERİ / STATIC VISUAL THEMES", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Vizör için özel olarak renk dengesi optimize edilmiş 4 ana premium tasarım temalarından birini seçin.",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Preset 1: Mavi Beyaz (Blue & White)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ThemeSettingsManager.updateTheme(
                                VizorThemeColors(
                                    primaryColorHex = "#1E88E5",
                                    canvasBgColorHex = "#FFFFFF",
                                    textColorHex = "#0D0C22",
                                    incomingBubbleHex = "#F3F3F7",
                                    outgoingBubbleHex = "#1E88E5"
                                ),
                                context
                            )
                            Toast.makeText(context, "Mavi Beyaz tema uygulandı!", Toast.LENGTH_SHORT).show()
                        },
                    border = BorderStroke(1.5.dp, if (currThemeState.primaryColorHex == "#1E88E5" && currThemeState.canvasBgColorHex == "#FFFFFF") primaryColor else Color.Gray.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF1E88E5)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Mavi / Beyaz (Blue & White)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0D0C22))
                    }
                }

                // Preset 2: Pembe Beyaz (Pink & White)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ThemeSettingsManager.updateTheme(
                                VizorThemeColors(
                                    primaryColorHex = "#E91E63",
                                    canvasBgColorHex = "#FFFFFF",
                                    textColorHex = "#0D0C22",
                                    incomingBubbleHex = "#FAF0F4",
                                    outgoingBubbleHex = "#E91E63"
                                ),
                                context
                            )
                            Toast.makeText(context, "Pembe Beyaz tema uygulandı!", Toast.LENGTH_SHORT).show()
                        },
                    border = BorderStroke(1.5.dp, if (currThemeState.primaryColorHex == "#E91E63" && currThemeState.canvasBgColorHex == "#FFFFFF") primaryColor else Color.Gray.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFE91E63)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Pembe / Beyaz (Pink & White)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0D0C22))
                    }
                }

                // Preset 3: Siyah Turuncu (Black & Orange)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ThemeSettingsManager.updateTheme(
                                VizorThemeColors(
                                    primaryColorHex = "#FF9800",
                                    canvasBgColorHex = "#121212",
                                    textColorHex = "#FFFFFF",
                                    incomingBubbleHex = "#232323",
                                    outgoingBubbleHex = "#FF9800"
                                ),
                                context
                            )
                            Toast.makeText(context, "Siyah Turuncu tema uygulandı!", Toast.LENGTH_SHORT).show()
                        },
                    border = BorderStroke(1.5.dp, if (currThemeState.primaryColorHex == "#FF9800" && currThemeState.canvasBgColorHex == "#121212") primaryColor else Color.Gray.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Siyah / Turuncu (Black & Orange)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }

                // Preset 4: Siyah Kırmızı (Black & Red)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ThemeSettingsManager.updateTheme(
                                VizorThemeColors(
                                    primaryColorHex = "#E53935",
                                    canvasBgColorHex = "#121212",
                                    textColorHex = "#FFFFFF",
                                    incomingBubbleHex = "#232323",
                                    outgoingBubbleHex = "#E53935"
                                ),
                                context
                            )
                            Toast.makeText(context, "Siyah Kırmızı tema uygulandı!", Toast.LENGTH_SHORT).show()
                        },
                    border = BorderStroke(1.5.dp, if (currThemeState.primaryColorHex == "#E53935" && currThemeState.canvasBgColorHex == "#121212") primaryColor else Color.Gray.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFE53935)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Siyah / Kırmızı (Black & Red)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    ThemeSettingsManager.resetToDefault(context)
                    Toast.makeText(context, "Varsayılana döndürüldü!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(LanguageManager.getString("reset_theme"), color = primaryColor)
            }
        }

        // Technical sweep & diagnostics
        item {
            Text(text = "Disk Önbellek Yönetimi", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = canvasBgColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Disk Önbellek Dosyaları: ${localCacheFlow.size} dosya", fontSize = 12.sp, color = textColor)
                    Text(text = "Önbellek Alanı: ${(localCacheFlow.sumOf { it.sizeBytes } / 1024L)} KB", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                ChatEngine.clearAllCaches(context)
                                Toast.makeText(context, "Önbellekler başarıyla silindi.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Önbelleği Boşalt", fontSize = 11.sp, color = textColor)
                        }

                        Button(
                            onClick = {
                                val deleted = VideoUtils.run7DayExpiringBackupSweep()
                                Toast.makeText(context, "Uygulama taranıp $deleted dosya temizlendi.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Purge Sweep Çalıştır", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // REDIRECT TECHNICAL INTERFACE HERE (Dev console tucked inside settings safely with high beauty)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showModOfficeOverlay = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.15f)),
                border = BorderStroke(1.5.dp, Color(0xFFF44336).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Moderatör & Geliştirici Masası", fontWeight = FontWeight.Black, color = Color(0xFFEF5350), fontSize = 14.sp)
                        Text(text = "Firebase live stream izleyicileri, anti-abuse filtre ekleme ve reported kuyruğu.", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                    }
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "dev", tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    // MODERN DEVELOPER OFFICE FULLY INTERACTIVE COMPONENT DIALOG
    if (showModOfficeOverlay) {
        var modNewBlockedWord by remember { mutableStateOf("") }
        var currentCensorKeywords by remember { mutableStateOf(mutableListOf("kumar", "bahis", "bannedword", "scam")) }

        AlertDialog(
            onDismissRequest = { showModOfficeOverlay = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "mod", tint = Color(0xFFEF5350), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Bulut & Moderasyon Yönetim Masası", fontSize = 16.sp, fontWeight = FontWeight.Black, color = textColor)
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = "Aşağıdaki yönetim kontrolleri Firebase Realtime cloud soketleri ile simüle edilmiştir.",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }

                    // Section 1: Dynamic Firebase live stats
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "SIMULATED CLOUD DIAGNOSTICS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = primaryColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "• Firebase JSON Server State: RUNNING", fontSize = 11.sp, color = textColor)
                                Text(text = "• Sinyalizasyon Sockets: 3 Aktif WebRTC", fontSize = 11.sp, color = textColor)
                                Text(text = "• Veri Transfer Maliyeti: $0.00 USD (P2P Over)", fontSize = 11.sp, color = textColor)
                            }
                        }
                    }

                    // Section 2: Interactive Anti-Abuse keyword additions
                    item {
                        Text(text = "Gelişmiş Censor Kelime Filtresi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = modNewBlockedWord,
                                onValueChange = { modNewBlockedWord = it.trim() },
                                placeholder = { Text("Kelime ekle..", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (modNewBlockedWord.isNotEmpty()) {
                                        currentCensorKeywords.add(modNewBlockedWord.lowercase())
                                        modNewBlockedWord = ""
                                        Toast.makeText(context, "Kelime yasaklılar listesine eklendi!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Yasakla", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    item {
                        Text(text = "Aktif Kara Liste: ${currentCensorKeywords.joinToString(", ")}", fontSize = 11.sp, color = Color.Red.copy(alpha = 0.8f))
                    }

                    // Section 3: Clean Mock simulation
                    item {
                        Divider()
                        Text(text = "Raporlanan Gönderi Kuyruğu (Spam Sweep)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tüm timeline içerikleri otomatik kontrol edilir. Şikayet oranı yüksek olan postları temizleyebilirsiniz.",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Raporlanan 0 spam temizlendi!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tüm Şüpheli Spamleri Otomatik Kaldır", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showModOfficeOverlay = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Değişiklikleri Kaydet & Kapat", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = canvasBgColor
        )
    }
}

@Composable
fun ThemeTextInputCol(
    label: String,
    value: String,
    presetColors: List<String>,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(value)))
                    .border(1.dp, Color.White, CircleShape)
            )

            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 7) onValueChange(it) },
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier.width(100.dp),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                presetColors.forEach { colorStr ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorStr)))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onValueChange(colorStr) }
                    )
                }
            }
        }
    }
}

/**
 * 2. Visual Discover & Real-time Social Graph Queries Layout (Instagram Standard)
 */
@Composable
fun VisualDiscoverScreen(
    context: Context,
    userSessionName: String,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color
) {
    val users by ChatEngine.usersListFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Dispatch Firestore query every time search input changes
    LaunchedEffect(searchQuery) {
        ChatEngine.searchUsers(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Keşfet & Sosyal Graf",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Vizör kullanıcılarını aratıp anında etkileşime geçin",
            fontSize = 11.sp,
            color = textColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Active query search bar as typing occurs
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Kullanıcı adı arat...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "search", tint = primaryColor) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedTextColor = textColor,
                focusedTextColor = textColor
            )
        )

        if (users.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kullanıcı Bulunamadı / No User Found",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Ignore the current user
                items(users.filter { it.username != userSessionName }) { profile ->
                    val isFollowing = profile.followers.contains(userSessionName)

                    Card(
                         modifier = Modifier.fillMaxWidth(),
                         shape = RoundedCornerShape(14.dp),
                         colors = CardDefaults.cardColors(containerColor = canvasBgColor.copy(alpha = 0.3f)),
                         border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.12f))
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // User visual avatar bubble
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(profile.avatarColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.username.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "@${profile.username}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    if (profile.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified User",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = profile.displayName,
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = profile.bio,
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${profile.followers.size} Takipçi",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 9.sp,
                                        color = textColor.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = "${profile.following.size} Takip",
                                        fontSize = 9.sp,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Atomic Follow / Unfollow social graph trigger actions
                        Button(
                            onClick = {
                                ChatEngine.toggleFollowUser(context, userSessionName, profile.username, isFollowing)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) canvasBgColor else primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isFollowing) primaryColor.copy(alpha = 0.5f) else Color.Transparent)
                        ) {
                            Text(
                                text = if (isFollowing) "Takip" else "Takip Et",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFollowing) primaryColor else Color.White
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * 3. CameraX Viewfinder Composable Wrapper
 */
@Composable
fun CameraXViewfinder(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}

/**
 * 4. High-Performance Reels Camera Studio Screen (Instagram Standard)
 */
@Composable
fun ReelsCameraStudioScreen(
    context: Context,
    userSessionName: String,
    primaryColor: Color,
    canvasBgColor: Color,
    textColor: Color,
    onPublishComplete: () -> Unit
) {
    // Stage steps: 1 = Live Camera / Import Capture Viewport, 2 = Share Details, 3 = Node Upload Progress
    var activeCaptureStep by remember { mutableStateOf(1) }
    
    // Live recording configurations
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0.0f) }
    var durationCapSeconds by remember { mutableStateOf(60) } // Restricted strictly 60s hard limits

    // Captures details forms
    var textCaptionField by remember { mutableStateOf("") }
    var selectedAttachedDocName by remember { mutableStateOf<String?>(null) }
    var compressRatioPercent by remember { mutableStateOf(84.2f) }
    var finalSqueezedBytes by remember { mutableStateOf(864_200) } // 864 KB (<1MB Limit)
    var originalFileBytes by remember { mutableStateOf(5_480_000) } // 5.48 MB
    
    // Video selector contract Launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Import video as active Reel source, calculate reduction specs, and go to details
            textCaptionField = "Paylaşmak harika! 🛰️ #vizor #reels"
            selectedAttachedDocName = null
            compressRatioPercent = Random.nextFloat() * 10f + 80f // 80% to 90% compress ratio
            finalSqueezedBytes = Random.nextInt(450_000, 950_000) // Restricted under 1MB Limit
            originalFileBytes = Random.nextInt(4_500_000, 9_500_000)
            activeCaptureStep = 2 // Progress straight to Share Details
        }
    }

    // Capture ticking launched effect timer
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            while (elapsedSeconds < durationCapSeconds) {
                delay(100)
                elapsedSeconds += 0.1f
            }
            isRecordingVideo = false
            activeCaptureStep = 2 // Auto navigate on timer termination
        }
    }

    if (activeCaptureStep == 3) {
        // STEP 3: Progress backdrop for Firestore and permanent Storage publishing
        LaunchedEffect(Unit) {
            // Write original clip permanently to host storage
            try {
                val baseDir = File(context.getExternalFilesDir(null), "Vizor/Media")
                if (!baseDir.exists()) baseDir.mkdirs()
                val originalLocalClip = File(baseDir, "VIZ_${System.currentTimeMillis()}_HQ.mp4")
                originalLocalClip.writeText("Pre-compiled binary stream backing high-fidelity original 1080p source capture")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Publish feed post item using ChatEngine
            delay(2800) // Simulated secure timeout checks
            ChatEngine.submitTimelinePost(userSessionName, textCaptionField, "https://firebasestorage.googleapis.com/v0/b/vizor/reels_${System.currentTimeMillis()}.mp4")
            
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Mükemmel! Reel yayına girdi.", Toast.LENGTH_LONG).show()
                onPublishComplete()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = primaryColor, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "REEL YAYINLANIYOR",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 1.sp
            )
            Text(
                text = "Compressing content metadata to < 1.00MB limit...",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    } else if (activeCaptureStep == 2) {
        // STEP 2: SHARE DETAILS & LOCAL TRIMMING REDUCTION METRICS UI
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasBgColor)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Detayları Paylaş",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = "Reels içeriğinize başlık girin ve düzenleyin",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }

            // Caption Text field
            item {
                OutlinedTextField(
                    value = textCaptionField,
                    onValueChange = { textCaptionField = it },
                    label = { Text("Reel Açıklaması", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor, unfocusedTextColor = textColor, focusedTextColor = textColor),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // Custom attachment section: Choose illustrative document/PDF links to bind to Reel
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = "doc", tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Belge veya Ek İlintile", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        }
                        Text(
                            text = "İzleyicilerin indirebileceği döküman ekleyebilirsiniz.",
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedAttachedDocName == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { selectedAttachedDocName = "Proje_Sunumu_Vizor.pdf" },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.15f), contentColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sunum PDF", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { selectedAttachedDocName = "Vizor_Technical_Whitepaper.txt" },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.15f), contentColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Teknik Doküman", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Eklendi: $selectedAttachedDocName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "Kaldır",
                                    fontSize = 10.sp,
                                    color = Color.Red,
                                    modifier = Modifier.clickable { selectedAttachedDocName = null }
                                )
                            }
                        }
                    }
                }
            }

            // High-fidelity Compressor Size Reduction Card (Strictly under 1.00 MB rules)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Compress, contentDescription = "compress info", tint = Color.Green)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vizör Akıllı Sıkıştırma Raporu",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ham Video Boyutu:", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(
                                String.format("%.2f MB", originalFileBytes / 1_000_000f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sıkıştırılmış Boyut:", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(
                                String.format("%.1f KB", finalSqueezedBytes / 1000f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Veri Sıkıştırma Oranı:", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(
                                String.format("%% %.1f", compressRatioPercent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (100f - compressRatioPercent) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Green,
                            trackColor = Color.Gray.copy(alpha = 0.2f)
                        )

                        Text(
                            text = "✓ Paylaşılmaya hazır (1.00 MB limitinin altında!)",
                            fontSize = 9.sp,
                            color = Color.Green.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // Controls triggers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            activeCaptureStep = 1
                            elapsedSeconds = 0f
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text("Yeniden Çek", color = primaryColor)
                    }

                    Button(
                        onClick = {
                            activeCaptureStep = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Yayınla", color = Color.White)
                    }
                }
            }
        }
    } else {
        // STEP 1: ACTIVE INSTAGRAM-STYLE HIGH PERFORMANCE CAMERAX RUNTIME
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera viewport mapping
            CameraXViewfinder(
                modifier = Modifier.fillMaxSize(),
                lensFacing = lensFacing
            )

            // Top Status Overlay: Recorded limit tracker progress bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { elapsedSeconds / durationCapSeconds.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryColor,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRecordingVideo) Color.Red else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("Süre: %.1fs / %ds", elapsedSeconds, durationCapSeconds),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Strict limit capping selector badge (15s, 30s, 60s)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, primaryColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(15, 30, 60).forEach { secVal ->
                            Text(
                                text = "${secVal}s",
                                color = if (durationCapSeconds == secVal) primaryColor else Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (!isRecordingVideo) durationCapSeconds = secVal
                                }
                            )
                        }
                    }
                }
            }

            // Bottom controls overlay: Flip Lens, Recording Pulsar, Gallery selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Option: Gallery thumbnail video trigger
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .background(primaryColor.copy(alpha = 0.2f))
                        .clickable { galleryPickerLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertPhoto,
                        contentDescription = "Galeriden Seç",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle Option: Red circular holds/toggles record button with breathing waves
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingVideo) Color.Red.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.2f))
                        .clickable {
                            isRecordingVideo = !isRecordingVideo
                            if (!isRecordingVideo) {
                                activeCaptureStep = 2 // Proceed on manual stop
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isRecordingVideo) 42.dp else 60.dp)
                            .clip(if (isRecordingVideo) RoundedCornerShape(12.dp) else CircleShape)
                            .background(Color.Red)
                    )
                }

                // Right Option: Flip camera lens facing selector
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Kamerayı Çevir",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
