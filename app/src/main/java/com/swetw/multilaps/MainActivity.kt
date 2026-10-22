package com.swetw.multilaps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swetw.multilaps.theme.MultiLapsTheme
import com.swetw.multilaps.timing.PlayRatingPrompt
import com.swetw.multilaps.timing.TimingScreen
import com.swetw.multilaps.timing.TimingViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ratingPrompt = PlayRatingPrompt(this)
        setContent {
            MultiLapsTheme {
                TimingScreen(viewModel(factory = TimingViewModel.Factory), ratingPrompt)
            }
        }
    }
}
