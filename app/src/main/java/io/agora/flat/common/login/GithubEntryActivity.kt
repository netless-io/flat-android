package io.agora.flat.common.login

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.LoginActivity
import javax.inject.Inject

/**
 * This activity is the unified Github callback.
 */
@AndroidEntryPoint
class GithubEntryActivity : ComponentActivity() {

    @Inject
    lateinit var loginManager: LoginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.data?.scheme == "x-agora-flat-client") {
            // check if the intent is from web to join room
            if (listOf("joinRoom", "replayRoom").contains(intent.data?.authority)) {
                Navigator.launchHomeActivity(this@GithubEntryActivity, intent)
            } else {
                // handle github auth
                loginManager.handleGithubAuth(this, intent)
            }
        }
        finish()
    }
}