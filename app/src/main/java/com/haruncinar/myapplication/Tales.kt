package com.haruncinar.myapplication

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sabitler
private object Constants {
    const val PREFS_NAME = "stories"
    const val SAVED_STORIES_KEY = "saved_stories"
    const val DEFAULT_STORIES_JSON = "[]"
    const val MAX_PREVIEW_LENGTH = 100
    const val DATE_FORMAT = "dd/MM/yyyy HH:mm"
}

// UI Tema Renkleri
private object ThemeColors {
    val gradientColors = listOf(
        Color(0xFFB5E3E4),
        Color(0xFF87CEEB)
    )
}

data class SavedStory(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val date: String,
    val imagePrompt: String
)

class StoryManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())

    fun saveStory(title: String, content: String, imagePrompt: String) {
        val currentDate = dateFormat.format(Date())
        val story = SavedStory(
            title = title.ifEmpty { "Masal $currentDate" },
            content = content,
            date = currentDate,
            imagePrompt = imagePrompt
        )

        val stories = getStories().toMutableList().apply { add(story) }
        saveStoriesToPrefs(stories)

//       deleteStory(story)
    }

    fun getStories(): List<SavedStory> {
        val storiesJson = sharedPreferences.getString(Constants.SAVED_STORIES_KEY, Constants.DEFAULT_STORIES_JSON)
        val type = object : TypeToken<List<SavedStory>>() {}.type
        return gson.fromJson(storiesJson, type)
    }

    fun deleteStory(story: SavedStory) {
        val stories = getStories().toMutableList().apply {
            removeAll { it.id == story.id }
        }
        saveStoriesToPrefs(stories)
    }

    private fun saveStoriesToPrefs(stories: List<SavedStory>) {
        sharedPreferences.edit()
            .putString(Constants.SAVED_STORIES_KEY, gson.toJson(stories))
            .apply()
    }
}

@Composable
fun SavedStoriesScreen(
    onStoryClick: (SavedStory) -> Unit,
    storyManager: StoryManager
) {
    var stories by remember { mutableStateOf(emptyList<SavedStory>()) }

    LaunchedEffect(Unit) {
        stories = storyManager.getStories().reversed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = ThemeColors.gradientColors))
    ) {
        if (stories.isEmpty()) {
            EmptyStoriesMessage()
        } else {
            StoriesList(stories, onStoryClick)
        }
    }
}

@Composable
private fun EmptyStoriesMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Henüz kaydedilmiş masal yok",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StoriesList(
    stories: List<SavedStory>,
    onStoryClick: (SavedStory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stories) { story ->
            StoryCard(story, onStoryClick)
        }
    }
}

@Composable
private fun StoryCard(
    story: SavedStory,
    onStoryClick: (SavedStory) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStoryClick(story) },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = story.content.take(Constants.MAX_PREVIEW_LENGTH) +
                        if (story.content.length > Constants.MAX_PREVIEW_LENGTH) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = story.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}


@Composable
private fun TopBar(
    onBack: () -> Unit,
    onShare: (Context) -> Unit  // Context parametresi ekleyin
) {
    val context = LocalContext.current  // Context'i alın

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
        }
        IconButton(onClick = { onShare(context) }) {  // Context'i geçin
            Icon(Icons.Default.Share, contentDescription = "Paylaş")
        }
    }
}

private fun shareStory(context: Context, story: SavedStory) {  // Context parametresi ekleyin
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${story.title}\n\n${story.content}")
            type = "text/plain"
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        val shareIntent = Intent.createChooser(sendIntent, "Masalı Paylaş")
        context.startActivity(shareIntent)  // Intent'i başlatın
    } catch (e: Exception) {
        Toast.makeText(context, "Paylaşım sırasında bir hata oluştu", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun StoryDetailScreen(
    story: SavedStory,
    storyManager: StoryManager,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = ThemeColors.gradientColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(
                onBack = onBack,
                onShare = { context -> shareStory(context, story) }  // Context'i geçin
            )
            Spacer(modifier = Modifier.height(16.dp))
            StoryContent(story)
        }
    }
}

@Composable
private fun StoryContent(story: SavedStory) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = story.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = story.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

