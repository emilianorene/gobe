package com.gobe.tv.ui.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R
import com.gobe.tv.emulation.input.PadButton

/** One detected controller for the list: stable descriptor, display name, assigned port (or null). */
data class ControllerRow(val descriptor: String, val name: String, val port: Int?)

/** List of detected controllers; selecting one opens its detail. */
@Composable
fun ControllersListScreen(rows: List<ControllerRow>, onSelect: (String) -> Unit) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(rows.isNotEmpty()) { if (rows.isNotEmpty()) runCatching { firstFocus.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(stringResource(R.string.controllers_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (rows.isEmpty()) {
            Text(stringResource(R.string.controllers_none), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.controllers_select_hint), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            rows.forEachIndexed { i, r ->
                val player = r.port?.let { "P${it + 1}" } ?: stringResource(R.string.controllers_unassigned)
                Button(
                    onClick = { onSelect(r.descriptor) },
                    modifier = if (i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                ) { Text("${r.name}    ·    $player") }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** Detail for one controller: assign it to a player port + live input test. */
@Composable
fun ControllerDetailScreen(
    name: String,
    assignedPort: Int?,
    swaps: com.gobe.tv.emulation.input.ButtonSwaps,
    activeButtons: Set<PadButton>,
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,
    onAssign: (Int) -> Unit,
    onToggleSwapAB: () -> Unit,
    onToggleSwapXY: () -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.controllers_player) + ":", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..3).forEach { p ->
                val selected = assignedPort == p
                Button(
                    onClick = { onAssign(p) },
                    modifier = if (p == 0) Modifier.focusRequester(playFocus) else Modifier,
                ) { Text(if (selected) "● P${p + 1}" else "P${p + 1}") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.controllers_buttons) + ":", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onToggleSwapAB) {
                Text((if (swaps.swapAB) "● " else "") + stringResource(R.string.controllers_swap_ab))
            }
            Button(onClick = onToggleSwapXY) {
                Text((if (swaps.swapXY) "● " else "") + stringResource(R.string.controllers_swap_xy))
            }
        }
        Spacer(Modifier.height(24.dp))
        ControllerTestPanel(activeButtons, leftStick, rightStick, lastInput)
    }
}

/** The button-highlight + sticks + last-input test panel (shared). */
@Composable
fun ControllerTestPanel(
    activeButtons: Set<PadButton>,
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,
) {
    Column {
        Text(stringResource(R.string.controllers_test), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        val panelRows = listOf(
            listOf(PadButton.A, PadButton.B, PadButton.X, PadButton.Y),
            listOf(PadButton.L1, PadButton.R1, PadButton.L2, PadButton.R2),
            listOf(PadButton.SELECT, PadButton.START, PadButton.L3, PadButton.R3),
            listOf(PadButton.DPAD_UP, PadButton.DPAD_DOWN, PadButton.DPAD_LEFT, PadButton.DPAD_RIGHT),
        )
        panelRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b -> PadChip(b.name, b in activeButtons) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
        val sticks = "L (%.2f, %.2f)   R (%.2f, %.2f)".format(
            leftStick.first, leftStick.second, rightStick.first, rightStick.second,
        )
        Text(stringResource(R.string.controllers_sticks) + ": " + sticks, style = MaterialTheme.typography.bodyMedium)
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
