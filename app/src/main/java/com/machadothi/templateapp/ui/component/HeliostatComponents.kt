package com.machadothi.templateapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BackHand
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.machadothi.templateapp.ui.theme.ModeColors
import com.machadothi.templateapp.ui.theme.Numeric

/** Visual identity of each operating mode: label, icon, colour. */
data class ModeLook(val label: String, val icon: ImageVector, val color: Color)

fun modeLook(mode: String): ModeLook = when (mode) {
    "track" -> ModeLook("Tracking", Icons.Rounded.WbSunny, ModeColors.Track)
    "defocus" -> ModeLook("Defocused", Icons.Rounded.CenterFocusWeak, ModeColors.Defocus)
    "stow" -> ModeLook("Stowed", Icons.Rounded.VerticalAlignBottom, ModeColors.Stow)
    "manual" -> ModeLook("Manual", Icons.Rounded.BackHand, ModeColors.Manual)
    "home" -> ModeLook("Homing", Icons.Rounded.Home, ModeColors.Home)
    "fault" -> ModeLook("Fault", Icons.Rounded.Warning, ModeColors.Fault)
    "estop" -> ModeLook("E-stop", Icons.Rounded.Report, ModeColors.Fault)
    else -> ModeLook("Idle", Icons.Rounded.PauseCircle, ModeColors.Idle)
}

/** A pill showing the mode, tinted in the mode's colour. */
@Composable
fun ModeBadge(mode: String, modifier: Modifier = Modifier) {
    val look = modeLook(mode)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = look.color.copy(alpha = 0.18f),
        contentColor = look.color,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(look.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(look.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** One live reading: a small label over a large number. */
@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, unit: String? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge.merge(Numeric),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                unit?.let {
                    Text(
                        " $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

/** A titled card grouping related content. */
@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

enum class BannerKind { Info, Warning, Error }

/** A tinted message box with an icon, for warnings the user must read. */
@Composable
fun Banner(
    text: String,
    kind: BannerKind,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content, icon) = when (kind) {
        BannerKind.Info -> Triple(scheme.secondaryContainer, scheme.onSecondaryContainer, Icons.Rounded.WbSunny)
        BannerKind.Warning -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, Icons.Rounded.Warning)
        BannerKind.Error -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Rounded.Report)
    }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container, contentColor = content)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                Text(text, style = MaterialTheme.typography.bodyMedium)
                actions?.let { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { it() } }
            }
        }
    }
}

/** Four bars, filled according to RSSI. */
@Composable
fun SignalBars(rssi: Int, modifier: Modifier = Modifier) {
    val level = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        rssi >= -90 -> 1
        else -> 0
    }
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (bar in 1..4) {
            Surface(
                color = if (bar <= level) on else off,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(width = 4.dp, height = (4 + bar * 3).dp),
            ) {}
        }
    }
}

/** Top bar with an optional back arrow and a subtitle under the title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeliostatTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
