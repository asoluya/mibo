package com.mibo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.UpdateInfo
import com.mibo.app.data.Updater
import com.mibo.app.data.User
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** 弹窗底部按钮栏的一项（替代 COUI layout 的 DialogButtonBarAction）。 */
internal data class DialogButtonBarAction(
    val text: String,
    val enabled: Boolean = true,
    /** COUI 里表示「推荐动作」；Miuix 约定正按钮恒为主色样式，此标记仅保留调用点兼容。 */
    val recommended: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 弹窗底部按钮栏（替代 COUI layout 的 DialogButtonBar，Miuix 无此组件）：
 * 按 Miuix 示例的排布——一行两颗等宽 [TextButton]、间隔 20dp，正按钮用主色样式。
 * COUI 那种浮在面板上的半透明胶囊按钮栏不再保留。
 */
@Composable
internal fun DialogButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        negative?.let { action ->
            TextButton(
                text = action.text,
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.weight(1f),
            )
        }
        positive?.let { action ->
            TextButton(
                text = action.text,
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    /** 居中弹出（缩放淡入），不从底部滑上来。 */
    centered: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    OverlayDialog(
        show = show,
        title = title,
        summary = message,
        outsideMargin = DpSize(16.dp, 8.dp),
        // 传 null 才交给窗口宽度判断；只有显式要居中时才把大屏形态固定打开。
        largeScreen = if (centered) true else null,
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                if (confirmed) onConfirm() else onDismiss()
            }
        },
    ) {
        DialogButtonBar(
            negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
            positive = DialogButtonBarAction(
                text = confirmText,
                onClick = {
                    confirmed = true
                    show = false
                },
            ),
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return "%.1f %s".format(Locale.US, value, units[digitGroups])
}

/**
 * 更新提示。release notes 按 Markdown 渲染，可能有几十行，正文自己滚动、封顶 260dp，别把按钮顶出屏幕。
 * 「跳过本次更新」是个勾选框：勾上后不管点稍后还是点外面关掉，都记成跳过这个版本；点下载更新则照常下载。
 * 更新一律在弹窗内拉取 release 的 APK 并调起安装请求，不跳转浏览器。
 *
 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var skip by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }

    val inApp = !info.apkUrl.isNullOrBlank()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) { show = true }
    val haptic = LocalHapticFeedback.current
    val textMargin = DialogDefaults.insideMargin.width

    fun startInAppDownload() {
        val apkUrl = info.apkUrl ?: return
        downloading = true
        downloadError = null
        progress = 0f
        progressText = "正在准备下载…"
        // 速度按相邻两次回调的差值算，下载中自己看得到实际带宽。
        var lastBytes = 0L
        var lastAt = 0L
        var speed = 0L
        downloadJob = scope.launch {
            try {
                val file = Updater.downloadApk(
                    context = context,
                    url = apkUrl,
                    version = info.version,
                    force = false,
                    onProgress = { p, current, total ->
                        val now = System.currentTimeMillis()
                        if (lastAt != 0L && now > lastAt) {
                            speed = ((current - lastBytes) * 1000 / (now - lastAt)).coerceAtLeast(0L)
                        }
                        lastBytes = current
                        lastAt = now
                        progress = p
                        val size = if (total > 0) {
                            "${(p * 100).toInt()}% (${formatSize(current)} / ${formatSize(total)})"
                        } else {
                            formatSize(current)
                        }
                        progressText = if (speed > 0) "$size · ${formatSize(speed)}/s" else size
                    },
                )
                progressText = "下载完成，正在调起安装…"
                delay(300)
                settled = true
                show = false
                App.dismissUpdate()
                Updater.installApk(context, file)
            } catch (c: CancellationException) {
                // 用户取消
            } catch (t: Throwable) {
                downloading = false
                downloadError = t.message ?: "下载失败"
            }
        }
    }

    OverlayDialog(
        show = show,
        title = when {
            downloading -> "正在下载新版本"
            downloadError != null -> "更新下载失败"
            else -> "发现新版本 ${info.version}"
        },
        summary = when {
            downloading -> "正在拉取安装包…"
            downloadError != null -> "请检查网络后重试"
            info.prerelease -> "预发布版本"
            else -> "正式版本"
        },
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = {
            if (downloading) {
                downloadJob?.cancel()
                downloading = false
            }
            show = false
        },
        onDismissFinished = {
            if (!settled) {
                settled = true
                when {
                    skip -> onSkip()
                    else -> onDismiss()
                }
            }
        },
    ) {
        Column {
            when {
                downloading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = textMargin, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MiboRefreshSpinner(
                            size = 44.dp,
                            strokeWidth = 3.dp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            progressText,
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                    DialogButtonBar(
                        negative = DialogButtonBarAction(
                            text = "取消",
                            onClick = {
                                downloadJob?.cancel()
                                downloading = false
                                show = false
                            },
                        ),
                        positive = null,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                downloadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = textMargin, vertical = 12.dp),
                    ) {
                        Text(
                            downloadError ?: "下载遇到错误",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                    DialogButtonBar(
                        negative = DialogButtonBarAction(
                            text = "取消",
                            onClick = { show = false },
                        ),
                        positive = DialogButtonBarAction(
                            text = "重试",
                            recommended = true,
                            onClick = { startInAppDownload() },
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                else -> {
                    if (info.notes.isNotBlank()) {
                        MarkdownText(
                            info.notes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .scrollEndHaptic()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = textMargin),
                        )
                    }
                    Row(
                        // 和 COUI 安全对话框的勾选行一致：起点让圆圈的内环对齐上面正文的左缘。
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = textMargin, top = 12.dp)
                            .toggleable(
                                value = skip,
                                interactionSource = null,
                                indication = null,
                                role = Role.Checkbox,
                                onValueChange = {
                                    skip = it
                                    haptic.performHapticFeedback(
                                        if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                                    )
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(state = ToggleableState(skip), onClick = null)
                        Text(
                            "跳过本次更新",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    DialogButtonBar(
                        negative = DialogButtonBarAction(text = "稍后", onClick = { show = false }),
                        positive = DialogButtonBarAction(
                            text = "立即安装",
                            recommended = true,
                            onClick = {
                                if (inApp) {
                                    startInAppDownload()
                                } else {
                                    downloadError = "这个版本没有可用的安装包"
                                }
                            },
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun UserRow(user: User, onClick: () -> Unit, subtitle: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Avatar(user.avatar, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, fontSize = 15.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        }
        TextButton(text = "查看", onClick = onClick)
    }
}
