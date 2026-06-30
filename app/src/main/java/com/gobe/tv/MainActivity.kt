package com.gobe.tv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.gobe.tv.i18n.LocaleManager
import com.gobe.tv.ui.GobeNavHost
import com.gobe.tv.ui.theme.GobeTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GobeTheme {
                // tv-material3 Text reads LocalContentColor, which defaults to black
                // outside a Surface. Provide the theme's light onBackground so all text
                // is legible on the dark background.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onBackground
                    ) {
                        GobeNavHost(application as GobeApp)
                    }
                }
            }
        }
    }
}
