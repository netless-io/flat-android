package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FlatTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(backgroundColor = MaterialTheme.colors.background, elevation = 0.dp) {
        if (navigationIcon == null) {
            Spacer(TitleInsetWithoutIcon)
        } else {
            Row(TitleIconModifier, verticalAlignment = Alignment.CenterVertically) {
                navigationIcon()
            }
        }
        Row(
            Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            title()
        }

        Row(
            Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun FlatTopAppBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FlatTopAppBar(
        title = { FlatTextTitle(text = title) },
        actions = actions
    )
}

@Composable
fun BackTopAppBar(
    title: String,
    onBackPressed: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FlatTopAppBar(
        title = { FlatTextTitle(text = title) },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = null)
            }
        },
        actions = actions
    )
}

@Composable
fun CloseTopAppBar(
    title: String,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FlatTopAppBar(
        title = { FlatTextTitle(text = title) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = null)
            }
        },
        actions = actions
    )
}


@Preview
@Composable
fun DefaultPreview() {
    FlatColumnPage {
        BackTopAppBar("Long Long Long Long Long Long Long Long Title", {})
        CloseTopAppBar("Foo Title", {})
    }
}

private val TitleInsetWithoutIcon = Modifier.width(16.dp)
private val TitleIconModifier = Modifier.fillMaxHeight()