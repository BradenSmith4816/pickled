package com.example.pickled.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickledScreen(vm: PickledViewModel = viewModel()) {
    val drag = remember { DragState() }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val onDrop: (String, DropTarget?) -> Unit = { name, target ->
        when (target) {
            is DropTarget.Court -> vm.move(name, target.slot)
            DropTarget.Waiting -> vm.move(name, null)
            null -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("🥒 Pickled", fontWeight = FontWeight.Bold) },
                )
            },
            bottomBar = {
                BottomAppBar {
                    OutlinedButton(
                        onClick = vm::reset,
                        modifier = Modifier.padding(start = 16.dp).height(52.dp),
                    ) { Text("Reset") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = vm::roll,
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
                GameSetup(
                    mode = vm.mode,
                    courtCount = vm.courts.size,
                    suggestedCourts = vm.suggestedCourts,
                    onMode = vm::selectMode,
                    onCourtCount = vm::setCourtCount,
                )
                PlayersSection(
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
                        onUnassign = { vm.move(it, null) },
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
    suggestedCourts: Int,
    onMode: (GameMode) -> Unit,
    onCourtCount: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            GameMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = m == mode,
                    onClick = { onMode(m) },
                    shape = SegmentedButtonDefaults.itemShape(i, GameMode.entries.size),
                ) { Text(m.label) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Courts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (suggestedCourts > 0 && suggestedCourts != courtCount) {
                TextButton(onClick = { onCourtCount(suggestedCourts) }) { Text("Fit $suggestedCourts") }
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayersSection(
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

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Players · $playingCount playing",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (roster.isNotEmpty()) {
                    val allIn = playingCount == roster.size
                    TextButton(onClick = { onAll(!allIn) }) { Text(if (allIn) "None" else "All") }
                }
            }
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
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
            Hint("Long-press a name and drag it onto a court to choose partners. Roll fills in everyone else.")
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
    onUnassign: (String) -> Unit,
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
                    courtIndex = index, court = court, drag = drag, onDrop = onDrop, onUnassign = onUnassign,
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
                    courtIndex = index, court = court, drag = drag, onDrop = onDrop, onUnassign = onUnassign,
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
    onUnassign: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in slots) {
            SlotBox(
                slot = Slot(courtIndex, i),
                placement = court.getOrNull(i),
                drag = drag,
                onDrop = onDrop,
                onUnassign = onUnassign,
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
    onUnassign: (String) -> Unit,
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
                Text(
                    "✕",
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onUnassign(placement.name) }
                        .padding(8.dp),
                )
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

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
