package com.touchchef.wearable.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

@Composable
fun TouchChefTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = Typography(
            body1 = androidx.wear.compose.material.LocalTextStyle.current.copy(
                fontFamily = TouchChefTypography.bricolageGrotesque
            )
        ),
        content = content
    )
}