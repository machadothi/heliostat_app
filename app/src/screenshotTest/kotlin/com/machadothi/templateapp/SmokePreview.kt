package com.machadothi.templateapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.MetricTile
import com.machadothi.templateapp.ui.component.ModeBadge
import com.machadothi.templateapp.ui.component.SafetyBarContent
import com.machadothi.templateapp.ui.component.SignalBars
import com.machadothi.templateapp.ui.component.SkyDial
import com.machadothi.templateapp.ui.component.SkyPoint
import com.machadothi.templateapp.ui.theme.MyApplicationTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Components() {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("track", "idle", "stow", "defocus", "manual", "fault").forEach { ModeBadge(it) }
            }
            SkyDial(
                sun = SkyPoint(165.0, 61.0),
                mirror = SkyPoint(175.0, 36.0),
                beam = SkyPoint(178.0, 9.8),
                target = SkyPoint(180.0, 10.0),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Efficiency", "90", Modifier.weight(1f), unit = "%")
                MetricTile("Servos", "12.4 / 9.9", Modifier.weight(1f), unit = "V")
            }
            Banner("The heliostat stopped for safety.", BannerKind.Error, title = "Latched: base tilted 7.5°")
            Row { SignalBars(-58); SignalBars(-78); SignalBars(-92) }
            SafetyBarContent(message = null, onMode = {})
        }
    }
}

@PreviewTest
@Preview(name = "dark", widthDp = 380, heightDp = 1000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComponentsDark() = MyApplicationTheme(darkTheme = true) { Components() }

@PreviewTest
@Preview(name = "light", widthDp = 380, heightDp = 1000)
@Composable
fun ComponentsLight() = MyApplicationTheme(darkTheme = false) { Components() }
