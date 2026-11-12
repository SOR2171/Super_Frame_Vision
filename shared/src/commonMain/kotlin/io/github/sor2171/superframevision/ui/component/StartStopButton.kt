package io.github.sor2171.superframevision.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Animated primary/error toggle button for starting or stopping queue processing.
 */
@Composable
fun StartStopButton(
    isProcessing: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isProcessing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 300),
        label = "buttonBackgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isProcessing) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(durationMillis = 300),
        label = "buttonContentColor"
    )

    Button(
        onClick = if (isProcessing) onStop else onStart,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        AnimatedContent(
            targetState = isProcessing,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.8f))
                    .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.8f))
            },
            label = "iconTransition"
        ) { targetIsProcessing ->
            Icon(
                imageVector = if (targetIsProcessing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (targetIsProcessing) "Stop Process" else "Start Process"
            )
        }
    }
}
