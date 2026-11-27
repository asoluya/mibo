package com.mibo.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalWindowInfo
import com.mibo.app.App
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全局玻璃菜单宿主：挂在根节点最顶层（与 Toast 宿主同层，LocalBackdrop 由外层提供）。
 *
 * 为什么不用 OverlayListPopup：它的内容实现把 surfaceContainer 实色画死在内容之下，
 * 玻璃层放里面盖不住；级联弹窗的 glassSurface 实测也只有顶部一条采样到背景。
 * 这里用 miuix-glass 专门为下拉菜单做的 GlassDropdownPopup——整块面板就是玻璃材质，
 * 跟随「顶栏与底栏透明度」滑杆；模糊关着时传 null backdrop，退回实色填充（带描边阴影）。
 *
 * 带 children 的条目走钻入式二级：点开换整表内容，首行「返回」。
 * 锚点是 Rect.Zero 时（没有落点信息的调用方）面板落在窗口中央。
 */
@Composable
fun GlassMenuHost() {
    val request = App.glassMenu
    var last by remember { mutableStateOf<App.GlassMenuRequest?>(null) }
    if (request != null) last = request
    val shown = request ?: last ?: return
    val backdrop = LocalBackdrop.current
    val glassActive = blurEffectsEnabled() && backdrop != null
    var drilled by remember(shown.id) { mutableStateOf<App.GlassMenuItem?>(null) }
    val container = LocalWindowInfo.current.containerSize
    val anchor = if (shown.anchorBounds == Rect.Zero) {
        val cx = container.width / 2f
        val cy = container.height / 2f
        Rect(cx, cy, cx, cy)
    } else {
        shown.anchorBounds
    }
    Box(Modifier.fillMaxSize()) {
        GlassDropdownPopup(
            show = request != null,
            onDismissRequest = { App.dismissGlassMenu() },
            anchorBounds = anchor,
            backdrop = if (glassActive) backdrop else null,
            // 默认上限 416dp 只装得下 8 行，「全部」菜单的「附近」和关注分组会被截在
            // 可滚动区域之外、看起来像不存在；放宽到 640dp 让常用菜单一屏放完。
            sizing = GlassPopupSizing(maxHeight = 640.dp),
            visuals = GlassPopupDefaults.visuals(alpha = App.settings.barGlassAlpha),
        ) {
            val current = drilled
            if (current == null) {
                shown.items.forEach { item ->
                    GlassMenuRow(item = item, onDrill = { drilled = it })
                }
            } else {
                GlassPopupItem(text = "返回", onClick = { drilled = null })
                current.children.orEmpty().forEach { child ->
                    GlassMenuRow(item = child, onDrill = { drilled = it })
                }
            }
        }
    }
}

@Composable
private fun GlassMenuRow(item: App.GlassMenuItem, onDrill: (App.GlassMenuItem) -> Unit) {
    val scheme = MiuixTheme.colorScheme
    GlassPopupItem(
        text = item.text,
        summary = item.summary,
        icon = item.icon,
        selected = item.selected,
        showArrow = item.children != null,
        contentColor = if (item.danger) scheme.error else scheme.onSurfaceContainer,
        onClick = {
            if (item.children != null) {
                onDrill(item)
            } else {
                App.dismissGlassMenu()
                item.onClick?.invoke()
            }
        },
    )
}

/** 点赞前确认宿主：有锚点时在点赞按钮旁弹整块柔光玻璃小弹窗（与保存图片菜单同款
 * GlassDropdownPopup，单条「点赞」即确认按钮）；没有锚点时退回居中玻璃对话框。 */
@Composable
fun LikeConfirmHost() {
    val request = App.likeConfirm
    var last by remember { mutableStateOf<App.LikeConfirmRequest?>(null) }
    if (request != null) last = request
    val shown = request ?: last ?: return
    val status = shown.status
    val backdrop = LocalBackdrop.current
    val glassActive = blurEffectsEnabled() && backdrop != null
    val name = status.user?.name?.takeIf { it.isNotBlank() } ?: "该微博"
    val text = status.plainText.replace('\n', ' ').take(60)
    val summary = if (text.isBlank()) "@$name" else "@$name：$text"
    val anchor = shown.anchor
    if (anchor != null && anchor != Rect.Zero) {
        Box(Modifier.fillMaxSize()) {
            GlassDropdownPopup(
                show = request != null,
                onDismissRequest = { App.resolveLikeConfirm(false) },
                anchorBounds = anchor,
                backdrop = if (glassActive) backdrop else null,
                visuals = GlassPopupDefaults.visuals(alpha = App.settings.barGlassAlpha),
            ) {
                GlassPopupItem(
                    text = "点赞",
                    summary = summary,
                    onClick = { App.resolveLikeConfirm(true) },
                )
            }
        }
    } else {
        GlassDialog(
            show = request != null,
            onDismissRequest = { App.resolveLikeConfirm(false) },
            backdrop = backdrop,
            title = "确认点赞？",
            summary = summary,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassDialogButton(
                    onClick = { App.resolveLikeConfirm(false) },
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                GlassDialogButton(
                    onClick = { App.resolveLikeConfirm(true) },
                    modifier = Modifier.weight(1f),
                    role = GlassDialogButtonRole.Primary,
                ) { Text("点赞") }
            }
        }
    }
}
