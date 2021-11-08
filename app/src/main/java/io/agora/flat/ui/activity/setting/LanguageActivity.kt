package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.CustomInteractionSource
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.viewmodel.LanguageViewModel

class LanguageActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: LanguageViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            FlatColumnPage {
                BackTopAppBar(title = stringResource(R.string.title_language), { finish() }) {
                    TextButton(onClick = {
                        viewModel.save()
                        Navigator.launchHomeActivity(this@LanguageActivity)
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }

                LazyColumn {
                    items(
                        count = state.items.size,
                        key = { index: Int -> state.items[index].display },
                    )
                    { index ->
                        LanguageItem(
                            state.items[index],
                            index == state.index,
                            Modifier
                                .height(54.dp)
                                .fillMaxWidth()
                                .clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = remember { CustomInteractionSource() },
                                    onClick = { viewModel.selectIndex(index) }
                                )
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(item: LanguageManager.Item, isSelected: Boolean, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(item.display))
        Spacer(Modifier.weight(1f))
        if (isSelected) Icon(Icons.Outlined.Check, "", tint = MaterialTheme.colors.primary)
    }
}