package com.gobe.tv.emulation

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import kotlinx.coroutines.launch
import java.io.File

/**
 * SPIKE ONLY (Fase 2, Task 1): validate that LibretroDroid + the bundled snes9x core can boot a
 * real SNES ROM from its on-device path, render, take gamepad input, and emit the first frame.
 * Replaced by the real EmulatorActivity in Task 6 and removed (with its launcher entry) in Task 8.
 */
class SpikeEmulatorActivity : ComponentActivity() {

    private var retroView: GLRetroView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val corePath = "${applicationInfo.nativeLibraryDir}/libsnes9x_libretro_android.so"
        val romPath = firstSnesRom()
        Log.i(TAG, "corePath=$corePath exists=${File(corePath).exists()}")
        Log.i(TAG, "romPath=$romPath")

        if (romPath == null) {
            Log.e(TAG, "No .sfc ROM found under the SNES folder")
            finish()
            return
        }

        val data = GLRetroViewData(this).apply {
            coreFilePath = corePath
            gameFilePath = romPath
            systemDirectory = filesDir.absolutePath
            savesDirectory = filesDir.absolutePath
        }

        val view = GLRetroView(this, data)
        retroView = view
        lifecycle.addObserver(view)

        val container = FrameLayout(this)
        container.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(container)

        lifecycleScope.launch {
            view.getGLRetroEvents().collect { event ->
                Log.i(TAG, "GLRetroEvent: $event")
            }
        }
    }

    /** Recursively finds the first .sfc/.smc under the on-device SNES folder (case-insensitive FS). */
    private fun firstSnesRom(): String? {
        val candidates = listOf(
            "/storage/emulated/0/Download/ROMs/Snes",
            "/storage/emulated/0/Download/roms/Snes",
            "/storage/emulated/0/Download/ROMs/SNES",
        )
        for (path in candidates) {
            val sfcs = File(path).walkTopDown().filter {
                it.isFile && (it.name.endsWith(".sfc", true) || it.name.endsWith(".smc", true))
            }.toList()
            if (sfcs.isEmpty()) continue
            // For spike testing, prefer "Final Fight (NA)"; else any "final fight"; else first.
            val preferred = sfcs.firstOrNull { it.name.contains("final fight (na)", true) }
                ?: sfcs.firstOrNull { it.name.contains("final fight", true) }
                ?: sfcs.first()
            return preferred.absolutePath
        }
        return null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let Back close the spike; forward everything else to the core on port 0.
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        retroView?.sendKeyEvent(event.action, event.keyCode, 0)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        retroView?.let { view ->
            view.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_DPAD,
                event.getAxisValue(MotionEvent.AXIS_HAT_X),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y),
                0,
            )
            view.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_LEFT,
                event.getAxisValue(MotionEvent.AXIS_X),
                event.getAxisValue(MotionEvent.AXIS_Y),
                0,
            )
            view.sendMotionEvent(
                GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
                event.getAxisValue(MotionEvent.AXIS_Z),
                event.getAxisValue(MotionEvent.AXIS_RZ),
                0,
            )
        }
        return true
    }

    companion object {
        private const val TAG = "GobeSpike"
    }
}
