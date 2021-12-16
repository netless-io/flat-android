package io.agora.flat.ui.theme

import androidx.compose.foundation.layout.*
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

val RowScope.MaxHeightSpread
    get() = Modifier
        .fillMaxHeight()
        .weight(1f)

val ColumnScope.MaxWidth
    get() = Modifier.fillMaxWidth()

val BoxScope.MaxWidth
    get() = Modifier.fillMaxWidth()

val FillMaxSize
    get() = Modifier.fillMaxSize()

val MaxHeight
    get() = Modifier.fillMaxHeight()