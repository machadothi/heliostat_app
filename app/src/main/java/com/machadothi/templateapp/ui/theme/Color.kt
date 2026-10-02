package com.machadothi.templateapp.ui.theme

import androidx.compose.ui.graphics.Color

// Heliostat palette: solar amber on a deep night-sky navy, with a daylight-sky
// blue and a warm coral for the reflected beam.

// Amber: the sun, and the primary action colour.
val Amber20 = Color(0xFF3F2500)
val Amber40 = Color(0xFF8A5300)
val Amber80 = Color(0xFFFFB84D)
val Amber90 = Color(0xFFFFDDAE)

// Sky blue: secondary, information, the target.
val Sky30 = Color(0xFF00497A)
val Sky40 = Color(0xFF0061A1)
val Sky80 = Color(0xFF8FCBFF)
val Sky90 = Color(0xFFCCE5FF)

// Coral: the beam, and tertiary accents.
val Coral40 = Color(0xFFA33A1E)
val Coral80 = Color(0xFFFFB4A1)

// Night sky surfaces (dark theme).
val Night05 = Color(0xFF070C17)
val Night10 = Color(0xFF0B1220)
val Night15 = Color(0xFF111A2D)
val Night20 = Color(0xFF17223A)
val Night30 = Color(0xFF223050)
val Mist80 = Color(0xFFC3CAD9)
val Mist90 = Color(0xFFDDE3F0)

// Daylight surfaces (light theme): warm paper rather than stark white.
val Paper98 = Color(0xFFFFFAF3)
val Paper95 = Color(0xFFF6EEE3)
val Paper90 = Color(0xFFECE2D4)
val Ink10 = Color(0xFF1E1B16)
val Ink30 = Color(0xFF4D463C)

val ErrorLight = Color(0xFFBA1A1A)
val ErrorDark = Color(0xFFFFB4AB)

/** Per-mode accent colours, shared by the mode badge and the sky dial. */
object ModeColors {
    val Track = Color(0xFFFFB300)
    val Defocus = Color(0xFFFF8A3D)
    val Stow = Color(0xFF5B9BD5)
    val Idle = Color(0xFF8A93A6)
    val Manual = Color(0xFFB388FF)
    val Home = Color(0xFF26C6DA)
    val Fault = Color(0xFFFF5449)
}

/** Emergency stop: the same unmistakable red in light and dark themes. */
val StopRed = Color(0xFFD32F2F)
