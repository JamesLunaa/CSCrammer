package com.example.civora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.civora.R

private fun archivo(weight: Int, width: Float = 100f) = Font(
    resId = R.font.archivo,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
)

val Archivo = FontFamily(
    archivo(400), archivo(500), archivo(600), archivo(700), archivo(800),
)

/** Narrow Archivo (wdth 68) for amounts, scores and timers. */
val ArchivoNarrow = FontFamily(
    archivo(600, 68f), archivo(700, 68f), archivo(800, 68f),
)

private const val TABULAR = "tnum"

/** Hero score: 64 / 800. */
val HeroNumber = TextStyle(
    fontFamily = ArchivoNarrow, fontWeight = FontWeight.ExtraBold,
    fontSize = 64.sp, lineHeight = 68.sp, fontFeatureSettings = TABULAR,
)

/** Totals: 24 / 700. */
val TotalNumber = TextStyle(
    fontFamily = ArchivoNarrow, fontWeight = FontWeight.Bold,
    fontSize = 24.sp, lineHeight = 30.sp, fontFeatureSettings = TABULAR,
)

val CivoraTypography = Typography(
    displayLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)
