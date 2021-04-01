package link.netless.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import link.netless.flat.ui.compose.BackTopAppBar

class CreateRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(Modifier.fillMaxSize()) {
                BackTopAppBar("CreateRoom", onBackPressed = { finish() }) {

                }
            }
        }
    }
}