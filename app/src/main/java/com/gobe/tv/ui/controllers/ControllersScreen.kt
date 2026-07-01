package com.gobe.tv.ui.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R
import com.gobe.tv.emulation.input.PadButton

/** Stateless controller detection + test UI. The Activity supplies the live state. */
@Composable
fun ControllersScreen(
    devices: List<String>,          // display names of connected gamepads
    activeButtons: Set<PadButton>,  // currently-pressed buttons (keys + axis-derived)
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,              // e.g. "8BitDo Pro 2 · A"
) {
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(stringResource(R.string.controllers_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Connected list / empty state.
        if (devices.isEmpty()) {
            Text(stringResource(R.string.controllers_none), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.controllers_connected) + ":", style = MaterialTheme.typography.titleMedium)
            devices.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.controllers_test), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // Button panel: rows of labels that light up when active.
        val rows = listOf(
            listOf(PadButton.A, PadButton.B, PadButton.X, PadButton.Y),
            listOf(PadButton.L1, PadButton.R1, PadButton.L2, PadButton.R2),
            listOf(PadButton.SELECT, PadButton.START, PadButton.L3, PadButton.R3),
            listOf(PadButton.DPAD_UP, PadButton.DPAD_DOWN, PadButton.DPAD_LEFT, PadButton.DPAD_RIGHT),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b -> PadChip(b.name, b in activeButtons) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        val sticks = "L (%.2f, %.2f)   R (%.2f, %.2f)".format(
            leftStick.first, leftStick.second, rightStick.first, rightStick.second,
        )
        Text(
            stringResource(R.string.controllers_sticks) + ": " + sticks,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.controllers_last) + ": " + lastInput, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PadChip(label: String, active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2F3A)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
    Text(
        label,
        color = fg,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 10.dp),
    )
}
