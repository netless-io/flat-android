package io.agora.flat.ui.activity.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginHelper
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.theme.MaxWidthSpread
import io.agora.flat.util.showToast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val loginHelper = LoginHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
        loginHelper.register()

        observerState()
    }

    private fun observerState() {
        lifecycleScope.launchWhenStarted {
            viewModel.roomPlayInfo.filterNotNull().collect {
                Navigator.launchRoomPlayActivity(this@MainActivity, it)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.error.filterNotNull().collect {
                showToast(it.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionLoginState()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginHelper.unregister()
    }

    private fun actionLoginState() {
        if (!viewModel.isLoggedIn()) {
            Navigator.launchLoginActivity(this)
        }
    }
}

@Composable
fun MainContent() {
    val viewModel: MainViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    MainContent(viewState, viewModel::onMainTabSelected)
}

@Composable
fun MainContent(viewState: MainViewState, onTabSelected: (MainTab) -> Unit) {
    val context = LocalContext.current
    if (viewState.loginState == LoginState.Error) {
        LaunchedEffect(true) {
            Navigator.launchLoginActivity(context)
        }
    }

    FlatColumnPage {
        Box(MaxWidthSpread, Alignment.Center) {
            if (viewState.loginState == LoginState.Login) {
                when (viewState.mainTab) {
                    MainTab.Home -> Home()
                    MainTab.CloudStorage -> CloudStorage()
                }
            }
        }
        FlatHomeBottomBar(viewState.mainTab, onTabSelected)
    }
}

@Composable
private fun FlatHomeBottomBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.CloudStorage -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.CloudStorage -> R.drawable.ic_home_cloudstorage_selected
        MainTab.Home -> R.drawable.ic_home_cloudstorage_normal
    }

    Divider()
    BottomAppBar(elevation = 0.dp, backgroundColor = MaterialTheme.colors.background) {
        Box(Modifier.weight(1f), Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.Home) }) {
                Image(painterResource(homeResId), null)
            }
        }
        Box(Modifier.weight(1f), Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.CloudStorage) }) {
                Image(painterResource(csResId), null)
            }
        }
    }
}