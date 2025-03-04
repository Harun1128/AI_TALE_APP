package com.haruncinar.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// ThemeColors'a yeni renkler ekliyoruz
private object EnterScreenColors {
    val enterGradientColors = listOf(
        Color(0xFFFAEBD7),  // Pastel bej
        Color(0xFFF5B7B1),  // Pastel pembe
        Color(0xFFD2B4DE),  // Lila rengi
        Color(0xFFB5E3E4)   // Açık mavi
    )
    val buttonColor = Color(0xFFD2B4DE)
    val textColor = Color(0xFF4A4A4A)
}

sealed class Screen(val route: String) {
    object Enter : Screen("enter")
    object Tale : Screen("tale")
    object SavedStories : Screen("saved_stories")
    object StoryDetail : Screen("story_detail/{storyId}") {
        fun createRoute(storyId: Long) = "story_detail/$storyId"
    }
}

@Composable
fun NavigationSetup(storyManager: StoryManager) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Enter.route) {
        composable(Screen.Enter.route) {
            EnterScreen(navController)
        }
        composable(Screen.Tale.route) {
            Tale(
                onGenerateStory = { /* Story callback */ },
                onGenerateImagePrompt = { /* Image prompt callback */ },
                storyManager = storyManager
            )
        }
        composable(Screen.SavedStories.route) {
            SavedStoriesScreen(
                onStoryClick = { story ->
                    navController.navigate(Screen.StoryDetail.createRoute(story.id))
                },
                storyManager = storyManager
            )
        }
        composable(Screen.StoryDetail.route) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId")?.toLongOrNull()
            val story = storyManager.getStories().find { it.id == storyId }
            story?.let {
                StoryDetailScreen(
                    story = it,
                    storyManager = storyManager,
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
}

@Composable
fun EnterScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = EnterScreenColors.enterGradientColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
            ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeTexts()
            Spacer(modifier = Modifier.height(48.dp))
            NavigationButtons(navController)
        }
    }
}

@Composable
private fun WelcomeTexts() {
    Text(
        text = "Hoş Geldiniz",
        style = TextStyle(
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = EnterScreenColors.textColor,
            shadow = Shadow(
                color = Color.Gray,
                offset = Offset(2f, 2f),
                blurRadius = 3f
            )
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Masalcı Dedeye",
        style = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = EnterScreenColors.textColor
        )
    )
}

@Composable
private fun NavigationButtons(navController: NavController) {
    MenuButton(
        text = "Masallarım",
        onClick = { navController.navigate(Screen.SavedStories.route) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    MenuButton(
        text = "Masalcı Dede",
        onClick = { navController.navigate(Screen.Tale.route) }
    )
}


@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EnterScreenColors.buttonColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = Color.White
        )
    }
}