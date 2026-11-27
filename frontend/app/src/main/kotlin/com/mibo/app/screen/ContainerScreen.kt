package com.mibo.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mibo.app.App
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.listScrollAlpha
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.FullPageRefreshTexts
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.navBottomPadding
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ContainerScreen(nav: NavState, cid: String, title: String) {
    val scope = rememberCoroutineScope()
    val isFavorites = cid == "230267"
    val store = remember(cid) {
        if (isFavorites) {
            FeedStore(cid) { cursor, force -> App.repo.favorites(cursor, force) }
        } else {
            FeedStore(cid)
        }
    }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val density = LocalDensity.current
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    SyncDeletedStatuses(store)
    LaunchedEffect(cid) { store.loadInitial() }
    if (isFavorites) {
        val favVersion = App.repo.favoritesVersion
        LaunchedEffect(favVersion) {
            if (favVersion > 0) store.refresh()
        }
    }
    LaunchedEffect(listState, store) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= store.items.size - 2 && store.items.isNotEmpty()) store.loadMore()
            }
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdropIf(blur, backdrop)
                .background(pageContainerColor()),
        ) {
            CompositionLocalProvider(LocalBackdrop provides if (blur) backdrop else null) {
                PullToRefresh(
                isRefreshing = store.refreshing,
                onRefresh = { scope.launch { store.refresh() } },
                modifier = Modifier.fillMaxSize(),
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
                        item {
                            when {
                                store.refreshing -> LoadingState()
                                store.error != null -> ErrorState(store.error!!) {
                                    scope.launch { store.refresh() }
                                }
                                else -> EmptyState(if (isFavorites) "还没有收藏的微博" else "这里还没有内容")
                            }
                        }
                    }
                    itemsIndexed(store.items, key = { _, s -> s.id }) { index, status ->
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
                            onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive)) },
                            onLike = { anchor -> scope.launch { store.like(index, anchor) } },
                            onRepost = { nav.push(Route.Detail(status.id, status.mid), origin) },
                            onComment = { nav.push(Route.Detail(status.id, status.mid), origin) },
                        )
                    }
                    if (store.items.isNotEmpty()) {
                        item {
                            if (store.loading) LoadingState()
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
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter),
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
