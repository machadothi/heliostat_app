package com.machadothi.templateapp.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import kotlin.math.cos
import kotlin.math.sin

/** A direction in the sky: azimuth from true north, clockwise; elevation above the horizon. */
data class SkyPoint(val azimuth: Double, val elevation: Double)

/**
 * The sky seen from above: the rim is the horizon, the centre is straight up,
 * north at the top. Plots the sun, where the mirror faces, where the beam goes,
 * and where it should go -- so "is it working?" is answerable at a glance: the
 * beam dot should sit inside the target ring.
 *
 * Anything below the horizon (a face-down stowed mirror, the sun at night) is
 * pinned to the rim and drawn hollow.
 */
@Composable
fun SkyDial(
    sun: SkyPoint?,
    mirror: SkyPoint?,
    beam: SkyPoint?,
    target: SkyPoint?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val sunColor = Color(0xFFFFC53D)
    // Silver, like the mirror itself: reads as "the mirror" and never as the sun.
    val mirrorColor = colors.onSurface.copy(alpha = 0.85f)
    val labelColor = colors.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 12.sp)
    val ringColor = colors.outline.copy(alpha = 0.35f)
    val skyTop = colors.secondaryContainer.copy(alpha = 0.55f)
    val skyEdge = colors.surfaceContainerHigh

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 22.dp.toPx()

            // Sky: brighter overhead, darker toward the horizon.
            drawCircle(Brush.radialGradient(listOf(skyTop, skyEdge), center, radius), radius, center)
            drawCircle(colors.outline.copy(alpha = 0.7f), radius, center, style = Stroke(1.5.dp.toPx()))
            val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
            for (elevation in listOf(30, 60)) {
                drawCircle(ringColor, radius * (90 - elevation) / 90f, center,
                    style = Stroke(1.dp.toPx(), pathEffect = dashed))
            }
            drawLine(ringColor, center.copy(y = center.y - radius), center.copy(y = center.y + radius))
            drawLine(ringColor, center.copy(x = center.x - radius), center.copy(x = center.x + radius))

            for ((label, azimuth) in listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)) {
                val layout = measurer.measure(label, labelStyle)
                val p = polar(center, radius + 12.dp.toPx(), azimuth, 0.0, full = true)
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2, p.y - layout.size.height / 2))
            }

            // Beam to target: the gap is the pointing error.
            if (beam != null && target != null) {
                drawLine(colors.tertiary.copy(alpha = 0.5f), polar(center, radius, beam), polar(center, radius, target),
                    strokeWidth = 2.dp.toPx(), pathEffect = dashed)
            }
            target?.let { t ->
                val p = polar(center, radius, t)
                drawCircle(colors.secondary, 11.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                drawLine(colors.secondary, p.copy(x = p.x - 16.dp.toPx()), p.copy(x = p.x - 7.dp.toPx()), 2.dp.toPx())
                drawLine(colors.secondary, p.copy(x = p.x + 7.dp.toPx()), p.copy(x = p.x + 16.dp.toPx()), 2.dp.toPx())
                drawLine(colors.secondary, p.copy(y = p.y - 16.dp.toPx()), p.copy(y = p.y - 7.dp.toPx()), 2.dp.toPx())
                drawLine(colors.secondary, p.copy(y = p.y + 7.dp.toPx()), p.copy(y = p.y + 16.dp.toPx()), 2.dp.toPx())
            }
            mirror?.let { m -> drawDiamond(polar(center, radius, m), 8.dp.toPx(), mirrorColor, hollow = m.elevation < 0) }
            beam?.let { b -> drawCircle(colors.tertiary, 6.dp.toPx(), polar(center, radius, b)) }
            sun?.let { s ->
                val p = polar(center, radius, s)
                if (s.elevation >= 0) {
                    drawCircle(Brush.radialGradient(listOf(sunColor.copy(alpha = 0.55f), Color.Transparent), p,
                        26.dp.toPx()), 26.dp.toPx(), p)
                    drawCircle(sunColor, 10.dp.toPx(), p)
                } else {
                    drawCircle(sunColor, 9.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(Color(0xFFFFC53D), "Sun")
            Legend(mirrorColor, "Mirror")
            Legend(colors.tertiary, "Beam")
            Legend(colors.secondary, "Target")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Sky direction -> canvas point. Below-horizon points are pinned to the rim. */
private fun polar(center: Offset, radius: Float, point: SkyPoint) =
    polar(center, radius, point.azimuth, point.elevation, full = false)

private fun polar(center: Offset, radius: Float, azimuth: Double, elevation: Double, full: Boolean): Offset {
    val fraction = if (full) 1.0 else ((90.0 - elevation.coerceIn(0.0, 90.0)) / 90.0)
    val r = radius * fraction
    val a = Math.toRadians(azimuth)
    return Offset((center.x + r * sin(a)).toFloat(), (center.y - r * cos(a)).toFloat())
}

private fun DrawScope.drawDiamond(p: Offset, half: Float, color: Color, hollow: Boolean) {
    val path = Path().apply {
        moveTo(p.x, p.y - half)
        lineTo(p.x + half, p.y)
        lineTo(p.x, p.y + half)
        lineTo(p.x - half, p.y)
        close()
    }
    if (hollow) drawPath(path, color, style = Stroke(2.dp.toPx())) else drawPath(path, color)
}
