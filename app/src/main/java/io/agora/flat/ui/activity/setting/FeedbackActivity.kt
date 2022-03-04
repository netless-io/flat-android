package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextBodyOneSecondary
import io.agora.flat.ui.theme.FlatColorBorder
import io.agora.flat.ui.viewmodel.FeedbackViewModel

class FeedbackActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: FeedbackViewModel = viewModel()

            FeedbackScreen(
                onBackPressed = { finish() },
                onCommitFeedback = { text ->
                    viewModel.uploadFeedback(text)
                },
            )
        }
    }

}

@Composable
internal fun FeedbackScreen(onBackPressed: () -> Unit, onCommitFeedback: (text: String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_feedback), onBackPressed = onBackPressed)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(2f),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = FlatColorBorder,
                unfocusedIndicatorColor = FlatColorBorder
            ),
            placeholder = {
                FlatTextBodyOneSecondary(stringResource(R.string.feedback_input_hint))
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) {
            FlatPrimaryTextButton(stringResource(R.string.commit), onClick = {
                onCommitFeedback.invoke(text)
            })
        }
    }
}

@Composable
@Preview
internal fun PreviewFeedback() {
    FeedbackScreen({}, {})
}