package io.agora.flat.ui.activity.dev

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.setting.SettingsScreen
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.viewmodel.SettingsUiState

@AndroidEntryPoint
class DevSettingsActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                SettingsScreen(
                    state = SettingsUiState(),
                    onBack = { finish() },
                    onLogout = {},
                    onDownload = { Uri.EMPTY },
                    onAgreeStream = {},
                )
            }
        }
    }
}