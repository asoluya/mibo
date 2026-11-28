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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mibo.app.data.TopicChannel
import com.mibo.app.ui.MiboIcons
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
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
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun TopicFeedScreen(nav: NavState, topic: String) {
    val channels = TopicChannel.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { channels.size })
    val scope = rememberCoroutineScope()
    val stores = remember(topic) {
        channels.map { channel ->
            FeedStore("topic_${channel.type}_$topic") { cursor, force ->
                App.repo.topicChannelFeed(topic, channel, cursor, force)
            }
        }
    }
    val listStates = remember(topic) { List(channels.size) { LazyListState() } }
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val density = LocalDensity.current
    val horizontalPadding = searchHorizontalPadding()
    var filterMenuOpen by remember { mutableStateOf(false) }
    val fallbackTopHeight = pinnedHeight(TwoRowFirstRowHeight, TwoRowSecondRowHeight)
    var barHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeight = if (barHeightPx > 0) {
        with(density) { barHeightPx.toDp() }
    } else {
        fallbackTopHeight
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor()).testTag("topic_feed_screen")) {
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
                val channel = channels[pageIndex]
                val store = stores[pageIndex]
                val listState = listStates[pageIndex]
                val pullState = rememberPullToRefreshState()

                SyncDeletedStatuses(store)
                LaunchedEffect(store) {
                    if (!store.loaded) store.loadInitial()
                }
                LaunchedEffect(listState, store) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                        .collect { last ->
                            if (last >= store.items.size - 2 && store.items.isNotEmpty() && store.error == null) {
                                store.loadMore()
                            }
                        }
                }

                PullToRefresh(
                    isRefreshing = store.refreshing,
                    onRefresh = { scope.launch { store.refresh() } },
                    modifier = Modifier.fillMaxSize().testTag("topic_page_${channel.type}"),
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
                        if (store.items.isEmpty()) {
                            item(key = "state") {
                                when {
                                    store.refreshing || (!store.loaded && store.loading) -> LoadingState()
                                    store.error != null -> ErrorState(store.error!!) {
                                        scope.launch { store.refresh() }
                                    }
                                    channel == TopicChannel.Following && App.repo.guest ->
                                        EmptyState("登录后可查看关注中的结果")
                                    else -> EmptyState("没有找到相关微博")
                                }
                            }
                        }
                        itemsIndexed(store.items, key = { _, s -> "${channel.type}_${s.id}" }) { index, status ->
                            val origin = rememberExpandOrigin()
                            StatusCard(
                                status = status,
                                modifier = Modifier.expandOrigin(origin),
                                onOpen = { nav.push(Route.Detail(status.id, status.mid), origin) },
                                onOpenQuoted = { quoted, quotedOrigin ->
                                    nav.push(Route.Detail(quoted.id, quoted.mid), quotedOrigin)
                                },
                                onUser = { nav.push(Route.Profile(it.id)) },
                                onImage = { pics, i -> nav.push(Route.Images(pics, i)) },
                                onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive, it.altUrl, it.oid)) },
                                onLike = { anchor -> scope.launch { store.like(index, anchor) } },
                                onRepost = { App.requestRepost(status) },
                                onComment = { nav.push(Route.Detail(status.id, status.mid), origin) },
                            )
                        }
                        if (store.items.isNotEmpty()) {
                            item(key = "footer") {
                                when {
                                    store.loading -> LoadingState()
                                    store.end || store.sinceId == null -> Text(
                                        text = "没有更多了",
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 16.dp),
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
                // Miuix 没有 SearchFilterTopAppBar：与超话页相同的双行手排栏。
                AppBarSurface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { barHeightPx = it.height },
                    containerColor = pageContainerColor(),
                    scrollFraction = {
                        val activeIdx = pagerState.currentPage.coerceIn(0, listStates.lastIndex)
                        twoRowListScrollAlpha(listStates[activeIdx], barDensity, contentGap = 8.dp)
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
                                text = topic,
                                color = MiuixTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(550),
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false,
                                modifier = Modifier.weight(1f),
                            )
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
                                    modifier = Modifier.testTag("topic_filter_menu"),
                                ) {
                                    Icon(MiuixIcons.GridView, "全部分类", Modifier.size(22.dp))
                                }
                                TopicFilterMenu(
                                    channels = channels,
                                    currentChannel = channels[pagerState.currentPage],
                                    expanded = filterMenuOpen,
                                    onExpandedChange = { filterMenuOpen = it },
                                    horizontalPadding = horizontalPadding,
                                    onSelectChannel = { idx ->
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    },
                                    onRefresh = {
                                        scope.launch { stores[pagerState.currentPage].refresh() }
                                    },
                                    onOpenWeb = {
                                        val ch = channels[pagerState.currentPage]
                                        val url = "https://m.weibo.cn/search?containerid=" +
                                            URLEncoder.encode("100103type=${ch.type}&q=$topic", "UTF-8")
                                        nav.push(Route.Web(url, topic))
                                    },
                                    modifier = Modifier.matchParentSize(),
                                )
                            }
                            TabRow(
                                tabs = channels.map { it.label },
                                selectedTabIndex = pagerState.currentPage,
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
private fun TopicFilterMenu(
    channels: List<TopicChannel>,
    currentChannel: TopicChannel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    horizontalPadding: Dp,
    onSelectChannel: (Int) -> Unit,
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
    val menuEntries = remember(channels, currentChannel, hapticFeedback, iconTint, selectedTint) {
        val categoryItems = channels.mapIndexed { index, channel ->
            val selected = channel == currentChannel
            val tint = if (selected) selectedTint else iconTint
            DropdownItem(
                text = channel.label,
                selected = selected,
                icon = filterMenuItemIcon(channel.icon(), tint),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSelectChannel(index)
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
            collapseOnSelection = true,
        )
    }
}

internal fun filterMenuItemIcon(
    imageVector: ImageVector,
    tint: Color,
): @Composable (Modifier) -> Unit = { iconModifier ->
    Box(iconModifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
    }
}

private fun TopicChannel.icon(): ImageVector = when (this) {
    TopicChannel.Comprehensive -> MiuixIcons.All
    TopicChannel.Latest -> MiuixIcons.Recent
    TopicChannel.Pictures -> MiuixIcons.Image
    TopicChannel.Videos -> MiuixIcons.Play
    TopicChannel.Following -> MiuixIcons.Contacts
    TopicChannel.Hot -> MiuixIcons.Promotions
}
