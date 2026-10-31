package io.github.wlenaerts.mindmapper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.wlenaerts.mindmapper.ui.MindmapperApp
import io.github.wlenaerts.mindmapper.ui.theme.MindmapperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindmapperTheme {
                MindmapperApp()
            }
        }
    }
}
