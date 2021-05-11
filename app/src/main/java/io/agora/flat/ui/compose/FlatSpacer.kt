package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import io.agora.flat.ui.theme.*

@Composable
fun FlatNormalHorizontalSpacer() {
    Spacer(NormalHorizontalModifier)
}

@Composable
fun FlatLargeHorizontalSpacer() {
    Spacer(LargeHorizontalModifier)
}

@Composable
fun FlatSmallVerticalSpacer() {
    Spacer(SmallVerticalModifier)
}

@Composable
fun FlatNormalVerticalSpacer() {
    Spacer(NormalVerticalModifier)
}

@Composable
fun FlatLargeVerticalSpacer() {
    Spacer(LargeVerticalModifier)
}