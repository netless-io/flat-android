package com.agora.netless.flat.ui.activity.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    h6 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),

    body1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = FlatTextPrimary,
    ),

    body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = FlatTextSecondary,
    ),
)

val FlatTitleTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    color = FlatTextPrimary,
)

val FlatCommonTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    color = FlatTextPrimary,
)

val FlatSmallTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    color = FlatTextPrimary,
)

val FlatCommonTipTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    color = FlatTextSecondary,
)