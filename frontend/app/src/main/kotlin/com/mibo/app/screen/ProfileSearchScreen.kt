package com.mibo.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Status
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.rememberListScrollFraction
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.navBottomPadding
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 主页内搜索：从某人主页顶栏进来，按关键词搜 TA 发过的微博。
 * 数据走网页端 `ajax/statuses/searchProfile`（[com.mibo.app.data.Repo.searchProfileStatuses]），
 * 按页码翻页；没登录网页版时 cookie 受限，结果可能不完整，列表顶部会有一行提示。
 */
@Composable
fun ProfileSearchScreen(nav: NavState, uid: String) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember(uid) { mutableStateOf("") }
    var submitted by remember(uid) { mutableStateOf("") }
    var searched by remember(uid) { mutableStateOf(false) }
    val items = remember(uid) { mutableStateListOf<Status>() }
    var page by remember(uid) { mutableIntStateOf(1) }
    var total by remember(uid) { mutableIntStateOf(0) }
    var limited by remember(uid) { mutableStateOf(false) }
    var loading by remember(uid) { mutableStateOf(false) }
    var end by remember(uid) { mutableStateOf(false) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var moreError by remember(uid) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val focusRequester = remember { FocusRequester() }

    // 进页面就聚焦搜索框：这一页的目的就是打字，自动弹输入法是预期行为。
    LaunchedEffect(uid) {
        runCatching { focusRequester.requestFocus() }
    }

    suspend fun load(reset: Boolean) {
        if (loading) return
        val q = if (reset) query.trim() else submitted
        if (q.isBlank()) return
        loading = true
        val nextPage = if (reset) 1 else page
        try {
            val result = App.repo.searchProfileStatuses(uid, q, nextPage)
            if (reset) {
                items.clear()
                error = null
                end = false
                submitted = q
                searched = true
            }
            moreError = null
            limited = result.cookieLimited
            total = result.total
            val known = items.mapTo(HashSet()) { it.id }
            result.items.forEach { if (known.add(it.id)) items.add(it) }
            page = nextPage + 1
            if (result.items.isEmpty() || (total > 0 && items.size >= total)) end = true
        } catch (t: Throwable) {
            val message = t.message ?: "搜索失败"
            if (reset) {
                // 新一次搜索失败就整页报错，重试仍用当前输入框里的词。
                searched = true
                items.clear()
                error = message
            } else if (items.isEmpty()) {
                error = message
            } else {
                // 后面几页失败只在底部留一个重试，已看到的结果不动。
                moreError = message
            }
        } finally {
            loading = false
        }
    }

    fun submit() {
        if (query.isBlank() || loading) return
        keyboard?.hide()
        scope.launch { load(true) }
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= items.size - 2 && items.isNotEmpty() && !end && moreError == null) {
                    load(false)
                }
            }
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffectsEnabled(), backdrop)) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topBarHeight))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { submit() },
                        expanded = true,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("profile_search_input"),
                        label = "搜索 TA 的微博",
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .scrollEndHaptic()
                        .testTag("profile_search_list"),
                    contentPadding = PaddingValues(bottom = navBottomPadding() + 16.dp),
                ) {
                    if (limited && items.isNotEmpty()) {
                        item(key = "profile_search_limited") {
                            Text(
                                if (total > 0) "共约 $total 条 · 当前未登录网页版，搜索结果可能不完整"
                                else "当前未登录网页版，搜索结果可能不完整",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    } else if (total > 0 && items.isNotEmpty()) {
                        item(key = "profile_search_total") {
                            Text(
                                "共约 $total 条",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (items.isEmpty()) {
                        item(key = "profile_search_state") {
                            when {
                                loading -> LoadingState()
                                error != null -> ErrorState(error!!) {
                                    scope.launch { load(true) }
                                }
                                !searched -> EmptyState("输入关键词，搜索 TA 发过的微博")
                                limited -> EmptyState("没有搜到相关微博（当前未登录网页版，结果可能不完整）")
                                else -> EmptyState("没有搜到相关微博")
                            }
                        }
                    }
                    itemsIndexed(items, key = { _, s -> s.id }) { _, status ->
                        val origin = rememberExpandOrigin()
                        StatusCard(
                            status = status,
                            modifier = Modifier.expandOrigin(origin),
                            onOpen = { nav.push(Route.Detail(status.id, status.mid), origin) },
                            onDeleted = { items.removeAll { it.id == status.id } },
                            onOpenQuoted = { quoted, quotedOrigin ->
                                nav.push(Route.Detail(quoted.id, quoted.mid), quotedOrigin)
                            },
                            onUser = { nav.push(Route.Profile(it.id)) },
                            onImage = { pics, i -> nav.push(Route.Images(pics, i)) },
                            onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive, it.altUrl, it.oid)) },
                            onLike = { anchor ->
                                scope.launch {
                                    val updated = App.toggleStatusLike(status, anchor) ?: return@launch
                                    val idx = items.indexOfFirst { it.id == status.id }
                                    if (idx >= 0) items[idx] = updated
                                }
                            },
                            onRepost = { App.requestRepost(status) },
                            onComment = { nav.push(Route.Detail(status.id, status.mid), origin) },
                        )
                    }
                    if (items.isNotEmpty()) {
                        item(key = "profile_search_footer") {
                            when {
                                loading -> LoadingState()
                                moreError != null -> TextButton(
                                    text = "加载失败，点此重试",
                                    onClick = { scope.launch { load(false) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                end -> Text(
                                    "没有更多了",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageContainerColor(),
                    scrollFraction = rememberListScrollFraction(listState),
                ) {
                    SmallTopAppBar(
                        title = "主页搜索",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                    )
                }
            }
        }
    }
}
