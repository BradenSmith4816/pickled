package com.braden.pickleplayers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.braden.pickleplayers.ui.PickledScreen
import com.braden.pickleplayers.ui.PickledTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PickledTheme {
                PickledScreen()
            }
        }
    }
}
