package io.agora.flat.ui.activity.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Range
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.theme.FlatColorWhite
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPageLoading
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var api: IWXAPI
    private var wxReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainPage()
        }

        registerToWx()
        lifecycleScope.launch {
            mainViewModel.loginState.collect {
                if (it == LoginError) {
                    Navigator.launchLoginActivity(this@MainActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionLoginState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wxReceiver != null) {
            unregisterReceiver(wxReceiver)
        }
    }

    private fun registerToWx() {
        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, true)
        api.registerApp(Constants.WX_APP_ID)

        wxReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                api.registerApp(Constants.WX_APP_ID)
            }
        }
        registerReceiver(wxReceiver, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }

    private fun actionLoginState() {
        if (!mainViewModel.isLoggedIn()) {
            Navigator.launchLoginActivity(this)
        }
    }
}

@Composable
fun MainPage() {
    val viewModel = viewModel(MainViewModel::class.java)
    val mainTab by viewModel.mainTab.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    FlatColumnPage {
        Box(
            Modifier
                .fillMaxWidth(1f)
                .weight(1f),
            Alignment.Center
        ) {
            when (loginState) {
                LoginStart -> FlatPageLoading()
                LoginSuccess -> when (mainTab) {
                    MainTab.Home -> Home()
                    MainTab.CloudStorage -> CloudStorage()
                }
            }
        }
        FlatHomeBottomBar(mainTab, viewModel::onMainTabSelected)
    }
}

@Composable
private fun FlatHomeBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    val homeResId = when (selectedTab) {
        MainTab.Home -> R.drawable.ic_home_main_selected
        MainTab.CloudStorage -> R.drawable.ic_home_main_normal
    }
    val csResId = when (selectedTab) {
        MainTab.CloudStorage -> R.drawable.ic_home_cloudstorage_selected
        MainTab.Home -> R.drawable.ic_home_cloudstorage_normal
    }

    Divider()
    BottomAppBar(elevation = 0.dp, backgroundColor = FlatColorWhite) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.Home) }) {
                Image(painterResource(homeResId), null)
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { onTabSelected(MainTab.CloudStorage) }) {
                Image(painterResource(csResId), null)
            }
        }
    }
}