package com.mibo.app.screen

import android.content.Context
import android.Manifest
import androidx.compose.runtime.DisposableEffect
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mibo.app.notify.NotifyPollService
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import com.mibo.app.App
import com.mibo.app.data.Account
import com.mibo.app.data.FeedTabSelection
import com.mibo.app.data.FollowGroup
import com.mibo.app.data.Updater
import com.mibo.app.data.feedTabOptions
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.openExternalUrl
import com.mibo.app.ui.MiboIcons
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.FloatingBottomBar
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.ConfirmDialog
import com.mibo.app.ui.components.SectionTitle
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.glassBarContentTop
import com.mibo.app.ui.isDarkTheme
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberListScrollFraction
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 设置页分组定义，用于在「我」一级页面展示分组目录，并跳转到对应的二级设置页。
 */
enum class SettingsGroup(val title: String, val summary: String) {
    Display("显示交互", "深色模式、强调色、字号、手势与「我」页布局"),
    Feed("浏览设置", "主页分栏与昵称标识"),
    Other("应用相关", "检查更新、清理缓存、关于与账号退出"),
}

@Stable
private class SettingsUiState(context: Context) {
    val version: String = Updater.currentVersion(context)
    val guest: Boolean = App.repo.guest

    var darkMode by mutableIntStateOf(App.settings.darkMode)
    var fontScale by mutableFloatStateOf(App.settings.fontScale)
    var autoCheckUpdate by mutableStateOf(App.settings.autoCheckUpdate)
    var checkUpdateOnStartup by mutableStateOf(App.settings.checkUpdateOnStartup)
    var acceptPrerelease by mutableStateOf(App.settings.acceptPrerelease)
    var barGlassAlpha by mutableFloatStateOf(App.settings.barGlassAlpha)
    var barOffsetX by mutableFloatStateOf(App.settings.barOffsetX)
    var barOffsetY by mutableFloatStateOf(App.settings.barOffsetY)
    var expandAutoClose by mutableStateOf(App.settings.expandAutoClose)
    var hideBarsOnScroll by mutableStateOf(App.settings.hideBarsOnScroll)
    var inAppBrowser by mutableStateOf(App.settings.inAppBrowser)
    var message by mutableStateOf("")
    var feedLeftTab by mutableStateOf(App.settings.feedLeftTab)
    var feedRightTab by mutableStateOf(App.settings.feedRightTab)
    var feedGroups by mutableStateOf<List<FollowGroup>>(emptyList())
    var pickFeedLeft by mutableStateOf(false)
    var pickFeedRight by mutableStateOf(false)
}

@Composable
private fun rememberSettingsUiState(): SettingsUiState {
    val context = LocalContext.current
    val state = remember { SettingsUiState(context) }
    // 关注分组是账号里的数据：只在真的要选栏位内容时才去网页版拉一次，拉不到就只列内置流。
    LaunchedEffect(state.pickFeedLeft, state.pickFeedRight) {
        if (state.guest || !(state.pickFeedLeft || state.pickFeedRight)) {
            return@LaunchedEffect
        }
        runCatching { App.repo.followGroups() }.onSuccess { state.feedGroups = it }
    }
    return state
}

@Composable
fun SettingsScreen(nav: NavState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberSettingsUiState()

    SettingsScaffold(
        nav = nav,
        title = "设置",
        state = state,
    ) {
        displaySettingsItems(nav = nav, state = state, showHeader = true)
        feedSettingsItems(state = state, showHeader = true)
        otherSettingsItems(nav = nav, state = state, context = context, scope = scope, showHeader = true)

        item {
            Text(
                "基于 m.weibo.cn Web API 的第三方微博客户端。\n仅用于个人学习与研究。",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * 分组二级设置页：从「我」一级页面点进具体设置分组时使用，复用同一套设置项与弹窗。
 */
@Composable
fun SettingsGroupScreen(nav: NavState, group: SettingsGroup) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberSettingsUiState()

    SettingsScaffold(
        nav = nav,
        title = group.title,
        state = state,
    ) {
        when (group) {
            SettingsGroup.Display -> displaySettingsItems(nav = nav, state = state, showHeader = false)
            SettingsGroup.Feed -> feedSettingsItems(state = state, showHeader = false)
            SettingsGroup.Other -> otherSettingsItems(nav = nav, state = state, context = context, scope = scope, showHeader = false)
        }
    }
}

@Composable
private fun SettingsScaffold(
    nav: NavState,
    title: String,
    state: SettingsUiState,
    content: LazyListScope.() -> Unit,
) {
    val pageColor = pageContainerColor()
    // 顶栏玻璃要有东西可采样：把列表录进 backdrop，顶栏浮在上面做渐变模糊。
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffectsEnabled(), backdrop).background(pageColor)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollEndHaptic(),
                // 列表铺到屏幕最底，滚到末页时卡片从系统导航条下方穿过（沉浸），
                // 末项靠 contentPadding 抬到导航条之上，不会被挡住。
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = glassBarContentTop(topBarHeight, 10.dp),
                    end = 12.dp,
                    bottom = 24.dp + navBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageColor,
                    scrollFraction = rememberListScrollFraction(listState),
                ) {
                    SmallTopAppBar(
                        title = title,
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }

    SettingsDialogs(nav = nav, state = state)
}

private fun LazyListScope.displaySettingsItems(
    nav: NavState,
    state: SettingsUiState,
    showHeader: Boolean,
) {
    if (showHeader) {
        item { SectionTitle("显示交互", topPadding = 4.dp) }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CardDefaults.CornerRadius,
            insideMargin = PaddingValues(0.dp),
        ) {
            ArrowPreference(
                title = "深色模式",
                modifier = Modifier.testTag("settings_dark"),
                summary = when (state.darkMode) {
                    1 -> "浅色"
                    2 -> "深色"
                    else -> "跟随系统"
                },
                onClick = { state.darkMode = App.settings.cycleDarkMode() },
            )
            SliderPreference(
                value = state.fontScale,
                // 拖动时只动本地状态（滑块和百分比）：字号是全局密度，逐帧应用会让整棵界面
                // 每帧按新字号重排，必然卡顿。松手时一次性应用到全局。
                onValueChange = { state.fontScale = it },
                onValueChangeFinished = { App.settings.applyFontScale(state.fontScale) },
                title = "字号",
                summary = "全局字体缩放，100% 为标准大小",
                valueText = "${(state.fontScale * 100).toInt()}%",
                valueRange = 0.8f..1.2f,
                modifier = Modifier.testTag("settings_font"),
            )
            SliderPreference(
                value = state.barGlassAlpha,
                onValueChange = {
                    state.barGlassAlpha = it
                    App.settings.applyBarGlassAlpha(it)
                },
                title = "顶栏与底栏透明度",
                summary = "顶栏与悬浮底栏的玻璃浓度，越低越透",
                valueText = "${(state.barGlassAlpha * 100).toInt()}%",
                valueRange = 0.3f..1f,
                modifier = Modifier.testTag("settings_bar_glass_alpha"),
            )
            ArrowPreference(
                title = "底栏调节",
                summary = "左右 ${App.settings.barOffsetX.toInt()}dp · 上下 ${App.settings.barOffsetY.toInt()}dp · 大小 ${(App.settings.barScale * 100).toInt()}%，点开对着底栏调",
                onClick = { nav.push(Route.BarPosition) },
                modifier = Modifier.testTag("settings_bar_position"),
            )
            SwitchPreference(
                checked = state.expandAutoClose,
                onCheckedChange = {
                    state.expandAutoClose = it
                    App.settings.applyExpandAutoClose(it)
                },
                title = "展开按钮自动关闭",
                summary = "底栏按钮展开后，点任意其他地方自动收起",
                modifier = Modifier.testTag("settings_expand_auto_close"),
            )
            SwitchPreference(
                checked = state.hideBarsOnScroll,
                onCheckedChange = {
                    state.hideBarsOnScroll = it
                    App.settings.applyHideBarsOnScroll(it)
                },
                title = "滑动时隐藏顶栏和底栏",
                summary = "向下滑动浏览时自动收起顶栏与底栏，向上滑动恢复",
                modifier = Modifier.testTag("settings_hide_bars_on_scroll"),
            )
            SwitchPreference(
                checked = state.inAppBrowser,
                onCheckedChange = {
                    state.inAppBrowser = it
                    App.settings.applyInAppBrowser(it)
                },
                title = "使用内置浏览器打开外部网页",
                summary = "关闭后点击外部链接将直接调用系统浏览器打开",
                modifier = Modifier.testTag("settings_in_app_browser"),
            )
        }
    }
}

private fun LazyListScope.feedSettingsItems(
    state: SettingsUiState,
    showHeader: Boolean,
) {
    if (showHeader) {
        item { SectionTitle("浏览设置") }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CardDefaults.CornerRadius,
            insideMargin = PaddingValues(0.dp),
        ) {
            ArrowPreference(
                title = "微博左侧栏",
                modifier = Modifier.testTag("settings_feed_left_tab"),
                summary = state.feedLeftTab.label,
                onClick = { state.pickFeedLeft = true },
            )
            ArrowPreference(
                title = "微博右侧栏",
                modifier = Modifier.testTag("settings_feed_right_tab"),
                summary = state.feedRightTab.label,
                onClick = { state.pickFeedRight = true },
            )
            SwitchPreference(
                checked = App.settings.monitorNewPosts,
                onCheckedChange = { App.settings.applyMonitorNewPosts(it) },
                title = "监测新帖",
                summary = "前台时每 20 秒检测关注流，有新微博时刷新按钮会发光提醒",
                modifier = Modifier.testTag("settings_monitor_new_posts"),
            )
            SwitchPreference(
                checked = App.settings.livePhotoAutoPlay,
                onCheckedChange = { App.settings.applyLivePhotoAutoPlay(it) },
                title = "实况图自动播放",
                summary = "进入或滑动到实况图时自动播放一次，结束后仅支持长按重播",
                modifier = Modifier.testTag("settings_live_photo_auto_play"),
            )
            SwitchPreference(
                checked = App.settings.livePhotoSound,
                onCheckedChange = { App.settings.applyLivePhotoSound(it) },
                title = "实况图播放声音",
                summary = "播放实况图时同步播放附带的视频声音",
                modifier = Modifier.testTag("settings_live_photo_sound"),
            )
        }
    }
}

private fun LazyListScope.updateSettingsItems(
    nav: NavState,
    state: SettingsUiState,
    scope: CoroutineScope,
    showHeader: Boolean,
) {
    if (showHeader) {
        item { SectionTitle("更新") }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CardDefaults.CornerRadius,
            insideMargin = PaddingValues(0.dp),
        ) {
            ArrowPreference(
                title = "检查更新",
                modifier = Modifier.testTag("settings_update"),
                summary = if (App.checkingUpdate) "正在检查…" else "当前版本 ${state.version}",
                onClick = { scope.launch { App.checkUpdate(manual = true) } },
            )
            SwitchPreference(
                checked = state.autoCheckUpdate,
                onCheckedChange = {
                    state.autoCheckUpdate = it
                    App.settings.applyAutoCheckUpdate(it)
                },
                title = "自动检查更新",
                modifier = Modifier.testTag("settings_auto_update"),
            )
            SwitchPreference(
                checked = state.checkUpdateOnStartup,
                onCheckedChange = {
                    state.checkUpdateOnStartup = it
                    App.settings.applyCheckUpdateOnStartup(it)
                },
                title = "启动检测更新",
                modifier = Modifier.testTag("settings_startup_update"),
            )
            SwitchPreference(
                checked = state.acceptPrerelease,
                onCheckedChange = {
                    state.acceptPrerelease = it
                    App.settings.applyAcceptPrerelease(it)
                },
                title = "接收预发布版本",
                modifier = Modifier.testTag("settings_prerelease"),
            )
        }
    }
}

private fun LazyListScope.otherSettingsItems(
    nav: NavState,
    state: SettingsUiState,
    context: Context,
    scope: CoroutineScope,
    showHeader: Boolean,
) {
    if (showHeader) {
        item { SectionTitle("应用相关") }
    }
    item {
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CardDefaults.CornerRadius,
            insideMargin = PaddingValues(0.dp),
        ) {
            ArrowPreference(
                title = "清理缓存",
                modifier = Modifier.testTag("settings_clear_cache"),
                onClick = {
                    runCatching {
                        val loader = SingletonImageLoader.get(context)
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                        context.cacheDir.resolve("live").deleteRecursively()
                    }
                    App.cache.clear()
                    state.message = "已清理图片与内容缓存"
                },
            )
            ArrowPreference(
                title = "关于",
                modifier = Modifier.testTag("settings_about"),
                summary = "v${state.version} · 上游库与开源致谢",
                onClick = { nav.push(Route.About) },
            )
            ArrowPreference(
                title = "频道",
                modifier = Modifier.testTag("settings_channel"),
                summary = "Telegram · MiboWeibo",
                onClick = { openExternalUrl(context, "https://t.me/MiboWeibo") },
            )
        }
    }

    // 更新组已并入「其他」：更新卡排在其他卡之后。
    updateSettingsItems(nav = nav, state = state, scope = scope, showHeader = false)

    if (state.message.isNotBlank()) {
        item {
            Text(
                state.message,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
private fun SettingsDialogs(
    nav: NavState,
    state: SettingsUiState,
) {
    val leftOptions = feedTabOptions(state.feedGroups, allowNone = true, current = if (state.guest) null else state.feedLeftTab)
    val rightOptions = feedTabOptions(state.feedGroups, allowNone = true, current = if (state.guest) null else state.feedRightTab)

    if (state.pickFeedLeft) {
        FeedTabPicker(
            title = "微博左侧栏内容",
            selected = state.feedLeftTab,
            options = leftOptions,
            onDismiss = { state.pickFeedLeft = false },
            onPick = { selection ->
                App.settings.applyFeedLeftTab(selection)
                state.feedLeftTab = selection
                state.pickFeedLeft = false
            },
        )
    }

    if (state.pickFeedRight) {
        FeedTabPicker(
            title = "微博右侧栏内容",
            selected = state.feedRightTab,
            options = rightOptions,
            onDismiss = { state.pickFeedRight = false },
            onPick = { selection ->
                App.settings.applyFeedRightTab(selection)
                state.feedRightTab = selection
                state.pickFeedRight = false
            },
        )
    }

}

/**
 * 「账号相关」页：从长按底栏头像的菜单进入，收纳原设置页「账号与内容」里的
 * 本地关注使用账号与同步本地收藏两项（已按用户要求从设置页移走）。
 */
@Composable
fun AccountRelatedScreen(nav: NavState) {
    val scope = rememberCoroutineScope()
    var localFollowUid by remember { mutableStateOf(App.settings.localFollowUid) }
    var pickLocalFollow by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val accounts = remember(localFollowUid) { App.store.accounts() }
    val localFollowAccount = accounts.firstOrNull { it.uid == localFollowUid }
    val localFollowSummary = if (localFollowAccount == null) {
        "跟随当前账号"
    } else {
        "用 ${localFollowAccount.name.ifBlank { "UID ${localFollowAccount.uid}" }} 的身份取数"
    }
    val blurEffects = blurEffectsEnabled()
    val pageColor = pageContainerColor()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffects, backdrop).background(pageColor)) {
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
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        ArrowPreference(
                            title = "账号管理",
                            summary = "切换、重新登录或删除账号",
                            onClick = { nav.push(Route.Accounts) },
                            modifier = Modifier.testTag("account_related_manage_accounts"),
                        )
                        ArrowPreference(
                            title = "本地关注使用账号",
                            summary = localFollowSummary,
                            onClick = { pickLocalFollow = true },
                            modifier = Modifier.testTag("account_related_local_follow"),
                        )
                        ArrowPreference(
                            title = "同步本地收藏",
                            summary = if (syncing) {
                                "正在同步微博收藏"
                            } else {
                                "将微博收藏复制到本地收藏"
                            },
                            onClick = {
                                if (!syncing) {
                                    syncing = true
                                    scope.launch {
                                        runCatching { App.repo.syncLocalFavorites() }
                                            .onSuccess { count ->
                                                App.toast("已同步 $count 条微博到本地收藏")
                                            }
                                            .onFailure { App.toast(it.message ?: "同步失败") }
                                        syncing = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("account_related_sync_favorites"),
                        )
                    }
                }
                // 退出登录从设置「应用相关」移来：破坏性操作，单独一张卡。
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        if (App.repo.guest) {
                            ArrowPreference(
                                title = "退出纯本地模式",
                                modifier = Modifier.testTag("settings_leave_local"),
                                summary = "回到登录页，本地关注名单保留",
                                onClick = {
                                    App.settings.applyLocalMode(false)
                                    nav.reset(Route.SmsLogin())
                                },
                            )
                        } else {
                            ArrowPreference(
                                title = "退出登录",
                                modifier = Modifier.testTag("settings_logout"),
                                summary = "清除本地会话",
                                onClick = { confirmLogout = true },
                            )
                        }
                    }
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
                        title = "账号相关",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }

    if (pickLocalFollow) {
        LocalFollowPicker(
            accounts = accounts,
            selectedUid = localFollowUid,
            onDismiss = { pickLocalFollow = false },
            onPick = { uid ->
                App.settings.applyLocalFollowUid(uid)
                localFollowUid = uid
                pickLocalFollow = false
            },
        )
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "退出登录",
            message = "会清除本地保存的 Cookie，下次需要重新登录。",
            confirmText = "退出",
            onDismiss = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                App.repo.logout()
                nav.reset(Route.SmsLogin())
            },
        )
    }
}

/**
 * 底栏调节页：全屏只有调节滑杆和底栏本体。底栏按主页同一套定位贴在屏幕底部
 * （导航栏让位 + 底边 8dp + 当前偏移），拖滑杆时实时跟随，位置所见即所得。
 */
@Composable
fun BarPositionScreen(nav: NavState) {
    var offsetX by remember { mutableFloatStateOf(App.settings.barOffsetX) }
    var offsetY by remember { mutableFloatStateOf(App.settings.barOffsetY) }
    var scale by remember { mutableFloatStateOf(App.settings.barScale) }
    val blurEffects = blurEffectsEnabled()
    val pageColor = pageContainerColor()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffects, backdrop).background(pageColor)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = glassBarContentTop(topBarHeight, 10.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = CardDefaults.CornerRadius,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SliderPreference(
                        value = offsetX,
                        onValueChange = {
                            offsetX = it
                            App.settings.applyBarOffsetX(it)
                        },
                        title = "底栏左右偏移",
                        summary = "左右移动悬浮底栏的位置，0 为原位置",
                        valueText = "${offsetX.toInt()}dp",
                        valueRange = -32f..32f,
                        modifier = Modifier.testTag("bar_position_offset_x"),
                    )
                    SliderPreference(
                        value = offsetY,
                        onValueChange = {
                            offsetY = it
                            App.settings.applyBarOffsetY(it)
                        },
                        title = "底栏上下偏移",
                        summary = "上下移动悬浮底栏的位置，0 为原位置",
                        valueText = "${offsetY.toInt()}dp",
                        valueRange = -32f..32f,
                        modifier = Modifier.testTag("bar_position_offset_y"),
                    )
                    SliderPreference(
                        value = scale,
                        onValueChange = {
                            scale = it
                            App.settings.applyBarScale(it)
                        },
                        title = "底栏大小",
                        summary = "整体缩放悬浮底栏与展开的三个按钮，100% 为原大小",
                        valueText = "${(scale * 100).toInt()}%",
                        valueRange = 0.85f..1.15f,
                        modifier = Modifier.testTag("bar_position_scale"),
                    )
                }
                if (offsetX != 0f || offsetY != 0f || scale != 1f) {
                    TextButton(
                        text = "恢复默认",
                        onClick = {
                            offsetX = 0f
                            offsetY = 0f
                            scale = 1f
                            App.settings.applyBarOffsetX(0f)
                            App.settings.applyBarOffsetY(0f)
                            App.settings.applyBarScale(1f)
                        },
                        modifier = Modifier.testTag("bar_position_reset"),
                    )
                }
            }
        }

        // 直接渲染主页同款底栏组件（大小缩放由组件内部按设置生效，与主页一致），
        // 大小、位置与主页完全一致；guest=true 让「消息」只是普通项，回调全为空操作。
        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffects) backdrop else null,
        ) {
            PinnedBarDensity {
                FloatingBottomBar(
                    tab = 0,
                    guest = true,
                    onNotifications = {},
                    onSearch = {},
                    onTab = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = offsetX.dp, y = offsetY.dp),
                )
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffects) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageColor,
                    scrollFraction = { 0f },
                ) {
                    SmallTopAppBar(
                        title = "底栏调节",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }
}

/** 打赏浮窗：只展示作者的微信赞赏码图片，无标题无边框，点浮窗外关闭。 */
@Composable
internal fun RewardCodeDialog(onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    OverlayDialog(
        show = show,
        title = null,
        outsideMargin = DpSize(16.dp, 8.dp),
        largeScreen = true,
        onDismissRequest = { show = false },
        onDismissFinished = onDismiss,
    ) {
        Image(
            painter = painterResource(com.mibo.app.R.drawable.reward_code),
            contentDescription = "打赏",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LocalFollowPicker(
    accounts: List<Account>,
    selectedUid: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    OverlayDialog(
        show = show,
        title = "本地关注使用账号",
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                val uid = result
                if (uid != null) onPick(uid) else onDismiss()
            }
        },
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth(),
                propagateMinConstraints = true,
            ) {
                ListPopupColumn {
                    val optionCount = accounts.size + 1
                    Box(Modifier.testTag("local_follow_follow_current")) {
                        DropdownImpl(
                        item = DropdownItem(text = "跟随当前账号"),
                        optionSize = optionCount,
                        isSelected = selectedUid.isBlank(),
                        index = 0,
                        dialogMode = true,
                        onSelectedIndexChange = {
                            result = ""
                            show = false
                        },
                    )
                    }
                    accounts.forEachIndexed { i, account ->
                        Box(Modifier.testTag("local_follow_option_${account.uid}")) {
                            DropdownImpl(
                            item = DropdownItem(
                                text = account.name.ifBlank { "UID ${account.uid}" },
                                summary = if (account.name.isBlank()) null else "UID ${account.uid}",
                            ),
                            optionSize = optionCount,
                            isSelected = account.uid == selectedUid,
                            index = i + 1,
                            dialogMode = true,
                        onSelectedIndexChange = {
                                result = accounts[it - 1].uid
                                show = false
                            },
                        )
                        }
                    }
                }
            }
            DialogButtons(
                negativeText = "取消",
                onNegative = { show = false },
            )
        }
    }
}

@Composable
private fun FeedTabPicker(
    title: String,
    selected: FeedTabSelection,
    options: List<FeedTabSelection>,
    onDismiss: () -> Unit,
    onPick: (FeedTabSelection) -> Unit,
) {
    OptionPicker(
        title = title,
        options = options.map { it.key to it.label },
        selectedKey = selected.key,
        tagPrefix = "feed_tab",
        groupKey = "feed-tabs",
        summaryOf = { key -> if (options.firstOrNull { it.key == key }?.isGroup == true) "关注分组" else null },
        onDismiss = onDismiss,
        onPick = { key -> options.firstOrNull { it.key == key }?.let(onPick) ?: onDismiss() },
    )
}

/** 单列选项弹窗：[options] 是 key 到显示文案，选中项等窗口收完再交出去。 */
@Composable
private fun OptionPicker(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    tagPrefix: String,
    groupKey: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    /** 第二行小字，没有就不显示。 */
    summaryOf: (String) -> String? = { null },
) {
    var show by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    OverlayDialog(
        show = show,
        title = title,
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                val picked = result
                if (picked != null) onPick(picked) else onDismiss()
            }
        },
    ) {
        Column {
            // ListPopupColumn + DropdownImpl 负责选中态的勾和蓝字（Miuix 版没有
            // COUI 的按住上下拖选手势，改为逐项点击）。propagateMinConstraints 把对话框
            // 的宽度传给列，否则列会缩到弹层的最小宽度。不给高度设上限，整张表一口气排完。
            Box(
                Modifier
                    .fillMaxWidth(),
                propagateMinConstraints = true,
            ) {
                ListPopupColumn {
                    options.forEachIndexed { index, (key, label) ->
                        Box(Modifier.testTag("${tagPrefix}_option_$key")) {
                            DropdownImpl(
                            item = DropdownItem(text = label, summary = summaryOf(key)),
                            optionSize = options.size,
                            isSelected = key == selectedKey,
                            index = index,
                            dialogMode = true,
                            onSelectedIndexChange = {
                                result = options[it].first
                                show = false
                            },
                        )
                        }
                    }
                }
            }
            DialogButtons(
                negativeText = "取消",
                onNegative = { show = false },
            )
        }
    }
}

@Composable
private fun DialogButtons(
    negativeText: String,
    onNegative: () -> Unit,
    positiveText: String? = null,
    onPositive: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(text = negativeText, onClick = onNegative)
        if (positiveText != null && onPositive != null) {
            Spacer(Modifier.width(8.dp))
            TextButton(
                text = positiveText,
                onClick = onPositive,
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/** 通知设置：后台通知总开关 + 轮询间隔（填数字，离框保存）+ 自启动 / 忽略电池优化快捷入口。 */
@Composable
fun NotifySettingsScreen(nav: NavState) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(App.settings.notifyEnabled) }
    var newPosts by remember { mutableStateOf(App.settings.notifyNewPosts) }
    var intervalText by remember { mutableStateOf(App.settings.notifyIntervalSeconds.toString()) }
    var batteryIgnored by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryIgnored = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun applyEnabled(value: Boolean) {
        enabled = value
        App.settings.applyNotifyEnabled(value)
        NotifyPollService.sync(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            applyEnabled(true)
        } else {
            App.toast("没有通知权限，系统通知弹不出来")
        }
    }
    val blurEffects = blurEffectsEnabled()
    val pageColor = pageContainerColor()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffects, backdrop).background(pageColor)) {
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
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        SwitchPreference(
                            checked = enabled,
                            onCheckedChange = { want ->
                                if (!want) {
                                    applyEnabled(false)
                                } else if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    applyEnabled(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            title = "后台通知",
                            summary = "应用不在前台时，定时检查新的赞、评论和 @，有新的就发系统通知",
                            modifier = Modifier.testTag("notify_enabled"),
                        )
                        SwitchPreference(
                            checked = newPosts,
                            onCheckedChange = { v ->
                                newPosts = v
                                App.settings.applyNotifyNewPosts(v)
                            },
                            title = "新帖推送",
                            summary = "关注的人发新微博时也发一条通知",
                            modifier = Modifier.testTag("notify_new_posts"),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("轮询间隔")
                                Text(
                                    "单位秒，时间越短越耗电",
                                    fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            TextField(
                                value = intervalText,
                                onValueChange = { input ->
                                    intervalText = input.filter { it.isDigit() }.take(4)
                                },
                                modifier = Modifier
                                    .width(96.dp)
                                    .onFocusChanged { focus ->
                                        if (!focus.isFocused) {
                                            intervalText.trim().toIntOrNull()?.let {
                                                App.settings.applyNotifyIntervalSeconds(it)
                                            }
                                            intervalText = App.settings.notifyIntervalSeconds.toString()
                                        }
                                    }
                                    .testTag("notify_interval"),
                                label = "秒",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1,
                                minLines = 1,
                            )
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        ArrowPreference(
                            title = "自启动",
                            summary = "去系统里允许 Mibo 自启动，重启手机后通知监测才能自己恢复",
                            onClick = { openAutostartSettings(context) },
                            modifier = Modifier.testTag("notify_autostart"),
                        )
                        ArrowPreference(
                            title = "忽略电池优化",
                            summary = if (batteryIgnored) {
                                "已忽略电池优化，后台轮询不会被系统冻结"
                            } else {
                                "还没忽略，系统可能冻结后台轮询，点此去设置"
                            },
                            onClick = { if (!batteryIgnored) requestIgnoreBatteryOptimizations(context) },
                            modifier = Modifier.testTag("notify_battery"),
                        )
                    }
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
                        title = "通知设置",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(
        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:" + context.packageName),
    )
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

/** 小米的自启动管理页在安全中心里；打不开就退到应用详情页。 */
private fun openAutostartSettings(context: Context) {
    val miuiIntent = Intent().setComponent(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
    )
    val opened = runCatching { context.startActivity(miuiIntent); true }.getOrDefault(false)
    if (!opened) {
        runCatching {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName),
                ),
            )
        }
    }
}
