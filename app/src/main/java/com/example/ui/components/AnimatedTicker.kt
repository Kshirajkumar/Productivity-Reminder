package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.example.ui.util.HapticHelper

/**
 * High-performance tactile bounce modifier for buttons and cards.
 * Scales down smoothly on touch and springs back up with subtle haptic feedback.
 */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enableHaptic: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (enableHaptic) {
                    HapticHelper.click(context)
                }
                onClick()
            }
        )
}

/**
 * Optimized Animated Time Text.
 * Smoothly slides up/down when updated, without per-character layout overhead.
 */
@Composable
fun AnimatedTimeText(
    timeString: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null
) {
    AnimatedContent(
        targetState = timeString,
        transitionSpec = {
            (slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) { height -> height / 2 } + fadeIn(tween(150))) togetherWith
                    (slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) { height -> -height / 2 } + fadeOut(tween(150)))
        },
        label = "timeTextSlide",
        modifier = modifier
    ) { text ->
        Text(
            text = text,
            style = style,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

/**
 * Animated Number Ticker for Snooze or Counters that slides numbers up/down smoothly.
 */
@Composable
fun AnimatedNumberTicker(
    number: Int,
    suffix: String = "",
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = number,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                }
            },
            label = "numberTicker"
        ) { targetNumber ->
            Text(
                text = "$targetNumber",
                style = style,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }

        if (suffix.isNotBlank()) {
            Text(
                text = suffix,
                style = style,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        }
    }
}
