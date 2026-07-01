package com.gobe.tv.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R
import com.gobe.tv.emulation.GameSettings
import com.gobe.tv.emulation.input.DeadzoneLevel
import com.gobe.tv.emulation.input.MenuHotkey
import com.gobe.tv.i18n.AppLanguage
import com.gobe.tv.i18n.LocaleManager

@Composable
fun SettingsScreen(onOpenFolders: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val current = remember { LocaleManager.getLanguage(context) }
    var deadzone by remember { mutableStateOf(GameSettings.loadDeadzone(context)) }
    var hotkey by remember { mutableStateOf(GameSettings.loadMenuHotkey(context)) }

    fun choose(language: AppLanguage) {
        if (language != LocaleManager.getLanguage(context)) {
            LocaleManager.setLanguage(context, language)
            (context as? Activity)?.recreate()
        }
    }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { choose(AppLanguage.SYSTEM) }, modifier = Modifier.focusRequester(focus)) {
                Text(if (current == AppLanguage.SYSTEM) "● " + stringResource(R.string.lang_system) else stringResource(R.string.lang_system))
            }
            Button(onClick = { choose(AppLanguage.SPANISH) }) {
                Text(if (current == AppLanguage.SPANISH) "● " + stringResource(R.string.lang_es) else stringResource(R.string.lang_es))
            }
            Button(onClick = { choose(AppLanguage.ENGLISH) }) {
                Text(if (current == AppLanguage.ENGLISH) "● " + stringResource(R.string.lang_en) else stringResource(R.string.lang_en))
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onOpenFolders) { Text(stringResource(R.string.settings_folders)) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            context.startActivity(android.content.Intent(context, com.gobe.tv.controllers.ControllersActivity::class.java))
        }) { Text(stringResource(R.string.settings_controllers)) }

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.settings_deadzone), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val levels = listOf(
                DeadzoneLevel.OFF to R.string.deadzone_off,
                DeadzoneLevel.LOW to R.string.deadzone_low,
                DeadzoneLevel.MEDIUM to R.string.deadzone_medium,
                DeadzoneLevel.HIGH to R.string.deadzone_high,
            )
            levels.forEach { (level, labelRes) ->
                Button(onClick = { GameSettings.saveDeadzone(context, level); deadzone = level }) {
                    Text((if (deadzone == level) "● " else "") + stringResource(labelRes))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_menu_hotkey), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuHotkey.values().forEach { hk ->
                Button(onClick = { GameSettings.saveMenuHotkey(context, hk); hotkey = hk }) {
                    Text((if (hotkey == hk) "● " else "") + stringResource(hk.labelRes))
                }
            }
        }
    }
}
