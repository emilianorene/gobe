package com.gobe.tv.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp

@Composable
fun FoldersScreen(app: GobeApp, onBrowse: (String) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val vm: FoldersViewModel = viewModel(factory = vmFactory { FoldersViewModel(app.repository) })
    val folders by vm.folders.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text("Carpetas de ROMs", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onBrowse(app.defaultRomPath) }, modifier = Modifier.focusRequester(focus)) {
            Text("+ Agregar carpeta")
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders) { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.path, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { vm.toggle(f) }) { Text(if (f.enabled) "Activa" else "Inactiva") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { vm.remove(f) }) { Text("Quitar") }
                }
            }
        }
    }
}
