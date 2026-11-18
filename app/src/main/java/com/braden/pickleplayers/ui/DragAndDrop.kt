package com.braden.pickleplayers.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import com.braden.pickleplayers.Slot

/** Where a dragged name can be dropped. */
sealed interface DropTarget {
    data class Court(val slot: Slot) : DropTarget
    /** The Players pane: dropping here takes someone off their court. */
    data object Players : DropTarget
}

/**
 * Minimal long-press drag & drop. Drop targets register their bounds (in root
 * coordinates); drag sources update the finger position; on release we hit-test.
 */
class DragState {
    var dragging by mutableStateOf<String?>(null)
        private set
    var position by mutableStateOf(Offset.Zero)
        private set

    internal val targets = mutableMapOf<DropTarget, Rect>()

    /** The target under the finger, if any. */
    val hovered: DropTarget?
        get() {
            if (dragging == null) return null
            val p = position
            return targets.entries.firstOrNull { it.value.contains(p) }?.key
        }

    internal fun start(name: String, at: Offset) {
        dragging = name
        position = at
    }

    internal fun moveBy(delta: Offset) {
        position += delta
    }

    internal fun end() {
        dragging = null
    }
}

/** Registers this element as a drop target. */
@Composable
fun Modifier.dropTarget(target: DropTarget, state: DragState): Modifier {
    DisposableEffect(target, state) {
        onDispose { state.targets.remove(target) }
    }
    return onGloballyPositioned { state.targets[target] = it.boundsInRoot() }
}

private class CoordsHolder {
    var coords: LayoutCoordinates? = null
}

/** Makes this element draggable after a long-press; [onDrop] receives the target, or null. */
@Composable
fun Modifier.dragSource(
    name: String,
    state: DragState,
    onDrop: (name: String, target: DropTarget?) -> Unit,
): Modifier {
    val holder = remember { CoordsHolder() }
    val haptics = LocalHapticFeedback.current
    val currentOnDrop by rememberUpdatedState(onDrop)
    return this
        .onGloballyPositioned { holder.coords = it }
        .pointerInput(name, state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // Null if released early or cancelled (e.g. the list scrolled).
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val root = holder.coords?.takeIf { it.isAttached }?.localToRoot(longPress.position)
                    ?: longPress.position
                state.start(name, root)
                val completed = drag(longPress.id) { change ->
                    state.moveBy(change.positionChange())
                    change.consume()
                }
                val target = if (completed) state.hovered else null
                state.end()
                if (completed) currentOnDrop(name, target)
            }
        }
}
