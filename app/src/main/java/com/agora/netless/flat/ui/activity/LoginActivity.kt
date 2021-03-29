package com.agora.netless.flat.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agora.netless.flat.ui.activity.ui.theme.FlatTitleTextStyle
import com.agora.netless.flat.ui.compose.BackTopAppBar
import com.agora.netless.flat.ui.compose.FlatColumnPage
import com.agora.netless.flat.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

fun launchLoginActivity(context: Context) {
    val intent = Intent(context, LoginActivity::class.java)
    context.startActivity(intent)
}

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoginPage {
                finish()
            }
        }
    }
}

@Composable
private fun LoginPage(onBackPressed: () -> Unit) {
    val userViewModel: UserViewModel = viewModel()
    val loginState = userViewModel.loginResource.observeAsState()

    FlatColumnPage {
        BackTopAppBar(title = "Login", onBackPressed)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {
                userViewModel.login()
            }) {
                Text(style = FlatTitleTextStyle, text = "Tag Login")
            }
        }
    }
}