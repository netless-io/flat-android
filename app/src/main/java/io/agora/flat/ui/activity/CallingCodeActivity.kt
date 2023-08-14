package io.agora.flat.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.data.model.Country
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.theme.Blue_2
import io.agora.flat.ui.theme.Blue_6
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.util.JsonUtils
import io.agora.flat.util.getCurrentLocale
import java.util.*


class CallingCodeActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                CallingCodeScreen(
                    onBackPressed = { finish() },
                    onPickCountry = {
                        val intent = Intent().apply {
                            putExtra(Constants.IntentKey.COUNTRY, it)
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                )
            }
        }
    }
}

data class AllCountry(
    val zh: List<Country>,
    val en: List<Country>,
)

@Composable
fun CallingCodeScreen(onBackPressed: () -> Unit, onPickCountry: (Country) -> Unit) {
    val countries = CallingCodeManager.countries

    Column {
        BackTopAppBar(
            title = stringResource(R.string.title_country_or_area),
            onBackPressed = onBackPressed
        )

        LazyColumn(Modifier.weight(1f)) {
            items(
                count = countries.size,
                key = { index: Int ->
                    countries[index].name
                }
            ) {
                CallingCodeItem(
                    country = countries[it],
                    modifier = Modifier
                        .height(60.dp)
                        .clickable { onPickCountry(countries[it]) })
            }
        }
    }
}

@Composable
fun CallingCodeItem(country: Country, modifier: Modifier) {
    val ccColor = if (isDarkTheme()) Blue_2 else Blue_6

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FlatTextBodyTwo(text = country.name, Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.weight(1f))
        FlatTextBodyTwo(text = country.cc, Modifier.padding(horizontal = 16.dp), color = ccColor)
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
fun CallingCodeItemPreview() {
    CallingCodeItem(
        country = Country("China", "+86", "CN"),
        Modifier.height(44.dp)
    )
}
