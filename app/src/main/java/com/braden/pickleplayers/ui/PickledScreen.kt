package com.braden.pickleplayers.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braden.pickleplayers.Court
import com.braden.pickleplayers.GameMode
import com.braden.pickleplayers.PickledViewModel
import com.braden.pickleplayers.Placement
import com.braden.pickleplayers.Slot
import kotlin.math.roundToInt

@Composable
fun PickledScreen(vm: PickledViewModel = viewModel()) {
    val drag = remember { DragState() }

    val onDrop: (String, DropTarget?) -> Unit = { name, target ->
        when (target) {
            is DropTarget.Court -> vm.move(name, target.slot)
            DropTarget.Players -> vm.move(name, null)
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
                        onClick = vm::roll,
                        enabled = vm.roster.isNotEmpty(),
                        modifier = Modifier.weight(1f).padding(end = 16.dp).height(52.dp),
                    ) {
                        Text("🎲  Roll", style = MaterialTheme.typography.titleMedium)
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
                PlayersSection(
                    roster = vm.roster,
                    offCourt = vm.offCourt,
                    drag = drag,
                    onDrop = onDrop,
                    onAdd = vm::addPlayer,
                    onDelete = vm::deletePlayer,
                )
                vm.courts.forEachIndexed { index, court ->
                    CourtCard(
                        index = index,
                        court = court,
                        onMode = { vm.setCourtMode(index, it) },
                        // There must always be at least one court.
                        onRemove = if (vm.courts.size > 1) ({ vm.removeCourt(index) }) else null,
                        drag = drag,
                        onDrop = onDrop,
                        onUnpin = vm::unpin,
                    )
                }
                if (vm.courts.size < PickledViewModel.MAX_COURTS) {
                    OutlinedButton(
                        onClick = vm::addCourt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = "Add court" },
                    ) {
                        Text("+", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        DraggedChip(drag)
    }
}

/** Compact Doubles/Singles pill toggle shown in each court's header. */
@Composable
private fun ModeToggle(mode: GameMode, onMode: (GameMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(50), color = colors.surface.copy(alpha = 0.7f)) {
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * The single list of players. Shows everyone who isn't on a court: long-press to drag
 * onto a court, ✕ to remove. It is also the drop target for taking someone off a court.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersSection(
    roster: List<String>,
    offCourt: List<String>,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(newName)
        newName = ""
    }
    val colors = MaterialTheme.colorScheme
    val hovered = drag.hovered == DropTarget.Players
    val someoneOnCourt = offCourt.size < roster.size

    Card(
        modifier = Modifier.fillMaxWidth().dropTarget(DropTarget.Players, drag),
        border = if (hovered) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                buildString {
                    append("Players · ${roster.size}")
                    if (someoneOnCourt && offCourt.isNotEmpty()) append(" · ${offCourt.size} waiting")
                },
                style = MaterialTheme.typography.titleMedium,
            )
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
                Text(
                    "No players added yet",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                Hint("Long-press to drag a player onto a court. Roll fills in everyone else.")
                if (offCourt.isEmpty()) {
                    Text(
                        "Everyone's on a court",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        offCourt.forEach { name ->
                            PlayerChip(
                                name = name,
                                dragging = drag.dragging == name,
                                onDelete = { onDelete(name) },
                                modifier = Modifier.dragSource(name, drag, onDrop),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A player in the Players pane. Long-press handling comes in through [modifier];
 * the ✕ removes them entirely.
 */
@Composable
private fun PlayerChip(
    name: String,
    dragging: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        modifier = modifier.alpha(if (dragging) 0.3f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 40.dp).padding(start = 14.dp, end = 2.dp),
        ) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(
                "✕",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClickLabel = "Remove $name") { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CourtCard(
    index: Int,
    court: Court,
    onMode: (GameMode) -> Unit,
    /** Removes this court; null hides the ✕ (when it's the only court). */
    onRemove: (() -> Unit)?,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onUnpin: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Court ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                ModeToggle(mode = court.mode, onMode = onMode)
                if (onRemove != null) {
                    Text(
                        "✕",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClickLabel = "Remove court ${index + 1}") { onRemove() }
                            .padding(10.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamColumn(
                    slots = court.teamA(),
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
                    slots = court.teamB(),
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
    court: Court,
    drag: DragState,
    onDrop: (String, DropTarget?) -> Unit,
    onUnpin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in slots) {
            SlotBox(
                slot = Slot(courtIndex, i),
                placement = court.slots.getOrNull(i),
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

/** Floating copy of the name being dragged, shown just above the finger. */
@Composable
private fun DraggedChip(drag: DragState) {
    val name = drag.dragging ?: return
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(placeable.width, placeable.height) {
                placeable.place(
                    x = (drag.position.x - placeable.width / 2f).roundToInt(),
                    y = (drag.position.y - placeable.height * 1.5f).roundToInt(),
                )
            }
        },
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}


@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
