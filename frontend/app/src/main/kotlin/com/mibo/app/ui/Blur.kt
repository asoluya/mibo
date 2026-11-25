package com.mibo.app.ui

import androidx.compose.foundation.layout.Box
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.mibo.app.App
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用级 backdrop 通道（替代 COUI 的 LocalBackdrop）：由 Shell / Root 在内容根部
 * 用 rememberLayerBackdrop() 录制并 provide，顶栏与玻璃面从这里取样。
 * 模糊关闭时 provide null，消费端自动退回实色。
 */
val LocalBackdrop = compositionLocalOf<LayerBackdrop?> { null }


/** 设置里的「模糊效果」。关掉后顶栏与玻璃面改画纯色，滑动时不再重采样背后的内容。 */
@Composable
fun blurEffectsEnabled(): Boolean = App.settings.blurEffects

/** 模糊关掉时连 backdrop 的录制一起省掉：没人采样它，这个图层每帧重录就是纯浪费。 */
fun Modifier.layerBackdropIf(enabled: Boolean, backdrop: LayerBackdrop): Modifier =
    if (enabled) layerBackdrop(backdrop) else this

/**
 * 根据 [LazyListState] 的纵向滚动偏移换算顶栏材质透明度：
 * 位于列表顶部（或尚未越过 [startOffset] 留白）时为 `0f`，
 * 越过 [startOffset] 后在 [rampDistance] 内平滑渐显至 `1f`。
 * （原 COUI AppBarBlurOverlayDefaults.scrollAlpha 的等价实现。）
 */
fun listScrollAlpha(
    listState: LazyListState,
    density: Density,
    startOffset: Dp = 0.dp,
    rampDistance: Dp = 48.dp,
): Float {
    val scrolled = if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
    else listState.firstVisibleItemScrollOffset.toFloat()
    val startPx = with(density) { startOffset.toPx() }
    val rampPx = with(density) { rampDistance.toPx() }
    if (rampPx <= 0f) return if (scrolled > startPx) 1f else 0f
    return ((scrolled - startPx) / rampPx).coerceIn(0f, 1f)
}

/**
 * 双行顶栏的滚动淡入透明度：先跳过标签行下方留白 [contentGap]，再在 `56dp` 内平滑淡入。
 * （原 COUI TwoRowTopAppBarDefaults.scrollAlpha 的等价实现。）
 */
fun twoRowListScrollAlpha(
    listState: LazyListState,
    density: Density,
    contentGap: Dp = 64.dp,
): Float = listScrollAlpha(
    listState = listState,
    density = density,
    startOffset = contentGap,
    rampDistance = 56.dp,
)

/** [listScrollAlpha] 的 draw 阶段 producer 封装，供 [AppBarSurface] 等复用。 */
@Composable
fun rememberListScrollFraction(listState: LazyListState): () -> Float {
    val density = LocalDensity.current
    return remember(listState, density) {
        { listScrollAlpha(listState, density) }
    }
}

/**
 * 自定义顶栏的材质层：模糊开着且有 backdrop 时走 miuix-blur 的 textureBlur
 * （以 [containerColor] 高透明度混色，近似原 COUI AppBarBlurOverlay 的玻璃感），
 * 否则画整块实色，透明度由 [scrollFraction] 在绘制阶段决定。
 */
@Composable
fun AppBarSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = pageContainerColor(),
    scrollFraction: () -> Float = { 1f },
    cornerRadius: Dp = 0.dp,
    transparent: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    if (transparent) {
        // 全透明：只保留内容盒（布局测量与边界上报都在调用方的 content / modifier 里），不画任何材质。
        Box(modifier, content = content)
        return
    }
    val backdrop = LocalBackdrop.current
    if (blurEffectsEnabled() && backdrop != null) {
        // OS4 柔光玻璃：miuix-glass 的 glassPanel（薄玻璃材质 + 边缘柔光描边），
        // 浓度沿用「顶栏与底栏透明度」；滚动提白与磨砂分支同一套逻辑。
        val glassAlpha = App.settings.barGlassAlpha
        val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
        Box(
            modifier
                .glassPanel(
                    backdrop = backdrop,
                    shape = GlassShape(cornerRadius),
                    style = GlassDefaults.style(isDark),
                    alpha = glassAlpha,
                    material = GlassMaterials.puredThinGlass(isDark),
                    // 不描边：整幅顶栏的边缘高光会被看成分割线（分段行上下各一道）。
                    stroke = null,
                    shadow = null,
                    shading = false,
                )
                .drawBehind {
                    val alpha = scrollFraction().coerceIn(0f, 1f)
                    if (alpha > 0f) {
                        drawRoundRect(
                            containerColor,
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            alpha = alpha * (1f - glassAlpha),
                        )
                    }
                },
            content = content,
        )
        return
    }
    Box(
        modifier.drawBehind {
            val alpha = scrollFraction().coerceIn(0f, 1f)
            if (alpha > 0f) {
                drawRoundRect(
                    containerColor,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    alpha = alpha,
                )
            }
        },
        content = content,
    )
}

/**
 * 柔光玻璃小胶囊提示：Toast 与保存进度 HUD 共用。
 * 玻璃开着且有 backdrop 时用与底栏同一套 glassPanel（透明度跟随顶/底栏滑杆），否则实色胶囊。
 */
@Composable
fun GlassToastPill(text: String, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    val backdrop = LocalBackdrop.current
    val glassActive = blurEffectsEnabled() && backdrop != null
    val isDark = scheme.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .then(
                if (glassActive) {
                    Modifier.glassPanel(
                        backdrop = backdrop,
                        shape = GlassShape(50.dp),
                        style = GlassDefaults.style(isDark),
                        alpha = App.settings.barGlassAlpha,
                        material = GlassMaterials.puredThinGlass(isDark),
                        stroke = GlassEdgeStrokes.forTheme(isDark),
                        shadow = null,
                        shading = false,
                    )
                } else {
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(scheme.surfaceContainer)
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = scheme.onSurface,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
