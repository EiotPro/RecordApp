package com.example.recordapp.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.composed

fun Modifier.animateDeletion(
    isDeleting: Boolean,
    onDelete: () -> Unit
): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    this
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(isDeleting) {
            detectHorizontalDragGestures { _, dragAmount ->
                scope.launch {
                    val newOffset = (offsetX.value + dragAmount).coerceIn(-500f, 0f)
                    offsetX.snapTo(newOffset)
                    if (offsetX.value < -300f) {
                        offsetX.animateTo(-1000f, animationSpec = tween(300))
                        onDelete()
                    } else if (offsetX.value > -100f) {
                        offsetX.animateTo(0f, animationSpec = tween(300))
                    }
                }
            }
        }
}

fun Modifier.fadeInOut(
    isVisible: Boolean,
    durationMillis: Int = 300
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis)
    )
    this.graphicsLayer(alpha = alpha)
} 