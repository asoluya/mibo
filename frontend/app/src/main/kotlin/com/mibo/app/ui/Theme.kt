package com.mibo.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mibo.app.App
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? Activity
}

/**
 * The light page surface. The light scheme pairs a white card with this grey page; leaving the
 * page white made every card invisible and the app look like one flat sheet.
 */
private val LightPage = Color(0xFFF0F1F2)

/** 系统 Monet 色板的主色（基于当前壁纸生成，API 31+ 的 system_accent1 资源；本应用 minSdk 33 恒可用）。 */
fun systemMonetArgb(context: Context, dark: Boolean): Int {
    val resId = if (dark) {
        android.R.color.system_accent1_200
    } else {
        android.R.color.system_accent1_600
    }
    return context.resources.getColor(resId, context.theme)
}

/**
 * 在保留中性页面与卡片底色的前提下，仅把强调色家族替换成莫奈取色得到的颜色。
 *
 * 直接走 Monet 种子色会把中性灰一起染偏，且高饱和颜色会被 MD3 压暗；这里只替换
 * primary / containerTheme / focus 等强调色槽位，保证取色与页面中性灰都准确。
 */
internal fun Colors.withAccent(
    accentArgb: Int,
    dark: Boolean,
): Colors {
    if (accentArgb == 0) return this
    val raw = Color(accentArgb or 0xFF000000.toInt())
    val primaryColor = if (dark && raw.luminance() < 0.10f) lerp(raw, Color.White, 0.25f) else raw
    val onPrimaryColor = if (primaryColor.luminance() > 0.55f) Color(0xE6000000) else Color.White
    val container = if (dark) lerp(primaryColor, Color.White, 0.22f) else primaryColor
    return copy(
        primary = primaryColor,
        onPrimary = onPrimaryColor,
        primaryVariant = primaryColor,
        onPrimaryVariant = lerp(primaryColor, Color.White, if (dark) 0.55f else 0.68f),
        disabledPrimary = primaryColor.copy(alpha = 0.30f),
        disabledOnPrimary = if (dark) Color(0xFF404040) else onPrimaryColor,
        disabledPrimaryButton = primaryColor.copy(alpha = 0.30f),
        disabledOnPrimaryButton = onPrimaryColor.copy(alpha = if (dark) 0.16f else 0.54f),
        disabledPrimarySlider = primaryColor.copy(alpha = 0.30f),
        primaryContainer = container,
        onPrimaryContainer = onPrimaryColor,
        tertiaryContainer = primaryColor.copy(alpha = if (dark) 0.25f else 0.15f),
        onTertiaryContainer = primaryColor,
        tertiaryContainerVariant = primaryColor.copy(alpha = if (dark) 0.25f else 0.15f),
        sliderKeyPointForeground = lerp(primaryColor, Color.White, if (dark) 0.30f else 0.43f),
    )
}

@Composable
fun isDarkTheme(): Boolean = when (App.settings.darkMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun MiboTheme(content: @Composable () -> Unit) {
    val mode = App.settings.darkMode
    // 强调色恒定莫奈取色（原强调色选择已按用户要求去掉）：跟随系统基于壁纸生成的 Monet 色。
    val context = LocalContext.current
    val lightAccent = systemMonetArgb(context, dark = false)
    val darkAccent = systemMonetArgb(context, dark = true)
    val controller = remember(mode, lightAccent, darkAccent) {
        ThemeController(
            colorSchemeMode = when (mode) {
                1 -> ColorSchemeMode.Light
                2 -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.System
            },
            lightColors = lightColorScheme(background = LightPage)
                .withAccent(lightAccent, dark = false),
            darkColors = darkColorScheme()
                .withAccent(darkAccent, dark = true),
        )
    }
    val base = LocalDensity.current
    val scale = App.settings.fontScale
    val density = remember(base, scale) { Density(base.density, base.fontScale * scale) }

    MiuixTheme(controller = controller) {
        CompositionLocalProvider(LocalDensity provides density) {
            content()
        }
    }
}
