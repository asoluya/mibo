package com.mibo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The page surface the system apps paint behind their cards: the theme's background in the light
 * theme, `#303030` in the dark theme. Miuix's dark `background` is `#1E1E1E`, which reads a full
 * step darker than the platform pages and leaves every card floating on a darker sheet.
 * （原 COUI AppBarBlurOverlayDefaults.containerColor() 的等价实现。）
 */
@Composable
fun pageContainerColor(): Color =
    if (isDarkTheme()) Color(0xFF303030) else MiuixTheme.colorScheme.background
