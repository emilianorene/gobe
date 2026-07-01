package com.gobe.tv.emulation

import android.content.Context
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.controllers.ControllerPrefs
import com.gobe.tv.emulation.input.ButtonRemap
import com.gobe.tv.emulation.input.ButtonSwaps
import com.gobe.tv.emulation.input.ControllerAssignments
import com.gobe.tv.emulation.input.applyMapping
import com.gobe.tv.emulation.input.nextDisk
import com.gobe.tv.emulation.input.portForDevice
import com.gobe.tv.i18n.LocaleManager
import com.gobe.tv.emulation.ui.PauseOverlay
import com.gobe.tv.ui.theme.GobeTheme
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import kotlinx.coroutines.launch
import java.io.File

/**
 * Hosts LibretroDroid's GLRetroView (render/audio/input) for one game, with a Compose pause
 * overlay on top. Back toggles pause; while paused the core is frozen and the overlay handles
 * D-pad. Save state + SRAM auto-save on exit; "load state" restores the last save.
 */
class EmulatorActivity : ComponentActivity() {

    private lateinit var args: EmulatorArgs
    private lateinit var saveStore: SaveStateStore
    private var retroView: GLRetroView? = null

    // Compose state driving the overlay.
    private var paused by mutableStateOf(false)
    private var hasState by mutableStateOf(false)
    // Shown briefly at launch to teach the menu combo; hidden after a delay AND permanently once
    // the menu has been opened (so it never returns on resume).
    private var showControlsHint by mutableStateOf(true)
    private var diskCount by mutableStateOf(0)
    private var currentDisk by mutableStateOf(0)

    private var coreReadyHandled = false
    private var loadErrorHandled = false

    // Player/port routing: which controller (by descriptor) drives which port. Loaded in onCreate.
    private var assignments = ControllerAssignments()
    // Per-controller A/B, X/Y swaps, read once at launch (mid-session changes need a relaunch).
    private var swapsByDescriptor: Map<String, ButtonSwaps> = emptyMap()
    // Per-controller custom button remaps, read once at launch.
    private var remapsByDescriptor: Map<String, ButtonRemap> = emptyMap()

    private val savesDir: File get() = File(filesDir, "saves").apply { mkdirs() }
    private val sramFile: File get() = File(savesDir, "${args.gameId}.srm")

    /** User-accessible folder where the user drops arcade BIOS/aux files. Created if missing.
     *  Only systemDirectory moves out of filesDir; save states stay app-private. */
    private val systemDir: File
        get() = File(android.os.Environment.getExternalStorageDirectory(), "Download/ROMs/system").apply { mkdirs() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        args = EmulatorArgs.fromIntent(intent)
        assignments = ControllerPrefs.load(this)
        swapsByDescriptor = ControllerPrefs.loadSwaps(this)
        remapsByDescriptor = ControllerPrefs.loadRemaps(this)
        saveStore = SaveStateStore(filesDir)
        hasState = saveStore.hasState(args.gameId)

        val corePath = CoreManager(applicationInfo.nativeLibraryDir).corePath(args.system)
        if (corePath == null || !File(corePath).exists()) {
            Toast.makeText(this, getString(R.string.emu_core_unavailable, args.system.displayName), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!File(args.romPath).exists()) {
            Toast.makeText(this, getString(R.string.emu_rom_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val data = GLRetroViewData(this).apply {
            coreFilePath = corePath
            gameFilePath = args.romPath
            systemDirectory = systemDir.absolutePath
            savesDirectory = savesDir.absolutePath
            saveRAMState = sramFile.takeIf { it.exists() }?.readBytes()
        }

        val view = GLRetroView(this, data)
        retroView = view
        lifecycle.addObserver(view)

        val root = FrameLayout(this)
        root.addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val overlay = ComposeView(this).apply {
            setContent {
                GobeTheme {
                    if (paused) {
                        PauseOverlay(
                            hasState = hasState,
                            onResume = ::resume,
                            onSave = ::saveState,
                            onLoad = ::loadState,
                            onExit = ::exitToMenu,
                            diskCount = diskCount,
                            currentDisk = currentDisk,
                            onChangeDisk = ::changeDisk,
                        )
                    }
                    if (showControlsHint && !paused) {
                        com.gobe.tv.emulation.ui.ControlsHint()
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(5000)
                            showControlsHint = false
                        }
                    }
                }
            }
        }
        root.addView(
            overlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        setContentView(root)

        lifecycleScope.launch {
            view.getGLRetroEvents().collect { event ->
                if (event is GLRetroView.GLRetroEvents.FrameRendered && !coreReadyHandled) {
                    coreReadyHandled = true
                    onCoreReady()
                }
            }
        }

        lifecycleScope.launch {
            view.getGLRetroErrors().collect {
                if (!loadErrorHandled && !isFinishing) {
                    loadErrorHandled = true
                    Toast.makeText(this@EmulatorActivity, getString(R.string.emu_load_failed), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun onCoreReady() {
        lifecycleScope.launch {
            runCatching { (application as GobeApp).repository.updateLastPlayed(args.gameId) }
        }
        retroView?.let { v ->
            diskCount = runCatching { v.getAvailableDisks() }.getOrDefault(0)
            currentDisk = runCatching { v.getCurrentDisk() }.getOrDefault(0)
        }
        if (args.loadState) loadState()
    }

    // --- pause / resume ---

    private fun togglePause() = if (paused) resume() else pause()

    private fun pause() {
        retroView?.frameSpeed = 0
        retroView?.audioEnabled = false
        paused = true
        showControlsHint = false // gone for good once the menu is discovered
    }

    private fun resume() {
        retroView?.frameSpeed = 1
        retroView?.audioEnabled = true
        paused = false
    }

    // --- save / load / exit ---

    private fun saveState() {
        val v = retroView ?: return
        runCatching {
            saveStore.writeState(args.gameId, v.serializeState())
            hasState = true
            toast(getString(R.string.emu_state_saved))
        }.onFailure { toast(getString(R.string.emu_state_save_failed)) }
    }

    private fun loadState() {
        val v = retroView ?: return
        val bytes = saveStore.readState(args.gameId)
        if (bytes == null) { toast(getString(R.string.emu_no_state)); return }
        runCatching { v.unserializeState(bytes) }
            .onSuccess { resume() }
            .onFailure { toast(getString(R.string.emu_state_load_failed)) }
    }

    private fun exitToMenu() {
        autoSave()
        finish()
    }

    private fun changeDisk() {
        val v = retroView ?: return
        if (diskCount <= 1) return
        val next = nextDisk(currentDisk, diskCount)
        runCatching { v.changeDisk(next) }
        currentDisk = next
        toast(getString(R.string.emu_disk_changed, next + 1))
    }

    private fun autoSave() {
        val v = retroView ?: return
        runCatching {
            saveStore.writeState(args.gameId, v.serializeState())
            hasState = true
        }
        runCatching {
            val sram = v.serializeSRAM()
            if (sram.isNotEmpty()) {
                val tmp = File(sramFile.path + ".tmp")
                tmp.writeBytes(sram)
                if (!tmp.renameTo(sramFile)) { sramFile.writeBytes(sram); tmp.delete() }
            }
        }
    }

    override fun onPause() {
        // Auto-save before the GL/emulation thread is paused by the lifecycle observer.
        if (isFinishing.not()) autoSave()
        super.onPause()
    }

    // --- input forwarding (routed to each controller's assigned port; unassigned -> P1) ---

    private fun portForInput(deviceId: Int): Int =
        portForDevice(InputDevice.getDevice(deviceId)?.descriptor, assignments)

    // Null/unknown descriptor -> no-swap default, so unconfigured controllers play normally.
    private fun swapsForInput(deviceId: Int): ButtonSwaps =
        swapsByDescriptor[InputDevice.getDevice(deviceId)?.descriptor] ?: ButtonSwaps()

    private fun remapForInput(deviceId: Int): ButtonRemap =
        remapsByDescriptor[InputDevice.getDevice(deviceId)?.descriptor] ?: ButtonRemap()

    private val heldKeys = HashSet<Int>()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        // Remote Back: toggle the pause menu.
        if (code == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) togglePause()
            return true
        }
        // Gamepad Home/Mode: toggle.
        if (code == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.action == KeyEvent.ACTION_UP) togglePause()
            return true
        }
        // Track Select/Start to detect the combo.
        if (code == KeyEvent.KEYCODE_BUTTON_SELECT || code == KeyEvent.KEYCODE_BUTTON_START) {
            if (event.action == KeyEvent.ACTION_DOWN) heldKeys.add(code) else heldKeys.remove(code)
            val combo = heldKeys.contains(KeyEvent.KEYCODE_BUTTON_SELECT) &&
                heldKeys.contains(KeyEvent.KEYCODE_BUTTON_START)
            if (combo && !paused) {
                heldKeys.clear()
                togglePause()
                return true // swallow — don't leak Select/Start to the core
            }
            // While paused, let the overlay handle nav; otherwise forward to the core as normal input.
            if (paused) return super.dispatchKeyEvent(event)
            retroView?.sendKeyEvent(event.action, applyMapping(code, remapForInput(event.deviceId), swapsForInput(event.deviceId)), portForInput(event.deviceId))
            return true
        }
        if (paused) return super.dispatchKeyEvent(event) // overlay handles D-pad
        retroView?.sendKeyEvent(event.action, applyMapping(code, remapForInput(event.deviceId), swapsForInput(event.deviceId)), portForInput(event.deviceId))
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (paused) return super.onGenericMotionEvent(event)
        val v = retroView ?: return super.onGenericMotionEvent(event)
        val port = portForInput(event.deviceId)
        v.sendMotionEvent(
            GLRetroView.MOTION_SOURCE_DPAD,
            event.getAxisValue(MotionEvent.AXIS_HAT_X),
            event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            port,
        )
        v.sendMotionEvent(
            GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
            event.getAxisValue(MotionEvent.AXIS_X),
            event.getAxisValue(MotionEvent.AXIS_Y),
            port,
        )
        v.sendMotionEvent(
            GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
            event.getAxisValue(MotionEvent.AXIS_Z),
            event.getAxisValue(MotionEvent.AXIS_RZ),
            port,
        )
        return true
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).let { c ->
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
