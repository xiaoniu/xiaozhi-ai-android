package com.xiaozhi.ai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.xiaozhi.ai.ui.ConversationScreen
import com.xiaozhi.ai.ui.theme.DarkColorScheme
import com.xiaozhi.ai.ui.theme.YTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "应用启动，开始初始化...")
        
        setContent {
            YTheme(
                darkTheme = true
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkColorScheme.background
                ) {
                    ConversationScreen()
                }
            }
        }
    }
}