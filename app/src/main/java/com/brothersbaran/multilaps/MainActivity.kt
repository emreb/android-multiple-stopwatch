package com.brothersbaran.multilaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brothersbaran.multilaps.timing.TimingScreen
import com.brothersbaran.multilaps.timing.TimingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                TimingScreen(viewModel(factory = TimingViewModel.Factory))
            }
        }
    }
}
