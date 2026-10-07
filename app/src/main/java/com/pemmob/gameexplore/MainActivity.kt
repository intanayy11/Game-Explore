package com.pemmob.gameexplore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pemmob.gameexplore.ui.navigation.NavGraph
import com.pemmob.gameexplore.ui.theme.GameExploreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameExploreTheme {
                NavGraph()
            }
        }
    }
}
