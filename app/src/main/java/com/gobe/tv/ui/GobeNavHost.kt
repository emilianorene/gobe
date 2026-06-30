package com.gobe.tv.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gobe.tv.GobeApp
import com.gobe.tv.data.StoragePermission
import com.gobe.tv.ui.detail.DetailScreen
import com.gobe.tv.ui.folders.FolderBrowserScreen
import com.gobe.tv.ui.folders.FoldersScreen
import com.gobe.tv.ui.home.HomeScreen
import com.gobe.tv.ui.onboarding.PermissionScreen

@Composable
fun GobeNavHost(app: GobeApp) {
    var route by remember {
        mutableStateOf<Route>(if (StoragePermission.isGranted()) Route.Home else Route.Permission)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermission.isGranted()
                route = when {
                    !granted -> Route.Permission
                    granted && route is Route.Permission -> Route.Home
                    else -> route
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val r = route) {
        is Route.Permission -> PermissionScreen(onGranted = { route = Route.Home })
        is Route.Home -> HomeScreen(
            app = app,
            onOpenGame = { route = Route.Detail(it) },
            onOpenFolders = { route = Route.Folders },
        )
        is Route.Folders -> FoldersScreen(
            app = app,
            onBrowse = { start -> route = Route.FolderBrowser(start) },
            onBack = { route = Route.Home },
        )
        is Route.FolderBrowser -> FolderBrowserScreen(
            startPath = r.startPath,
            onPicked = { route = Route.Folders },
            onBack = { route = Route.Folders },
        )
        is Route.Detail -> DetailScreen(
            app = app, gameId = r.gameId, onBack = { route = Route.Home },
        )
    }
}
