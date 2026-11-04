package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Adds a tactile bounce animation to any clickable element.
 * Scales down to 0.94f when pressed and springs back when released.
 */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Animated Time Text that slides characters vertically when updating (e.g. 3:43 PM -> 3:44 PM).
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeString.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) { height -> height } togetherWith
                            slideOutVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) { height -> -height }
                },
                label = "char_$index"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            }
        }
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
                    slideInVertically { height -> height } togetherWith
                            slideOutVertically { height -> -height }
                } else {
                    slideInVertically { height -> -height } togetherWith
                            slideOutVertically { height -> height }
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
