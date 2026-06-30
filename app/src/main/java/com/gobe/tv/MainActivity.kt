package com.gobe.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.ui.theme.GobeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GobeTheme { GobePlaceholder() } }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GobePlaceholder() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text("Gobe", style = MaterialTheme.typography.displayMedium)
    }
}
