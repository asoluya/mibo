package com.mibo.app.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Status
import com.mibo.app.data.parseWeiboTimeMillis
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalTopBarBounds
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.TopBarBounds
import com.mibo.app.ui.attachToRow
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.FullPageRefreshTexts
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.glassBarContentTop
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.listScrollAlpha
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.reportTopBarBottom
import com.mibo.app.ui.LocalBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.launch

/**
 * 收藏页面：已登录账号提供「微博收藏」（云端）与「本地收藏」两栏，纯本地模式直接展示本地收藏。
 */
@Composable
fun FavoritesScreen(nav: NavState) {
    val guest = remember { App.repo.guest }
    val localStore = remember {
        FeedStore("local_favorites") { cursor, force -> App.repo.favorites(cursor, force) }
    }
    val favVersion = App.repo.favoritesVersion
    LaunchedEffect(favVersion) {
        if (favVersion > 0) localStore.refresh()
    }

    if (guest) {
        SimpleStatusFeedScreen(
            nav = nav,
            title = "收藏",
            store = localStore,
            emptyText = "还没有收藏的微博",
            testTag = "favorites_page",
        )
        return
    }

    val scope = rememberCoroutineScope()
    val cloudStore = remember {
        FeedStore("cloud_favorites") { cursor, force -> App.repo.cloudFavorites(cursor, force) }
    }
    val tabs = remember { listOf("微博收藏", "本地收藏") }
    val stores = remember(cloudStore, localStore) { listOf(cloudStore, localStore) }
    val listStates = remember { listOf(LazyListState(), LazyListState()) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val topBarBounds = remember { TopBarBounds() }
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    var menuExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(FavoritesSortOrder.NewestFirst) }
    var hideExpired by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filterRowPx by remember { mutableIntStateOf(0) }
    val filterRowHeight = with(LocalDensity.current) { filterRowPx.toDp() }
    val filterInset by animateDpAsState(
        targetValue = if (filterOpen) filterRowHeight else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "favoritesFilterInset",
    )

    BackHandler(enabled = filterOpen) {
        filterOpen = false
        query = ""
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor()).testTag("favorites_page")) {
        CompositionLocalProvider(LocalTopBarBounds provides topBarBounds) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdropIf(blur, backdrop)
                    .background(pageContainerColor()),
            ) {
                CompositionLocalProvider(LocalBackdrop provides if (blur) backdrop else null) {
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        val store = stores[page]
                        val listState = listStates[page]
                        StatusFeedList(
                            nav = nav,
                            store = store,
                            listState = listState,
                            topInset = topBarHeight,
                            extraTopInset = filterInset,
                            emptyText = if (page == 0) "微博账号里还没有收藏" else "还没有本地收藏",
                            filteredEmptyText = when {
                                query.isNotBlank() -> "没有匹配的收藏"
                                hideExpired -> "当前列表均为已失效微博"
                                else -> null
                            },
                            transformItems = { items ->
                                filterAndSortFavorites(
                                    items = items,
                                    sortOrder = sortOrder,
                                    hideExpired = hideExpired,
                                    query = query,
                                )
                            },
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            PinnedBarDensity {
                val barDensity = LocalDensity.current
                val menuScheme = MiuixTheme.colorScheme
                val selectedIdx = pagerState.currentPage.coerceIn(0, tabs.lastIndex)
                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                    AppBarSurface(
                        modifier = Modifier.fillMaxWidth().reportTopBarBottom(topBarBounds),
                        containerColor = pageContainerColor(),
                        scrollFraction = {
                            val activeIdx = pagerState.currentPage.coerceIn(0, listStates.lastIndex)
                            listScrollAlpha(listStates[activeIdx], barDensity)
                        },
                    ) {
                        // Miuix 的 SmallTopAppBar 没有 segmentButton 槽位，分段标签改为手动排进一行。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TopAppBarDefaults.SmallTopAppBarCenterHeight),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.padding(start = TopAppBarDefaults.NavigationIconPadding)) {
                                BackIconButton { nav.pop() }
                            }
                            TabRowWithContour(
                                tabs = tabs,
                                selectedTabIndex = selectedIdx,
                                onTabSelected = { idx ->
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Box(Modifier.padding(end = TopAppBarDefaults.ActionIconPadding)) {
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("favorites_more"),
                                    ) {
                                        Icon(MiuixIcons.More, "更多", Modifier.size(22.dp))
                                    }
                                    OverlayCascadingListPopup(
                                        show = menuExpanded,
                                        entries = listOf(
                                            DropdownEntry(
                                                items = listOf(
                                                    DropdownItem(
                                                        text = "排序",
                                                        summary = if (sortOrder == FavoritesSortOrder.NewestFirst) {
                                                            "最新优先"
                                                        } else {
                                                            "最早优先"
                                                        },
                                                        icon = { iconModifier ->
                                                            Box(iconModifier, contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = MiuixIcons.Sort,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(18.dp),
                                                                    tint = menuScheme.onSurfaceContainer,
                                                                )
                                                            }
                                                        },
                                                        children = listOf(
                                                            DropdownItem(
                                                                text = "最新优先",
                                                                selected = sortOrder == FavoritesSortOrder.NewestFirst,
                                                                onClick = {
                                                                    if (sortOrder != FavoritesSortOrder.NewestFirst) {
                                                                        sortOrder = FavoritesSortOrder.NewestFirst
                                                                        scope.launch { listStates[selectedIdx].scrollToItem(0) }
                                                                    }
                                                                },
                                                            ),
                                                            DropdownItem(
                                                                text = "最早优先",
                                                                selected = sortOrder == FavoritesSortOrder.OldestFirst,
                                                                onClick = {
                                                                    if (sortOrder != FavoritesSortOrder.OldestFirst) {
                                                                        sortOrder = FavoritesSortOrder.OldestFirst
                                                                        scope.launch { listStates[selectedIdx].scrollToItem(0) }
                                                                    }
                                                                },
                                                            ),
                                                        ),
                                                    ),
                                                    DropdownItem(
                                                        text = "过滤失效的微博",
                                                        selected = hideExpired,
                                                        icon = { iconModifier ->
                                                            Box(iconModifier, contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = MiuixIcons.Hide,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(18.dp),
                                                                    tint = menuScheme.onSurfaceContainer,
                                                                )
                                                            }
                                                        },
                                                        onClick = { hideExpired = !hideExpired },
                                                    ),
                                                    DropdownItem(
                                                        text = "搜索",
                                                        summary = query.trim().takeIf { it.isNotEmpty() },
                                                        selected = filterOpen,
                                                        icon = { iconModifier ->
                                                            Box(iconModifier, contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = MiuixIcons.Search,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(18.dp),
                                                                    tint = menuScheme.onSurfaceContainer,
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            filterOpen = !filterOpen
                                                            if (!filterOpen) query = ""
                                                        },
                                                    ),
                                                ),
                                            ),
                                        ),
                                        popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                        onDismissRequest = { menuExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                    if (filterOpen || filterInset > 0.dp) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(filterInset)
                                .clipToBounds(),
                        ) {
                            Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { filterRowPx = it.height }
                                        .padding(
                                            start = TopAppBarDefaults.NavigationIconPadding,
                                            end = TopAppBarDefaults.ActionIconPadding,
                                            top = 6.dp,
                                            bottom = 6.dp,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    InputField(
                                        query = query,
                                        onQueryChange = { query = it },
                                        onSearch = {},
                                        expanded = filterOpen,
                                        onExpandedChange = {},
                                        modifier = Modifier.weight(1f).testTag("favorites_filter_input"),
                                        label = "搜索收藏内容或作者",
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "取消",
                                        fontSize = 14.sp,
                                        color = menuScheme.primary,
                                        modifier = Modifier
                                            .clickable {
                                                filterOpen = false
                                                query = ""
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp)
                                            .testTag("favorites_filter_cancel"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 我赞过的微博列表页面。 */
@Composable
fun MyLikesScreen(nav: NavState) {
    val store = remember {
        FeedStore("my_likes") { cursor, force ->
            val uid = App.repo.ownUid()
            App.repo.likedStatuses(uid, cursor, force)
        }
    }
    SimpleStatusFeedScreen(
        nav = nav,
        title = "我赞过的",
        store = store,
        emptyText = "还没有赞过的微博",
        testTag = "my_likes_page",
    )
}

@Composable
private fun SimpleStatusFeedScreen(
    nav: NavState,
    title: String,
    store: FeedStore,
    emptyText: String,
    testTag: String,
) {
    val listState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val density = LocalDensity.current
    val topBarBounds = remember { TopBarBounds() }
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    Box(Modifier.fillMaxSize().background(pageContainerColor()).testTag(testTag)) {
        CompositionLocalProvider(LocalTopBarBounds provides topBarBounds) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdropIf(blur, backdrop)
                    .background(pageContainerColor()),
            ) {
                CompositionLocalProvider(LocalBackdrop provides if (blur) backdrop else null) {
                    StatusFeedList(
                        nav = nav,
                        store = store,
                        listState = listState,
                        topInset = topBarHeight,
                        emptyText = emptyText,
                    )
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).reportTopBarBottom(topBarBounds),
                    containerColor = pageContainerColor(),
                    scrollFraction = { listScrollAlpha(listState, density) },
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
}

@Composable
private fun StatusFeedList(
    nav: NavState,
    store: FeedStore,
    listState: LazyListState,
    topInset: Dp,
    emptyText: String,
    extraTopInset: Dp = 0.dp,
    filteredEmptyText: String? = null,
    transformItems: (List<Status>) -> List<Status> = { it },
) {
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val shownItems = transformItems(store.items)

    SyncDeletedStatuses(store)
    LaunchedEffect(store) {
        if (!store.loaded) store.loadInitial()
    }
    LaunchedEffect(listState, store, shownItems.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= shownItems.size - 2 && store.items.isNotEmpty()) store.loadMore()
            }
    }

    PullToRefresh(
        isRefreshing = store.refreshing,
        onRefresh = { scope.launch { store.refresh() } },
        modifier = Modifier.fillMaxSize(),
        pullToRefreshState = pullState,
        contentPadding = PaddingValues(top = topInset + extraTopInset),
        refreshTexts = FullPageRefreshTexts,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical(),
            contentPadding = PaddingValues(
                start = 0.dp,
                top = glassBarContentTop(topInset, 4.dp) + extraTopInset,
                end = 0.dp,
                bottom = 16.dp + navBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (store.items.isEmpty()) {
                item {
                    when {
                        store.refreshing || (!store.loaded && store.error == null) -> LoadingState()
                        store.error != null -> ErrorState(store.error!!) {
                            scope.launch { store.refresh() }
                        }
                        else -> EmptyState(emptyText)
                    }
                }
            } else if (shownItems.isEmpty()) {
                item { EmptyState(filteredEmptyText ?: emptyText) }
            }
            itemsIndexed(shownItems, key = { _, s -> s.id }) { index, status ->
                val origin = rememberExpandOrigin()
                val topBar = LocalTopBarBounds.current
                SideEffect {
                    origin.listState = listState
                    origin.rowKey = status.id
                    origin.topBar = topBar
                }
                StatusCard(
                    status = status,
                    modifier = Modifier.expandOrigin(origin),
                    onOpen = { nav.push(Route.Detail(status.id, status.mid, store.readOnly), origin) },
                    onUser = { nav.push(Route.Profile(it.id)) },
                    onImage = { pics, i -> nav.push(Route.Images(pics, i)) },
                    onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive, it.altUrl, it.oid)) },
                    onLike = { anchor ->
                        val rawIdx = store.items.indexOfFirst { it.id == status.id }
                        if (rawIdx >= 0) scope.launch { store.like(rawIdx, anchor) }
                    },
                    onRepost = { App.requestRepost(status) },
                    onComment = { nav.push(Route.Detail(status.id, status.mid, store.readOnly), origin) },
                    actionsEnabled = !store.readOnly,
                    onOpenQuoted = { quoted, quotedOrigin ->
                        quotedOrigin.attachToRow(listState, status.id, topBar)
                        nav.push(Route.Detail(quoted.id, quoted.mid, store.readOnly), quotedOrigin)
                    },
                )
            }
            if (shownItems.isNotEmpty()) {
                item {
                    if (store.loading) {
                        LoadingState()
                    } else if (store.end) {
                        Text(
                            text = "没有更多了",
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

private enum class FavoritesSortOrder { NewestFirst, OldestFirst }

private fun filterAndSortFavorites(
    items: List<Status>,
    sortOrder: FavoritesSortOrder,
    hideExpired: Boolean,
    query: String,
): List<Status> {
    val key = query.trim()
    val filtered = items.filter { status ->
        (!hideExpired || !isExpiredFavorite(status)) &&
            (key.isEmpty() || statusMatchesQuery(status, key))
    }
    return when (sortOrder) {
        FavoritesSortOrder.NewestFirst -> filtered.sortedByDescending(::statusTimestampKey)
        FavoritesSortOrder.OldestFirst -> filtered.sortedBy(::statusTimestampKey)
    }
}

private fun statusTimestampKey(status: Status): Long =
    parseWeiboTimeMillis(status.createdAt).takeIf { it > 0L }
        ?: status.id.toLongOrNull()
        ?: 0L

private fun statusMatchesQuery(status: Status, keyword: String): Boolean {
    if (status.plainText.contains(keyword, ignoreCase = true)) return true
    if (status.user?.name?.contains(keyword, ignoreCase = true) == true) return true
    val quoted = status.retweeted ?: return false
    if (quoted.plainText.contains(keyword, ignoreCase = true)) return true
    return quoted.user?.name?.contains(keyword, ignoreCase = true) == true
}

private fun isExpiredFavorite(status: Status): Boolean {
    if (App.repo.isDeleted(status.id)) return true
    val author = status.user
    if (author == null || (author.id.isBlank() && author.name.isBlank())) return true
    val text = status.plainText.trim()
    val hasMedia = status.pics.isNotEmpty() || status.video != null || status.bigPic != null
    if (!hasMedia && status.retweeted == null && text.isBlank()) return true
    if (!hasMedia && isWeiboExpiredNotice(text)) return true
    val quoted = status.retweeted
    if (!hasMedia && quoted != null && (text.isBlank() || text == "转发微博")) {
        return isExpiredFavorite(quoted)
    }
    return false
}

private val WeiboExpiredNoticeMarkers = listOf(
    "抱歉，此微博已被",
    "此微博已被作者删除",
    "该微博已被删除",
    "该微博已删除",
    "原微博已删除",
    "此条微博已被删除",
    "此微博已不可见",
    "该微博已不可见",
    "暂时没有这条微博的查看权限",
    "由于作者设置",
    "由于博主设置",
    "微博已不存在",
    "该内容暂时无法查看",
    "内容暂不可见",
    "此内容暂不可见",
    "该账号因被投诉违反",
    "根据相关法律法规和政策",
)

private fun isWeiboExpiredNotice(text: String): Boolean =
    text.length <= 80 && WeiboExpiredNoticeMarkers.any { text.contains(it) }
