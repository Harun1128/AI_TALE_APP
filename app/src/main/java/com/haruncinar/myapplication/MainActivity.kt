package com.haruncinar.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.haruncinar.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // StoryManager'ı activity scope'unda oluştur
        val storyManager = StoryManager(this)

        setContent {
            MyApplicationTheme {
                // Surface ile tema renklerini ve davranışlarını uygula
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // StoryManager'ı NavigationSetup'a ilet
                    NavigationSetup(storyManager = storyManager)
                }
            }
        }
    }

}

