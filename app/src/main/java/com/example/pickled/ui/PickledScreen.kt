package com.example.pickled.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pickled.GameMode
import com.example.pickled.PickledViewModel
import com.example.pickled.Placement
import com.example.pickled.Slot
import kotlin.math.roundToInt

@Composable
fun PickledScreen(vm: PickledViewModel = viewModel()) {
    val drag = remember { DragState() }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var playersExpanded by rememberSaveable { mutableStateOf(true) }

    val onDrop: (String, DropTarget?) -> Unit = { name, target ->
        when (target) {
            is DropTarget.Court -> vm.move(name, target.slot)
            DropTarget.Waiting -> vm.move(name, null)
            null -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomAppBar {
                    OutlinedButton(
                        onClick = vm::reset,
                        modifier = Modifier.padding(start = 16.dp).height(52.dp),
                    ) { Text("Reset") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            vm.roll()
                            playersExpanded = false
                        },
                        enabled = vm.activePlayers.isNotEmpty(),
                        modifier = Modifier.weight(1f).padding(end = 16.dp).height(52.dp),
                    ) {
                        Text("🎲  Roll ${vm.mode.label}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState(), enabled = drag.dragging == null)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Pickle Players",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                GameSetup(
                    mode = vm.mode,
                    courtCount = vm.courts.size,
                    onMode = vm::selectMode,
                    onCourtCount = vm::setCourtCount,
                )
                PlayersSection(
                    expanded = playersExpanded,
                    onExpandedChange = { playersExpanded = it },
                    roster = vm.roster,
                    active = vm.active,
                    onAdd = vm::addPlayer,
                    onToggle = vm::toggleActive,
                    onDelete = { pendingDelete = it },
                    onAll = vm::setEveryoneActive,
                )
                WaitingSection(
                    waiting = vm.waiting,
                    drag = drag,
                    onDrop = onDrop,
                )
                vm.courts.forEachIndexed { index, court ->
                    CourtCard(
                        index = index,
                        court = court,
                        mode = vm.mode,
                        drag = drag,
                        onDrop = onDrop,
                        onUnpin = vm::unpin,
                    )
                }
            }
        }
        DraggedChip(drag)
    }

    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove $name?") },
            text = { Text("They'll be removed from your saved player list.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePlayer(name); pendingDelete = null }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GameSetup(
    mode: GameMode,
    courtCount: Int,
    onMode: (GameMode) -> Unit,
    onCourtCount: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Game", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            ModeToggle(mode = mode, onMode = onMode)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Courts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalIconButton(onClick = { onCourtCount(courtCount - 1) }, enabled = courtCount > 1) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "$courtCount",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            FilledTonalIconButton(
                onClick = { onCourtCount(courtCount + 1) },
                enabled = courtCount < PickledViewModel.MAX_COURTS,
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Compact pill toggle, sized to sit in a row like the court stepper. */
@Composable
private fun ModeToggle(mode: GameMode, onMode: (GameMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(50), color = colors.surfaceVariant) {
        Row(Modifier.padding(3.dp)) {
            GameMode.entries.forEach { m ->
                val selected = m == mode
                Text(
                    m.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) colors.primary else Color.Transparent)
                        .clickable { onMode(m) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    roster: List<String>,
    active: Set<String>,
    onAdd: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAll: (Boolean) -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(newName)
        newName = ""
    }
    val playingCount = roster.count { it in active }
    val colors = MaterialTheme.colorScheme
    // Chevron points down when open, right when collapsed.
    val arrowRotation by animateFloatAsState(if (expanded) 0f else -90f, label = "arrow")

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Whole header row toggles.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandedChange(!expanded) }
                    .heightIn(min = 44.dp),
            ) {
                Chevron(
                    color = colors.primary,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(24.dp)
                        .rotate(arrowRotation),
                )
                Text(
                    "Players · $playingCount playing",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Add a name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = submit, enabled = newName.isNotBlank()) { Text("Add") }
                    }
                    if (roster.isEmpty()) {
                        Hint("Add everyone who plays. Names are saved for next time.")
                    } else {
                        val allIn = playingCount == roster.size
                        OutlinedButton(onClick = { onAll(!allIn) }) {
                            Text(if (allIn) "Deselect all" else "Select all")
                        }
                        Hint("Tap a name to mark who's here today.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            roster.forEach { name ->
                                FilterChip(
                                    selected = name in active,
                                    onClick = { onToggle(name) },
                                    label = { Text(name) },
                                    trailingIcon = {
                                        Text(
                                            "✕",
                                            modifier = Modifier
                                                .clickable { onDelete(name) }
                                                .padding(4.dp),
                                            color = colors.onSurfaceVariant,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaitingSection(
    waiting: List<String>,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
) {
    val hovered = drag.hovered == DropTarget.Waiting
    Card(
        modifier = Modifier.fillMaxWidth().dropTarget(DropTarget.Waiting, drag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = if (hovered) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Waiting · ${waiting.size}", style = MaterialTheme.typography.titleMedium)
            Hint("Long-press to drag a player onto a court. Roll fills in everyone else.")
            if (waiting.isEmpty()) {
                Text(
                    "No one waiting",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    waiting.forEach { name ->
                        NameChip(
                            name = name,
                            modifier = Modifier
                                .alpha(if (drag.dragging == name) 0.3f else 1f)
                                .dragSource(name, drag, onDrop),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourtCard(
    index: Int,
    court: List<Placement?>,
    mode: GameMode,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onUnpin: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Court ${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamColumn(
                    slots = 0 until mode.perSide,
                    courtIndex = index, court = court, drag = drag, onDrop = onDrop, onUnpin = onUnpin,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "vs",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TeamColumn(
                    slots = mode.perSide until mode.perCourt,
                    courtIndex = index, court = court, drag = drag, onDrop = onDrop, onUnpin = onUnpin,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(
    slots: IntRange,
    courtIndex: Int,
    court: List<Placement?>,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onUnpin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in slots) {
            SlotBox(
                slot = Slot(courtIndex, i),
                placement = court.getOrNull(i),
                drag = drag,
                onDrop = onDrop,
                onUnpin = onUnpin,
            )
        }
    }
}

@Composable
private fun SlotBox(
    slot: Slot,
    placement: Placement?,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onUnpin: (String) -> Unit,
) {
    val target = DropTarget.Court(slot)
    val hovered = drag.hovered == target
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (placement == null) Color.Transparent else colors.surface,
        border = BorderStroke(
            width = if (hovered) 2.dp else 1.dp,
            color = if (hovered) colors.primary else colors.outline.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .dropTarget(target, drag)
            .then(
                if (placement != null) {
                    Modifier
                        .alpha(if (drag.dragging == placement.name) 0.3f else 1f)
                        .dragSource(placement.name, drag, onDrop)
                } else Modifier,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp).heightIn(min = 48.dp),
        ) {
            if (placement == null) {
                Text("—", color = colors.onSurfaceVariant)
            } else {
                if (placement.pinned) Text("📌 ", style = MaterialTheme.typography.bodySmall)
                Text(
                    placement.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (placement.pinned) {
                    // Un-pin only: the player stays here but can be reshuffled by the next roll.
                    Text(
                        "✕",
                        color = colors.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onUnpin(placement.name) }
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NameChip(name: String, modifier: Modifier = Modifier, lifted: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (lifted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (lifted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (lifted) 8.dp else 1.dp,
        modifier = modifier,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Floating copy of the name being dragged, shown just above the finger. */
@Composable
private fun DraggedChip(drag: DragState) {
    val name = drag.dragging ?: return
    NameChip(
        name = name,
        lifted = true,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(placeable.width, placeable.height) {
                placeable.place(
                    x = (drag.position.x - placeable.width / 2f).roundToInt(),
                    y = (drag.position.y - placeable.height * 1.5f).roundToInt(),
                )
            }
        },
    )
}

/** A single downward-pointing chevron line (∨). */
@Composable
private fun Chevron(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.8f, h * 0.35f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
