package com.mibo.app.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Account
import com.mibo.app.data.HistoryItem
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.NavState
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.rememberListScrollFraction
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.Route
import com.mibo.app.ui.components.Avatar
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.ConfirmDialog
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.TopBarTextButton
import com.mibo.app.ui.components.WeiboRichText
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.LocalBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatStamp(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(millis))

@Composable
fun DraftsScreen(nav: NavState) {
    var drafts by remember { mutableStateOf(App.store.drafts()) }
    val current = nav.current

    LaunchedEffect(current) {
        if (current is Route.Drafts) drafts = App.store.drafts()
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            PinnedBarDensity {
                SmallTopAppBar(
                    color = pageContainerColor(),
                    title = "草稿",
                    navigationIcon = { BackIconButton { nav.pop() } },
                )
            }
        },
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollEndHaptic().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + navBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (drafts.isEmpty()) {
                item { EmptyState("没有草稿") }
            }
            itemsIndexed(drafts, key = { _, draft -> draft.time }) { index, draft ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("draft_row_$index"),
                    cornerRadius = CardDefaults.CornerRadius,
                    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = {
                        App.store.removeDraft(draft.time)
                        drafts = App.store.drafts()
                        nav.push(Route.Publish(draft.text))
                    },
                ) {
                    Text(draft.text, fontSize = 14.sp, maxLines = 4)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatStamp(draft.time),
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                App.store.removeDraft(draft.time)
                                drafts = App.store.drafts()
                            },
                        ) {
                            Icon(MiuixIcons.Delete, "删除", Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** 补位的位移：定长缓出，到位置就停，不回弹。 */
private val HistoryPlacementSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = 220, easing = FastOutSlowInEasing)

@Composable
fun HistoryScreen(nav: NavState) {
    var history by remember { mutableStateOf(App.store.history()) }
    var confirm by remember { mutableStateOf(false) }
    // 同一时间只展开一行，和通知栏一样：拉开新的一行，上一行自己收回去。
    var openId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // 筛选：点漏斗展开搜索框，输入即过滤；再点一次收起，同时清掉关键词和过滤结果。
    var filterOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filterRowPx by remember { mutableIntStateOf(0) }
    val filterRowHeight = with(LocalDensity.current) { filterRowPx.toDp() }
    val filterInset by animateDpAsState(
        targetValue = if (filterOpen) filterRowHeight else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "historyFilterInset",
    )
    val shown = remember(history, query) {
        val key = query.trim()
        if (key.isEmpty()) history else history.filter { it.matches(key) }
    }

    // 从详情页回来时不重排：看过的记录只把浏览时间写回文件，顺序要等离开本页再进来才生效。
    // 否则刚看完的那一条会当着人的面被抬到最前面，整个列表跟着错位。
    LaunchedEffect(nav.current) {
        if (nav.current is Route.History) openId = null
    }

    val pageColor = pageContainerColor()
    // 顶栏玻璃要有东西可采样：把列表录进 backdrop，顶栏浮在上面做渐变模糊。
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    Box(Modifier.fillMaxSize().background(pageColor)) {
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffectsEnabled(), backdrop).background(pageColor)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().scrollEndHaptic(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = topBarHeight + filterInset + 8.dp,
                    end = 12.dp,
                    bottom = 12.dp + navBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    history.isEmpty() -> item { EmptyState("还没有浏览记录") }
                    shown.isEmpty() -> item { EmptyState("没有匹配的记录") }
                    else -> item {
                        Text(
                            "左滑记录可删除",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                itemsIndexed(shown, key = { _, item -> item.id }) { index, item: HistoryItem ->
                    val swipe = rememberNotificationSwipeState()
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(swipe.isRevealed) {
                        if (swipe.isRevealed) openId = item.id
                    }
                    LaunchedEffect(openId) {
                        if (openId != item.id && swipe.isRevealed) swipe.close()
                    }
                    NotificationSwipeBox(
                        onSettings = null,
                        onDismiss = {
                            App.store.removeHistory(item.id)
                            history = history.filterNot { it.id == item.id }
                        },
                        settingsIcon = null,
                        dismissIcon = null,
                        // 删掉的一行已经滑走淡出，不再要淡出动画；下面的条目按 animateItem 补位。
                        modifier = Modifier
                            .animateItem(fadeInSpec = null, placementSpec = HistoryPlacementSpec, fadeOutSpec = null)
                            .testTag("history_row_$index"),
                        state = swipe,
                        minHeight = 0.dp,
                        containerColor = CardDefaults.defaultColors().color,
                        dismissOnFullSwipe = true,
                        dismissDescription = "删除这条记录",
                    ) {
                        HistoryRowBody(
                            item = item,
                            modifier = Modifier.clickable {
                                if (swipe.isRevealed) {
                                    scope.launch { swipe.close() }
                                } else {
                                    nav.push(Route.Detail(item.id, item.mid))
                                }
                            },
                        )
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
                containerColor = pageColor,
                scrollFraction = rememberListScrollFraction(listState),
            ) {
            Column(Modifier.fillMaxWidth()) {
                SmallTopAppBar(
                    title = "历史",
                    color = Color.Transparent,
                    navigationIcon = { BackIconButton { nav.pop() } },
                    actions = {
                        if (history.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    filterOpen = !filterOpen
                                    if (!filterOpen) query = ""
                                },
                                modifier = Modifier.testTag("history_filter"),
                            ) {
                                Icon(MiuixIcons.Filter, "筛选", Modifier.size(24.dp))
                            }
                            TopBarTextButton("清空", onClick = { confirm = true })
                        }
                    },
                )
                // 搜索框从返回键的左沿铺到「清空」的右沿，也就是顶栏两端的 16dp 内边距之间。
                // 外层高度跟着动画走，内层用 unbounded 量到自然高度，列表顶部留白也读同一个值，
                // 这样下拉和列表让位是同一条曲线。
                // 这里不能铺底色：顶栏的渐变玻璃画在自己下方一条 extra band 上，铺一层实心底正好把它
                // 从顶栏下沿切断。留空之后搜索框就浮在那条渐变上，和 COUI 的搜索框一样是半透明的。
                if (filterOpen || filterInset > 0.dp) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(filterInset)
                            .clipToBounds(),
                    ) {
                        Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { filterRowPx = it.height }
                                    .padding(
                                        start = TopAppBarDefaults.NavigationIconPadding,
                                        end = TopAppBarDefaults.ActionIconPadding,
                                        top = 6.dp,
                                        bottom = 6.dp,
                                    ),
                            ) {
                                InputField(
                                    query = query,
                                    onQueryChange = { query = it },
                                    onSearch = {},
                                    expanded = filterOpen,
                                    onExpandedChange = {},
                                    modifier = Modifier.fillMaxWidth().testTag("history_filter_input"),
                                    label = "搜索历史",
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

    if (confirm) {
        ConfirmDialog(
            title = "清空浏览历史",
            message = "会删除本地保存的全部浏览记录。",
            confirmText = "清空",
            onDismiss = { confirm = false },
            onConfirm = {
                App.store.clearHistory()
                history = emptyList()
                // 清空之后没有东西可筛，搜索框一并收回去。
                filterOpen = false
                query = ""
                confirm = false
            },
        )
    }
}

/** 筛选只匹配看得见的字：作者名和正文纯文本。 */
private fun HistoryItem.matches(key: String): Boolean =
    user.contains(key, ignoreCase = true) || text.contains(key, ignoreCase = true)

/** 一条历史记录的正文。 */
@Composable
private fun HistoryRowBody(item: HistoryItem, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (item.html.isNotBlank()) {
            WeiboRichText(
                html = item.html,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                selectable = false,
            )
        } else {
            // 旧记录没存 HTML，只能按当初裁剪下来的纯文本兜底。
            Text(
                item.text.ifBlank { "（无正文）" },
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${item.user}  ${formatStamp(item.time)}",
            fontSize = 11.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
    }
}

internal fun isCurrentAccount(
    account: Account,
    currentUid: String?,
    currentCookie: String?,
    guest: Boolean,
): Boolean {
    if (guest) return false
    if (!currentUid.isNullOrBlank()) return account.uid == currentUid
    return !currentCookie.isNullOrBlank() && account.cookie == currentCookie
}

@Composable
fun AccountsScreen(nav: NavState) {
    var accounts by remember { mutableStateOf(App.store.accounts()) }
    var currentUid by remember { mutableStateOf(App.repo.takeUnless { it.guest }?.session?.uid) }
    var currentCookie by remember { mutableStateOf(if (App.repo.guest) null else App.repo.cookie()) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }
    var renewing by remember { mutableStateOf(false) }
    val screenScope = rememberCoroutineScope()

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            PinnedBarDensity {
                SmallTopAppBar(
                    color = pageContainerColor(),
                    title = "账号管理",
                    navigationIcon = { BackIconButton { nav.pop() } },
                    actions = {
                        IconButton(
                            onClick = {
                                val active = App.repo.session
                                val hasGsid = active?.loginType == com.mibo.app.data.LOGIN_TYPE_CLIENT &&
                                    active.gsid.isNotBlank()
                                if (hasGsid) {
                                    renewing = true
                                    App.toast("正在刷新…")
                                    screenScope.launch {
                                        try {
                                            val err = App.repo.refreshClientSessionNow()
                                            if (err == null) {
                                                accounts = App.store.accounts()
                                                currentUid = App.repo.takeUnless { it.guest }?.session?.uid
                                                currentCookie = if (App.repo.guest) null else App.repo.cookie()
                                            }
                                            App.toast(
                                                err?.let { "刷新失败：$it" }
                                                    ?: "已刷新",
                                            )
                                        } finally {
                                            renewing = false
                                        }
                                    }
                                } else {
                                    val currentName = accounts.firstOrNull { it.uid == active?.uid }?.name
                                    nav.push(Route.SmsLogin(addAccount = true, relogin = currentName))
                                }
                            },
                            enabled = !renewing,
                            modifier = Modifier.testTag("accounts_renew"),
                        ) {
                            Icon(MiuixIcons.Refresh, "刷新账号状态", Modifier.size(24.dp))
                        }
                        IconButton(
                            onClick = { nav.push(Route.SmsLogin(addAccount = true)) },
                        ) {
                            Icon(MiuixIcons.Add, "添加账号", Modifier.size(24.dp))
                        }
                    },
                )
            }
        },
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollEndHaptic().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + navBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (accounts.isEmpty()) {
                item { EmptyState("还没有保存的账号") }
            } else {
                item {
                    Text(
                        "长按账号可重新登录或删除",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            itemsIndexed(accounts, key = { _, account -> account.uid }) { index, account ->
                val current = isCurrentAccount(account, currentUid, currentCookie, App.repo.guest)
                val name = account.name.ifBlank { "微博账号" }
                var rowBounds by remember { mutableStateOf(Rect.Zero) }
                Row(
                    modifier = Modifier
                        .animateItem(fadeInSpec = null, placementSpec = HistoryPlacementSpec, fadeOutSpec = null)
                        .testTag("account_row_$index")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardDefaults.CornerRadius))
                        .background(CardDefaults.defaultColors().color)
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInWindow()
                            rowBounds = Rect(
                                pos.x,
                                pos.y,
                                pos.x + coordinates.size.width.toFloat(),
                                pos.y + coordinates.size.height.toFloat(),
                            )
                        }
                        .combinedClickable(
                            onClick = {
                                App.repo.switchAccount(account)
                                currentUid = account.uid
                                currentCookie = account.cookie
                                nav.reset(Route.Main)
                            },
                            onLongClick = {
                                App.showGlassMenu(
                                    rowBounds,
                                    listOf(
                                        App.GlassMenuItem(
                                            text = "重新登录",
                                            icon = MiuixIcons.Refresh,
                                            onClick = {
                                                nav.push(Route.SmsLogin(addAccount = true, relogin = name))
                                            },
                                        ),
                                        App.GlassMenuItem(
                                            text = "删除",
                                            danger = true,
                                            onClick = { pendingDelete = account },
                                        ),
                                    ),
                                )
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(account.avatar, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            if (current) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "当前",
                                    fontSize = 10.sp,
                                    color = MiuixTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .background(MiuixTheme.colorScheme.primary, CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            "UID ${account.uid}",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                }
                }
            }
    }

    pendingDelete?.let { account ->
        ConfirmDialog(
            title = "移除账号",
            message = "只删除本地保存的登录信息，不会影响微博账号本身。",
            confirmText = "移除",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                App.store.removeAccount(account.uid)
                accounts = App.store.accounts()
                pendingDelete = null
            },
        )
    }
}

/**
 * COUI NotificationSwipeBox 的本地替代（Miuix 无对应组件）：整行左滑露出尾部操作，
 * 有设置动作时先露设置再露删除；划过半幅多时直接触发删除回调，随后行身回弹。
 * 状态只暴露 [isRevealed] 与 [close]，与原 COUI 状态的用法保持一致。
 */
private class NotificationSwipeState {
    val offset = Animatable(0f)
    var revealWidthPx: Float = 0f
    val isRevealed: Boolean
        get() = revealWidthPx > 0f && offset.value <= -revealWidthPx / 2f

    suspend fun close() {
        offset.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }
}

@Composable
private fun rememberNotificationSwipeState(): NotificationSwipeState =
    remember { NotificationSwipeState() }

@Composable
private fun NotificationSwipeBox(
    onSettings: (() -> Unit)?,
    onDismiss: () -> Unit,
    settingsIcon: ImageVector?,
    dismissIcon: ImageVector?,
    modifier: Modifier = Modifier,
    state: NotificationSwipeState,
    minHeight: Dp = 0.dp,
    containerColor: Color,
    dismissOnFullSwipe: Boolean = true,
    settingsDescription: String? = null,
    dismissDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scheme = MiuixTheme.colorScheme
    val actionWidth = 76.dp
    val actionCount = (if (onSettings != null) 1 else 0) + 1
    val revealPx = with(density) { (actionWidth * actionCount).toPx() }
    state.revealWidthPx = revealPx
    var boxWidthPx by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(CardDefaults.CornerRadius)
    Box(
        modifier
            .clip(shape)
            .background(containerColor)
            .defaultMinSize(minHeight = minHeight)
            .onSizeChanged { boxWidthPx = it.width.toFloat() },
    ) {
        Row(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
            if (onSettings != null) {
                Box(
                    Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                        .background(scheme.secondaryVariant)
                        .clickable {
                            onSettings()
                            scope.launch { state.close() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            settingsIcon ?: MiuixIcons.Refresh,
                            settingsDescription,
                            Modifier.size(20.dp),
                            tint = scheme.onSecondaryVariant,
                        )
                        if (!settingsDescription.isNullOrBlank()) {
                            Text(
                                settingsDescription,
                                fontSize = 11.sp,
                                color = scheme.onSecondaryVariant,
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(scheme.error)
                    .clickable {
                        onDismiss()
                        scope.launch { state.close() }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        dismissIcon ?: MiuixIcons.Delete,
                        dismissDescription,
                        Modifier.size(20.dp),
                        tint = scheme.onError,
                    )
                    Text(
                        "删除",
                        fontSize = 11.sp,
                        color = scheme.onError,
                    )
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(state.offset.value.roundToInt(), 0) }
                .background(containerColor)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            state.offset.snapTo(
                                (state.offset.value + delta).coerceIn(-boxWidthPx.coerceAtLeast(revealPx), 0f),
                            )
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            val off = state.offset.value
                            when {
                                dismissOnFullSwipe && boxWidthPx > 0f && off <= -boxWidthPx * 0.55f -> {
                                    onDismiss()
                                    state.close()
                                }
                                off <= -revealPx / 2f -> state.offset.animateTo(
                                    -revealPx,
                                    tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                )
                                else -> state.close()
                            }
                        }
                    },
                ),
        ) {
            content()
        }
    }
}
