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
import com.gobe.tv.ui.library.LibraryScreen
import com.gobe.tv.ui.onboarding.PermissionScreen
import com.gobe.tv.ui.settings.SettingsScreen

@Composable
fun GobeNavHost(app: GobeApp) {
    // Minimal back stack: push to navigate, pop on back. Top of stack is the current screen.
    var stack by remember {
        mutableStateOf(listOf<Route>(if (StoragePermission.isGranted()) Route.Home else Route.Permission))
    }
    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun resetTo(r: Route) { stack = listOf(r) }
    val route = stack.last()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (app.returnToHomeOnResume) {
                    app.returnToHomeOnResume = false
                    if (StoragePermission.isGranted()) { resetTo(Route.Home); return@LifecycleEventObserver }
                }
                // Read the stack FRESH here — do not use the composition-captured `route` local,
                // which the observer closure would freeze at first composition (stale-value bug).
                if (!StoragePermission.isGranted()) resetTo(Route.Permission)
                else if (stack.last() is Route.Permission) resetTo(Route.Home)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val r = route) {
        is Route.Permission -> PermissionScreen(onGranted = { resetTo(Route.Home) })
        is Route.Home -> HomeScreen(
            app = app,
            onOpenSection = { push(Route.Library(it)) },
            onOpenGame = { push(Route.Detail(it)) },
            onOpenSettings = { push(Route.Settings) },
        )
        is Route.Library -> LibraryScreen(
            app = app,
            section = r.section,
            onOpenGame = { push(Route.Detail(it)) },
            onBack = { pop() },
        )
        is Route.Settings -> SettingsScreen(
            onOpenFolders = { push(Route.Folders) },
            onBack = { pop() },
        )
        is Route.Folders -> FoldersScreen(
            app = app,
            onBrowse = { start -> push(Route.FolderBrowser(start)) },
            onBack = { pop() },
        )
        is Route.FolderBrowser -> FolderBrowserScreen(
            startPath = r.startPath,
            onPicked = { pop() },
            onBack = { pop() },
        )
        is Route.Detail -> DetailScreen(
            app = app, gameId = r.gameId, onBack = { pop() },
        )
    }
}
