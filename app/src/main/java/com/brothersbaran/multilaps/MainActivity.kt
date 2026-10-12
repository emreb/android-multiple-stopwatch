package com.brothersbaran.multilaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brothersbaran.multilaps.stopwatch.StopwatchScreen
import com.brothersbaran.multilaps.stopwatch.StopwatchViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                StopwatchScreen(viewModel(factory = StopwatchViewModel.Factory))
            }
        }
    }
}
