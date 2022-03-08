package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.DarkModeManager
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.DarkModeViewModel

class DarkModeActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: DarkModeViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            FlatColumnPage {
                BackTopAppBar(title = stringResource(R.string.title_dark_mode), { finish() }) {
                    TextButton(onClick = {
                        viewModel.save()
                        Navigator.launchHomeActivity(this@DarkModeActivity)
                    }) {
                        FlatTextButton(stringResource(R.string.save))
                    }
                }

                LazyColumn {
                    items(
                        count = state.modes.size,
                        key = { index: Int -> state.modes[index].display },
                    )
                    { index ->
                        DarkModeItem(
                            state.modes[index], state.modes[index] == state.current,
                            Modifier
                                .height(54.dp)
                                .fillMaxWidth()
                                .clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = remember { CustomInteractionSource() },
                                    onClick = { viewModel.selectMode(state.modes[index]) }
                                )
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkModeItem(mode: DarkModeManager.Mode, isSelected: Boolean, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FlatTextBodyOne(stringResource(mode.display))
        Spacer(Modifier.weight(1f))
        if (isSelected) Icon(Icons.Outlined.Check, "", tint = MaterialTheme.colors.primary)
    }
}