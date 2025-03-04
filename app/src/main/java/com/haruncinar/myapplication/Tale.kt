package com.haruncinar.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

@Composable
fun Tale(
    onGenerateStory: (String) -> Unit,
    onGenerateImagePrompt: (String) -> Unit,
    storyManager: StoryManager
) {
    var inputText by remember { mutableStateOf("") }
    var wordList by remember { mutableStateOf(listOf<String>()) }
    var generatedStory by remember { mutableStateOf("") }
    var imagePrompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = ""
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.LightGray)

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            WordInputSection(
                inputText = inputText,
                onInputChange = { inputText = it },
                onAddWord = {
                    if (inputText.isNotBlank()) {
                        wordList = wordList + inputText.trim()
                        inputText = ""
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            WordChips(
                wordList = wordList,
                onRemoveWord = { word -> wordList = wordList - word }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GenerateStoryButton(
                isLoading = isLoading,
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch {
                            try {
                                val kelimeler = wordList.joinToString(", ")
                                val prompt = buildStoryPrompt(kelimeler)
                                val response = generativeModel.generateContent(prompt)
                                generatedStory = response.text ?: ""
                                onGenerateStory(generatedStory)

                                if (generatedStory.isNotEmpty()) {
                                    val imagePromptRequest = "$generatedStory buradaki masalı anlatan bir resim oluşturmam için prompt oluştur"
                                    val imagePromptResponse = generativeModel.generateContent(imagePromptRequest)
                                    imagePrompt = imagePromptResponse.text ?: ""
                                    onGenerateImagePrompt(imagePrompt)
                                }
                            } catch (e: Exception) {
                                generatedStory = "Bir hata oluştu: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (generatedStory.isNotEmpty()) {
                StoryCard(
                    story = generatedStory,
                    imagePrompt = imagePrompt,
                    onSave = {
                        scope.launch {
                            storyManager.saveStory(
                                title = "Masal ${wordList.joinToString(", ")}",
                                content = generatedStory,
                                imagePrompt = imagePrompt
                            )
                            showSavedMessage = true
                        }
                    }
                )
            }
        }

        // Kaydetme mesajı
        if (showSavedMessage) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { showSavedMessage = false }) {
                        Text("Tamam")
                    }
                }
            ) {
                Text("Masal kaydedildi!")
            }
        }
    }
}

@Composable
private fun WordInputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onAddWord: () -> Unit
) {
    OutlinedTextField(
        value = inputText,
        onValueChange = onInputChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Kelime Ekle") },
        trailingIcon = {
            if (inputText.isNotBlank()) {
                IconButton(onClick = { onInputChange("") }) {
                    Icon(Icons.Default.Close, "Temizle")
                }
            }
        }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onAddWord,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        Text("Ekle", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WordChips(
    wordList: List<String>,
    onRemoveWord: (String) -> Unit
) {
    if (wordList.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wordList) { word ->
                AssistChip(
                    onClick = { },
                    label = { Text(word) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveWord(word) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kaldır",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GenerateStoryButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(8.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onSecondary
            )
        } else {
            Text("Masal Oluştur", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StoryCard(
    story: String,
    imagePrompt: String,
    onSave: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Masal",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Save, contentDescription = "Kaydet")
                }
            }

            Text(
                text = story,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (imagePrompt.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Resim Promptu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = imagePrompt,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun buildStoryPrompt(kelimeler: String): String {
    return "Çocuklar için eğlenceli, öğretici ve yaratıcı bir masal yaz. " +
            if (kelimeler.isNotEmpty()) {
                "Şu kelimeleri doğal bir şekilde masala dahil et: $kelimeler. "
            } else {
                "Özgün bir masal oluştur. "
            } +
            "Masalın içinde macera, dostluk ve anlamlı bir öğüt olsun. " +
            "Hikaye kısa, akıcı ve çocukların ilgisini çekecek şekilde yazılsın."
}