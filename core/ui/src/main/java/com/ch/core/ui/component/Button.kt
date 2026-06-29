package com.ch.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    TEXT,
    DANGER
}

enum class ButtonSize {
    LARGE,
    MEDIUM,
    SMALL
}

@Composable
fun GlobalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    val height = when (size) {
        ButtonSize.LARGE -> 52.dp
        ButtonSize.MEDIUM -> 44.dp
        ButtonSize.SMALL -> 36.dp
    }

    val shape = RoundedCornerShape(10.dp)

    val containerColor = when (variant) {
        ButtonVariant.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonVariant.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        ButtonVariant.TEXT -> Color.Transparent
        ButtonVariant.DANGER -> MaterialTheme.colorScheme.error
    }

    val contentColor = when (variant) {
        ButtonVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonVariant.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonVariant.TEXT -> MaterialTheme.colorScheme.primary
        ButtonVariant.DANGER -> MaterialTheme.colorScheme.onError
    }

    if (variant == ButtonVariant.TEXT) {
        TextButton(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = enabled && !loading,
            shape = shape
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                icon?.invoke()
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = when (size) {
                        ButtonSize.LARGE -> 16.sp
                        ButtonSize.MEDIUM -> 14.sp
                        ButtonSize.SMALL -> 12.sp
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = enabled && !loading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.5f),
                disabledContentColor = contentColor.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                icon?.invoke()
                Text(
                    text = text,
                    fontSize = when (size) {
                        ButtonSize.LARGE -> 16.sp
                        ButtonSize.MEDIUM -> 14.sp
                        ButtonSize.SMALL -> 12.sp
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
