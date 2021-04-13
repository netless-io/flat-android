package link.netless.flat.ui.activity.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puculek.pulltorefresh.PullToRefresh
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import link.netless.flat.Constants
import link.netless.flat.R
import link.netless.flat.common.Navigator
import link.netless.flat.ui.activity.ui.theme.FlatColorBlue
import link.netless.flat.ui.activity.ui.theme.FlatColorBlueAlpha50
import link.netless.flat.ui.activity.ui.theme.FlatSmallTextStyle
import link.netless.flat.ui.activity.ui.theme.FlatTitleTextStyle
import link.netless.flat.ui.compose.FlatColumnPage
import link.netless.flat.ui.compose.FlatTopAppBar
import link.netless.flat.ui.viewmodel.UserViewModel

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var api: IWXAPI
    private var wxReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerToWx()
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

    // TODO 存在token时调用调用login接口
    private fun actionLoginState() {
        if (userViewModel.isLoggedIn()) {
            setContent { HomePage() }
            homeViewModel.loadRooms()
            homeViewModel.loadHistoryRooms()
        } else {
            Navigator.launchLoginActivity(this)
        }
    }
}

@Composable
fun HomePage() {
    val viewModel = viewModel(HomeViewModel::class.java)
    val viewState = viewModel.state.collectAsState()

    PullToRefresh(
        progressColor = FlatColorBlue,
        isRefreshing = viewState.value.refreshing,
        onRefresh = {
            viewModel.reloadRoomList()
        }
    ) {
        FlatColumnPage {
            // 顶部栏
            FlatHomeTopBar()
            // 操作区
            TopOperations()
            // 房间列表区
            HomeRoomLists(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onCategorySelected = viewModel::onRoomCategorySelected,
                selectedHomeCategory = viewState.value.selectedHomeCategory,
                roomList = viewState.value.roomList,
                roomHistory = viewState.value.roomHistoryList
            )
            // 底部状态区
            FlatHomeBottomBar()
        }
    }
}

@Composable
fun TopOperations() {
    Row(Modifier.fillMaxWidth()) {
        OperationItem(R.drawable.ic_home_create_room, R.string.create_room)
        OperationItem(R.drawable.ic_home_join_room, R.string.join_room)
        OperationItem(R.drawable.ic_home_subscribe_room, R.string.subscribe_room)
    }
}

@Composable
private fun RowScope.OperationItem(@DrawableRes id: Int, @StringRes tip: Int) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id),
                contentDescription = "",
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(id = tip), style = FlatSmallTextStyle)
        }
    }
}

@Composable
fun FlatHomeTopBar() {
    FlatTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.title_home), style = FlatTitleTextStyle)
        },
        actions = {
            Box {
                val context = LocalContext.current
                var expanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { expanded = true }) {
                    Image(
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .clip(shape = RoundedCornerShape(12.dp)),
                        painter = painterResource(id = R.drawable.header),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(onClick = { Navigator.launchSettingActivity(context) }) {
                        Image(
                            painter = painterResource(R.drawable.ic_user_profile_head),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("我的资料", Modifier.width(100.dp))
                    }
                    DropdownMenuItem(
                        onClick = { /* Handle settings! */ },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_user_profile_aboutus),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // TODO
                        Text("个人信息", Modifier.width(100.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun FlatHomeBottomBar() {
    BottomAppBar(elevation = 0.dp, backgroundColor = FlatColorBlueAlpha50) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { /*TODO*/ }) {
                Image(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null
                )
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            IconButton(onClick = { /*TODO*/ }) {
                Image(
                    painter = painterResource(R.drawable.ic_cloudstorage),
                    contentDescription = null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlatHomeTopBar()
}