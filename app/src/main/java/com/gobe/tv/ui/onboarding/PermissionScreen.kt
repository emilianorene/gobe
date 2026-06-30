package com.gobe.tv.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.data.StoragePermission

@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().padding(64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Permiso de almacenamiento", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Gobe necesita acceso a todos los archivos para encontrar tus ROMs. " +
                "Toca el boton, activa \"Permitir acceso a todos los archivos\" y vuelve.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { context.startActivity(StoragePermission.settingsIntent(context)) },
            modifier = Modifier.focusRequester(focus),
        ) { Text("Conceder acceso") }
    }
}
