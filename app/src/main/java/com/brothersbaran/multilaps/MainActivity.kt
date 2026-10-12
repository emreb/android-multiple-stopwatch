package com.brothersbaran.multilaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brothersbaran.multilaps.theme.MultiLapsTheme
import com.brothersbaran.multilaps.timing.TimingScreen
import com.brothersbaran.multilaps.timing.TimingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiLapsTheme {
                TimingScreen(viewModel(factory = TimingViewModel.Factory))
            }
        }
    }
}
