package com.example.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LanguageCode {
    EN, TR
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(LanguageCode.TR) // Default to Turkish as requested
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setLanguage(code: LanguageCode, context: Context) {
        _currentLanguage.value = code
        val prefs = context.getSharedPreferences("vizor_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", code.name).apply()
    }

    fun loadLanguage(context: Context) {
        val prefs = context.getSharedPreferences("vizor_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("app_language", LanguageCode.TR.name)
        _currentLanguage.value = LanguageCode.valueOf(saved ?: LanguageCode.TR.name)
    }

    private val trTranslations = mapOf(
        "app_name" to "Vizör",
        "feed" to "Akış",
        "chat" to "Sohbet",
        "arcade" to "Atari",
        "settings" to "Ayarlar",
        "permissions" to "İzinler",
        "camera_perm" to "Kamera İzni",
        "audio_perm" to "Mikrofon İzni",
        "storage_perm" to "Depolama İzni",
        "network_perm" to "Ağ İzni",
        "perm_explain" to "Vizör'ün tam kapasite çalışabilmesi için kamera, ses, depolama ve ağ durum izinlerine ihtiyacı vardır. Lütfen bu izinleri verin.",
        "grant_perm" to "İzin Ver",
        "go_to_settings" to "Ayarlara Git",
        "auth_title" to "Vizör Giriş",
        "register_title" to "Hesap Oluştur",
        "login_button" to "Giriş Yap",
        "register_button" to "Kaydol",
        "no_account" to "Hesabınız yok mu? Hemen kaydolun",
        "has_account" to "Zaten üye misiniz? Giriş yapın",
        "username" to "Kullanıcı Adı",
        "email" to "E-posta",
        "password" to "Şifre",
        "country" to "Ülke Seçimi",
        "password_strength" to "Şifre Gücü:",
        "strength_weak" to "Zayıf",
        "strength_medium" to "Orta",
        "strength_strong" to "Güçlü",
        "captcha_challenge" to "Lütfen aşağıdaki captcha işlemini çözün:",
        "captcha_error" to "Hatalı Captcha Çözümü!",
        "censor_warning" to "Girişinizde yasaklı kelimeler veya bağlantılar tespit edildi!",
        "themes_subtitle" to "Uygulama Temasını Özelleştir",
        "primary_color" to "Birincil Renk",
        "canvas_bg" to "Arka Plan Rengi",
        "text_color" to "Metin Rengi",
        "incoming_bubble" to "Gelen Balon Rengi",
        "outgoing_bubble" to "Giden Balon Rengi",
        "reset_theme" to "Varsayılana Sıfırla",
        "video_duration_err" to "Videolar en fazla 60 saniye olabilir!",
        "video_compressing" to "Video 480p/360p kalitesine ve en fazla 1MB boyutuna sıkıştırılıyor...",
        "video_compressed" to "Video sıkıştırıldı! Boyut:",
        "send_message" to "Mesaj gönder...",
        "timeline_feed" to "Gönderiler",
        "reels_feed" to "Reels",
        "add_post" to "Yeni Gönderi Ekle",
        "post_desc" to "Açıklama girin...",
        "post_btn" to "Paylaş",
        "offline_okey" to "Çevrimdışı Okey",
        "endless_car" to "Sonsuz Yol Araba Yarışı",
        "retro_snake" to "Süper Yılan Oyunu",
        "okey_draw" to "Taş Çek",
        "okey_discard" to "Taş At",
        "okey_bot_turn" to "Bot sırasını oynuyor...",
        "okey_user_turn" to "Sizin sıranız! Taş çekin veya yerdeki taşı alın.",
        "okey_win" to "Tebrikler, elinizi açtınız ve kazandınız!",
        "okey_restart" to "Yeniden Başlat",
        "snake_score" to "Skor:",
        "car_score" to "Mesafe:",
        "rate_limit_err" to "Çok hızlı işlem yapıyorsunuz! Lütfen biraz bekleyin.",
        "webrtc_calling" to "Güvenli P2P WebRTC Bağlantısı Kuruluyor...",
        "webrtc_connected" to "Doğrudan Cihazdan Cihaza Bağlantı Aktif (Maliyet: $0)",
        "p2p_call_btn" to "P2P Sesli/Görüntülü Arama",
        "remote_expiry_banner" to "Yüklenen Reels ve medya dosyaları 7 gün sonra sunuculardan kalıcı olarak silinir. Yerel kopyanız /Vizor/Media klasöründe saklanır."
    )

    private val enTranslations = mapOf(
        "app_name" to "Vizör",
        "feed" to "Feed",
        "chat" to "Chat",
        "arcade" to "Arcade",
        "settings" to "Settings",
        "permissions" to "Permissions",
        "camera_perm" to "Camera Permission",
        "audio_perm" to "Microphone Permission",
        "storage_perm" to "Storage Permission",
        "network_perm" to "Network Permission",
        "perm_explain" to "Vizör requires access to your camera, audio recorder, storage, and network state to function. Please grant these permissions.",
        "grant_perm" to "Grant Permission",
        "go_to_settings" to "Go to Settings",
        "auth_title" to "Vizör Login",
        "register_title" to "Create Account",
        "login_button" to "Login",
        "register_button" to "Sign Up",
        "no_account" to "Don't have an account? Sign up now",
        "has_account" to "Already have an account? Log in",
        "username" to "Username",
        "email" to "Email Address",
        "password" to "Password",
        "country" to "Select Country",
        "password_strength" to "Password Strength:",
        "strength_weak" to "Weak",
        "strength_medium" to "Medium",
        "strength_strong" to "Strong",
        "captcha_challenge" to "Please solve the following captcha challenge:",
        "captcha_error" to "Incorrect Captcha solution!",
        "censor_warning" to "Your input contains forbidden keywords or links!",
        "themes_subtitle" to "Customize App Theme",
        "primary_color" to "Primary Color",
        "canvas_bg" to "Canvas Background",
        "text_color" to "Text Color",
        "incoming_bubble" to "Incoming Bubble Color",
        "outgoing_bubble" to "Outgoing Bubble Color",
        "reset_theme" to "Reset to Default",
        "video_duration_err" to "Videos can be a maximum of 60 seconds!",
        "video_compressing" to "Compressing video to 480p/360p and under 1MB limit...",
        "video_compressed" to "Video compressed! Size:",
        "send_message" to "Send a message...",
        "timeline_feed" to "Posts",
        "reels_feed" to "Reels",
        "add_post" to "Create New Post",
        "post_desc" to "Enter a description...",
        "post_btn" to "Post",
        "offline_okey" to "Offline Okey Game",
        "endless_car" to "Endless Traffic Racer",
        "retro_snake" to "Super Retro Snake",
        "okey_draw" to "Draw Tile",
        "okey_discard" to "Discard Tile",
        "okey_bot_turn" to "Bot is playing their turn...",
        "okey_user_turn" to "Your turn! Draw from wall or pick from discard discard pile.",
        "okey_win" to "Congratulations! You have formed winning groups!",
        "okey_restart" to "New Game",
        "snake_score" to "Score:",
        "car_score" to "Distance:",
        "rate_limit_err" to "You are acting too fast! Please slow down.",
        "webrtc_calling" to "Establishing Secure P2P WebRTC Connection...",
        "webrtc_connected" to "Direct Device-to-Device Stream Active (Cost: $0)",
        "p2p_call_btn" to "P2P Voice/Video Call",
        "remote_expiry_banner" to "Uploaded Reels & media files automatically expire and delete in 7 days. Your high-quality copy is kept locally in /Vizor/Media."
    )

    fun getString(key: String): String {
        val transMap = if (_currentLanguage.value == LanguageCode.TR) trTranslations else enTranslations
        return transMap[key] ?: key
    }
}
