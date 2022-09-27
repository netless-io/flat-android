package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlatNormalHorizontalSpacer() {
    Spacer(Modifier.width(16.dp))
}

@Composable
fun FlatLargeHorizontalSpacer() {
    Spacer(Modifier.width(32.dp))
}

@Composable
fun FlatSmallVerticalSpacer() {
    Spacer(Modifier.height(8.dp))
}

@Composable
fun FlatNormalVerticalSpacer() {
    Spacer(Modifier.height(16.dp))
}

@Composable
fun FlatLargeVerticalSpacer() {
    Spacer(Modifier.height(24.dp))
}