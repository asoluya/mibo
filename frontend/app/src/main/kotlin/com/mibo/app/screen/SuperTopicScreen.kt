package com.mibo.app.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.ApiException
import com.mibo.app.data.FeedPage
import com.mibo.app.data.SuperTopicInfo
import com.mibo.app.data.SuperTopicPage
import com.mibo.app.data.SuperTopicTab
import com.mibo.app.data.SuperTopicTabKind
import com.mibo.app.data.defaultSuperTopicTabs
import com.mibo.app.data.superTopicId
import com.mibo.app.ui.MiboIcons
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.components.Avatar
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.FullPageRefreshTexts
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.twoRowListScrollAlpha
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.net.URLEncoder

/** Keeps the existing feed's request serialization, refresh and duplicate filtering. */
class SuperTopicStore(
    containerId: String,
    val readOnly: Boolean = false,
    loader: suspend (String?) -> SuperTopicPage,
) {
    var info by mutableStateOf<SuperTopicInfo?>(null)
        private set
    var tabs by mutableStateOf<List<SuperTopicTab>>(emptyList())
        private set
    var lastRequestWasRefresh = true
        private set
    private val cursors = HashSet<String>()
    val feed = FeedStore(containerId, readOnly = readOnly) { cursor, _ ->
        lastRequestWasRefresh = cursor == null
        val page = loader(cursor)
        if (cursor == null) cursors.clear() else cursors.add(cursor)
        if (page.nextCursor != null && page.nextCursor in cursors) {
            throw ApiException("超话分页游标重复，请刷新或网页查看")
        }
        info = page.info
        if (page.tabs.isNotEmpty()) tabs = page.tabs
        FeedPage(page.items, page.nextCursor)
    }
}

@Composable
fun SuperTopicScreen(nav: NavState, containerId: String, fallbackUrl: String) {
    val baseId = remember(containerId) { superTopicId(containerId) ?: containerId }
    val scope = rememberCoroutineScope()
    var tabs by remember(baseId) { mutableStateOf(defaultSuperTopicTabs(baseId)) }
    val stores = remember(baseId) { mutableStateMapOf<String, SuperTopicStore>() }
    val listStates = remember(baseId) { mutableStateMapOf<String, LazyListState>() }

    fun storeFor(tab: SuperTopicTab): SuperTopicStore =
        stores.getOrPut(tab.containerId) {
            SuperTopicStore(tab.containerId, readOnly = App.repo.guest) { cursor ->
                App.repo.superTopicTab(baseId, tab.containerId, cursor)
            }
        }

    fun listStateFor(tab: SuperTopicTab): LazyListState =
        listStates.getOrPut(tab.containerId) { LazyListState() }

    val defaultTab = tabs.first()
    val defaultStore = storeFor(defaultTab)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val density = LocalDensity.current
    val horizontalPadding = searchHorizontalPadding()
    var filterMenuOpen by remember { mutableStateOf(false) }
    var checkinBusy by remember(baseId) { mutableStateOf(false) }
    var checkedInToday by remember(baseId) {
        mutableStateOf(App.repo.superTopicCheckedInToday(baseId))
    }

    val fallbackTopHeight = pinnedHeight(TwoRowFirstRowHeight, TwoRowSecondRowHeight)
    var barHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeight = if (barHeightPx > 0) {
        with(density) { barHeightPx.toDp() }
    } else {
        fallbackTopHeight
    }

    LaunchedEffect(defaultStore.tabs) {
        if (defaultStore.tabs.isNotEmpty()) {
            tabs = defaultStore.tabs
        }
    }

    val headerInfo = defaultStore.info ?: stores.values.firstNotNullOfOrNull { it.info }
    val title = headerInfo?.title ?: "超话"

    Box(Modifier.fillMaxSize().background(pageContainerColor()).testTag("supertopic_page")) {
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
                ) { pageIndex ->
                    val tab = tabs[pageIndex]
                    val store = storeFor(tab)
                    val feed = store.feed
                    val listState = listStateFor(tab)
                    val pullState = rememberPullToRefreshState()

                    SyncDeletedStatuses(feed)
                    LaunchedEffect(store) {
                        if (!feed.loaded) feed.loadInitial()
                    }
                    LaunchedEffect(listState, feed, feed.items.size) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                            .collect { last ->
                                if (feed.items.isNotEmpty() && last >= feed.items.size - 1 && feed.error == null) {
                                    feed.loadMore()
                                }
                            }
                    }
                    LaunchedEffect(feed.generation) {
                        if (feed.loaded) listState.scrollToItem(0)
                    }

                    PullToRefresh(
                        isRefreshing = feed.refreshing,
                        onRefresh = { scope.launch { feed.refresh() } },
                        modifier = Modifier.fillMaxSize().testTag("supertopic_tab_${tab.id}"),
                        pullToRefreshState = pullState,
                        contentPadding = PaddingValues(top = topBarHeight),
                        refreshTexts = FullPageRefreshTexts,
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical(),
                            contentPadding = PaddingValues(
                                start = 0.dp,
                                top = topBarHeight + 8.dp,
                                end = 0.dp,
                                bottom = 12.dp + navBottomPadding(),
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            if (pageIndex == 0) {
                                item(key = "header") {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        (store.info ?: headerInfo)?.let { info ->
                                            Row {
                                                Avatar(info.avatar, 56.dp)
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(info.title, fontSize = 20.sp)
                                                    Spacer(Modifier.height(4.dp))
                                                    val counts = listOf(
                                                        info.readCount.takeIf { it.isNotBlank() }?.let { "阅读 $it" },
                                                        info.postCount.takeIf { it.isNotBlank() }?.let { "帖子 $it" },
                                                        info.fanCount.takeIf { it.isNotBlank() }?.let { "粉丝 $it" },
                                                    ).filterNotNull().joinToString(" · ")
                                                    if (counts.isNotBlank()) Text(counts, fontSize = 12.sp)
                                                }
                                                if (!App.repo.guest) {
                                                    Spacer(Modifier.weight(1f))
                                                    TextButton(
                                                        text = when {
                                                            checkinBusy -> "签到中…"
                                                            checkedInToday -> "已签到"
                                                            else -> "签到"
                                                        },
                                                        enabled = !checkinBusy && !checkedInToday,
                                                        onClick = {
                                                            checkinBusy = true
                                                            scope.launch {
                                                                try {
                                                                    val result = App.repo.superTopicCheckin(baseId)
                                                                    if (result.success || result.alreadyChecked) {
                                                                        checkedInToday = true
                                                                    }
                                                                    App.toast(result.message)
                                                                } catch (e: CancellationException) {
                                                                    throw e
                                                                } catch (e: Exception) {
                                                                    App.toast(e.message ?: "签到失败")
                                                                } finally {
                                                                    checkinBusy = false
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("supertopic_checkin"),
                                                    )
                                                }
                                            }
                                            if (info.description.isNotBlank()) {
                                                Spacer(Modifier.height(10.dp))
                                                Text(info.description, fontSize = 14.sp)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        if (feed.error?.contains("登录") == true) {
                                            TextButton(text = "前往登录", onClick = { nav.push(Route.SmsLogin()) })
                                        }
                                    }
                                }
                            }
                            if (feed.items.isEmpty()) {
                                item(key = "state") {
                                    when {
                                        feed.refreshing || (!feed.loaded && feed.error == null) -> LoadingState()
                                        feed.error != null -> Column {
                                            ErrorState(feed.error!!) {
                                                scope.launch { feed.refresh() }
                                            }
                                            if (pageIndex != 0 && feed.error?.contains("登录") == true) {
                                                TextButton(text = "前往登录", onClick = { nav.push(Route.SmsLogin()) })
                                            }
                                        }
                                        else -> EmptyState("这个分类暂时没有内容")
                                    }
                                }
                            }
                            itemsIndexed(feed.items, key = { _, status -> "${tab.containerId}-${status.id}" }) { index, status ->
                                val origin = rememberExpandOrigin()
                                StatusCard(
                                    status = status,
                                    modifier = Modifier.expandOrigin(origin),
                                    actionsEnabled = !feed.readOnly,
                                    onOpen = { nav.push(Route.Detail(status.id, status.mid, feed.readOnly), origin) },
                                    onOpenQuoted = { quoted, quotedOrigin ->
                                        nav.push(Route.Detail(quoted.id, quoted.mid, feed.readOnly), quotedOrigin)
                                    },
                                    onUser = { if (it.id.isNotBlank()) nav.push(Route.Profile(it.id)) },
                                    onImage = { pics, idx -> nav.push(Route.Images(pics, idx)) },
                                    onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive)) },
                                    onLike = { anchor -> scope.launch { feed.like(index, anchor) } },
                                    onRepost = { nav.push(Route.Detail(status.id, status.mid, feed.readOnly), origin) },
                                    onComment = { nav.push(Route.Detail(status.id, status.mid, feed.readOnly), origin) },
                                )
                            }
                            if (feed.items.isNotEmpty()) {
                                item(key = "pagination") {
                                    when {
                                        feed.loading || feed.refreshing -> LoadingState()
                                        feed.error != null -> ErrorState(feed.error!!) {
                                            scope.launch {
                                                if (store.lastRequestWasRefresh) feed.refresh() else feed.loadMore()
                                            }
                                        }
                                        feed.end || feed.sinceId == null -> EmptyState("已到最后一页")
                                        else -> TextButton(
                                            text = "加载更多",
                                            onClick = { scope.launch { feed.loadMore() } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            PinnedBarDensity {
                val barDensity = LocalDensity.current
                val currentTab = tabs[pagerState.currentPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))]
                val selectedIdx = pagerState.currentPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
                // Miuix 没有 SearchFilterTopAppBar：双行栏改为 AppBarSurface 里手排两行，
                // 第二行 = 分类菜单按钮（锚点）+ Miuix TabRow（原胶囊标签去掉图标，菜单里仍带图标）。
                AppBarSurface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height },
                    containerColor = pageContainerColor(),
                    scrollFraction = {
                        val activeTab = tabs[pagerState.currentPage.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))]
                        twoRowListScrollAlpha(listStateFor(activeTab), barDensity, contentGap = 8.dp)
                    },
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TwoRowFirstRowHeight)
                                .padding(horizontal = horizontalPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BackIconButton { nav.pop() }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = title,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(550),
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { nav.push(Route.Web(fallbackUrl, title)) },
                                modifier = Modifier.testTag("supertopic_web"),
                            ) {
                                Icon(MiboIcons.Globe, "网页查看", Modifier.size(24.dp))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TwoRowSecondRowHeight)
                                .padding(horizontal = horizontalPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                IconButton(
                                    onClick = { filterMenuOpen = true },
                                    modifier = Modifier.testTag("supertopic_filter_menu"),
                                ) {
                                    Icon(MiuixIcons.GridView, "全部分类", Modifier.size(22.dp))
                                }
                                SuperTopicFilterMenu(
                                    tabs = tabs,
                                    currentTab = currentTab,
                                    expanded = filterMenuOpen,
                                    onExpandedChange = { filterMenuOpen = it },
                                    horizontalPadding = horizontalPadding,
                                    onSelectTab = { idx ->
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    },
                                    onRefresh = {
                                        scope.launch { storeFor(currentTab).feed.refresh() }
                                    },
                                    onOpenWeb = {
                                        val url = if (currentTab.containerId == "${baseId}_-_feed") {
                                            fallbackUrl
                                        } else {
                                            "https://m.weibo.cn/p/index?containerid=" +
                                                URLEncoder.encode(currentTab.containerId, "UTF-8")
                                        }
                                        nav.push(Route.Web(url, title))
                                    },
                                    modifier = Modifier.matchParentSize(),
                                )
                            }
                            TabRow(
                                tabs = tabs.map { it.label },
                                selectedTabIndex = selectedIdx,
                                onTabSelected = { idx ->
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                },
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperTopicFilterMenu(
    tabs: List<SuperTopicTab>,
    currentTab: SuperTopicTab,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    horizontalPadding: Dp,
    onSelectTab: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val iconTint = MiuixTheme.colorScheme.onSurfaceContainer
    val selectedTint = MiuixTheme.colorScheme.primary
    val dropdownPositionProvider = remember(horizontalPadding) {
        ListPopupDefaults.dropdownPositionProvider(horizontalMargin = horizontalPadding)
    }
    val menuEntries = remember(tabs, currentTab, hapticFeedback, iconTint, selectedTint) {
        val categoryItems = tabs.mapIndexed { index, tab ->
            val selected = tab == currentTab
            val tint = if (selected) selectedTint else iconTint
            DropdownItem(
                text = tab.label,
                selected = selected,
                icon = filterMenuItemIcon(tab.kind.icon(), tint),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSelectTab(index)
                },
            )
        }
        val actionItems = listOf(
            DropdownItem(
                text = "刷新当前分类",
                icon = filterMenuItemIcon(MiuixIcons.Refresh, iconTint),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    onRefresh()
                },
            ),
            DropdownItem(
                text = "在网页中打开",
                icon = filterMenuItemIcon(MiboIcons.Globe, iconTint),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    onOpenWeb()
                },
            ),
        )
        listOf(
            DropdownEntry(items = categoryItems),
            DropdownEntry(items = actionItems),
        )
    }

    Box(modifier) {
        OverlayCascadingListPopup(
            show = expanded,
            entries = menuEntries,
            onDismissRequest = { onExpandedChange(false) },
            popupPositionProvider = dropdownPositionProvider,
            alignment = PopupPositionProvider.Align.Start,
            maxHeight = 440.dp,
            collapseOnSelection = true,
        )
    }
}

private fun SuperTopicTabKind.icon(): ImageVector = when (this) {
    SuperTopicTabKind.Feed -> MiuixIcons.All
    SuperTopicTabKind.SortTime -> MiuixIcons.Recent
    SuperTopicTabKind.Recommend -> MiuixIcons.Promotions
    SuperTopicTabKind.Soul -> MiuixIcons.Favorites
    SuperTopicTabKind.Official -> MiuixIcons.Community
    SuperTopicTabKind.Album -> MiuixIcons.Image
    SuperTopicTabKind.Video -> MiuixIcons.Play
    SuperTopicTabKind.Detail -> MiuixIcons.Info
    SuperTopicTabKind.Tag -> MiuixIcons.Layers
}
