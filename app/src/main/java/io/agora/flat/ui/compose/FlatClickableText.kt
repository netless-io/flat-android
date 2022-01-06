package io.agora.flat.ui.compose

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import io.agora.flat.common.Navigator

data class ClickableItem(val part: String, val tag: String, val annotation: String)

@Composable
fun FlatClickableText(text: String, items: List<ClickableItem>) {
    val context = LocalContext.current

    val annotatedText = buildAnnotatedString {
        var last = 0
        while (last < text.length) {
            val pair = text.findAnyOf(strings = items.map { it.part }, last)
            if (pair != null) {
                withStyle(style = SpanStyle(MaterialTheme.colors.onSurface)) {
                    append(text.substring(last, pair.first))
                }
                items.find { it.part == pair.second }?.run {
                    pushStringAnnotation(tag = tag, annotation = annotation)
                }
                withStyle(style = SpanStyle(MaterialTheme.colors.primary)) {
                    append(pair.second)
                }
                pop()

                last = pair.first + pair.second.length
            } else {
                withStyle(style = SpanStyle(MaterialTheme.colors.onSurface)) {
                    append(text.substring(last))
                }
                break
            }
        }
    }

    ClickableText(text = annotatedText, onClick = { offset ->
        items.forEach { item ->
            annotatedText.getStringAnnotations(tag = item.tag, start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    Navigator.launchWebViewActivity(context, annotation.item)
                }
        }
    })
}