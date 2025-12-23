package com.example.ceviriapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ceviriapp.data.HistoryRepository
import com.example.ceviriapp.data.SettingsRepository
import com.example.ceviriapp.data.SmartcatApi
import com.example.ceviriapp.ui.theme.CeviriAppTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsRepository = remember { SettingsRepository(context) }
            
            // Tema tercihini yükle, yoksa sistem temasını kullan
            val initialTheme = settingsRepository.getThemePreference() ?: isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(initialTheme) }

            CeviriAppTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    TranslationScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeChange = {
                            isDarkTheme = !isDarkTheme
                            settingsRepository.saveThemePreference(isDarkTheme)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// DeepL API Key (:fx ile biten)
private const val API_KEY = "091a9ba0-ae62-42db-b2f9-86fd33385085:fx"

private data class Language(val code: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    isDarkTheme: Boolean,
    onThemeChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val historyRepository = remember { HistoryRepository(context) }
    
    val languages = remember {
        listOf(
            Language("TR", "Türkçe"),
            Language("EN", "İngilizce"),
            Language("DE", "Almanca"),
            Language("FR", "Fransızca"),
            Language("ES", "İspanyolca"),
            Language("RU", "Rusça"),
            Language("AR", "Arapça")
        )
    }

    var sourceLanguage by remember { mutableStateOf(languages.first()) }
    var targetLanguage by remember { mutableStateOf(languages.getOrElse(1) { languages.first() }) }
    var textToTranslate by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val historyList = remember { mutableStateListOf<HistoryItem>() }

    LaunchedEffect(Unit) {
        val savedHistory = historyRepository.getHistory()
        historyList.addAll(savedHistory)
    }

    val coroutineScope = rememberCoroutineScope()

    // İçeriği ortalamak ve geniş ekranlarda yayılmasını engellemek için Box ve widthIn kullanıyoruz
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(), // Klavye açıldığında içeriği yukarı itmek için
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp) // İçerik genişliğini 600dp ile sınırla (tablet/yatay mod için)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 24.dp)
        ) {
            // --- BAŞLIK VE TEMA BUTONU ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = if (isLandscape) 2.dp else 12.dp)
            ) {
                // Başlık
                Text(
                    text = "Çeviri",
                    modifier = Modifier.align(Alignment.Center),
                    style = if (isLandscape) {
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                )

                // Tema Değiştirme Butonu (Sağ Üst)
                IconButton(
                    onClick = onThemeChange,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = "Temayı Değiştir",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- YAN YANA KARTLAR VE DEĞİŞTİRME BUTONU ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yükseklik ayarı: Yan modda daha da kısa kartlar (150dp)
                val cardHeight = if (isLandscape) 150.dp else 280.dp

                // 1. SOL KART (KAYNAK)
                Card(
                    modifier = Modifier.weight(1f).height(cardHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageDropdownCompact(
                                languages = languages,
                                selected = sourceLanguage,
                                onSelected = { sourceLanguage = it }
                            )
                            if (textToTranslate.isNotEmpty()) {
                                IconButton(onClick = { textToTranslate = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Temizle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = textToTranslate,
                            onValueChange = { textToTranslate = it },
                            placeholder = { Text("Yazın...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxSize(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrect = false, keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                        )
                    }
                }

                // --- DİL DEĞİŞTİRME BUTONU ---
                IconButton(onClick = {
                    val tempLang = sourceLanguage
                    sourceLanguage = targetLanguage
                    targetLanguage = tempLang
                    val tempText = textToTranslate
                    textToTranslate = translatedText
                    translatedText = tempText
                }, modifier = Modifier.size(50.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.ArrowForward, "Dilleri Değiştir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).rotate(180f))
                        Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                // 2. SAĞ KART (HEDEF)
                Card(
                    modifier = Modifier.weight(1f).height(cardHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageDropdownCompact(languages = languages, selected = targetLanguage, onSelected = { targetLanguage = it })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = translatedText,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Çeviri...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxSize(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // --- ÇEVİR BUTONU ---
            Button(onClick = {
                if (textToTranslate.isBlank()) {
                    errorMessage = "Lütfen çevirmek için metin girin."
                    return@Button
                }
                errorMessage = null
                translatedText = ""
                isLoading = true
                coroutineScope.launch {
                    try {
                        val authHeader = "DeepL-Auth-Key $API_KEY"
                        val currentText = textToTranslate
                        val currentSource = sourceLanguage
                        val currentTarget = targetLanguage

                        val response = SmartcatApi.service.translate(authHeader, currentText, currentTarget.code, currentSource.code)

                        if (response.isSuccessful && response.body() != null) {
                            val translations = response.body()!!.translations
                            if (translations.isNotEmpty()) {
                                val result = translations[0].text
                                translatedText = result
                                val newItem = HistoryItem(currentText, result, currentSource.code, currentTarget.code)
                                historyList.add(0, newItem)
                                historyRepository.saveHistory(historyList)
                            } else {
                                errorMessage = "Çeviri boş döndü."
                            }
                        } else {
                            errorMessage = "Hata: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Hata: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }, 
            // Yan modda buton yüksekliğini düşür
            modifier = Modifier.fillMaxWidth().height(if (isLandscape) 48.dp else 64.dp), 
            shape = RoundedCornerShape(20.dp), 
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ), 
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(24.dp), color = MaterialTheme.colorScheme.onSecondary, trackColor = Color.Transparent)
                }
                Text("Çevir", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }

            // --- HATA KARTI ---
            if (errorMessage != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // --- GEÇMİŞ BÖLÜMÜ ---
            if (historyList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Geçmiş", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                    historyList.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = item.sourceLang, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline), modifier = Modifier.width(30.dp))
                                    Text(item.sourceText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = item.targetLang, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary), modifier = Modifier.width(30.dp))
                                    Text(item.targetText, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dil seçici 
@Composable
private fun LanguageDropdownCompact(languages: List<Language>, selected: Language, onSelected: (Language) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true }.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.code,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.label) }, 
                    onClick = { onSelected(language); expanded = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslationScreenPreview() {
    CeviriAppTheme(darkTheme = false) {
        // Preview için boş callback
        TranslationScreen(isDarkTheme = false, onThemeChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun TranslationScreenPreviewDark() {
    CeviriAppTheme(darkTheme = true) {
        // Preview için boş callback
        TranslationScreen(isDarkTheme = true, onThemeChange = {})
    }
}
