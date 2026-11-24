package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeScheme(
    val displayName: String,
    val subtitle: String,
    val previewPrimary: Color,
    val previewAccent: Color,
) {
    BENTO_NEON(
        displayName = "Bento Neon",
        subtitle = "Ungu Violet & Lime",
        previewPrimary = NeonHeroStart,
        previewAccent = NeonLime,
    ),
    EMERALD_MINT(
        displayName = "Emerald Mint",
        subtitle = "Mint Segar & Emas",
        previewPrimary = MintHeroStart,
        previewAccent = MintGold,
    ),
    OCEAN_COBALT(
        displayName = "Ocean Cobalt",
        subtitle = "Biru Langit & Koral",
        previewPrimary = OceanHeroStart,
        previewAccent = OceanCoral,
    ),
    SUNSET_ROSE(
        displayName = "Sunset Rose",
        subtitle = "Mawar Hangat & Kuning",
        previewPrimary = RoseHeroStart,
        previewAccent = RoseButter,
    );

    companion object {
        fun fromRaw(raw: String?): ThemeScheme =
            entries.firstOrNull { it.name == raw } ?: BENTO_NEON
    }
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("Sistem"),
    DARK("Gelap"),
    LIGHT("Terang");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

@Immutable
data class BentoColors(
    val isDark: Boolean,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val heroTrack: Color,
    val heroDivider: Color,
    val accentTile: Color,
    val onAccentTile: Color,
    val accentIconContainer: Color,
    val cardSurface: Color,
    val nestedCardSurface: Color,
    val iconBadgeBg: Color,
    val borderSubtle: Color,
    val mutedText: Color,
    val highlightLabel: Color,
    val incomeColor: Color,
    val expenseColor: Color,
    val warningColor: Color,
    val pocketColors: List<Color>,
)

val LocalBentoColors = staticCompositionLocalOf {
    bentoColorsFor(ThemeScheme.BENTO_NEON, isDark = true)
}

val MaterialTheme.bentoColors: BentoColors
    @Composable get() = LocalBentoColors.current

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun bentoColorsFor(scheme: ThemeScheme, isDark: Boolean): BentoColors =
    when (scheme) {
        ThemeScheme.BENTO_NEON -> if (isDark) {
            BentoColors(
                isDark = true,
                heroStart = NeonHeroStart,
                heroEnd = NeonHeroEnd,
                onHero = NeonHeroInk,
                heroTrack = NeonHeroInk.copy(alpha = 0.18f),
                heroDivider = NeonHeroInk.copy(alpha = 0.14f),
                accentTile = NeonLime,
                onAccentTile = NeonHeroInk,
                accentIconContainer = NeonLimeIconBg,
                cardSurface = NeonDarkCard,
                nestedCardSurface = NeonDarkNested,
                iconBadgeBg = Color(0xFF282838),
                borderSubtle = NeonDarkBorder,
                mutedText = NeonDarkMuted,
                highlightLabel = NeonLime,
                incomeColor = SemanticIncomeDark,
                expenseColor = SemanticExpenseDark,
                warningColor = SemanticWarningDark,
                pocketColors = listOf(NeonHeroStart, NeonLime, Color(0xFF60A5FA), Color(0xFFF472B6), Color(0xFF34D399), Color(0xFFFBBF24)),
            )
        } else {
            BentoColors(
                isDark = false,
                heroStart = NeonHeroStart,
                heroEnd = NeonHeroEnd,
                onHero = NeonHeroInk,
                heroTrack = NeonHeroInk.copy(alpha = 0.18f),
                heroDivider = NeonHeroInk.copy(alpha = 0.14f),
                accentTile = NeonLime,
                onAccentTile = NeonHeroInk,
                accentIconContainer = NeonLimeIconBg,
                cardSurface = NeonLightCard,
                nestedCardSurface = NeonLightNested,
                iconBadgeBg = Color(0xFFE3DFF2),
                borderSubtle = NeonLightBorder,
                mutedText = NeonLightMuted,
                highlightLabel = NeonVioletAccent,
                incomeColor = SemanticIncomeLight,
                expenseColor = SemanticExpenseLight,
                warningColor = SemanticWarningLight,
                pocketColors = listOf(NeonVioletAccent, Color(0xFF65A30D), Color(0xFF2563EB), Color(0xFFDB2777), Color(0xFF059669), Color(0xFFD97706)),
            )
        }

        ThemeScheme.EMERALD_MINT -> if (isDark) {
            BentoColors(
                isDark = true,
                heroStart = MintHeroStart,
                heroEnd = MintHeroEnd,
                onHero = MintHeroInk,
                heroTrack = MintHeroInk.copy(alpha = 0.18f),
                heroDivider = MintHeroInk.copy(alpha = 0.14f),
                accentTile = MintGold,
                onAccentTile = Color(0xFF1A1404),
                accentIconContainer = MintGoldIconBg,
                cardSurface = MintDarkCard,
                nestedCardSurface = MintDarkNested,
                iconBadgeBg = Color(0xFF22362E),
                borderSubtle = MintDarkBorder,
                mutedText = MintDarkMuted,
                highlightLabel = MintGold,
                incomeColor = SemanticIncomeDark,
                expenseColor = SemanticExpenseDark,
                warningColor = SemanticWarningDark,
                pocketColors = listOf(MintHeroStart, MintGold, Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFFFB7185), Color(0xFF4ADE80)),
            )
        } else {
            BentoColors(
                isDark = false,
                heroStart = MintHeroStart,
                heroEnd = MintHeroEnd,
                onHero = MintHeroInk,
                heroTrack = MintHeroInk.copy(alpha = 0.18f),
                heroDivider = MintHeroInk.copy(alpha = 0.14f),
                accentTile = MintGold,
                onAccentTile = Color(0xFF1A1404),
                accentIconContainer = MintGoldIconBg,
                cardSurface = MintLightCard,
                nestedCardSurface = MintLightNested,
                iconBadgeBg = Color(0xFFD4EAE0),
                borderSubtle = MintLightBorder,
                mutedText = MintLightMuted,
                highlightLabel = MintPrimaryAccent,
                incomeColor = SemanticIncomeLight,
                expenseColor = SemanticExpenseLight,
                warningColor = SemanticWarningLight,
                pocketColors = listOf(MintPrimaryAccent, Color(0xFFD97706), Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFFE11D48), Color(0xFF16A34A)),
            )
        }

        ThemeScheme.OCEAN_COBALT -> if (isDark) {
            BentoColors(
                isDark = true,
                heroStart = OceanHeroStart,
                heroEnd = OceanHeroEnd,
                onHero = OceanHeroInk,
                heroTrack = OceanHeroInk.copy(alpha = 0.18f),
                heroDivider = OceanHeroInk.copy(alpha = 0.14f),
                accentTile = OceanCoral,
                onAccentTile = Color(0xFF1F0D02),
                accentIconContainer = OceanCoralIconBg,
                cardSurface = OceanDarkCard,
                nestedCardSurface = OceanDarkNested,
                iconBadgeBg = Color(0xFF23304A),
                borderSubtle = OceanDarkBorder,
                mutedText = OceanDarkMuted,
                highlightLabel = OceanCoral,
                incomeColor = SemanticIncomeDark,
                expenseColor = SemanticExpenseDark,
                warningColor = SemanticWarningDark,
                pocketColors = listOf(OceanHeroStart, OceanCoral, Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFF472B6), Color(0xFFFBBF24)),
            )
        } else {
            BentoColors(
                isDark = false,
                heroStart = OceanHeroStart,
                heroEnd = OceanHeroEnd,
                onHero = OceanHeroInk,
                heroTrack = OceanHeroInk.copy(alpha = 0.18f),
                heroDivider = OceanHeroInk.copy(alpha = 0.14f),
                accentTile = OceanCoral,
                onAccentTile = Color(0xFF1F0D02),
                accentIconContainer = OceanCoralIconBg,
                cardSurface = OceanLightCard,
                nestedCardSurface = OceanLightNested,
                iconBadgeBg = Color(0xFFD6E2F5),
                borderSubtle = OceanLightBorder,
                mutedText = OceanLightMuted,
                highlightLabel = OceanPrimaryAccent,
                incomeColor = SemanticIncomeLight,
                expenseColor = SemanticExpenseLight,
                warningColor = SemanticWarningLight,
                pocketColors = listOf(OceanPrimaryAccent, Color(0xFFEA580C), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFFDB2777), Color(0xFFD97706)),
            )
        }

        ThemeScheme.SUNSET_ROSE -> if (isDark) {
            BentoColors(
                isDark = true,
                heroStart = RoseHeroStart,
                heroEnd = RoseHeroEnd,
                onHero = RoseHeroInk,
                heroTrack = RoseHeroInk.copy(alpha = 0.18f),
                heroDivider = RoseHeroInk.copy(alpha = 0.14f),
                accentTile = RoseButter,
                onAccentTile = Color(0xFF1C1602),
                accentIconContainer = RoseButterIconBg,
                cardSurface = RoseDarkCard,
                nestedCardSurface = RoseDarkNested,
                iconBadgeBg = Color(0xFF382430),
                borderSubtle = RoseDarkBorder,
                mutedText = RoseDarkMuted,
                highlightLabel = RoseButter,
                incomeColor = SemanticIncomeDark,
                expenseColor = SemanticExpenseDark,
                warningColor = SemanticWarningDark,
                pocketColors = listOf(RoseHeroStart, RoseButter, Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFFB923C)),
            )
        } else {
            BentoColors(
                isDark = false,
                heroStart = RoseHeroStart,
                heroEnd = RoseHeroEnd,
                onHero = RoseHeroInk,
                heroTrack = RoseHeroInk.copy(alpha = 0.18f),
                heroDivider = RoseHeroInk.copy(alpha = 0.14f),
                accentTile = RoseButter,
                onAccentTile = Color(0xFF1C1602),
                accentIconContainer = RoseButterIconBg,
                cardSurface = RoseLightCard,
                nestedCardSurface = RoseLightNested,
                iconBadgeBg = Color(0xFFF0D7E0),
                borderSubtle = RoseLightBorder,
                mutedText = RoseLightMuted,
                highlightLabel = RosePrimaryAccent,
                incomeColor = SemanticIncomeLight,
                expenseColor = SemanticExpenseLight,
                warningColor = SemanticWarningLight,
                pocketColors = listOf(RosePrimaryAccent, Color(0xFFCA8A04), Color(0xFF0284C7), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFFEA580C)),
            )
        }
    }

private fun materialSchemeFor(scheme: ThemeScheme, isDark: Boolean, bento: BentoColors): ColorScheme =
    when (scheme) {
        ThemeScheme.BENTO_NEON -> if (isDark) {
            darkColorScheme(
                primary = NeonHeroStart,
                onPrimary = NeonHeroInk,
                primaryContainer = NeonDarkNested,
                onPrimaryContainer = NeonDarkText,
                secondary = NeonLime,
                onSecondary = NeonHeroInk,
                secondaryContainer = Color(0xFF283016),
                onSecondaryContainer = NeonLime,
                tertiary = Color(0xFF60A5FA),
                background = NeonDarkBg,
                onBackground = NeonDarkText,
                surface = NeonDarkCard,
                onSurface = NeonDarkText,
                surfaceVariant = NeonDarkNested,
                onSurfaceVariant = NeonDarkMuted,
                outline = NeonDarkBorder,
                error = bento.expenseColor,
            )
        } else {
            lightColorScheme(
                primary = NeonVioletAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEAE3FF),
                onPrimaryContainer = NeonLightText,
                secondary = Color(0xFF536900),
                onSecondary = Color.White,
                secondaryContainer = NeonLime,
                onSecondaryContainer = NeonHeroInk,
                tertiary = Color(0xFF2563EB),
                background = NeonLightBg,
                onBackground = NeonLightText,
                surface = NeonLightCard,
                onSurface = NeonLightText,
                surfaceVariant = NeonLightNested,
                onSurfaceVariant = NeonLightMuted,
                outline = NeonLightBorder,
                error = bento.expenseColor,
            )
        }

        ThemeScheme.EMERALD_MINT -> if (isDark) {
            darkColorScheme(
                primary = MintHeroStart,
                onPrimary = MintHeroInk,
                primaryContainer = MintDarkNested,
                onPrimaryContainer = MintDarkText,
                secondary = MintGold,
                onSecondary = Color(0xFF1A1404),
                background = MintDarkBg,
                onBackground = MintDarkText,
                surface = MintDarkCard,
                onSurface = MintDarkText,
                surfaceVariant = MintDarkNested,
                onSurfaceVariant = MintDarkMuted,
                outline = MintDarkBorder,
                error = bento.expenseColor,
            )
        } else {
            lightColorScheme(
                primary = MintPrimaryAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = MintLightText,
                secondary = Color(0xFFB45309),
                onSecondary = Color.White,
                secondaryContainer = MintGold,
                onSecondaryContainer = Color(0xFF1A1404),
                background = MintLightBg,
                onBackground = MintLightText,
                surface = MintLightCard,
                onSurface = MintLightText,
                surfaceVariant = MintLightNested,
                onSurfaceVariant = MintLightMuted,
                outline = MintLightBorder,
                error = bento.expenseColor,
            )
        }

        ThemeScheme.OCEAN_COBALT -> if (isDark) {
            darkColorScheme(
                primary = OceanHeroStart,
                onPrimary = OceanHeroInk,
                primaryContainer = OceanDarkNested,
                onPrimaryContainer = OceanDarkText,
                secondary = OceanCoral,
                onSecondary = Color(0xFF1F0D02),
                background = OceanDarkBg,
                onBackground = OceanDarkText,
                surface = OceanDarkCard,
                onSurface = OceanDarkText,
                surfaceVariant = OceanDarkNested,
                onSurfaceVariant = OceanDarkMuted,
                outline = OceanDarkBorder,
                error = bento.expenseColor,
            )
        } else {
            lightColorScheme(
                primary = OceanPrimaryAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = OceanLightText,
                secondary = Color(0xFFC2410C),
                onSecondary = Color.White,
                secondaryContainer = OceanCoral,
                onSecondaryContainer = Color(0xFF1F0D02),
                background = OceanLightBg,
                onBackground = OceanLightText,
                surface = OceanLightCard,
                onSurface = OceanLightText,
                surfaceVariant = OceanLightNested,
                onSurfaceVariant = OceanLightMuted,
                outline = OceanLightBorder,
                error = bento.expenseColor,
            )
        }

        ThemeScheme.SUNSET_ROSE -> if (isDark) {
            darkColorScheme(
                primary = RoseHeroStart,
                onPrimary = RoseHeroInk,
                primaryContainer = RoseDarkNested,
                onPrimaryContainer = RoseDarkText,
                secondary = RoseButter,
                onSecondary = Color(0xFF1C1602),
                background = RoseDarkBg,
                onBackground = RoseDarkText,
                surface = RoseDarkCard,
                onSurface = RoseDarkText,
                surfaceVariant = RoseDarkNested,
                onSurfaceVariant = RoseDarkMuted,
                outline = RoseDarkBorder,
                error = bento.expenseColor,
            )
        } else {
            lightColorScheme(
                primary = RosePrimaryAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE4E6),
                onPrimaryContainer = RoseLightText,
                secondary = Color(0xFFA16207),
                onSecondary = Color.White,
                secondaryContainer = RoseButter,
                onSecondaryContainer = Color(0xFF1C1602),
                background = RoseLightBg,
                onBackground = RoseLightText,
                surface = RoseLightCard,
                onSurface = RoseLightText,
                surfaceVariant = RoseLightNested,
                onSurfaceVariant = RoseLightMuted,
                outline = RoseLightBorder,
                error = bento.expenseColor,
            )
        }
    }

@Composable
fun KantongKuTheme(
    themeScheme: ThemeScheme = ThemeScheme.BENTO_NEON,
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val bento = bentoColorsFor(themeScheme, useDark)
    val colorScheme = materialSchemeFor(themeScheme, useDark, bento)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalBentoColors provides bento) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
