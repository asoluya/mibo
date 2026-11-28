package com.mibo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Account
import com.mibo.app.data.FeedPage
import com.mibo.app.data.FeedTabItem
import com.mibo.app.data.FeedTabSelection
import com.mibo.app.data.FollowGroup
import com.mibo.app.data.WEIBO_PROFILE_EDIT_URL
import com.mibo.app.data.feedTabOptions
import com.mibo.app.data.currentCoords
import com.mibo.app.data.hasLocationPermission
import com.mibo.app.data.nearbyCoords
import com.mibo.app.screen.BindSearchSuggestionsEffect
import com.mibo.app.screen.FeedFilter
import com.mibo.app.screen.FeedStore
import com.mibo.app.screen.LocalMessagesPlaceholder
import com.mibo.app.screen.MessagesScreen
import com.mibo.app.screen.MessagesTabState
import com.mibo.app.screen.ProfileHubContent
import com.mibo.app.screen.ProfileTabState
import com.mibo.app.screen.SearchDiscoverState
import com.mibo.app.screen.SearchFilterMenu
import com.mibo.app.screen.SearchResultsState
import com.mibo.app.screen.SearchTab
import com.mibo.app.screen.TimelineScreen
import com.mibo.app.screen.TimelineScrollHolder
import com.mibo.app.screen.dismissHistoryEditingOnTap
import com.mibo.app.screen.rememberCoverContrast
import com.mibo.app.screen.rememberSearchDiscoverState
import com.mibo.app.screen.rememberSearchFilterTabs
import com.mibo.app.screen.rememberSearchResultsState
import com.mibo.app.screen.searchDiscoverItems
import com.mibo.app.screen.searchHorizontalPadding
import com.mibo.app.screen.searchResultsItems
import com.mibo.app.screen.NearbyCityPickerDialog
import com.mibo.app.ui.components.Avatar
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.ProfileEditDialog
import com.mibo.app.ui.components.navBottomPadding
import top.yukonga.miuix.kmp.anim.AccelerateEasing
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 搜索面板进出场曲线（原 COUIInEase / COUIOutEase 的近似）。 */
private val PanelInEasing = AccelerateEasing()
private val PanelOutEasing = DecelerateEasing()

/** 搜索筛选胶囊的行高（原 ChipTabDefaults.Height 的近似值）。 */
internal val SearchFilterChipHeight = 40.dp

/** 搜索结果两行顶栏第二行的底部留白（原 TwoRowTopAppBarDefaults.SecondRowBottom 的近似值）。 */
internal val SearchSecondRowBottom = 8.dp

/** 导航弹窗菜单的尺寸默认值（原 COUI NavigationPopupMenuDefaults；Miuix 无对应常量，取近似值）。 */
internal object MiboPopupMenuDefaults {
    val MaxHeight = 480.dp
    val MinWidth = 200.dp
    val HorizontalMargin = 16.dp
    val VerticalMargin = 8.dp
}

/** 顶栏动作项的固定占位（原 couiTopAppBarActionItem 的近似：48dp 见方触控区）。 */
internal fun Modifier.topAppBarActionItem(): Modifier = size(48.dp)

internal sealed class FeedTabSpec {
    abstract val key: String
    abstract val position: String
    abstract val label: String
    abstract val tag: String
    abstract val emptyText: String
    abstract val isFollow: Boolean

    /** 只有中间栏带「更多」菜单：它列的就是「微博中间栏内容」那份选项。 */
    abstract val isCenter: Boolean

    /** 顶栏格子里实际显示的名字：本地关注只显示「本地」，和其余两字标签对称。 */
    open val tabLabel: String get() = label

    data class Custom(val selection: FeedTabSelection, override val position: String, val guest: Boolean) : FeedTabSpec() {
        override val key: String get() = "${position}_${selection.key}"
        override val label: String get() = selection.label
        override val tabLabel: String get() = if (selection.item == FeedTabItem.Local) "本地" else label
        override val tag: String get() = if (selection.isGroup) {
            "feed_tab_group_${selection.group?.gid.orEmpty()}"
        } else {
            selection.item.tag
        }
        override val isFollow: Boolean get() = false
        override val isCenter: Boolean get() = position == "center"
        override val emptyText: String get() = when {
            selection.isGroup -> "「${selection.label}」还没有内容"
            else -> when (selection.item) {
                FeedTabItem.Follow -> if (guest) "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里" else "还没有关注的人"
                FeedTabItem.Circle -> "好友还没有内容"
                FeedTabItem.Recommend -> "这里还没有微博"
                FeedTabItem.Local -> if (guest) "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里" else "还没有本地关注"
                FeedTabItem.Original -> "还没有原创微博"
                FeedTabItem.Special -> "特别关注还没有内容"
                FeedTabItem.Secret -> "还没有悄悄关注的人"
                FeedTabItem.Favorites -> if (App.settings.favoritesCloud && !App.repo.guest) "微博账号里还没有收藏" else "还没有本地收藏"
                FeedTabItem.Nearby -> "附近需要定位\n打开系统定位后，重新点进附近会自动读取当前位置"
                FeedTabItem.HotSearch -> "热搜暂时没有内容"
                FeedTabItem.Group -> "在顶栏的「更多」里选一个关注分组"
                FeedTabItem.None -> "这里还没有微博"
            }
        }
    }

    data class Follow(val filter: FeedFilter, val guest: Boolean, override val position: String = "center") : FeedTabSpec() {
        override val key: String get() = if (position == "center") "follow" else "${position}_follow"
        override val label: String get() = if (guest) "本地关注" else filter.label
        override val tabLabel: String get() = if (guest) "本地" else label
        override val tag: String get() = "feed_tab_follow"
        override val isFollow: Boolean get() = true
        override val isCenter: Boolean get() = position == "center"
        override val emptyText: String get() = if (guest) {
            "还没有本地关注的人\n在别人主页点「本地关注」，他的微博会出现在这里"
        } else {
            filter.emptyText
        }
    }
}

@Composable
fun MainShell(nav: NavState) {
    val tab = nav.mainTab
    val holder = rememberSaveableStateHolder()
    val backdrop = rememberLayerBackdrop()
    val searchBackdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val scope = rememberCoroutineScope()
    // The bar floats over all three pages, so the line it ends at is reported once here and read
    // by whatever has to be placed clear of it.
    val topBarBounds = remember { TopBarBounds() }

    // 纯本地模式：没有账号可读关注流，首页只剩「本地关注」和「推荐」两段，筛选菜单也收起来。
    // 登录 / 退出都会 reset 导航栈，这个值在整个 MainShell 生命周期里不会变。
    val guest = remember { App.repo.guest }
    val accountOwner = remember { App.repo.session?.uid.orEmpty() }
    LaunchedEffect(Unit) { App.repo.refreshSessionIfStale() }
    val farLeftTab = App.settings.feedFarLeftTab
    val leftTab = App.settings.feedLeftTab
    val centerTab = App.settings.feedCenterTab
    val rightTab = App.settings.feedRightTab
    val farRightTab = App.settings.feedFarRightTab

    // 中间栏看哪条流由「中间栏内容」决定，菜单只是把这一项搬到页面上。原来的「筛选」只剩
    // 一份好友流，显示时统一叫「全部」；纯本地模式没有好友流，仍按「本地关注」走。
    val filter = if (guest) FeedFilter.Local else FeedFilter.All
    val friendsStore = remember {
        FeedStore("friends", resumable = true, dropStaleOutliers = true, snapshotOwner = { accountOwner }) { cursor, force ->
            App.repo.friends(cursor, force)
        }
    }
    // 前台新微博检测：应用在前台时每隔设定秒数轻查一次关注流第一页，顶部 ID 变了就
    // 走专用通道自动刷新（回顶刷新），不再发光提醒；在推荐等其它页时不触发，
    // 自动刷新只针对关注页与特别关注页。游客没有关注流，不查。
    val lifecycleOwner = LocalLifecycleOwner.current
    var appResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appResumed = true
            if (event == Lifecycle.Event.ON_PAUSE) appResumed = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(appResumed, guest, App.settings.monitorNewPosts, App.settings.monitorIntervalSeconds) {
        if (!appResumed || guest || !App.settings.monitorNewPosts) {
            return@LaunchedEffect
        }
        val intervalSeconds = App.settings.monitorIntervalSeconds
        while (true) {
            delay(intervalSeconds * 1_000L)
            if (!friendsStore.loaded || friendsStore.refreshing || friendsStore.loading) continue
            val topId = friendsStore.items.firstOrNull()?.id ?: continue
            val page = runCatching { App.repo.friends(null, force = true) }.getOrNull() ?: continue
            val newestId = page.items.firstOrNull()?.id ?: continue
            if (newestId != topId) App.requestFriendsTop()
        }
    }
    val filterStores = remember {
        mapOf(
            FeedFilter.All to friendsStore,
            FeedFilter.Following to friendsStore,
            FeedFilter.Original to FeedStore("original", resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
                originalFeed(cursor, force)
            },
            FeedFilter.Unread to FeedStore("unread", snapshotOwner = { accountOwner }) { _, _ -> FeedPage(emptyList(), null) },
            FeedFilter.Favorites to FeedStore("favorites") { cursor, force ->
                // 「收藏」流两份来源：本地收藏（默认）与账号云端收藏，菜单里切换。
                if (App.settings.favoritesCloud && !App.repo.guest) {
                    App.repo.cloudFavorites(cursor, force)
                } else {
                    App.repo.favorites(cursor, force)
                }
            },
            FeedFilter.Special to FeedStore("special", resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
                App.repo.special(cursor, force)
            },
            FeedFilter.Circle to FeedStore("circle", resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
                App.repo.circle(cursor, force)
            },
            FeedFilter.Secret to FeedStore("secret", resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
                App.repo.secret(cursor, force)
            },
            FeedFilter.Local to FeedStore("local", readOnly = true, resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
                App.repo.localFollows(cursor, force)
            },
        )
    }
    val favoritesVersion = App.repo.favoritesVersion
    LaunchedEffect(favoritesVersion) {
        val favStore = filterStores[FeedFilter.Favorites] ?: return@LaunchedEffect
        if (favoritesVersion > 0 && favStore.loaded) favStore.refresh()
    }
    // 本地/账号收藏切换后整条流重拉。
    val favoritesCloud = App.settings.favoritesCloud
    LaunchedEffect(favoritesCloud) {
        val favStore = filterStores[FeedFilter.Favorites] ?: return@LaunchedEffect
        if (favStore.loaded) favStore.refresh()
    }
    val recommendStore = remember { FeedStore("102803", readOnly = guest, resumable = true, snapshotOwner = { accountOwner }) }
    // 热搜栏不是微博流：这个占位 store 只为让 stores 列表与 pager 页数对齐（永远不会被
    // TimelinePage 渲染），热搜页本体由 TimelineScreen 按页号换成热搜榜。
    val hotSearchStore = remember { FeedStore("hotsearch") { _, _ -> FeedPage(emptyList(), null) } }
    val circleStore = filterStores.getValue(FeedFilter.Circle)
    val followStore = filterStores.getValue(filter)

    // 关注分组：分组是账号里的数据，gid 不同就是不同的一条流，一组一个 store。
    val groupStores = remember { HashMap<String, FeedStore>() }

    // 附近：坐标来自设备定位或用户选的城市，换位置之后这条流要整条重来。
    val nearbyStore = remember {
        FeedStore("nearby", resumable = true, snapshotOwner = { accountOwner }) { cursor, force ->
            val coords = nearbyCoords(App.context)
            if (coords == null) {
                FeedPage(emptyList(), null)
            } else {
                App.repo.nearby(coords.first.toString(), coords.second.toString(), cursor, force)
            }
        }
    }
    val nearbyLocationKey = "${App.settings.nearbyCustom}:${App.settings.nearbyPlaceName}"
    var lastNearbyLocationKey by remember { mutableStateOf(nearbyLocationKey) }
    LaunchedEffect(nearbyLocationKey) {
        if (nearbyLocationKey != lastNearbyLocationKey) {
            lastNearbyLocationKey = nearbyLocationKey
            nearbyStore.refresh()
        }
    }

    fun storeFor(selection: FeedTabSelection): FeedStore = when (selection.item) {
        FeedTabItem.Follow -> followStore
        FeedTabItem.Circle -> circleStore
        FeedTabItem.Recommend -> recommendStore
        FeedTabItem.Local -> filterStores.getValue(FeedFilter.Local)
        FeedTabItem.Original -> filterStores.getValue(FeedFilter.Original)
        FeedTabItem.Special -> filterStores.getValue(FeedFilter.Special)
        FeedTabItem.Secret -> filterStores.getValue(FeedFilter.Secret)
        FeedTabItem.Favorites -> filterStores.getValue(FeedFilter.Favorites)
        FeedTabItem.Nearby -> nearbyStore
        FeedTabItem.HotSearch -> hotSearchStore
        FeedTabItem.Group -> selection.group?.let { group ->
            groupStores.getOrPut(group.gid) {
                FeedStore("group_${group.gid}", resumable = true, snapshotOwner = { accountOwner }) { cursor, _ ->
                    App.repo.groupFeed(group.gid, cursor)
                }
            }
        } ?: friendsStore
        FeedTabItem.None -> friendsStore
    }

    // 动态构建主页 Tab 列表：五格从左到右——最左 / 左 / 中间（不可隐藏） / 右 / 最右；
    // 最左与最右是可选栏，默认隐藏，不启用时与原来的三栏完全一样。
    val feedTabs: List<FeedTabSpec> = remember(farLeftTab, leftTab, centerTab, rightTab, farRightTab, filter, guest) {
        buildList {
            // 关注分组是账号里的数据：纯本地模式读不到，这一栏退回「本地关注」。
            fun addColumn(selection: FeedTabSelection, position: String) {
                val effective = if (guest && selection.isGroup) FeedTabSelection(FeedTabItem.Follow) else selection
                when (effective.item) {
                    FeedTabItem.None -> Unit
                    FeedTabItem.Follow -> add(FeedTabSpec.Follow(filter, guest, position))
                    else -> add(FeedTabSpec.Custom(effective, position, guest))
                }
            }
            addColumn(farLeftTab, "far_left")
            addColumn(leftTab, "left")
            addColumn(centerTab, "center")
            addColumn(rightTab, "right")
            addColumn(farRightTab, "far_right")
        }
    }

    val feedStores = feedTabs.map { spec ->
        when (spec) {
            is FeedTabSpec.Follow -> followStore
            is FeedTabSpec.Custom -> storeFor(spec.selection)
        }
    }
    val feedEmptyTexts = feedTabs.map { it.emptyText }
    // 哪几页是热搜栏：TimelineScreen 按页号把这几页渲染成热搜榜而不是微博流。
    val feedHotPages = feedTabs.indices.filter { index ->
        (feedTabs[index] as? FeedTabSpec.Custom)?.selection?.item == FeedTabItem.HotSearch
    }.toSet()

    // 「更多」菜单里的关注分组。分组列表在网页版，只在打开菜单时取一次（Repo 里还有一层缓存）；
    // 拉不到就只显示内置流，不影响其它栏。
    var feedGroups by remember { mutableStateOf<List<FollowGroup>>(emptyList()) }
    val loadFeedGroups: () -> Unit = {
        if (!guest) {
            scope.launch {
                runCatching { App.repo.followGroups() }
                    .onSuccess { feedGroups = it }
            }
        }
    }
    LaunchedEffect(guest) { loadFeedGroups() }
    val centerOptions = feedTabOptions(feedGroups, allowNone = false, current = centerTab)

    val followIndex = feedTabs.indexOfFirst { it.isFollow }.coerceAtLeast(0)
    val initialPage = feedTabs.indexOfFirst { it.key == App.settings.feedTabKey }.takeIf { it >= 0 }
        ?: followIndex

    val feedPager = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (feedTabs.size - 1).coerceAtLeast(0)),
        pageCount = { feedTabs.size },
    )
    LaunchedEffect(feedPager.currentPage, feedTabs) {
        val currentTab = feedTabs.getOrNull(feedPager.currentPage)
        if (currentTab != null) {
            App.settings.applyFeedTabKey(currentTab.key)
            val legacyKind = when {
                currentTab.isFollow -> 1
                currentTab is FeedTabSpec.Custom && currentTab.selection.item == FeedTabItem.Circle -> 0
                else -> 2
            }
            App.settings.applyFeedTab(legacyKind)
        }
    }

    LaunchedEffect(feedTabs.size) {
        if (feedPager.currentPage >= feedTabs.size) {
            feedPager.scrollToPage(followIndex)
        }
    }

    // 点进「附近」就主动读一次当前位置：没权限先弹系统授权；拿到新定位后整条流重拉。
    // 设了自定义城市时不打扰，继续看那座城。
    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) {
            scope.launch {
                currentCoords(App.context)
                nearbyStore.refresh()
            }
        } else {
            App.toast("需要定位权限才能看附近")
        }
    }
    val currentFeedSpec = feedTabs.getOrNull(feedPager.currentPage)
    val onNearbyFeed = tab == 0 &&
        (currentFeedSpec as? FeedTabSpec.Custom)?.selection?.item == FeedTabItem.Nearby
    LaunchedEffect(onNearbyFeed) {
        if (onNearbyFeed && !App.settings.nearbyCustom) {
            if (hasLocationPermission(App.context)) {
                currentCoords(App.context)
                nearbyStore.refresh()
            } else {
                nearbyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    val feedHasTabs = feedTabs.size > 1
    val feedSlotKeys = feedTabs.map { it.position }
    val barDensity = rememberBarDensity()
    val barTitleHeightPx = with(barDensity) { FeedTitleRowHeight.toPx() }
    // The top bar floats over the lists; the first row starts below it and later rows scroll under.
    // 栏钉在自己的密度上，页面按系统密度排版，所以这里要按像素换算回当前密度。
    // 消息 and 我 wear the feed's title row alone, so their bar is exactly one row tall.
    val topBarHeight = pinnedHeight(FeedTitleRowHeight)
    // The feed header is two rows: the title row folds away as the list scrolls, the segment row
    // stays pinned under the status bar.
    // 标题行去掉后页面顶 inset 收紧：只剩状态栏下留白（微博页再加分段行）；搜索面板仍用 topBarHeight 原值。
    val pageTopInset = pinnedHeight(CompactTitleGap)
    val feedPageTopInset = if (feedHasTabs) {
        pinnedHeight(CompactTitleGap, TopAppBarDefaults.SmallTopAppBarCenterHeight)
    } else {
        pageTopInset
    }
    var feedCollapse by remember { mutableFloatStateOf(0f) }
    val timelineScrollHolder = remember { TimelineScrollHolder() }
    val messagesState = remember { MessagesTabState() }
    val profileTabState = remember { ProfileTabState() }
    var editProfileOpen by remember { mutableStateOf(false) }
    var showNearbyCityPicker by remember { mutableStateOf(false) }
    val profileListState = rememberLazyListState()
    var profileActiveCover by remember { mutableStateOf<String?>(null) }
    val profileScrolledPast by remember(profileListState, barDensity) {
        derivedStateOf {
            listScrollAlpha(profileListState, barDensity) >= 0.45f
        }
    }
    val isTopRoute = rememberIsTopRoute(nav)
    val profileCoverContrast = rememberCoverContrast(
        coverUrl = if (guest) null else profileActiveCover,
        topBarHeight = pageTopInset,
        active = tab == 2 && !guest && !profileScrolledPast && isTopRoute,
        manageStatusBar = isTopRoute,
    )
    val meTitleColor = if (profileCoverContrast.useWhiteContent) Color.White else MiuixTheme.colorScheme.onSurface
    var feedSearchQuery by remember { mutableStateOf("") }
    var feedSearchExpanded by remember { mutableStateOf(false) }
    // 每次提交搜索 +1：标题栏据此在提交后压掉输入框挂载时的自动焦点（点热搜条进入结果不弹输入法）。
    var searchSubmitNonce by remember { mutableStateOf(0) }
    val panelAlpha = remember { Animatable(0f) }
    val panelMounted by remember {
        derivedStateOf {
            feedSearchExpanded || panelAlpha.value > 0.001f
        }
    }
    val discoverState = rememberSearchDiscoverState()
    LaunchedEffect(Unit) { discoverState.ensureHotLoaded() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 首页搜索展开期间锁定标题行不折叠，防止部分折叠或软键盘弹出时挤压/裁切搜索胶囊；收起后恢复跟踪列表滚动。
    // 顶栏悬浮常驻：下滑时也不折叠，标题行与分段行始终可见可点（原折叠目标停用）。
    val collapseTarget = 0f
    val collapseShown = remember { Animatable(collapseTarget) }
    var wasSearchExpanded by remember { mutableStateOf(feedSearchExpanded) }
    LaunchedEffect(collapseTarget, feedSearchExpanded) {
        val searchTransition = (wasSearchExpanded != feedSearchExpanded) || (feedSearchExpanded && collapseShown.value > 0f)
        wasSearchExpanded = feedSearchExpanded
        if (abs(collapseTarget - collapseShown.value) > 0.6f || searchTransition) {
            collapseShown.animateTo(collapseTarget, tween(MainTabFadeMillis))
        } else {
            collapseShown.snapTo(collapseTarget)
        }
    }
    val collapsed = collapseShown.value

    val homeResultsState = rememberSearchResultsState(scope)
    val searchListState = rememberLazyListState()

    // 搜索面板的两种中间态：关键词还没提交（「建议中」，底下仍是热搜/历史），以及已有结果在显示。
    // 顶栏的筛选行与列表区都读同一份判断，避免两处各写一份后漂移。
    val searchSuggesting =
        feedSearchQuery.isNotBlank() && feedSearchQuery.trim() != homeResultsState.submitted
    val searchShowingResults =
        homeResultsState.submitted.isNotBlank() && !searchSuggesting && feedSearchQuery.isNotBlank()
    var searchFilterMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(searchShowingResults) {
        if (!searchShowingResults) searchFilterMenuOpen = false
    }

    // 搜索页两种内容的进出场由本文件内的 SearchResultContainer 本地实现编排（原 COUI 同名组件的替代）：
    // 提交后先淡出旧内容再翻到结果页淡入，加载指示器叠在列表顶部留白处；
    // 返回搜索页时结果淡出、再淡入热搜与历史，顶栏搜索框本身保持稳住不闪烁。
    // queryKey 只认提交的关键词：换分类、换排序走的是较轻的「刷新上浮」，换关键词才整段重新上浮。
    val searchTransition = rememberSearchResultsTransitionState(
        showingResults = searchShowingResults,
        loading = homeResultsState.loading,
        queryKey = homeResultsState.submitted,
    )

    val openFeedSearch: () -> Unit = {
        discoverState.clearEditing()
        homeResultsState.clearResults()
        feedSearchQuery = ""
        feedSearchExpanded = true
    }
    val cancelFeedSearch: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        discoverState.clearEditing()
        feedSearchExpanded = false
    }
    val closeFeedSearchImmediately: suspend () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        discoverState.clearEditing()
        homeResultsState.clearResults()
        feedSearchQuery = ""
        feedSearchExpanded = false
        panelAlpha.snapTo(0f)
    }
    val submitHomeSearch: (String) -> Unit = { raw ->
        val keyword = raw.trim()
        if (keyword.isNotEmpty()) {
            searchSubmitNonce++
            feedSearchQuery = keyword
            keyboard?.hide()
            focusManager.clearFocus()
            discoverState.clearEditing()
            homeResultsState.runSearch(keyword, discoverState)
        }
    }

    LaunchedEffect(feedSearchExpanded) {
        if (feedSearchExpanded) {
            scope.launch { discoverState.ensureHotLoaded() }
            // 整块面板淡入，没有几何变化。
            panelAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = SearchPanelFadeMillis, easing = PanelInEasing),
            )
        } else {
            panelAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = SearchPanelFadeMillis, easing = PanelOutEasing),
            )
            if (!feedSearchExpanded) {
                homeResultsState.clearResults()
                feedSearchQuery = ""
                discoverState.clearEditing()
            }
        }
    }

    LaunchedEffect(tab) {
        if (tab != 0 && (feedSearchExpanded || feedSearchQuery.isNotEmpty() ||
                panelAlpha.value > 0f)
        ) {
            closeFeedSearchImmediately()
        }
    }

    BindSearchSuggestionsEffect(
        state = discoverState,
        query = feedSearchQuery,
        active = tab == 0 && feedSearchExpanded,
        submitted = homeResultsState.submitted,
    )

    val feedPanelCovering = tab == 0 && panelMounted

    // 右下展开钮的状态：默认展开后常驻、再点展开钮收回；设置里开了自动关闭则点任意地方收回。
    var fabExpanded by remember { mutableStateOf(false) }

    // 滑动隐藏顶/底栏（设置开关）：嵌套滚动只观察不消费——向下滚过阈值收起两栏，
    // 向上滚一点就恢复；切标签页、搜索面板盖住时强制恢复显示。
    var barsHidden by remember { mutableStateOf(false) }
    val panelCoveringNow = rememberUpdatedState(feedPanelCovering)
    LaunchedEffect(App.settings.hideBarsOnScroll, feedPanelCovering, tab) {
        if (!App.settings.hideBarsOnScroll || feedPanelCovering) barsHidden = false
    }
    LaunchedEffect(tab) { barsHidden = false }
    val hideBarsConnection = remember {
        object : NestedScrollConnection {
            var downAcc = 0f
            var upAcc = 0f
            override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (!App.settings.hideBarsOnScroll || panelCoveringNow.value) return Offset.Zero
                val y = available.y
                if (y < 0f) {
                    downAcc += -y; upAcc = 0f
                } else if (y > 0f) {
                    upAcc += y; downAcc = 0f
                }
                if (downAcc > 160f) { barsHidden = true; downAcc = 0f }
                if (upAcc > 60f) { barsHidden = false; upAcc = 0f }
                return Offset.Zero
            }
        }
    }
    val barsHideFraction = animateFloatAsState(
        targetValue = if (barsHidden && App.settings.hideBarsOnScroll) 1f else 0f,
        animationSpec = tween(240),
        label = "barsHideFraction",
    )


    Box(Modifier.fillMaxSize().nestedScroll(hideBarsConnection)) {
        // The bar floats over the page, so the line it ends at is reported from the bar itself and
        // read by anything that has to be placed clear of it.
        CompositionLocalProvider(LocalTopBarBounds provides topBarBounds) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdropIf(blur, backdrop)
                    .background(pageContainerColor())
                    .then(if (feedPanelCovering) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                CompositionLocalProvider(LocalBackdrop provides if (blur) backdrop else null) {
                    // The bottom bar only switches on tap, so the page carries the transition itself:
                    // both tabs stay composed through the same state holder and cross-fade.
                    Crossfade(
                        targetState = tab,
                        animationSpec = tween(MainTabFadeMillis),
                        modifier = Modifier.fillMaxSize(),
                        label = "mainTabPage",
                    ) { page ->
                        holder.SaveableStateProvider(page) {
                            when (page) {
                                0 -> TimelineScreen(
                                    nav = nav,
                                    pagerState = feedPager,
                                    stores = feedStores,
                                    emptyTexts = feedEmptyTexts,
                                    followStore = followStore,
                                    recommendStore = recommendStore,
                                    specialStore = filterStores.getValue(FeedFilter.Special),
                                    topInset = feedPageTopInset,
                                    onScrollOffset = { feedCollapse = it },
                                    slotKeys = feedSlotKeys,
                                    scrollHolder = timelineScrollHolder,
                                    hotPages = feedHotPages,
                                    onHotSearchKeyword = { keyword ->
                                        // 热搜页点词 = 在首页搜索面板里搜这个词，返回即回热搜页。
                                        feedSearchExpanded = true
                                        submitHomeSearch(keyword)
                                    },
                                )
                                1 -> if (guest) {
                                    LocalMessagesPlaceholder(nav, pageTopInset)
                                } else {
                                    MessagesScreen(nav, pageTopInset, state = messagesState)
                                }
                                else -> ProfileHubContent(
                                    nav = nav,
                                    guest = guest,
                                    modifier = Modifier.fillMaxSize(),
                                    topInset = pageTopInset,
                                    extraBottom = FloatingBarContentGap,
                                    listState = profileListState,
                                    state = profileTabState,
                                    onActiveCover = { profileActiveCover = it },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Layer 2: 顶栏模糊材质层 + 首页分段标签行（在搜索面板之下，展开时会被面板盖住）。
        // （COUI 的 LocalEdgeToEdge 在 Miuix 无对应，Miuix 组件各自处理窗口插图，已去掉。）
        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            PinnedBarDensity {
                MainBarUnderlay(
                    tab = tab,
                    feedPager = feedPager,
                    feedTabs = feedTabs,
                    feedSlotKeys = feedSlotKeys,
                    guest = guest,
                    centerTab = centerTab,
                    centerOptions = centerOptions,
                    collapsed = collapsed,
                    titleHeightPx = barTitleHeightPx,
                    timelineScrollHolder = timelineScrollHolder,
                    messagesState = messagesState,
                    profileListState = profileListState,
                    searchExpanded = feedSearchExpanded,
                    panelCovering = feedPanelCovering,
                    bounds = topBarBounds,
                    onMenuOpened = loadFeedGroups,
                    onScrollToTop = { if (!feedPanelCovering) App.requestFeedTop(scrollToTop = true) },
                    onCenterSelected = { selection ->
                        App.settings.applyFeedCenterTab(selection)
                        // 只补一次「还没拉到过 / 上次失败」的流：已经加载好的分组和它的续看位置
                        // 留给 TimelinePage，不要在切回来的时候冲掉。
                        val target = storeFor(selection)
                        if (target.error != null) scope.launch { target.refresh() }
                        // 中间栏的页号随启用的栏数变（五格全开时是 2），按 spec 现算，不写死。
                        val targetPage = feedTabs.indexOfFirst { it.isCenter }.coerceAtLeast(0)
                        scope.launch { feedPager.animateScrollToPage(targetPage) }
                    },
                    onPickNearbyCity = { showNearbyCityPicker = true },
                    onOpenSuperTopic = { nav.push(Route.Topic()) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .offset { IntOffset(0, -(200.dp * barsHideFraction.value).roundToPx()) }
                        .graphicsLayer { alpha = 1f - barsHideFraction.value },
                )
            }
        }

        // Layer 3: 悬浮底部导航栏（在面板之下，展开时被面板盖住）。
        val activeUid = App.repo.session?.uid.orEmpty()
        var profileAvatarUrl by remember(activeUid, guest) {
            mutableStateOf(
                if (guest || activeUid.isBlank()) null
                else App.store.accounts().firstOrNull { it.uid == activeUid }?.avatar?.takeIf { it.isNotBlank() },
            )
        }
        LaunchedEffect(activeUid, guest) {
            if (guest || activeUid.isBlank() || !profileAvatarUrl.isNullOrBlank()) {
                return@LaunchedEffect
            }
            val fetched = runCatching { App.repo.profile(activeUid) }.getOrNull() ?: return@LaunchedEffect
            val avatar = fetched.avatar.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
            profileAvatarUrl = avatar
            App.store.accounts().firstOrNull { it.uid == activeUid }?.let { existing ->
                App.store.saveAccount(
                    existing.copy(
                        name = fetched.name.ifBlank { existing.name },
                        avatar = avatar,
                    ),
                )
            }
        }
        LaunchedEffect(activeUid, guest) {
            if (guest || activeUid.isBlank()) return@LaunchedEffect
            App.profileUpdatedEvents.collect { updated ->
                if (updated.id == activeUid && updated.avatar.isNotBlank()) {
                    profileAvatarUrl = updated.avatar
                }
            }
        }
        // （COUI 的 LocalPlatformBottomBarMaterialEnabled 是其自家玻璃引擎开关，Miuix 无对应，已去掉；
        // Miuix FloatingNavigationBar 用自己的材质采样同一块页面 backdrop。）
        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            PinnedBarDensity {
                FloatingBottomBar(
                    tab = tab,
                    guest = guest,
                    onSearch = {
                        if (!feedPanelCovering) {
                            nav.setTab(0)
                            openFeedSearch()
                        }
                    },
                    onTab = { target ->
                        if (!feedPanelCovering) {
                            // 已在「微博」主页时再点主页键 = 回顶刷新（与悬浮刷新钮同一通道）。
                            if (target == 0 && nav.mainTab == 0) {
                                App.requestFeedTop(scrollToTop = true)
                            } else {
                                nav.setTab(target)
                            }
                        }
                    },
                    fabExpanded = fabExpanded,
                    onExpandToggle = { if (!feedPanelCovering) fabExpanded = !fabExpanded },
                    onExpandLongPress = {
                        if (!feedPanelCovering) {
                            val next = !App.settings.expandAutoClose
                            App.settings.applyExpandAutoClose(next)
                            App.toast(if (next) "展开按钮自动关闭已开启" else "展开按钮自动关闭已关闭")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = App.settings.barOffsetX.dp, y = App.settings.barOffsetY.dp)
                        .offset { IntOffset(0, (140.dp * barsHideFraction.value).roundToPx()) }
                        .graphicsLayer { alpha = 1f - barsHideFraction.value }
                        .then(if (feedPanelCovering) Modifier.clearAndSetSemantics {} else Modifier),
                )
            }
        }

        // 右下悬浮竖列：底栏展开钮点开后出现（自上而下 刷新 / 发布 / 我的），
        // 盖在页面之上、搜索面板之下；开了自动关闭时先铺一层透明遮罩，接住任意点击收起。
        if (!feedPanelCovering) {
            CompositionLocalProvider(
                LocalBackdrop provides if (blur) backdrop else null,
            ) {
              PinnedBarDensity {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .fillMaxSize()
                        // 三钮跟随底栏一起隐藏/恢复：只是视觉上随栏位移淡出，
                        // 展开状态本身保留，两栏恢复时三钮仍是展开的。
                        .offset { IntOffset(0, (140.dp * barsHideFraction.value).roundToPx()) }
                        .graphicsLayer { alpha = 1f - barsHideFraction.value },
                ) {
                  BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (App.settings.expandAutoClose) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { fabExpanded = false },
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = barSideMarginDp(maxWidth.value, App.settings.barScale).dp -
                                    App.settings.barOffsetX.dp,
                                // 与下方展开钮的间距恒为 12dp：展开钮上沿 = 缩放后的钮高
                                // 52×大小比例 + 底边距 8，再加 12 间距；底栏 offset 向下移时
                                // 竖列要反向补偿，故减 offsetY。
                                bottom = 20.dp + 52.dp * App.settings.barScale -
                                    App.settings.barOffsetY.dp +
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding(),
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ScaledBarDensity(App.settings.barScale) {
                            RefreshCircleButton(
                                onRefresh = {
                                    if (tab != 0) nav.setTab(0)
                                    App.requestFeedTop(scrollToTop = true)
                                },
                            )
                        }
                        ScaledBarDensity(App.settings.barScale) {
                            GlassCircleButton(
                                icon = MiboIcons.Feather,
                                contentDescription = "发布",
                                onClick = {
                                    if (guest) {
                                        App.toast("纯本地模式不能发微博，登录后可用")
                                    } else {
                                        nav.push(Route.Publish())
                                    }
                                },
                                modifier = Modifier.testTag("publish_fab"),
                            )
                        }
                        ScaledBarDensity(App.settings.barScale) {
                        ProfileCircleButton(
                            selected = tab == 2,
                            onClick = { nav.setTab(2) },
                            avatarUrl = if (!guest) profileAvatarUrl else null,
                            overflowMenu = {
                                ProfileQuickSwitchMenu(
                                        guest = guest,
                                        onSwitchAccount = { account ->
                                            App.repo.switchAccount(account)
                                            nav.reset(Route.Main)
                                        },
                                        onAddAccount = { nav.push(Route.SmsLogin(addAccount = true)) },
                                        onAccountRelated = { nav.push(Route.AccountRelated) },
                                    )
                            },
                            glassActive = blurEffectsEnabled() && LocalBackdrop.current != null,
                            backdrop = LocalBackdrop.current,
                            isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f,
                            modifier = Modifier.testTag("bottom_tab_profile"),
                        )
                        }
                    }
                  }
                }
              }
            }
        }

        // Layer 4: 首页搜索向下展开的整屏面板（从屏幕顶 0 铺满到屏幕底部，位于顶栏渐变材质之下、底栏之上）。
        if (tab == 0 && panelMounted) {
            HomeSearchOverlay(
                topBarHeight = topBarHeight,
                blur = blur,
                searchBackdrop = searchBackdrop,
                searchExpanded = feedSearchExpanded,
                panelAlpha = { panelAlpha.value },
                searchQuery = feedSearchQuery,
                showingResults = searchShowingResults,
                discoverState = discoverState,
                resultsState = homeResultsState,
                transitionState = searchTransition,
                listState = searchListState,
                nav = nav,
                scope = scope,
                onSelectKeyword = submitHomeSearch,
                onRetryHot = { scope.launch { discoverState.loadHot(force = true) } },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Layer 5: 标题行与展开后的搜索栏（在最顶层，确保搜索胶囊和“取消”按钮不被面板遮挡）。
        CompositionLocalProvider(
            LocalBackdrop provides if (blur) {
                if (tab == 0 && panelMounted) searchBackdrop else backdrop
            } else {
                null
            },
        ) {
            PinnedBarDensity {
                MainTitleRow(
                    tab = tab,
                    nav = nav,
                    guest = guest,
                    meTitleColor = meTitleColor,
                    collapsed = collapsed,
                    titleHeightPx = barTitleHeightPx,
                    searchQuery = feedSearchQuery,
                    onSearchQueryChange = { next ->
                        if (next != feedSearchQuery) discoverState.clearEditing()
                        feedSearchQuery = next
                        if (next.isBlank()) {
                            homeResultsState.clearResults()
                            scope.launch { discoverState.ensureHotLoaded() }
                        }
                    },
                    searchExpanded = feedSearchExpanded,
                    onSearchOpen = openFeedSearch,
                    onSearchCancel = cancelFeedSearch,
                    onSearchSubmit = submitHomeSearch,
                    submitNonce = searchSubmitNonce,
                    filtersVisible = searchTransition.filtersVisible,
                    resultsState = homeResultsState,
                    searchListState = searchListState,
                    panelAlpha = { panelAlpha.value },
                    onFilterMenuOpen = { searchFilterMenuOpen = true },
                    onFeedTitleClick = { if (!feedPanelCovering) App.requestFeedTop(scrollToTop = true) },
                    onEditProfile = { editProfileOpen = true },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                )
                // 筛选行左侧四宫格圆钮的菜单：菜单渲染在根 Scaffold，锚点按筛选行的行高与
                // 间距落在那颗圆钮上，栏的几何一改这里跟着走，不用两边各写一份坐标。
                if (searchShowingResults) {
                    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = statusBarTop + FeedTitleRowHeight + SearchChipRowGap),
                    ) {
                        SearchFilterMenu(
                            state = homeResultsState,
                            expanded = searchFilterMenuOpen,
                            onExpandedChange = { searchFilterMenuOpen = it },
                            horizontalPadding = searchHorizontalPadding(),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = searchHorizontalPadding())
                                .size(SearchFilterChipHeight),
                        )
                    }
                }
            }
        }
    }

    val inHomeSearchResults =
        nav.stack.size == 1 && tab == 0 && feedSearchExpanded && homeResultsState.submitted.isNotEmpty()
    // 搜索面板开着时按返回 = 关面板回主页，而不是退出应用（结果页、发现页都一样）。
    if (nav.stack.size == 1 && tab == 0 && feedSearchExpanded) {
        BackHandler { cancelFeedSearch() }
    }
    key(feedSearchExpanded, inHomeSearchResults) { MiboPredictiveBackHandler(nav) }

    if (showNearbyCityPicker) {
        NearbyCityPickerDialog(
            selected = if (App.settings.nearbyCustom) App.settings.nearbyPlaceName else "",
            onDismiss = { showNearbyCityPicker = false },
            onPick = { place ->
                App.settings.applyNearbyPlace(place.name, place.lat, place.lon)
                showNearbyCityPicker = false
                // 选完城市直接切到附近流看那座城（流的刷新由位置变更的副作用负责）。
                centerOptions.firstOrNull { it.item == FeedTabItem.Nearby }?.let { selection ->
                    App.settings.applyFeedCenterTab(selection)
                    val targetPage = feedTabs.indexOfFirst { it.isCenter }.coerceAtLeast(0)
                    scope.launch { feedPager.animateScrollToPage(targetPage) }
                }
            },
        )
    }

    if (editProfileOpen) {
        ProfileEditDialog(
            user = profileTabState.user,
            onOpenWeb = { nav.push(Route.Web(WEIBO_PROFILE_EDIT_URL, "编辑资料")) },
            onDismiss = { editProfileOpen = false },
        )
    }
}

/**
 * The "原创微博" filter keeps only posts that are not reposts. Pages with nothing but reposts are
 * skipped so the first screen is not an empty state with more pages behind it.
 */
private suspend fun originalFeed(cursor: String?, force: Boolean): FeedPage {
    var next = cursor
    var first = true
    repeat(5) {
        val page = App.repo.friends(next, force && first)
        val originals = page.items.filter { it.retweeted == null }
        if (originals.isNotEmpty()) return FeedPage(originals, page.sinceId)
        if (page.items.isEmpty() || page.sinceId.isNullOrBlank()) return FeedPage(emptyList(), null)
        next = page.sinceId
        first = false
    }
    return FeedPage(emptyList(), next)
}

private val FloatingBarContentGap = 80.dp

/** Cross-fade length for a bottom-bar tab switch. */
private const val MainTabFadeMillis = 220

@Composable
private fun MainBarUnderlay(
    tab: Int,
    feedPager: PagerState,
    feedTabs: List<FeedTabSpec>,
    feedSlotKeys: List<String>,
    guest: Boolean,
    centerTab: FeedTabSelection,
    centerOptions: List<FeedTabSelection>,
    collapsed: Float,
    titleHeightPx: Float,
    timelineScrollHolder: TimelineScrollHolder,
    messagesState: MessagesTabState,
    profileListState: LazyListState,
    searchExpanded: Boolean,
    panelCovering: Boolean,
    bounds: TopBarBounds,
    onMenuOpened: () -> Unit,
    onCenterSelected: (FeedTabSelection) -> Unit,
    onPickNearbyCity: () -> Unit,
    onOpenSuperTopic: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pageBarHeight = statusBarTop + CompactTitleGap
    val feedHasTabs = feedTabs.size > 1
    val feedBarHeight = if (feedHasTabs) {
        pageBarHeight + TopAppBarDefaults.SmallTopAppBarCenterHeight
    } else {
        pageBarHeight
    }
    val feedWeight = animateFloatAsState(
        targetValue = if (tab == 0) 1f else 0f,
        animationSpec = tween(MainTabFadeMillis),
        label = "mainBarFeedWeight",
    )
    val messagesWeight = animateFloatAsState(
        targetValue = if (tab == 1) 1f else 0f,
        animationSpec = tween(MainTabFadeMillis),
        label = "mainBarMessagesWeight",
    )
    val meWeight = animateFloatAsState(
        targetValue = if (tab == 2) 1f else 0f,
        animationSpec = tween(MainTabFadeMillis),
        label = "mainBarMeWeight",
    )
    val feedScrollAlpha = remember(feedPager, feedSlotKeys, timelineScrollHolder, density, feedHasTabs) {
        {
            val slotKey = feedSlotKeys.getOrElse(feedPager.currentPage) { feedPager.currentPage.toString() }
            val listState = timelineScrollHolder.listStateFor(slotKey)
            if (feedHasTabs) {
                twoRowListScrollAlpha(listState, density, contentGap = 16.dp)
            } else {
                listScrollAlpha(listState, density)
            }
        }
    }
    val messagesScrollAlpha = remember(messagesState, guest, density) {
        {
            if (guest) 0f else listScrollAlpha(messagesState.listState, density)
        }
    }
    val profileScrollAlpha = remember(profileListState, density) {
        { listScrollAlpha(profileListState, density) }
    }
    val materialShift = { (collapsed * titleHeightPx * feedWeight.value).roundToInt() }
    val materialHeight = animateDpAsState(
        targetValue = if (tab == 0) feedBarHeight else pageBarHeight,
        animationSpec = tween(MainTabFadeMillis),
        label = "mainBarMaterialHeight",
    )
    var filterMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(searchExpanded, tab) {
        if (searchExpanded || tab != 0) filterMenuExpanded = false
    }
    // 打开菜单时补一次分组列表：设置里改过分组、或者在网页版新加了分组，这里跟着更新。
    LaunchedEffect(filterMenuExpanded) {
        if (filterMenuExpanded) onMenuOpened()
    }

    Box(modifier) {
        AppBarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset { IntOffset(0, -materialShift()) }
                .reportTopBarBottom(bounds),
            containerColor = pageContainerColor(),
            cornerRadius = 24.dp,
            transparent = true,
            scrollFraction = {
                (feedWeight.value * feedScrollAlpha() +
                    messagesWeight.value * messagesScrollAlpha() +
                    meWeight.value * profileScrollAlpha()).coerceIn(0f, 1f)
            },
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val height = materialHeight.value.roundToPx()
                        val placeable = measurable.measure(
                            Constraints.fixed(constraints.maxWidth, height)
                        )
                        layout(placeable.width, height) { placeable.place(0, 0) }
                    }
            )
        }
        AnimatedVisibility(
            visible = tab == 0 && feedHasTabs && !searchExpanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTop)
                    .then(if (panelCovering) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                Spacer(Modifier.height(CompactTitleGap))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TopAppBarDefaults.SmallTopAppBarCenterHeight)
                        .graphicsLayer { translationY = -collapsed * titleHeightPx },
                ) {
                    FeedTabs(
                        pager = feedPager,
                        feedTabs = feedTabs,
                        filterMenu = !guest,
                        centerTab = centerTab,
                        centerOptions = centerOptions,
                        filterMenuExpanded = filterMenuExpanded,
                        onFilterMenuDismiss = { filterMenuExpanded = false },
                        onCenterSelected = {
                            filterMenuExpanded = false
                            onCenterSelected(it)
                        },
                        onPickNearbyCity = onPickNearbyCity,
                        onOpenSuperTopic = onOpenSuperTopic,
                        onSelect = { page -> scope.launch { feedPager.animateScrollToPage(page) } },
                        onFilterClick = { filterMenuExpanded = true },
                        onScrollToTop = onScrollToTop,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

private const val SearchPanelFadeMillis = 220

/**
 * 搜索「发现页 ↔ 结果页」切换的过渡状态（原 COUI SearchResultsTransitionState 的本地等价物）。
 * [shownResults] 是当前实际在画的内容：切换时先把旧内容淡出、再翻转、再淡入，
 * 避免提交瞬间发现页在淡出前往下跳一截。
 */
@Stable
private class SearchResultsTransitionState {
    var shownResults by mutableStateOf(false)
    var resultsVisible by mutableStateOf(false)
    var filtersVisible by mutableStateOf(false)
    var loading by mutableStateOf(false)
    val contentAlpha = Animatable(1f)
}

@Composable
private fun rememberSearchResultsTransitionState(
    showingResults: Boolean,
    loading: Boolean,
    queryKey: String,
): SearchResultsTransitionState {
    val state = remember { SearchResultsTransitionState() }
    SideEffect {
        state.filtersVisible = showingResults
        state.loading = loading
    }
    LaunchedEffect(showingResults, queryKey) {
        if (state.shownResults == showingResults) {
            // 关键词变了但仍在结果态：轻微淡出再淡入，近似原来的「换关键词整段重新上浮」。
            if (showingResults) {
                state.contentAlpha.animateTo(0f, tween(120))
                state.contentAlpha.animateTo(1f, tween(180))
            }
            return@LaunchedEffect
        }
        state.contentAlpha.animateTo(0f, tween(120))
        state.shownResults = showingResults
        state.resultsVisible = showingResults
        state.contentAlpha.animateTo(1f, tween(180))
    }
    return state
}

/**
 * 承接 [SearchResultsTransitionState] 的容器（原 COUI SearchResultContainer 的本地等价物）：
 * 内容按状态的透明度绘制；加载中且已在结果态时，在列表顶部留白处叠加加载指示。
 */
@Composable
private fun SearchResultContainer(
    state: SearchResultsTransitionState,
    modifier: Modifier = Modifier,
    loadingTopPadding: Dp = 0.dp,
    loadingContent: @Composable () -> Unit = {},
    content: @Composable (shownResults: Boolean, resultsVisible: Boolean) -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = state.contentAlpha.value },
        ) {
            content(state.shownResults, state.resultsVisible)
        }
        if (state.loading && state.shownResults) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = loadingTopPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                loadingContent()
            }
        }
    }
}

@Composable
private fun HomeSearchOverlay(
    topBarHeight: Dp,
    blur: Boolean,
    searchBackdrop: LayerBackdrop,
    searchExpanded: Boolean,
    panelAlpha: () -> Float,
    searchQuery: String,
    showingResults: Boolean,
    discoverState: SearchDiscoverState,
    resultsState: SearchResultsState,
    transitionState: SearchResultsTransitionState,
    listState: LazyListState,
    nav: NavState,
    scope: CoroutineScope,
    onSelectKeyword: (String) -> Unit,
    onRetryHot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = pageContainerColor()
    // 原 COUI 的 BindScrollToTop（点状态栏把该列表回顶）Miuix 无对应，已去掉，列表本体保留。
    val horizontalPadding = searchHorizontalPadding()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(listState, resultsState.submitted, resultsState.tab, resultsState.stateFor(resultsState.tab).count) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (resultsState.submitted.isBlank()) return@collect
                val tabState = resultsState.stateFor(resultsState.tab)
                if (!tabState.started || tabState.end || tabState.count == 0) return@collect
                if (last >= tabState.count - 2) resultsState.load(resultsState.tab, reset = false, force = false)
            }
    }

    val suggesting = searchQuery.isNotBlank() && !showingResults

    LaunchedEffect(showingResults) {
        if (showingResults) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(transitionState.shownResults, resultsState.submitted, suggesting, resultsState.tab, discoverState.hotBoard) {
        // 淡出过程中旧内容还在画，先把滚动位置钉住；换完内容再回到顶部。
        if (transitionState.shownResults != showingResults) return@LaunchedEffect
        listState.scrollToItem(0)
    }

    val activeSelectKeyword: (String) -> Unit = { keyword ->
        if (searchExpanded) onSelectKeyword(keyword)
    }

    Box(modifier) {
        val history = App.settings.searchHistory
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = panelAlpha().coerceIn(0f, 1f)
                }
                .layerBackdropIf(blur, searchBackdrop)
                .background(surfaceColor)
                .then(
                    if (searchExpanded) {
                        Modifier
                            .dismissHistoryEditingOnTap(discoverState)
                            .testTag("home_search_panel")
                    } else {
                        Modifier.pointerInput(Unit) {}
                    },
                ),
        ) {
            CompositionLocalProvider(LocalBackdrop provides if (blur) searchBackdrop else null) {
                // 结果态列表顶沿与加载球顶沿要给第二行筛选胶囊让出位置；
                // 列表内容读的是 shownResults，避免提交瞬间发现页在淡出前往下跳一截。
                val baseTopPadding = glassBarContentTop(topBarHeight, 8.dp)
                val resultsBarTopPadding = baseTopPadding + pinnedRowsHeight(
                    listOf(
                        SearchChipRowGap,
                        SearchFilterChipHeight,
                        SearchSecondRowBottom,
                    ),
                )
                val listTopPadding = if (transitionState.shownResults) resultsBarTopPadding else baseTopPadding
                SearchResultContainer(
                    state = transitionState,
                    modifier = Modifier.fillMaxSize(),
                    loadingTopPadding = resultsBarTopPadding,
                    loadingContent = { LoadingState() },
                ) { shownResults, resultsVisible ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().scrollEndHaptic(),
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            top = listTopPadding,
                            end = 0.dp,
                            bottom = 16.dp + navBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!shownResults) {
                            searchDiscoverItems(
                                state = discoverState,
                                suggesting = suggesting,
                                history = history.toList(),
                                onSelectKeyword = activeSelectKeyword,
                                onRetryHot = onRetryHot,
                                onSelectUser = { user -> nav.push(Route.Profile(user.id)) },
                            )
                        } else if (resultsVisible) {
                            searchResultsItems(
                                state = resultsState,
                                nav = nav,
                                horizontalPadding = horizontalPadding,
                                scope = scope,
                                onSelectKeyword = activeSelectKeyword,
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun MainTitleRow(
    tab: Int,
    nav: NavState,
    guest: Boolean,
    meTitleColor: Color,
    collapsed: Float,
    titleHeightPx: Float,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchOpen: () -> Unit,
    onSearchCancel: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    submitNonce: Int,
    filtersVisible: Boolean,
    resultsState: SearchResultsState,
    searchListState: LazyListState,
    panelAlpha: () -> Float,
    onFilterMenuOpen: () -> Unit,
    onFeedTitleClick: () -> Unit,
    onEditProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = tab,
        animationSpec = tween(MainTabFadeMillis),
        modifier = modifier,
        label = "mainTitleRow",
    ) { page ->
        when (page) {
            0 -> FeedTitleRow(
                collapsed = collapsed,
                titleHeightPx = titleHeightPx,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                searchExpanded = searchExpanded,
                onSearchOpen = onSearchOpen,
                onSearchCancel = onSearchCancel,
                onSearchSubmit = onSearchSubmit,
                submitNonce = submitNonce,
                filtersVisible = filtersVisible,
                resultsState = resultsState,
                searchListState = searchListState,
                panelAlpha = panelAlpha,
                onFilterMenuOpen = onFilterMenuOpen,
                onTitleClick = onFeedTitleClick,
            )

            // 「通知」入口在消息页顶部，底栏「消息」Tab 单击直接切页。
            1 -> PageTitleBar()

            // 「我」页内已有设置分组入口与资料页菜单（设置 / 编辑资料），标题栏右上角不再重复放。
            else -> PageTitleBar()
        }
    }
}

/**
 * Single-row page header for the tabs that have no segment row. It repeats the feed's title row
 * exactly — same height, title size and action slot — so the search, 通知 and 设置 buttons all land
 * on the same pixel across the three tabs.
 */
@Composable
private fun PageTitleBar(
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        Box(Modifier.fillMaxWidth().height(FeedTitleRowHeight)) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TopAppBarDefaults.ActionIconPadding),
                horizontalArrangement = Arrangement.spacedBy(
                    // Miuix 的 TopAppBarDefaults 没有 ActionMenuItemSpacing，用 8dp 近似 COUI 的取值。
                    8.dp,
                    Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

private const val CirclePage = 0
private const val FollowPage = 1
private const val RecommendPage = 2

/** Height of the feed header's title row; the list folds exactly this much away when it scrolls. */
internal val FeedTitleRowHeight = 44.dp

/** 标题行去掉后顶栏只剩分段胶囊：页面顶部留白从一整行收紧到这一点（与底栏的悬浮间距观感一致）。 */
internal val CompactTitleGap = 8.dp

/**
 * 搜索结果态下搜索框与筛选胶囊行之间的间距：便签两行顶栏的实测视觉距离是 17dp，
 * 44dp 的标题行里搜索框下方留 2dp，再空 15dp 正好对齐。
 */
internal val SearchChipRowGap = 15.dp

@Composable
private fun FeedTitleRow(
    collapsed: Float,
    titleHeightPx: Float,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchOpen: () -> Unit,
    onSearchCancel: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    submitNonce: Int,
    filtersVisible: Boolean,
    resultsState: SearchResultsState,
    searchListState: LazyListState,
    panelAlpha: () -> Float,
    onFilterMenuOpen: () -> Unit,
    onTitleClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val blur = blurEffectsEnabled()
    val barDensity = LocalDensity.current
    val titleFocusManager = LocalFocusManager.current
    val titleKeyboard = LocalSoftwareKeyboardController.current

    // 带结果进入搜索时（如在热搜页点热搜条），Miuix InputField 挂载展开会自动请求焦点、
    // 把输入法弹起来；提交后稍等它挂载完，再清一次焦点收起键盘。用户手动点搜索框输入时
    // submitNonce 不变，不受影响。
    LaunchedEffect(submitNonce) {
        if (submitNonce > 0) {
            delay(50)
            titleKeyboard?.hide()
            titleFocusManager.clearFocus(force = true)
        }
    }

    // 原 COUI 的 SearchFilterTopAppBar（两行顶栏 + 材质 + 胶囊采样一体）Miuix 没有对应组件，
    // 这里用 Blur.kt 的 AppBarSurface 承担材质与滚动提白，两行布局在本函数内自己排：
    // 第一行是标题/搜索框，第二行（仅结果态）是筛选胶囊行。
    // 搜索框刻意不用 Miuix SearchBar 容器——它内部自带 NavigationBackHandler，
    // 会和应用自己的预测返回手势打架；改用它的 InputField 放在普通行里。
    AppBarSurface(
        containerColor = pageContainerColor(),
        transparent = true,
        scrollFraction = {
            val base = panelAlpha()
            if (base <= 0f) {
                0f
            } else {
                val scroll = if (filtersVisible) {
                    twoRowListScrollAlpha(searchListState, barDensity)
                } else {
                    listScrollAlpha(searchListState, barDensity)
                }
                base * scroll
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Spacer(
                Modifier.height(
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                ),
            )
            Box(Modifier.fillMaxWidth().height(FeedTitleRowHeight)) {
                if (searchExpanded) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InputField(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onSearch = onSearchSubmit,
                            expanded = true,
                            onExpandedChange = { isExpanded ->
                                if (!isExpanded) onSearchCancel()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    val enter = event.key == Key.Enter ||
                                        event.key == Key.NumPadEnter ||
                                        event.key == Key.Search
                                    if (event.type == KeyEventType.KeyDown && enter) {
                                        onSearchSubmit(searchQuery)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            label = "搜索微博 / 图片 / 视频 / 用户 / 超话",
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    val clearTint = scheme.onSurface.copy(alpha = 0.28f)
                                    Canvas(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .semantics { contentDescription = "清除" }
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                role = Role.Button,
                                                onClick = { onSearchQueryChange("") },
                                            ),
                                    ) {
                                        val unit = size.width / 36f
                                        drawCircle(clearTint, radius = 9f * unit)
                                        val path = Path().apply {
                                            moveTo(18f, 16.868f)
                                            lineTo(15.031f, 13.898f)
                                            lineTo(13.899f, 15.03f)
                                            lineTo(16.869f, 18f)
                                            lineTo(13.898f, 20.97f)
                                            lineTo(15.03f, 22.101f)
                                            lineTo(18f, 19.131f)
                                            lineTo(20.97f, 22.101f)
                                            lineTo(22.102f, 20.969f)
                                            lineTo(19.132f, 18f)
                                            lineTo(22.101f, 15.03f)
                                            lineTo(20.969f, 13.899f)
                                            close()
                                        }
                                        withTransform({ scale(unit, unit, Offset.Zero) }) {
                                            drawPath(path, Color.White)
                                        }
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            text = "取消",
                            onClick = onSearchCancel,
                        )
                    }
                } else {
                    // 标题、回顶点击区与搜索钮都已去掉（搜索入口移到底栏）：未展开时这一行不渲染任何东西，
                    // 顶栏只剩 MainBarUnderlay 里的分段胶囊。
                }
            }
            if (filtersVisible) {
                Spacer(Modifier.height(SearchChipRowGap))
                SearchFilterChipsRow(
                    selectedIndex = resultsState.tab.ordinal,
                    onSelected = { index ->
                        SearchTab.entries.getOrNull(index)?.let(resultsState::selectTab)
                    },
                    onLeadingIconClick = onFilterMenuOpen,
                )
                Spacer(Modifier.height(SearchSecondRowBottom))
            }
        }
    }
}

/**
 * 搜索结果态的第二行：左侧四宫格圆钮（弹出分类/排序菜单）+ 可横滑的分类胶囊。
 * 原 COUI ChipTab 胶囊由顶栏组件内部绘制，Miuix 无对应，这里在 Shell 内自绘；
 * 分类数据消费 screen/SearchResults.kt 的 rememberSearchFilterTabs()
 * （元素类型为该文件定义的 SearchFilterTab，替代已删除的 COUI ChipTabData，label/icon 不变）。
 */
@Composable
private fun SearchFilterChipsRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onLeadingIconClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val tabs = rememberSearchFilterTabs()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = searchHorizontalPadding()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SearchFilterChipHeight)
                .clip(CircleShape)
                .background(scheme.surfaceContainer)
                .clickable(role = Role.Button, onClick = onLeadingIconClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                MiuixIcons.GridView,
                contentDescription = "筛选菜单",
                modifier = Modifier.size(20.dp),
                tint = scheme.onSurface,
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val chipBackground = if (selected) scheme.primary else scheme.surfaceContainer
                val chipContent = if (selected) scheme.onPrimary else scheme.onSurface
                Row(
                    Modifier
                        .height(SearchFilterChipHeight)
                        .clip(RoundedCornerShape(50))
                        .background(chipBackground)
                        .clickable(role = Role.Tab, onClick = { onSelected(index) })
                        .semantics { this.selected = selected }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = chipContent)
                    Text(
                        tab.label,
                        color = chipContent,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 首页分段选择器的自绘实现（替代 COUI TabRowWithContour）。
 *
 * Miuix 自带的 TabRowWithContour 只收纯文本标签，不支持自定义格内容、横向拖动与逐格 testTag，
 * 而中间格还要挂级联菜单锚点，所以这里自绘：等宽分段、选中胶囊跟随选中项与拖动、
 * 松手时按拖动距离提交选择；玻璃档走 miuix-blur 的 textureBlur，否则退回不透明容器色。
 * COUI 版的跟手高光（touchMotion）没有对应实现，已去掉。
 */
@Composable
private fun SegmentedTabRow(
    count: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onTap: (Int) -> Unit,
    onLongTap: (Int) -> Unit = {},
    segmentWidth: Dp,
    glass: Boolean,
    modifier: Modifier = Modifier,
    tabModifier: (Int) -> Modifier = { Modifier },
    tabContent: @Composable (index: Int, active: Boolean) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val backdrop = LocalBackdrop.current
    val density = LocalDensity.current
    val segmentWidthPx = with(density) { segmentWidth.toPx() }
    // 灵动滑动：位置走弹簧（带回弹），胶囊宽度按速度拉伸——动得越快拉得越长，
    // 停下时缩回一格宽，就是酷安底栏那种果冻感。位置以「格」为单位动画。
    val posAnim = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        posAnim.animateTo(
            selectedIndex.toFloat(),
            spring(dampingRatio = 0.7f, stiffness = 380f),
        )
    }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val maxOffset = segmentWidth * (count - 1).coerceAtLeast(0)
    val stretchSeg = (abs(posAnim.velocity) * 0.12f).coerceAtMost(0.6f)
    val indicatorWidth = segmentWidth * (1f + stretchSeg)
    val indicatorX = (segmentWidth * (posAnim.value + 0.5f) - indicatorWidth / 2 +
        with(density) { dragOffsetPx.toDp() })
        .coerceIn(0.dp, maxOffset + segmentWidth - indicatorWidth)
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(45.dp)
            .clip(shape)
            .then(
                if (glass && backdrop != null) {
                    // 与底栏同款材质：OS4 柔光玻璃（薄玻璃 + 描边 + 顶栏底栏透明度）。
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
                    Modifier.background(
                        scheme.surfaceContainer.copy(
                            alpha = App.settings.barGlassAlpha,
                        ),
                        shape,
                    )
                },
            )
            .padding(5.dp),
    ) {
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(shape)
                // 选中格衬底：带透明度的强调色填充 + 同色半透明轮廓（参考微信底栏选中态
                // 那种淡淡的彩色胶囊），配选中文字变强调色，一眼能分清选中的是哪格。
                .background(scheme.primary.copy(alpha = 0.15f))
                .border(1.5.dp, scheme.primary.copy(alpha = 0.55f), shape),
        )
        Row(
            Modifier.pointerInput(count, segmentWidthPx) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val basePx = selectedIndex * segmentWidthPx
                        val target = ((basePx + dragOffsetPx) / segmentWidthPx).roundToInt()
                            .coerceIn(0, count - 1)
                        dragOffsetPx = 0f
                        if (target != selectedIndex) onSelect(target)
                    },
                    onDragCancel = { dragOffsetPx = 0f },
                ) { _, dragAmount ->
                    val basePx = selectedIndex * segmentWidthPx
                    dragOffsetPx = (basePx + dragOffsetPx + dragAmount)
                        .coerceIn(0f, segmentWidthPx * (count - 1).coerceAtLeast(0)) - basePx
                }
            },
        ) {
            for (index in 0 until count) {
                Box(
                    Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .then(tabModifier(index))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onLongClick = { onLongTap(index) },
                            onClick = {
                                onSelect(index)
                                onTap(index)
                            },
                        )
                        .semantics { selected = index == selectedIndex },
                    contentAlignment = Alignment.Center,
                ) {
                    tabContent(index, index == selectedIndex)
                }
            }
        }
    }
}

/**
 * 首页顶栏的分段选择器。
 *
 * 这里只负责三件事：把页面尺寸固定成三格等宽、把中间那格接上内置流与关注分组的二级菜单、
 * 把点击与左右拖动都换算成翻页。外观与交互见 [SegmentedTabRow]。
 */
@Composable
private fun FeedTabs(
    pager: PagerState,
    feedTabs: List<FeedTabSpec>,
    filterMenu: Boolean,
    centerTab: FeedTabSelection,
    centerOptions: List<FeedTabSelection>,
    filterMenuExpanded: Boolean,
    onFilterMenuDismiss: () -> Unit,
    onCenterSelected: (FeedTabSelection) -> Unit,
    onPickNearbyCity: () -> Unit,
    onOpenSuperTopic: () -> Unit,
    onSelect: (Int) -> Unit,
    onFilterClick: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    val backdrop = LocalBackdrop.current
    // 透明模糊那档要 API 34+（原生 RuntimeShader）且用户没关模糊，否则退回不透明胶囊。
    // 这里的判断和库内一致，颜色也跟着换，免得玻璃档还挂着一层不透明底色。
    // 与底栏同款玻璃条件：OS4 柔光玻璃开着、有 backdrop、模糊没关（glassPanel 自行处理低版本降级）。
    val glass = backdrop != null && blurEffectsEnabled()
    val labels = feedTabs.map { it.tabLabel }
    // 「全部」玻璃菜单的锚点：中间格的窗口坐标，点开时交给全局玻璃菜单宿主。
    var centerAnchorBounds by remember { mutableStateOf(Rect.Zero) }
    // 左右两格已经占住的内容：顶栏一格一步可达，中间栏菜单里不再重复列出。
    val sideSelectionKeys = feedTabs.filterNot { it.isCenter }.map { spec ->
        when (spec) {
            is FeedTabSpec.Custom -> spec.selection.key
            is FeedTabSpec.Follow -> FeedTabItem.Follow.key
        }
    }.toSet()

    // 三格同宽，按「4 个字 + 箭头」预留：分组名是 2/3/4 个字时胶囊和格子都不动。
    // 用 sp 折算，系统字体放大时格子跟着变宽；先落到 px 再折算回去，容器宽度与
    // 「格宽 × 格数 + 两侧内边距」严格相等。内边距 5dp 与 Miuix TabRowWithContour 的固定值一致。
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val segmentWidthPx = with(density) { ((4 * 14).sp.toDp() + 2.dp + 10.dp + 22.dp).roundToPx() }
    val contourPadPx = with(density) { 5.dp.roundToPx() }
    // 五格全开时按原公式整排会超出屏幕：总宽封顶为屏宽减两侧各 32dp 留白，格宽等比
    // 收窄，放不下的文字走省略号。三格及以下永远命中原公式，外观与之前一致。
    val maxTotalWidthPx = with(density) { (configuration.screenWidthDp.dp - 64.dp).roundToPx() }
    val fittedSegmentWidthPx = if (feedTabs.isEmpty()) {
        segmentWidthPx
    } else {
        minOf(segmentWidthPx, (maxTotalWidthPx - contourPadPx * 2) / feedTabs.size)
    }
    val segmentWidth = with(density) { fittedSegmentWidthPx.toDp() }
    val containerWidth = with(density) { (fittedSegmentWidthPx * feedTabs.size + contourPadPx * 2).toDp() }

    // 点一下先把指示器挪过去、再让列表跟上；等 pager 停稳就把临时值交还给 pager。
    // 不跟 pager 的中间页走，长距离切换时指示器不会一格一格地过。
    var pendingPage by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pager.currentPage, pager.isScrollInProgress) {
        if (!pager.isScrollInProgress) pendingPage = null
    }
    val selectedIndex = (pendingPage ?: pager.targetPage).coerceIn(0, max(0, feedTabs.lastIndex))

    SegmentedTabRow(
        count = feedTabs.size,
        selectedIndex = selectedIndex,
        onSelect = { index ->
            if (filterMenu && feedTabs.getOrNull(index)?.isCenter == true) {
                // 中间那格（全部）单击直接展开全部分组菜单，切到哪个分组在菜单里选。
                onFilterClick()
            } else if (index == pager.currentPage) {
                // 点已选中的格子 = 回到顶部。
                onScrollToTop()
            } else {
                pendingPage = index
                onSelect(index)
            }
        },
        onTap = { },
        // 长按中间那格也展开菜单（与单击一致）。
        onLongTap = { index ->
            if (filterMenu && feedTabs.getOrNull(index)?.isCenter == true) onFilterClick()
        },
        segmentWidth = segmentWidth,
        glass = glass,
        modifier = modifier.width(containerWidth),
        tabModifier = { index -> Modifier.testTag(feedTabs[index].tag) },
        tabContent = { index, active ->
            val label = labels[index]
            val menu = filterMenu && feedTabs.getOrNull(index)?.isCenter == true
            // 选中格按强调色显示（文字与展开三角同色），再配指示器的强调色轮廓：
            // 参考微信底栏那种选中即变色的读法，一眼能分清选中的是哪格。
            val labelColor = if (active) scheme.primary else scheme.onSurface
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .onGloballyPositioned { coordinates ->
                if (menu) {
                    val pos = coordinates.positionInWindow()
                    centerAnchorBounds = Rect(
                        pos.x,
                        pos.y,
                        pos.x + coordinates.size.width.toFloat(),
                        pos.y + coordinates.size.height.toFloat(),
                    )
                }
            },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        // 箭头固定占一格，文字让出这一段：长分组名不会把箭头挤出格子。
                        modifier = Modifier.widthIn(max = segmentWidth - 12.dp - if (menu) 12.dp else 0.dp),
                    )
                    if (menu) {
                        Spacer(Modifier.width(2.dp))
                        // Hyper 风格展开标：实心小三角（宽 9dp、高 6dp），不用线条 chevron。
                        Canvas(Modifier.size(10.dp)) {
                            val triW = 9.dp.toPx()
                            val triH = 6.dp.toPx()
                            val left = (size.width - triW) / 2f
                            val top = (size.height - triH) / 2f
                            val path = Path().apply {
                                moveTo(left, top)
                                lineTo(left + triW, top)
                                lineTo(left + triW / 2f, top + triH)
                                close()
                            }
                            drawPath(path, labelColor)
                        }
                    }
                }
                if (menu) {
                    // 菜单本体由根节点的全局玻璃菜单宿主渲染（整块柔光玻璃），这里只做触发：
                    // 展开标记置真时把条目和锚点交出去，随即复位标记。
                    LaunchedEffect(filterMenuExpanded) {
                        if (filterMenuExpanded) {
                            val items = buildCenterMenuEntries(
                                centerOptions = centerOptions,
                                centerTab = centerTab,
                                onCenterSelected = onCenterSelected,
                                sideKeys = sideSelectionKeys,
                                onPickNearbyCity = onPickNearbyCity,
                                onOpenSuperTopic = onOpenSuperTopic,
                            ).flatMap { it.items }.map { it.toGlassMenuItem() }
                            App.showGlassMenu(centerAnchorBounds, items)
                            onFilterMenuDismiss()
                        }
                    }
                }
            }
        },
    )
}

/**
 * 构建首页中间栏下拉菜单分段：第一段为「全部」，第二段为其余内置流，
 * 第三段将关注分组收进二级子菜单。
 */
internal fun buildCenterMenuEntries(
    centerOptions: List<FeedTabSelection>,
    centerTab: FeedTabSelection,
    onCenterSelected: (FeedTabSelection) -> Unit,
    sideKeys: Set<String> = emptySet(),
    onPickNearbyCity: () -> Unit = {},
    onOpenSuperTopic: () -> Unit = {},
): List<DropdownEntry> {
    // 左右两格占住的内容从菜单里剔除（中间栏当前项除外，它被选中时仍要能看到勾）。
    val excluded = sideKeys - centerTab.key
    val builtinOptions = centerOptions.filterNot { it.isGroup || it.key in excluded }
    val groupOptions = centerOptions.filter { it.isGroup && it.key !in excluded }
    val currentGroupTitle = centerTab.group
        ?.takeIf { centerTab.isGroup }
        ?.title
        ?.takeIf { it.isNotBlank() }
    return buildList {
        builtinOptions.firstOrNull { it.item == FeedTabItem.Follow }?.let { follow ->
            add(
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = "关注",
                            selected = follow.key == centerTab.key,
                            onClick = { onCenterSelected(follow) },
                        )
                    )
                )
            )
        }
        val filters = builtinOptions
            .filterNot { it.item == FeedTabItem.Follow }
            .map { option ->
                if (option.item == FeedTabItem.Favorites && !App.repo.guest) {
                    // 「收藏」带二级菜单：本地收藏（默认）与账号云端收藏。
                    DropdownItem(
                        text = option.label,
                        selected = option.key == centerTab.key,
                        children = listOf(
                            DropdownItem(
                                text = "本地收藏",
                                summary = "存在本机的收藏",
                                selected = !App.settings.favoritesCloud,
                                onClick = {
                                    App.settings.applyFavoritesCloud(false)
                                    onCenterSelected(option)
                                },
                            ),
                            DropdownItem(
                                text = "账号收藏",
                                summary = "微博账号里的收藏",
                                selected = App.settings.favoritesCloud,
                                onClick = {
                                    App.settings.applyFavoritesCloud(true)
                                    onCenterSelected(option)
                                },
                            ),
                        ),
                    )
                } else if (option.item == FeedTabItem.Nearby) {
                    // 「附近」带二级菜单：默认读当前位置，也可固定看一座自定义城市。
                    DropdownItem(
                        text = option.label,
                        selected = option.key == centerTab.key,
                        children = listOf(
                            DropdownItem(
                                text = "当前位置",
                                summary = "读取手机定位",
                                selected = !App.settings.nearbyCustom,
                                onClick = {
                                    App.settings.applyNearbyDevice()
                                    onCenterSelected(option)
                                },
                            ),
                            DropdownItem(
                                text = "自定义城市",
                                summary = if (App.settings.nearbyCustom) {
                                    App.settings.nearbyPlaceName.ifBlank { "选一座城市" }
                                } else {
                                    "选一座城市"
                                },
                                selected = App.settings.nearbyCustom,
                                onClick = { onPickNearbyCity() },
                            ),
                        ),
                    )
                } else {
                    DropdownItem(
                        text = option.label,
                        selected = option.key == centerTab.key,
                        onClick = { onCenterSelected(option) },
                    )
                }
            }
        if (filters.isNotEmpty()) add(DropdownEntry(items = filters))
        if (groupOptions.isNotEmpty()) {
            add(
                DropdownEntry(
                    items = listOf(
                        DropdownItem(
                            text = FeedTabItem.Group.label,
                            selected = centerTab.isGroup,
                            summary = currentGroupTitle,
                            children = groupOptions.map { option ->
                                DropdownItem(
                                    text = option.label,
                                    selected = option.key == centerTab.key,
                                    onClick = { onCenterSelected(option) },
                                )
                            },
                        )
                    )
                )
            )
        }
        add(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "超话",
                        selected = false,
                        onClick = onOpenSuperTopic,
                    )
                )
            )
        )
    }
}

/** 级联菜单的条目模型转全局玻璃菜单条目（分组段落拍平，children 保留给宿主钻入）。 */
private fun DropdownItem.toGlassMenuItem(): App.GlassMenuItem = App.GlassMenuItem(
    text = text,
    summary = summary,
    selected = selected,
    children = children?.map { it.toGlassMenuItem() },
    onClick = onClick,
)

@Composable
fun FloatingBottomBar(
    tab: Int,
    guest: Boolean,
    onSearch: () -> Unit,
    onTab: (Int) -> Unit,

    fabExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    onExpandLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 「微博」单击切 tab；已在本页时再点 = 回顶刷新（MainShell 的 onTab 接线），长按发布已取消。
    // 「消息」Tab 单击直接切页：通知入口已移到消息页顶部，不再弹二选一菜单。
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // 底栏与屏幕底边的额外距离固定 8dp（在系统导航栏让位之上），不再提供调节。
            .padding(bottom = 8.dp)
    ) {
        // 原 COUI BottomFloatingAccessoryNavigationBar（中央胶囊 + 两侧独立圆钮）Miuix 无对应，
        // 改用 Miuix FloatingNavigationBar：左右配件收进同一条胶囊，两侧各留一个 48dp 的圆钮位，
        // 三个 Tab 用自绘的图标+标签项（Miuix 的 FloatingNavigationBarItem 只画图标，标签仅作描述）。
        // 底栏的系统栏让位仍由外层 Box 的 navigationBarsPadding 负责，故关掉组件自带的内边距处理。
        // OS4 柔光玻璃开着时：组件底色改透明，胶囊面改由 glassPanel 采样页面 backdrop 绘制
        // （组件链里调用方 modifier 位于 squircleBackground 之后、内容之前，正好盖住底色层）。
        val backdrop = LocalBackdrop.current
        val glassActive = blurEffectsEnabled() && backdrop != null
        val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
        // 大小缩放只套在胶囊与展开钮本体上，定位锚点仍按钉住密度；但内容变宽后
        // 固定边距排不下（276dp 内容 × 比例 + 12dp 间隔 + 两边边距要装进屏宽），
        // 所以放大时左右边距按 barSideMarginDp 让出空间，间隔恒 12dp、永不重叠。
        val barScale = App.settings.barScale
        val sideMargin = barSideMarginDp(maxWidth.value, barScale)
        ScaledBarDensity(barScale) {
        FloatingNavigationBar(
            horizontalOutSidePadding = (sideMargin / barScale).dp,
            modifier = if (glassActive) {
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
            },
            color = if (glassActive) {
                Color.Transparent
            } else {
                MiuixTheme.colorScheme.surfaceContainer.copy(
                    alpha = App.settings.barGlassAlpha,
                )
            },
            horizontalAlignment = Alignment.Start,
            defaultWindowInsetsPadding = false,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 位置按用户要求交换：左「消息」、中「微博」、右「我」。tab 值仍沿用
                // 0=微博 / 1=消息 / 2=我，只换视觉顺序，避免影响 nav.mainTab 的持久化语义。
                FloatingTabItem(
                    selected = tab == 1,
                    onClick = { onTab(1) },
                    icon = MiboIcons.MessagesOutline,
                    selectedIcon = MiboIcons.MessagesFilled,
                    label = "消息",
                    modifier = Modifier.testTag("bottom_tab_messages"),
                )
                FloatingTabItem(
                    selected = tab == 0,
                    onClick = { onTab(0) },
                    icon = MiboIcons.FeedOutline,
                    selectedIcon = MiboIcons.FeedFilled,
                    label = "微博",
                    modifier = Modifier.testTag("bottom_tab_feed"),
                )
                // 胶囊内顺序：消息、微博、搜索（「我的」已拆到右侧独立圆钮）。搜索是动作不是 Tab，点了切到微博页并打开搜索面板。
                FloatingTabItem(
                    selected = false,
                    onClick = onSearch,
                    icon = MiuixIcons.Search,
                    selectedIcon = MiuixIcons.Search,
                    label = "搜索",
                    modifier = Modifier.testTag("bottom_tab_search"),
                )
            }
        }
        }
        // 展开钮：占据原「我的」圆钮的位置（同尺寸、同玻璃材质），
        // 点按展开/收回上方的悬浮竖列（刷新 / 发布 / 我的）；展开状态由外层持有，不自动收回。
        // 右边距与胶囊左边距同为 sideMargin（放大时一起让出空间），圆钮本体在缩放密度里长大/缩小。
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = sideMargin.dp),
        ) {
            ScaledBarDensity(barScale) {
                ExpandCircleButton(
                    expanded = fabExpanded,
                    onClick = onExpandToggle,
                    onLongClick = onExpandLongPress,
                    glassActive = glassActive,
                    backdrop = backdrop,
                    isDark = isDark,
                    modifier = Modifier.testTag("bottom_expand_toggle"),
                )
            }
        }
    }
}

/**
 * 悬浮底栏的普通 Tab 项（图标 + 标签）。Miuix 的 FloatingNavigationBarItem 只画图标、
 * 标签仅作无障碍描述，与原 COUI 项的图文样式不符，故自绘；未选色用
 * FloatingNavigationBarDefaults.UnselectedAlpha 的透明度约定。
 */
@Composable
private fun FloatingTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    // 选中项图标与文字都按强调色（与顶栏分段一致，参考微信底栏选中即变色）。
    val tint = if (selected) {
        colorScheme.primary
    } else {
        colorScheme.onSurface.copy(alpha = FloatingNavigationBarDefaults.UnselectedAlpha)
    }
    Column(
        modifier
            .width(64.dp)
            .height(52.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(tint),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileCircleButton(
    selected: Boolean,
    onClick: () -> Unit,
    avatarUrl: String?,
    overflowMenu: (@Composable () -> Unit)?,
    glassActive: Boolean,
    backdrop: LayerBackdrop?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val tint = if (selected) colorScheme.onSurface else colorScheme.onSurfaceSecondary
    var menuExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val extraRightShiftPx = with(density) { 68.dp.roundToPx() }
    val popupPositionProvider = remember(extraRightShiftPx) {
        profilePopupPositionProvider(extraRightShiftPx)
    }
    val background = if (glassActive && backdrop != null) {
        Modifier.glassPanel(
            backdrop = backdrop,
            shape = GlassShape(26.dp),
            style = GlassDefaults.style(isDark),
            alpha = App.settings.barGlassAlpha,
            material = GlassMaterials.puredThinGlass(isDark),
            stroke = GlassEdgeStrokes.forTheme(isDark),
            shadow = null,
            shading = false,
        )
    } else {
        Modifier.background(
            colorScheme.surfaceContainer.copy(
                alpha = App.settings.barGlassAlpha,
            ),
            CircleShape,
        )
    }
    Box(
        modifier = modifier
            .size(52.dp)
            .then(background)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onLongClick = if (overflowMenu == null) null else {
                    { menuExpanded = true }
                },
                onClick = onClick,
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            val ringModifier = if (selected) {
                Modifier.border(1.5.dp, colorScheme.onSurface, CircleShape)
            } else {
                Modifier
            }
            Avatar(
                url = avatarUrl,
                size = 30.dp,
                modifier = ringModifier,
            )
        } else {
            Image(
                modifier = Modifier.size(26.dp),
                imageVector = if (selected) MiboIcons.ProfileFilled else MiboIcons.ProfileOutline,
                contentDescription = "我的",
                colorFilter = ColorFilter.tint(tint),
            )
        }
        if (overflowMenu != null) {
            OverlayListPopup(
                show = menuExpanded,
                popupPositionProvider = popupPositionProvider,
                alignment = PopupPositionProvider.Align.BottomEnd,
                maxHeight = MiboPopupMenuDefaults.MaxHeight,
                minWidth = MiboPopupMenuDefaults.MinWidth,
                onDismissRequest = { menuExpanded = false },
            ) {
                ListPopupColumn {
                    overflowMenu()
                }
            }
        }
    }
}

/**
 * 底栏右下展开钮：与「我的」圆钮同款玻璃（52dp），箭头随展开状态翻向——
 * 收起时向上（点开向上展开），展开时向下（点按收回）。
 */
@Composable
private fun ExpandCircleButton(
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    glassActive: Boolean,
    backdrop: LayerBackdrop?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val background = if (glassActive && backdrop != null) {
        Modifier.glassPanel(
            backdrop = backdrop,
            shape = GlassShape(26.dp),
            style = GlassDefaults.style(isDark),
            alpha = App.settings.barGlassAlpha,
            material = GlassMaterials.puredThinGlass(isDark),
            stroke = GlassEdgeStrokes.forTheme(isDark),
            shadow = null,
            shading = false,
        )
    } else {
        Modifier.background(
            colorScheme.surfaceContainer.copy(
                alpha = App.settings.barGlassAlpha,
            ),
            CircleShape,
        )
    }
    Box(
        modifier = modifier
            .size(52.dp)
            .then(background)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = if (expanded) MiuixIcons.ExpandMore else MiuixIcons.ExpandLess,
            contentDescription = if (expanded) "收起" else "展开",
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(colorScheme.onSurface),
        )
    }
}

/** 悬浮刷新圆钮：点按触发回顶刷新，箭头同时转一整圈。 */
@Composable
private fun RefreshCircleButton(onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    GlassCircleButton(
        icon = MiuixIcons.Refresh,
        contentDescription = "刷新",
        onClick = {
            scope.launch {
                rotation.snapTo(0f)
                rotation.animateTo(360f, tween(600))
            }
            onRefresh()
        },
        iconModifier = Modifier.rotate(rotation.value),
        modifier = Modifier.testTag("refresh_fab"),
    )
}

/** 与「我的」圆钮同款材质的 52dp 玻璃圆钮（悬浮按钮组专用）。 */
@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val backdrop = LocalBackdrop.current
    val colorScheme = MiuixTheme.colorScheme
    val glassActive = blurEffectsEnabled() && backdrop != null
    val isDark = colorScheme.background.luminance() < 0.5f
    val background = if (glassActive && backdrop != null) {
        Modifier.glassPanel(
            backdrop = backdrop,
            shape = GlassShape(26.dp),
            style = GlassDefaults.style(isDark),
            alpha = App.settings.barGlassAlpha,
            material = GlassMaterials.puredThinGlass(isDark),
            stroke = GlassEdgeStrokes.forTheme(isDark),
            shadow = null,
            shading = false,
        )
    } else {
        Modifier.background(
            colorScheme.surfaceContainer.copy(
                alpha = App.settings.barGlassAlpha,
            ),
            CircleShape,
        )
    }
    Box(
        modifier = modifier
            .size(52.dp)
            .then(background)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = iconModifier.size(26.dp),
            colorFilter = ColorFilter.tint(colorScheme.onSurface),
        )
    }
}

private fun profilePopupPositionProvider(extraRightShiftPx: Int): PopupPositionProvider =
    object : PopupPositionProvider {
        private val margins = PaddingValues(
            horizontal = MiboPopupMenuDefaults.HorizontalMargin,
            vertical = MiboPopupMenuDefaults.VerticalMargin,
        )

        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val minX = windowBounds.left + popupMargin.left
            val maxX = (windowBounds.right - popupContentSize.width - popupMargin.right)
                .coerceAtLeast(minX)
            val desiredX = anchorBounds.right + extraRightShiftPx - popupContentSize.width
            val minY = windowBounds.top + popupMargin.top
            val maxY = (windowBounds.bottom - popupContentSize.height - popupMargin.bottom)
                .coerceAtLeast(minY)
            val offsetY = (anchorBounds.top - popupContentSize.height - popupMargin.bottom)
                .coerceIn(minY, maxY)
            return IntOffset(
                x = desiredX.coerceIn(minX, maxX),
                y = offsetY,
            )
        }

        override fun getMargins(): PaddingValues = margins
    }

/** 长按悬浮底栏「我」弹出的快捷切换账号菜单。 */
@Composable
private fun ProfileQuickSwitchMenu(
    guest: Boolean,
    onSwitchAccount: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onAccountRelated: () -> Unit,
) {
    val accounts = remember { App.store.accounts() }
    val currentUid = remember { App.repo.session?.uid }
    val currentCookie = remember { App.repo.cookie() }
    val dismiss = LocalDismissState.current
    // 「账号相关」只在已登录且当前账号在册时出现。
    val currentAccount = accounts.firstOrNull {
        !guest && (it.uid == currentUid || it.cookie == currentCookie)
    }
    val totalCount = accounts.size + 1 + (if (currentAccount != null) 1 else 0)
    val actionIconTint = MiuixTheme.colorScheme.onSurfaceContainer

    // Miuix 的 DropdownImpl 没有 COUI 的 groupKey 与 modifier 参数：
    // 分组语义去掉（行间分隔由 isFirst/isLast 推导），testTag 移到外层 Box 上。
    accounts.forEachIndexed { index, account ->
        val isCurrent = !guest && (account.uid == currentUid || account.cookie == currentCookie)
        Box(Modifier.testTag("quick_switch_account_$index")) {
            DropdownImpl(
                item = DropdownItem(
                    text = account.name.ifBlank { "微博账号" },
                    summary = "UID ${account.uid}",
                    icon = { iconModifier ->
                        Box(iconModifier, contentAlignment = Alignment.Center) {
                            Avatar(account.avatar, 28.dp)
                        }
                    },
                ),
                optionSize = totalCount,
                isSelected = isCurrent,
                index = index,
                onSelectedIndexChange = {
                    dismiss?.invoke()
                    if (!isCurrent) onSwitchAccount(account)
                },
            )
        }
    }

    val addIndex = accounts.size
    Box(Modifier.testTag("quick_switch_add_account")) {
        DropdownImpl(
            item = DropdownItem(
                text = if (guest) "登录 / 添加账号" else "添加账号",
                icon = { iconModifier ->
                    Box(iconModifier, contentAlignment = Alignment.Center) {
                        Icon(MiuixIcons.Add, null, Modifier.size(20.dp), tint = actionIconTint)
                    }
                },
            ),
            optionSize = totalCount,
            isSelected = false,
            index = addIndex,
            onSelectedIndexChange = {
                dismiss?.invoke()
                onAddAccount()
            },
        )
    }

    if (currentAccount != null) {
        Box(Modifier.testTag("quick_switch_account_related")) {
            DropdownImpl(
                item = DropdownItem(
                    text = "账号相关",
                    icon = { iconModifier ->
                        Box(iconModifier, contentAlignment = Alignment.Center) {
                            Icon(MiboIcons.AdminShield, null, Modifier.size(20.dp), tint = actionIconTint)
                        }
                    },
                ),
                optionSize = totalCount,
                isSelected = false,
                index = addIndex + 1,
                onSelectedIndexChange = {
                    dismiss?.invoke()
                    onAccountRelated()
                },
            )
        }

    }
}
