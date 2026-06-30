package com.gobe.tv.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import java.io.File

@Composable
fun FolderBrowserScreen(startPath: String, onPicked: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val app = LocalContext.current.applicationContext as GobeApp
    val vm: FoldersViewModel = viewModel(factory = vmFactory { FoldersViewModel(app.repository) })
    var current by remember {
        mutableStateOf(File(if (File(startPath).isDirectory) startPath else "/storage/emulated/0"))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(current) { focus.requestFocus() }

    val dirs = remember(current) {
        (current.listFiles()?.filter { it.isDirectory && it.canRead() } ?: emptyList())
            .sortedBy { it.name.lowercase() }
    }

    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(current.absolutePath, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row {
            Button(
                onClick = { vm.add(current.absolutePath); onPicked() },
                modifier = Modifier.focusRequester(focus),
            ) { Text("Usar esta carpeta") }
            Spacer(Modifier.width(8.dp))
            current.parentFile?.let { p ->
                Button(onClick = { current = p }) { Text(".. Subir") }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(dirs) { d ->
                Button(onClick = { current = d }) { Text("📁 ${d.name}") }
            }
        }
    }
}
