package io.agora.flat.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
// TODO 排版信息中不包含颜色配置，将具体颜色配置定义在业务自定义组件中

val Typography = Typography(
    h6 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),

    body1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = FlatColorTextPrimary,
    ),

    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = FlatColorTextSecondary,
    ),
)

val FlatTitleTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    color = FlatColorTextPrimary,
)

val FlatCommonTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    color = FlatColorTextPrimary,
)

val FlatSmallTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    color = FlatColorTextPrimary,
)

val FlatCommonTipTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    color = FlatColorTextSecondary,
)

val FlatSmallTipTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    color = FlatColorTextSecondary,
)