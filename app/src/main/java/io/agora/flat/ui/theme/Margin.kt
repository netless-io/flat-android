package io.agora.flat.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val SmallVerticalModifier = Modifier.height(
    8.dp
)

val NormalVerticalModifier = Modifier.height(
    16.dp
)

val LargeVerticalModifier = Modifier.height(
    24.dp
)

val NormalHorizontalModifier = Modifier.width(
    16.dp
)

val LargeHorizontalModifier = Modifier.width(
    32.dp
)

val ColumnScope.MaxWidthSpread
    get() = Modifier
        .fillMaxWidth()
        .weight(1f)