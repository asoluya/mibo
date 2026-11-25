package com.mibo.app.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Updater
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.SectionTitle
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.glassBarContentTop
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.openExternalUrl
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberListScrollFraction
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private data class UpstreamRef(
    val title: String,
    val summary: String,
    val status: String,
    val url: String,
)

private val UI_UPSTREAMS = listOf(
    UpstreamRef(
        title = "Miuix",
        summary = "本应用的 UI 组件库 · compose-miuix-ui/miuix",
        status = "Apache-2.0",
        url = "https://github.com/compose-miuix-ui/miuix",
    )
)

private val CORE_LIBRARIES = listOf(
    UpstreamRef(
        title = "AndroidX Media3",
        summary = "ExoPlayer 视频流播放与媒体控制引擎",
        status = "Apache-2.0",
        url = "https://github.com/androidx/media",
    ),
    UpstreamRef(
        title = "Coil 3",
        summary = "Kotlin Multiplatform 头像、九宫格与动图加载",
        status = "Apache-2.0",
        url = "https://github.com/coil-kt/coil",
    ),
    UpstreamRef(
        title = "OkHttp",
        summary = "HTTP/2 网络连接池、Cookie 复用与媒体上传",
        status = "Apache-2.0",
        url = "https://github.com/square/okhttp",
    ),
    UpstreamRef(
        title = "Jetpack Compose",
        summary = "声明式 UI 框架与预测性返回手势转场",
        status = "Apache-2.0",
        url = "https://github.com/JetBrains/compose-multiplatform",
    ),
)

@Composable
fun AboutScreen(nav: NavState) {
    val context = LocalContext.current
    val appIcon = rememberAppIconPainter(context)
    val versionName = remember(context) { Updater.currentVersion(context) }
    val versionCode = remember(context) { currentVersionCode(context) }
    val versionText = if (versionCode > 0L) "v$versionName ($versionCode)" else "v$versionName"

    val pageColor = pageContainerColor()
    val blurEffects = blurEffectsEnabled()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdropIf(blurEffects, backdrop)
                .background(pageColor),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollEndHaptic(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = glassBarContentTop(topBarHeight, 10.dp),
                    end = 12.dp,
                    bottom = 24.dp + navBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "app-header") {
                    AppHeader(
                        appName = "Mibo",
                        appVersion = versionText,
                        icon = appIcon,
                        modifier = Modifier.testTag("about_header"),
                        onVersionLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("mibo", "Mibo $versionText"))
                            App.toast("已复制版本信息")
                        },
                    )
                }

                item(key = "project-title") { SectionTitle("项目", topPadding = 8.dp) }

                item(key = "project") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        ArrowPreference(
                            title = "开源仓库",
                            summary = "https://github.com/asoluya/mibo",
                            endActions = { Text("GitHub", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                            modifier = Modifier.testTag("about_repo"),
                            onClick = { openExternalUrl(context, "https://github.com/asoluya/mibo") },
                        )
                    }
                }

                item(key = "ui-upstreams-title") { SectionTitle("前端界面库") }

                item(key = "ui-upstreams") {
                    UpstreamGroupCard(
                        items = UI_UPSTREAMS,
                        tagPrefix = "about_ui",
                        onOpen = { url -> openExternalUrl(context, url) },
                    )
                }

                item(key = "core-libs-title") { SectionTitle("核心开源依赖") }

                item(key = "core-libs") {
                    UpstreamGroupCard(
                        items = CORE_LIBRARIES,
                        tagPrefix = "about_lib",
                        onOpen = { url -> openExternalUrl(context, url) },
                    )
                }

                item(key = "disclaimer") {
                    Text(
                        text = "第三方微博客户端，基于 m.weibo.cn Web API 构建，仅用于个人学习与研究。所有内容、商标与版权归原平台及对应开源项目作者所有。",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffects) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageColor,
                    scrollFraction = rememberListScrollFraction(listState),
                ) {
                    SmallTopAppBar(
                        title = "关于",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpstreamGroupCard(
    items: List<UpstreamRef>,
    tagPrefix: String,
    onOpen: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = CardDefaults.CornerRadius,
        insideMargin = PaddingValues(0.dp),
    ) {
        items.forEachIndexed { index, item ->
            ArrowPreference(
                title = item.title,
                summary = item.summary,
                endActions = { Text(item.status, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                modifier = Modifier.testTag("${tagPrefix}_$index"),
                onClick = { onOpen(item.url) },
            )
        }
    }
}

/**
 * 关于页头部（替代 COUI 的 AppInfoPreference，Miuix 无对应件）：
 * 应用图标 + 名称 + 版本号，版本号长按复制。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppHeader(
    appName: String,
    appVersion: String,
    icon: Painter?,
    modifier: Modifier = Modifier,
    onVersionLongClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CardDefaults.CornerRadius,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Text(appName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    appVersion,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onVersionLongClick,
                    ),
                )
            }
        }
    }
}

@Composable
private fun rememberAppIconPainter(context: Context): Painter? = remember(context) {
    runCatching {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val sizePx = 192
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }.getOrNull()
}

private fun currentVersionCode(context: Context): Long = runCatching {
    val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
}.getOrDefault(0L)
