package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatTheme

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

/**
 * HomeScreen and CloudScreen TopAppbar Style
 */
@Composable
fun FlatMainTopAppBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    FlatTopAppBar(
        title = {
            Text(
                text = title,
                color = FlatTheme.colors.textTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W600
                ),
            )
        },
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
                Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null)
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
                Icon(painterResource(R.drawable.ic_title_close), contentDescription = null)
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