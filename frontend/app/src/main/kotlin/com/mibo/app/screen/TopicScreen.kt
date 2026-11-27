package com.mibo.app.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.ApiException
import com.mibo.app.data.TopicItem
import com.mibo.app.data.superTopicId
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.glassBarContentTop
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberListScrollFraction
import com.mibo.app.ui.resolveWeiboLink
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.navBottomPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SearchClearButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassEdgeStrokes
import top.yukonga.miuix.kmp.material.GlassMaterials
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class SuperTopicSearchStore(
    private val loader: suspend (String) -> List<TopicItem> = { query ->
        val primary = try {
            App.repo.searchSuperTopics(query)
        } catch (e: ApiException) {
            if (e.errno == "-100" || e.errno == "432") throw e
            emptyList()
        }
        if (primary.isNotEmpty()) {
            primary
        } else {
            // 综合搜索里没扒到超话卡时，再走专门的超话分类（type=98）。
            runCatching { App.repo.searchSuperTopicCards(query) }
                .getOrDefault(emptyList())
                .filter { it.containerId != null }
        }
    },
) {
    var results by mutableStateOf(emptyList<TopicItem>())
        private set
    var loading by mutableStateOf(false)
        private set
    var completed by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loginRequired by mutableStateOf(false)
        private set
    private var generation = 0

    fun reset() {
        generation++
        results = emptyList()
        loading = false
        completed = false
        error = null
        loginRequired = false
    }

    suspend fun search(query: String): TopicItem? {
        reset()
        val name = query.trim().trim('#').trim()
        if (name.isBlank()) return null
        val request = generation
        loading = true
        try {
            val found = loader(name)
            if (request != generation) return null
            results = found
            completed = true
            val exact = found.filter { normalizedTopicName(it.title) == normalizedTopicName(name) }
            return exact.singleOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (request == generation) {
                loginRequired = e is ApiException && (e.errno == "-100" || e.errno == "432")
                error = if (loginRequired) "请先登录再搜索超话" else "超话搜索失败，请重试"
            }
        } finally {
            if (request == generation) loading = false
        }
        return null
    }
}

private fun normalizedTopicName(name: String): String =
    name.trim().trim('#').trim().removeSuffix("[超话]").removeSuffix("超话").trim().lowercase()

/** 柔光玻璃胶囊：有采样层时用整块玻璃（与返回钮、播放器胶囊同一套材质），否则回退实色填充。 */
@Composable
private fun Modifier.softGlassCapsule(radius: Dp): Modifier {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = LocalBackdrop.current
    val shape = RoundedCornerShape(radius)
    return if (blurEffectsEnabled() && backdrop != null) {
        this.glassPanel(
            backdrop = backdrop,
            shape = GlassShape(radius),
            style = GlassDefaults.style(isDark),
            alpha = App.settings.barGlassAlpha,
            material = GlassMaterials.puredThinGlass(isDark),
            stroke = GlassEdgeStrokes.forTheme(isDark),
            shadow = null,
            shading = false,
        ).clip(shape)
    } else {
        this.clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
    }
}

@Composable
private fun TopicRow(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softGlassCapsule(18.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontSize = 15.sp, maxLines = 1)
        if (description.isNotBlank()) {
            Text(
                description,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TopicScreen(nav: NavState, initial: String = "") {
    var query by remember { mutableStateOf(initial) }
    val store = remember { SuperTopicSearchStore() }
    var recents by remember { mutableStateOf(App.settings.recentSuperTopics()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val blur = blurEffectsEnabled()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val searchRowHeight = 52.dp

    fun openTopic(topic: TopicItem) {
        val id = topic.containerId ?: return
        val original = topic.scheme.takeIf { it != id && superTopicId(it) == id }
        val fallback = original?.let(::resolveWeiboLink) ?: "https://m.weibo.cn/p/index?containerid=$id"
        nav.push(Route.SuperTopic(id, fallback))
    }

    fun submit() {
        val name = query.trim()
        if (name.isBlank()) return
        val id = superTopicId(name)
        if (id != null) {
            openTopic(TopicItem(name, scheme = name, containerId = id))
            return
        }
        if (name.startsWith("https://") || name.startsWith("http://") ||
            name.startsWith("sinaweibo://") || name.startsWith("intent://")
        ) {
            resolveWeiboLink(name)?.let { nav.push(Route.Web(it, "网页")) }
            return
        }
        searchJob?.cancel()
        val entry = nav.stack.last()
        searchJob = scope.launch {
            val match = store.search(name)
            if (match != null && nav.stack.lastOrNull() === entry && !entry.exiting) openTopic(match)
        }
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blur, backdrop)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollEndHaptic(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = glassBarContentTop(topBarHeight + searchRowHeight + 16.dp, 8.dp),
                    bottom = 12.dp + navBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val showSections = !store.loading && store.error == null && !store.completed && store.results.isEmpty()
                if (showSections && recents.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("浏览过的超话", fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .softGlassCapsule(50.dp)
                                    .clickable {
                                        App.settings.clearRecentSuperTopics()
                                        recents = emptyList()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("清空", fontSize = 13.sp)
                            }
                        }
                    }
                    items(recents.chunked(2), key = { "recent_row_" + it.first().containerId }) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (topic in pair) {
                                Box(Modifier.weight(1f)) {
                                    TopicRow(
                                        title = topic.title,
                                        description = "",
                                        onClick = {
                                            openTopic(TopicItem(topic.title, scheme = topic.containerId, containerId = topic.containerId))
                                        },
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                when {
                    store.loading -> item { LoadingState() }
                    store.error != null -> item {
                        Column {
                            ErrorState(store.error!!, onRetry = ::submit)
                            if (store.loginRequired) {
                                TextButton(text = "前往登录", onClick = { nav.push(Route.SmsLogin()) })
                            }
                        }
                    }
                    store.completed && store.results.isEmpty() -> item {
                        EmptyState("未找到对应超话，可以换个名称重试")
                    }
                    store.results.isNotEmpty() -> {
                        item { Text("请选择超话", fontSize = 14.sp) }
                        items(store.results, key = { it.containerId!! }) { topic ->
                            TopicRow(
                                title = topic.title,
                                description = listOf(topic.desc1, topic.desc2).filter { it.isNotBlank() }.joinToString(" · "),
                                onClick = { openTopic(topic) },
                            )
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blur) backdrop else null,
        ) {
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                PinnedBarDensity {
                    AppBarSurface(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = pageContainerColor(),
                        scrollFraction = rememberListScrollFraction(listState),
                    ) {
                        SmallTopAppBar(
                            title = "话题 / 超话",
                            color = Color.Transparent,
                            navigationIcon = {
                                BackIconButton {
                                    searchJob?.cancel()
                                    store.reset()
                                    nav.pop()
                                }
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(searchRowHeight)
                            .softGlassCapsule(26.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            MiuixIcons.Search,
                            null,
                            Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    "搜索超话，或粘贴链接 / ID",
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    maxLines = 1,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    query = it
                                    searchJob?.cancel()
                                    store.reset()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MiuixTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                ),
                                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { submit() }),
                            )
                        }
                        if (query.isNotEmpty()) {
                            SearchClearButton(
                                onClick = {
                                    query = ""
                                    searchJob?.cancel()
                                    store.reset()
                                },
                                contentDescription = "清空",
                            )
                        }
                    }
                    val direct = superTopicId(query.trim()) != null
                    val canSubmit = query.isNotBlank() && !store.loading
                    Box(
                        modifier = Modifier
                            .height(searchRowHeight)
                            .softGlassCapsule(26.dp)
                            .clickable(enabled = canSubmit) { submit() }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (direct) "打开" else "搜索",
                            fontSize = 15.sp,
                            color = if (canSubmit) {
                                MiuixTheme.colorScheme.onSurface
                            } else {
                                MiuixTheme.colorScheme.onSurfaceSecondary
                            },
                        )
                    }
                }
            }
        }
    }
}
