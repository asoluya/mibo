package com.mibo.app.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.WindowCompat
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.mibo.app.ui.findActivity
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.isDarkTheme
import com.mibo.app.ui.rememberIsTopRoute
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mibo.app.App
import com.mibo.app.data.FollowTag
import com.mibo.app.data.FollowTags
import com.mibo.app.data.Membership
import com.mibo.app.data.NotificationTab
import com.mibo.app.data.Pic
import com.mibo.app.data.ProfileDetail
import com.mibo.app.data.ProfileGroupSelection
import com.mibo.app.data.ProfileRenameRecord
import com.mibo.app.data.RelationKind
import com.mibo.app.data.Status
import com.mibo.app.data.Updater
import com.mibo.app.data.User
import com.mibo.app.data.WEIBO_PROFILE_EDIT_URL
import com.mibo.app.data.computeProfileRenameStatus
import com.mibo.app.ui.MeIcons
import com.mibo.app.ui.NavState
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.isDarkTheme
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.LocalImageOrigins
import com.mibo.app.ui.LocalTopBarBounds
import com.mibo.app.ui.attachToRow
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.imageOrigin
import com.mibo.app.ui.Route
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.components.Avatar
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.ConfirmDialog
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.FollowTagBadge
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.MemberBadge
import com.mibo.app.ui.components.ProfileEditDialog
import com.mibo.app.ui.components.ProfileGroupDialog
import com.mibo.app.ui.components.ProfileInfoDialog
import com.mibo.app.ui.components.ProfileQrDialog
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.VerifiedBadge
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.components.fmtCount
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import com.mibo.app.ui.MiboIcons
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.PullToRefreshState
import top.yukonga.miuix.kmp.basic.RefreshState
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder
import kotlin.math.roundToInt

internal data class CoverContrast(
    val useWhiteContent: Boolean = false,
)

/**
 * 判断封面裁切绘制区中 `[bandTopPx, bandBottomPx]` 这一横条叠完顶部 30% 黑色遮罩后是否偏深：
 * 用于个人主页和“我”页未滚动离开封面时，自适应把顶部文字与状态栏图标反色为白色。
 */
internal fun isCoverCropBandDark(
    width: Int,
    height: Int,
    pixelAt: (x: Int, y: Int) -> Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    bandTopPx: Float,
    bandBottomPx: Float,
    overlayAlpha: Float = 0.3f,
): Boolean {
    if (width <= 0 || height <= 0 || containerWidthPx <= 0f || containerHeightPx <= 0f) return false
    val clampedTop = bandTopPx.coerceIn(0f, containerHeightPx)
    val clampedBottom = bandBottomPx.coerceIn(0f, containerHeightPx)
    if (clampedBottom <= clampedTop) return false

    val scale = maxOf(containerWidthPx / width.toFloat(), containerHeightPx / height.toFloat())
    val drawnW = width * scale
    val drawnH = height * scale
    val offsetX = (containerWidthPx - drawnW) / 2f
    val offsetY = (containerHeightPx - drawnH) / 2f
    val factor = (1f - overlayAlpha).coerceIn(0f, 1f).toDouble()

    val cols = 12
    val rows = 6
    var sum = 0.0
    var count = 0
    for (rIdx in 0 until rows) {
        val sy = clampedTop + (clampedBottom - clampedTop) * ((rIdx + 0.5f) / rows)
        val py = ((sy - offsetY) / scale).toInt().coerceIn(0, height - 1)
        for (cIdx in 0 until cols) {
            val sx = containerWidthPx * ((cIdx + 0.5f) / cols)
            val px = ((sx - offsetX) / scale).toInt().coerceIn(0, width - 1)
            val argb = pixelAt(px, py)
            val red = ((argb ushr 16) and 0xFF) * factor
            val green = ((argb ushr 8) and 0xFF) * factor
            val blue = (argb and 0xFF) * factor
            sum += (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
            count++
        }
    }
    return count > 0 && (sum / count) < 0.48
}

@Composable
internal fun rememberCoverContrast(
    coverUrl: String?,
    topBarHeight: Dp,
    active: Boolean,
    manageStatusBar: Boolean,
): CoverContrast {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val statusBarBottomPx = WindowInsets.statusBars.getTop(density).toFloat()
    val containerWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val topBarBottomPx = with(density) { topBarHeight.toPx() }
    val coverHeightPx = with(density) { (topBarHeight + ProfileAvatarRing * 1.5f).toPx() }

    val sampled by produceState(
        initialValue = CoverContrast(),
        coverUrl,
        containerWidthPx,
        coverHeightPx,
        statusBarBottomPx,
        topBarBottomPx,
    ) {
        if (coverUrl.isNullOrBlank()) {
            value = CoverContrast()
            return@produceState
        }
        value = runCatching {
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .size(144)
                .build()
            val raw = ((context.imageLoader.execute(request) as? SuccessResult)?.image as? BitmapImage)
                ?.bitmap ?: return@runCatching CoverContrast()
            val bitmap = if (raw.config == Bitmap.Config.HARDWARE) {
                raw.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching CoverContrast()
            } else {
                raw
            }
            val bw = bitmap.width
            val bh = bitmap.height
            // 以“主页”标题所在顶栏条带为准：状态栏与标题颜色统一受同一个判定控制，同步变化。
            val bandTop = if (topBarBottomPx > statusBarBottomPx) statusBarBottomPx else 0f
            val bandDark = isCoverCropBandDark(
                width = bw,
                height = bh,
                pixelAt = { x, y -> bitmap.getPixel(x, y) },
                containerWidthPx = containerWidthPx,
                containerHeightPx = coverHeightPx,
                bandTopPx = bandTop,
                bandBottomPx = topBarBottomPx,
            )
            CoverContrast(useWhiteContent = bandDark)
        }.getOrDefault(CoverContrast())
    }

    val effective = if (active && !coverUrl.isNullOrBlank()) sampled else CoverContrast()
    val darkTheme = isDarkTheme()
    val window = remember(context) { context.findActivity()?.window }
    val controller = remember(window) {
        window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }
    val lightStatusBars = !(darkTheme || effective.useWhiteContent)
    LaunchedEffect(controller, manageStatusBar, lightStatusBars) {
        if (manageStatusBar) {
            controller?.isAppearanceLightStatusBars = lightStatusBars
        }
    }
    DisposableEffect(controller, manageStatusBar, darkTheme) {
        onDispose {
            if (manageStatusBar) {
                controller?.isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    return effective
}

@Composable
fun ProfileScreen(nav: NavState, uid: String?) {
    val key = uid.orEmpty()
    val tag = FollowTags[key]
    // 顶栏的「更多」菜单要用到头像和昵称（二维码弹窗），而资料本身在内容区里加载，
    // 这里只收一份镜像，不参与页面布局。
    var profileUser by remember(uid) { mutableStateOf<User?>(null) }
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var activeCoverUrl by remember(uid) { mutableStateOf<String?>(null) }
    val scrollAlpha = remember(listState, density) {
        {
            val scrolled = if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE
            else listState.firstVisibleItemScrollOffset.toFloat()
            (scrolled / with(density) { 48.dp.toPx() }).coerceIn(0f, 1f)
        }
    }
    val scrolledPastCover by remember(scrollAlpha) {
        derivedStateOf { scrollAlpha() >= 0.45f }
    }
    val isTopRoute = rememberIsTopRoute(nav)
    val coverContrast = rememberCoverContrast(
        coverUrl = activeCoverUrl,
        topBarHeight = topBarHeight,
        active = !scrolledPastCover && isTopRoute,
        manageStatusBar = isTopRoute,
    )
    val titleColor = if (coverContrast.useWhiteContent) Color.White else MiuixTheme.colorScheme.onSurface
    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        // 内容铺到顶栏底下，滚动后顶栏的渐变玻璃才渐入；未滑动时完全透明不模糊封面。
        Box(Modifier.fillMaxSize().layerBackdropIf(blurEffectsEnabled(), backdrop)) {
            CompositionLocalProvider(LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null) {
                ProfileContent(
                    nav = nav,
                    uid = uid,
                    modifier = Modifier.fillMaxSize(),
                    topInset = topBarHeight,
                    listState = listState,
                    onActiveCover = { activeCoverUrl = it },
                    onUser = { profileUser = it },
                )
            }
        }
        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageContainerColor(),
                    scrollFraction = scrollAlpha,
                ) {
                    SmallTopAppBar(
                        title = "主页",
                        titleColor = titleColor,
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                        actions = {
                            ProfileMoreAction(
                                tag = tag,
                                uid = uid.orEmpty().ifBlank { App.repo.session?.uid.orEmpty() },
                                user = profileUser,
                                nav = nav,
                                onSelect = { picked -> FollowTags.set(key, picked) },
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * 主页右上角的三点菜单：给这个人挂一个关注标记，三种互斥，再点一次已选中的那项就取消。
 * 用 COUI 的 ListPopup 而不是 Material 的 DropdownMenu，和首页筛选菜单同一套观感。
 */
/**
 * 主页右上角的三点菜单。
 *
 * 第一段是三种互斥的关注标记（再点一次取消）；后面依次是关注分组、只读资料、举报与拉黑。
 * 自己的主页上不出现分组 / 举报 / 拉黑这三项，纯本地模式也藏起来（没有账号可写）。
 */
private val ProfileRefreshTexts = listOf("下拉刷新", "松开刷新", "刷新中…", "刷新完成")

@Composable
private fun ProfileMoreAction(
    tag: FollowTag?,
    uid: String,
    user: User?,
    nav: NavState,
    onSelect: (FollowTag?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var groupSelection by remember(uid) { mutableStateOf<ProfileGroupSelection?>(null) }
    var loadingGroups by remember(uid) { mutableStateOf(false) }
    var infoOpen by remember(uid) { mutableStateOf(false) }
    var info by remember(uid) { mutableStateOf<ProfileDetail?>(null) }
    var infoLoading by remember(uid) { mutableStateOf(false) }
    var qrOpen by remember(uid) { mutableStateOf(false) }
    var blockOpen by remember(uid) { mutableStateOf(false) }
    var editOpen by remember(uid) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val guest = App.repo.guest
    val self = uid.isNotBlank() && uid == App.repo.session?.uid
    val canManage = uid.isNotBlank() && !self && !guest
    val profileUrl = "https://weibo.com/u/$uid"

    fun copyLink() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("url", profileUrl))
        App.toast("已复制链接")
    }

    LaunchedEffect(infoOpen, uid) {
        if (!infoOpen) return@LaunchedEffect
        info = null
        infoLoading = true
        info = runCatching { App.repo.profileDetail(uid) }.getOrNull()
        infoLoading = false
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("profile_more"),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.semantics { contentDescription = "更多" },
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(LocalContentColor.current)
                    )
                }
            }
        }
        OverlayCascadingListPopup(
            show = expanded,
            entries = buildList {
                add(
                    DropdownEntry(
                        items = buildList {
                            FollowTag.entries.forEach { item ->
                                add(
                                    DropdownItem(
                                        text = item.menuLabel,
                                        selected = item == tag,
                                        onClick = { onSelect(if (item == tag) null else item) },
                                    )
                                )
                            }
                            if (canManage) {
                                add(
                                    DropdownItem(
                                        text = "编辑关注分组",
                                        enabled = !loadingGroups,
                                        onClick = {
                                            if (!loadingGroups) {
                                                if (user?.following == false) {
                                                    // 服务端对未关注的人只会静默吞掉，先在本地拦下来。
                                                    App.toast("先关注对方才能设置关注分组")
                                                } else {
                                                    scope.launch {
                                                        loadingGroups = true
                                                        runCatching { App.repo.profileGroups(uid) }
                                                            .onSuccess { groupSelection = it }
                                                            .onFailure { App.toast(it.message ?: "分组加载失败") }
                                                        loadingGroups = false
                                                    }
                                                }
                                            }
                                        },
                                    )
                                )
                            }
                        },
                    )
                )
                add(
                    DropdownEntry(
                        items = buildList {
                            if (self) {
                                add(DropdownItem(text = "设置", onClick = { nav.push(Route.Settings) }))
                                add(DropdownItem(text = "编辑资料", onClick = { editOpen = true }))
                                add(
                                    DropdownItem(
                                        text = "网页端编辑资料",
                                        onClick = { nav.push(Route.Web(WEIBO_PROFILE_EDIT_URL, "编辑资料")) },
                                    ),
                                )
                            }
                            add(DropdownItem(text = "基本资料", onClick = { infoOpen = true }))
                            add(DropdownItem(text = "复制链接", onClick = { copyLink() }))
                            add(DropdownItem(text = "二维码", onClick = { qrOpen = true }))
                        },
                    )
                )
                if (canManage) {
                    add(
                        DropdownEntry(
                            items = listOf(
                                DropdownItem(
                                    text = "举报",
                                    onClick = {
                                        nav.push(Route.Web(reportUrl(uid), "举报"))
                                    },
                                ),
                                DropdownItem(
                                    text = "加入黑名单",
                                    onClick = { blockOpen = true },
                                ),
                            ),
                        )
                    )
                }
            },
            popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
            onDismissRequest = { expanded = false },
        )
    }

    var deletedGroupIds by remember(uid) { mutableStateOf<Set<String>>(emptySet()) }
    groupSelection?.let { selection ->
        ProfileGroupDialog(
            selection = selection,
            onCreate = { App.repo.createProfileGroup(it) },
            onRename = { group, name -> App.repo.renameProfileGroup(group.gid, name) },
            onDelete = { group ->
                App.repo.deleteProfileGroup(group.gid)
                deletedGroupIds = deletedGroupIds + group.gid
            },
            onReorder = { App.repo.reorderProfileGroups(it) },
            onDismiss = {
                groupSelection = null
                deletedGroupIds = emptySet()
            },
            onConfirm = { selected ->
                val deleted = deletedGroupIds
                groupSelection = null
                deletedGroupIds = emptySet()
                val effectiveOrigin = selection.assigned.filterNot { it in deleted }.toSet()
                val effectiveSelected = selected.filterNot { it in deleted }.toSet()
                if (effectiveSelected != effectiveOrigin) {
                    scope.launch {
                        runCatching {
                            App.repo.setProfileGroups(uid, effectiveSelected.toList(), effectiveOrigin.toList(), deleted)
                        }
                            .onSuccess { App.toast("已更新关注分组") }
                            .onFailure { App.toast(it.message ?: "设置分组失败") }
                    }
                }
            },
        )
    }

    if (infoOpen) {
        ProfileInfoDialog(
            user = user,
            detail = info,
            loading = infoLoading,
            onEdit = if (self) ({ editOpen = true }) else null,
            onDismiss = { infoOpen = false },
        )
    }

    if (editOpen) {
        ProfileEditDialog(
            user = user,
            onOpenWeb = { nav.push(Route.Web(WEIBO_PROFILE_EDIT_URL, "编辑资料")) },
            onDismiss = { editOpen = false },
        )
    }

    if (qrOpen) {
        ProfileQrDialog(user = user, url = profileUrl, onDismiss = { qrOpen = false })
    }

    if (blockOpen) {
        ConfirmDialog(
            title = "加入黑名单",
            message = "加入后，对方将无法查看你的微博、与你互动或关注你。",
            confirmText = "加入黑名单",
            onDismiss = { blockOpen = false },
            onConfirm = {
                blockOpen = false
                scope.launch {
                    runCatching { App.repo.blockUser(uid) }
                        .onSuccess {
                            App.toast("已加入黑名单")
                            App.notifyFollowChanged()
                        }
                        .onFailure { App.toast(it.message ?: "操作失败") }
                }
            },
        )
    }
}

/** 网页版举报入口：`type=3` 是举报用户，`url` 指向他的主页。 */
private fun reportUrl(uid: String): String {
    val target = URLEncoder.encode("/u/$uid", "UTF-8")
    return "https://service.account.weibo.com/reportspam" +
        "?rid=$uid&from=10106&type=3&url=$target&bottomnav=1&wvr=5"
}

/**
 * 个人主页内容状态，供主壳在切换底部 Tab 时保留已加载的资料与列表，避免切回时重置。
 */
@Stable
class ProfileTabState {
    var user by mutableStateOf<User?>(null)
    val items = mutableStateListOf<Status>()
    var sinceId by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var end by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var resolvedUid by mutableStateOf<String?>(null)
    var loadedUid by mutableStateOf<String?>(null)
}

@Composable
fun ProfileContent(
    nav: NavState,
    uid: String?,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    extraBottom: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    state: ProfileTabState? = null,
    onActiveCover: (String?) -> Unit = {},
    onUser: (User?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val tabState = state ?: remember(uid) { ProfileTabState() }
    val pullState = rememberPullToRefreshState()

    // showRefresh 只给用户主动刷新用：进入页面时的静默加载不驱动下拉动画。
    suspend fun loadStatuses(reset: Boolean, force: Boolean = false, showRefresh: Boolean = false) {
        val target = tabState.resolvedUid ?: return
        if (tabState.loading) return
        tabState.loading = true
        if (reset && showRefresh) tabState.refreshing = true
        try {
            val page = App.repo.userStatuses(target, if (reset) null else tabState.sinceId, force = force)
            if (reset) {
                tabState.items.clear()
                tabState.end = false
            }
            val known = tabState.items.mapTo(HashSet()) { it.id }
            page.items.forEach { if (known.add(it.id)) tabState.items.add(it) }
            tabState.sinceId = page.sinceId
            if (page.items.isEmpty()) tabState.end = true
            tabState.error = null
            tabState.loadedUid = target
        } catch (t: Throwable) {
            tabState.error = t.message ?: "加载失败"
        } finally {
            tabState.loading = false
            tabState.refreshing = false
        }
    }

    SyncDeletedStatuses(tabState.items)
    LaunchedEffect(uid, tabState) {
        val target = uid ?: runCatching { App.repo.ownUid() }.getOrNull()
        if (target.isNullOrBlank()) {
            tabState.error = "未登录"
            return@LaunchedEffect
        }
        tabState.resolvedUid = target
        if (tabState.loadedUid == target && tabState.user != null) {
            return@LaunchedEffect
        }
        tabState.user = runCatching { App.repo.profile(target) }.getOrNull()
        loadStatuses(true, force = false)
    }

    // 顶栏菜单要用头像和昵称，资料加载完就往上同步一份。
    LaunchedEffect(tabState.user) { onUser(tabState.user) }
    LaunchedEffect(tabState) {
        App.profileUpdatedEvents.collect { updated ->
            val target = tabState.resolvedUid ?: tabState.user?.id
            if (!target.isNullOrBlank() && updated.id == target) {
                tabState.user = updated
            }
        }
    }

    // 服务端说这个人被设了特别关注、本机又没记过标记，就补一次初值；之后一律以本机为准。
    LaunchedEffect(tabState.user?.id, tabState.user?.specialFollow) {
        val loaded = tabState.user ?: return@LaunchedEffect
        if (loaded.specialFollow && !FollowTags.has(loaded.id)) {
            FollowTags.set(loaded.id, FollowTag.Special)
        }
    }

    LaunchedEffect(listState, tabState.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= tabState.items.size && tabState.items.isNotEmpty() && !tabState.end) {
                    loadStatuses(false)
                }
            }
    }

    PullToRefresh(
        isRefreshing = tabState.refreshing,
        onRefresh = {
            scope.launch {
                tabState.resolvedUid?.let { tabState.user = runCatching { App.repo.profile(it, force = true) }.getOrNull() }
                loadStatuses(true, force = true, showRefresh = true)
            }
        },
        modifier = modifier,
        pullToRefreshState = pullState,
        contentPadding = PaddingValues(top = topInset),
        // 下拉时不让整页往下走，而是把封面往下拉长；刷新提示由封面自己显示，
        // 所以把 Miuix 自带指示器设为透明、文案置空（Miuix 没有 colors 参数可关）。
        color = Color.Transparent,
        refreshTexts = listOf("", "", "", ""),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // 先反向抵消 FullPagePullToRefresh 的下移，再套 overScrollVertical() 的 clipToBounds()，
                // 避免下拉时把封面顶部裁掉只剩固定高度的窄条。
                .offset { IntOffset(0, -pullState.dragOffset.roundToInt()) }
                .scrollEndHaptic()
                .overScrollVertical(),
            // 封面从屏幕最顶上铺起，顶栏的玻璃直接压在封面上；顶栏的高度由封面自己让出来。
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 16.dp + navBottomPadding() + extraBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val currentUser = tabState.user
                val tag = FollowTags[currentUser?.id ?: uid]
                val targetId = currentUser?.id ?: tabState.resolvedUid
                val isSelf = !App.repo.guest && !targetId.isNullOrBlank() && targetId == App.repo.session?.uid
                ProfileHeader(
                    user = currentUser,
                    tag = tag,
                    topInset = topInset,
                    pullState = pullState,
                    editableProfile = isSelf,
                    onActiveCover = onActiveCover,
                    onCover = { pics, i -> nav.push(Route.Images(pics, i)) },
                    onAvatar = { avatarUrl ->
                        nav.push(
                            Route.Images(
                                pics = listOf(Pic(thumb = avatarUrl, large = avatarUrl)),
                                index = 0,
                                editableAvatar = isSelf,
                            ),
                        )
                    },
                    onRename = if (isSelf) {
                        { newName ->
                            runCatching {
                                App.repo.updateProfileBasic(
                                    screenName = newName,
                                    membership = currentUser?.membership ?: Membership.None,
                                )
                            }
                        }
                    } else null,
                    onRelations = { kind ->
                        val target = currentUser?.id ?: tabState.resolvedUid
                        if (!target.isNullOrBlank()) nav.push(Route.Relations(target, kind))
                    },
                    followLabel = when {
                        App.repo.guest -> if (tag == FollowTag.Local) "已本地关注" else "本地关注"
                        currentUser?.following == true -> "已关注"
                        else -> "关注"
                    },
                    onFollow = currentUser?.let { target ->
                        if (App.repo.guest) {
                            // 纯本地模式没有账号可关注，按钮直接切本机的「本地关注」标记。
                            {
                                val on = FollowTags[target.id] == FollowTag.Local
                                FollowTags.set(target.id, if (on) null else FollowTag.Local)
                                App.notifyFollowChanged()
                                App.toast(if (on) "已取消本地关注" else "已加入本地关注")
                            }
                        } else if (target.id == App.repo.session?.uid) {
                            null
                        } else {
                            {
                                scope.launch {
                                    runCatching { App.repo.follow(target.id, !target.following) }
                                        .onSuccess {
                                            tabState.user = target.copy(
                                                following = !target.following,
                                                followers = (target.followers +
                                                    if (target.following) -1 else 1).coerceAtLeast(0),
                                            )
                                            App.notifyFollowChanged()
                                            App.toast(if (target.following) "已取消关注" else "已关注")
                                        }
                                        .onFailure { App.toast(it.message ?: "操作失败") }
                                }
                            }
                        }
                    },
                )
            }
            if (tabState.items.isEmpty()) {
                item {
                    when {
                        tabState.loading || tabState.refreshing -> LoadingState()
                        tabState.error != null -> ErrorState(tabState.error!!) {
                            scope.launch { loadStatuses(true, force = true, showRefresh = true) }
                        }
                        else -> EmptyState("还没有微博")
                    }
                }
            }
            itemsIndexed(tabState.items, key = { _, s -> s.id }) { index, status ->
                val origin = rememberExpandOrigin()
                val topBar = LocalTopBarBounds.current
                SideEffect {
                    origin.listState = listState
                    origin.rowKey = status.id
                    origin.topBar = topBar
                }
                StatusCard(
                    status = status,
                    modifier = Modifier.padding(horizontal = 12.dp).expandOrigin(origin),
                    onOpen = { nav.push(Route.Detail(status.id, status.mid), origin) },
                    onUser = { nav.push(Route.Profile(it.id)) },
                    onImage = { pics, i -> nav.push(Route.Images(pics, i)) },
                    onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive)) },
                    onLike = {
                        scope.launch {
                            val updated = App.toggleStatusLike(status) ?: return@launch
                            val currentIdx = tabState.items.indexOfFirst { it.id == status.id }
                            if (currentIdx >= 0) tabState.items[currentIdx] = updated
                        }
                    },
                    onRepost = { nav.push(Route.Detail(status.id, status.mid), origin) },
                    onComment = { nav.push(Route.Detail(status.id, status.mid), origin) },
                    onOpenQuoted = { quoted, quotedOrigin ->
                        quotedOrigin.attachToRow(listState, status.id, topBar)
                        nav.push(Route.Detail(quoted.id, quoted.mid), quotedOrigin)
                    },
                )
            }
            if (tabState.items.isNotEmpty()) {
                item {
                    if (tabState.loading) LoadingState()
                    else if (tabState.end) Text(
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

/**
 * 「我」页面的一级功能与设置中心形态：保留顶部个人资料头，下方聚合我的微博、收藏、
 * 赞过/收到的赞、发出/收到的评论、本机记录以及跳转各个设置分组二级页的入口。
 */
@Composable
fun ProfileHubContent(
    nav: NavState,
    guest: Boolean,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    extraBottom: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    state: ProfileTabState = remember { ProfileTabState() },
    onActiveCover: (String?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val scheme = MiuixTheme.colorScheme
    var showRewardCode by remember { mutableStateOf(false) }

    LaunchedEffect(guest, state) {
        if (guest) {
            onActiveCover(null)
            return@LaunchedEffect
        }
        val target = runCatching { App.repo.ownUid() }.getOrNull()
        if (target.isNullOrBlank()) return@LaunchedEffect
        state.resolvedUid = target
        if (state.user == null || state.loadedUid != target) {
            state.user = runCatching { App.repo.profile(target) }.getOrNull()
            if (state.user != null) state.loadedUid = target
        }
    }
    LaunchedEffect(guest, state) {
        if (guest) return@LaunchedEffect
        App.profileUpdatedEvents.collect { updated ->
            val target = state.resolvedUid ?: state.user?.id
            if (target.isNullOrBlank() || updated.id == target) {
                state.user = updated
            }
        }
    }

    val content: @Composable () -> Unit = {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                // 先反向抵消 FullPagePullToRefresh 的下移，再套 overScrollVertical() 的 clipToBounds()，
                // 避免下拉时把封面顶部裁掉只剩固定高度的窄条。
                .let {
                    if (guest) it else it.offset { IntOffset(0, -pullState.dragOffset.roundToInt()) }
                }
                .scrollEndHaptic()
                .overScrollVertical(),
            contentPadding = PaddingValues(
                top = if (guest) topInset + 14.dp else 0.dp,
                bottom = 16.dp + navBottomPadding() + extraBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (guest) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text("纯本地模式", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "未登录账号，可通过底部入口、草稿、收藏与设置分组管理本机内容。",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = scheme.onSurfaceSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { nav.push(Route.SmsLogin()) },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth().testTag("local_login"),
                        ) {
                            Text("登录微博账号")
                        }
                    }
                }
            } else {
                item {
                    val currentUser = state.user
                    val targetUid = currentUser?.id ?: state.resolvedUid
                    val isSelf = !guest && !targetUid.isNullOrBlank()
                    ProfileHeader(
                        user = currentUser,
                        tag = null,
                        topInset = topInset,
                        pullState = pullState,
                        editableProfile = isSelf,
                        showIdentityBand = false,
                        onActiveCover = onActiveCover,
                        onCover = { pics, i -> nav.push(Route.Images(pics, i)) },
                        onAvatar = { avatarUrl ->
                            nav.push(
                                Route.Images(
                                    pics = listOf(Pic(thumb = avatarUrl, large = avatarUrl)),
                                    index = 0,
                                    editableAvatar = isSelf,
                                ),
                            )
                        },
                        onRename = if (isSelf) {
                            { newName ->
                                runCatching {
                                    App.repo.updateProfileBasic(
                                        screenName = newName,
                                        membership = currentUser?.membership ?: Membership.None,
                                    )
                                }
                            }
                        } else null,
                        onStatusesClick = {
                            if (!targetUid.isNullOrBlank()) nav.push(Route.Profile(targetUid))
                        },
                        onRelations = { kind ->
                            if (!targetUid.isNullOrBlank()) nav.push(Route.Relations(targetUid, kind))
                        },
                        followLabel = "",
                        onFollow = null,
                    )
                }

                item {
                    val currentUser = state.user
                    val targetUid = currentUser?.id ?: state.resolvedUid
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        cornerRadius = CardDefaults.CornerRadius,
                        insideMargin = PaddingValues(0.dp),
                    ) {
                        ArrowPreference(
                            title = "我的微博",
                            startAction = { HubRowIcon(MeIcons.Statuses) },
                            modifier = Modifier.testTag("hub_my_statuses"),
                            onClick = {
                                if (!targetUid.isNullOrBlank()) nav.push(Route.Profile(targetUid))
                            },
                        )
                        ArrowPreference(
                            title = "我的收藏",
                            startAction = { HubRowIcon(MeIcons.Favorites) },
                            modifier = Modifier.testTag("hub_favorites"),
                            onClick = { nav.push(Route.Favorites) },
                        )
                        ArrowPreference(
                            title = "我赞过的",
                            startAction = { HubRowIcon(MeIcons.MyLikes) },
                            modifier = Modifier.testTag("hub_my_likes"),
                            onClick = { nav.push(Route.MyLikes) },
                        )
                        ArrowPreference(
                            title = "收到的赞",
                            startAction = { HubRowIcon(MeIcons.ReceivedLikes) },
                            modifier = Modifier.testTag("hub_received_likes"),
                            onClick = { nav.push(Route.Notifications(NotificationTab.Like)) },
                        )
                        ArrowPreference(
                            title = "收到的评论",
                            startAction = { HubRowIcon(MeIcons.ReceivedComments) },
                            modifier = Modifier.testTag("hub_received_comments"),
                            onClick = { nav.push(Route.Notifications(NotificationTab.Comment)) },
                        )
                        ArrowPreference(
                            title = "发出的评论",
                            startAction = { HubRowIcon(MeIcons.SentComments) },
                            modifier = Modifier.testTag("hub_sent_comments"),
                            onClick = { nav.push(Route.Notifications(NotificationTab.SentComment)) },
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    cornerRadius = CardDefaults.CornerRadius,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    if (guest) {
                        ArrowPreference(
                            title = "本地收藏",
                            startAction = { HubRowIcon(MeIcons.Favorites) },
                            modifier = Modifier.testTag("local_favorites"),
                            onClick = { nav.push(Route.Favorites) },
                        )
                    }
                    ArrowPreference(
                        title = "打赏",
                        startAction = { HubRowIcon(MiboIcons.PraiseThumb) },
                        modifier = Modifier.testTag("hub_reward_code"),
                        onClick = { showRewardCode = true },
                    )
                    ArrowPreference(
                        title = "草稿箱",
                        startAction = { HubRowIcon(MeIcons.Drafts) },
                        modifier = Modifier.testTag("hub_drafts"),
                        onClick = { nav.push(Route.Drafts) },
                    )
                    ArrowPreference(
                        title = "浏览历史",
                        startAction = { HubRowIcon(MeIcons.History) },
                        modifier = Modifier.testTag("hub_history"),
                        onClick = { nav.push(Route.History) },
                    )
                    ArrowPreference(
                        title = "话题 / 超话",
                        startAction = { HubRowIcon(MeIcons.Topic) },
                        modifier = Modifier.testTag("hub_topic"),
                        onClick = { nav.push(Route.Topic()) },
                    )
                    if (!guest) {
                        ArrowPreference(
                            title = "附近",
                            startAction = { HubRowIcon(MeIcons.Nearby) },
                            modifier = Modifier.testTag("hub_nearby"),
                            onClick = { nav.push(Route.Nearby) },
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    cornerRadius = CardDefaults.CornerRadius,
                    insideMargin = PaddingValues(0.dp),
                ) {
                    val groups = SettingsGroup.entries
                    groups.forEachIndexed { index, group ->
                        ArrowPreference(
                            title = group.title,
                            startAction = { HubRowIcon(group.hubIcon()) },
                            modifier = Modifier.testTag("hub_settings_${group.name.lowercase()}"),
                            onClick = { nav.push(Route.SettingsDetail(group)) },
                        )
                    }
                }
            }
        }
    }

    if (guest) {
        Box(modifier) { content() }
    } else {
        PullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = {
                scope.launch {
                    state.refreshing = true
                    try {
                        val target = state.resolvedUid ?: runCatching { App.repo.ownUid() }.getOrNull()
                        if (!target.isNullOrBlank()) {
                            state.resolvedUid = target
                            state.user = runCatching { App.repo.profile(target, force = true) }.getOrNull()
                            state.loadedUid = target
                        }
                    } finally {
                        state.refreshing = false
                    }
                }
            },
            modifier = modifier,
            pullToRefreshState = pullState,
            contentPadding = PaddingValues(top = topInset),
            // 同上：刷新提示由封面显示，Miuix 自带指示器透明化、文案置空。
            color = Color.Transparent,
            refreshTexts = listOf("", "", "", ""),
        ) {
            content()
        }
    }

    if (showRewardCode) {
        RewardCodeDialog(onDismiss = { showRewardCode = false })
    }
}

@Composable
private fun HubRowIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MiuixTheme.colorScheme.primary,
    )
}

private fun SettingsGroup.hubIcon(): ImageVector = when (this) {
    SettingsGroup.Display -> MeIcons.DisplaySettings
    SettingsGroup.Feed -> MeIcons.FeedSettings
    SettingsGroup.AccountAndContent -> MeIcons.AccountSettings
    SettingsGroup.Other -> MeIcons.OtherSettings
}

private val ProfileAvatarRing = 68.dp

/** 封面下沿那道分隔线的粗细。 */
private val CoverDivider = 0.5.dp

/**
 * 关注按钮：顶栏返回键 / 三点键那颗 [HighlightedIconButton] 的材质拉成胶囊 —— 同样的半透明
 * 径向填充、材质描边和按下回弹缩放，只是形状换成能放下文字的胶囊。
 */
@Composable
private fun FollowCapsule(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Miuix 没有 HighlightedIconButtonDefaults 的按压弹簧/填充常量，以下数值按原视觉近似：
    // 按压缩放 0.94、填充提亮 0.12、内容提亮 0.15、填充不透明度 0.85、边缘停点 0.75。
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "followPress",
    )
    val brighten = 0.12f * press
    val capsuleShape = remember { RoundedCornerShape(50) }
    val fillCenter = if (isDark) scheme.surfaceContainerHigh else scheme.surfaceContainer
    val fillEdge = if (isDark) scheme.surfaceContainer else scheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .height(36.dp)
            .graphicsLayer {
                val scale = 1f - (1f - 0.94f) * press
                scaleX = scale
                scaleY = scale
            }
            .clip(capsuleShape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to lerp(fillCenter, Color.White, brighten).copy(alpha = 0.85f),
                            0.75f to lerp(fillEdge, Color.White, brighten).copy(alpha = 0.85f),
                        ),
                        center = center,
                        radius = size.maxDimension / 2f,
                    ),
                )
            }
            // 原 materialShadowEdge 的材质描边改为普通 1dp 描边近似。
            .border(1.dp, scheme.outline.copy(alpha = 0.5f), capsuleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = lerp(scheme.onBackground, Color.White, 0.15f * press),
            maxLines = 1,
        )
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    onFollow: (() -> Unit)?,
    followLabel: String,
    pullState: PullToRefreshState,
    tag: FollowTag? = null,
    /** 顶栏（含状态栏）的高度：封面顶到屏幕最上沿，多出这一截藏在顶栏玻璃下面。 */
    topInset: Dp = 0.dp,
    editableProfile: Boolean = false,
    /** 「我的」页传 false：封面/头像/昵称整块头部区域不渲染，页面从统计卡片开始。 */
    showIdentityBand: Boolean = true,
    onActiveCover: (String?) -> Unit = {},
    onCover: (List<Pic>, Int) -> Unit = { _, _ -> },
    onAvatar: ((String) -> Unit)? = null,
    onRename: (suspend (String) -> Result<User>)? = null,
    onStatusesClick: (() -> Unit)? = null,
    onRelations: (RelationKind) -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val covers = user?.covers.orEmpty()
    // 封面图不再展示（用户要求去掉头像后的图片）：头像行直接落在渐变底上，
    // hasCover 恒假，封面 pager、压暗层与白字逻辑整体停用，顶栏也不再采样封面对比度。
    val hasCover = false
    val onCoverColor = if (hasCover) Color.White else scheme.onSurface
    val pagerState = rememberPagerState { covers.size }
    LaunchedEffect(user?.id) {
        onActiveCover(null)
    }
    // 封面在列表里只露中间一条，真实宽高比要等图片解码完才知道。记下来交给看图页，
    // 点开时才能从这一条长出来，而不是从整张图的居中位置凭空出现。
    val coverAspects = remember(covers) { mutableStateMapOf<Int, Float>() }
    Column(Modifier.fillMaxWidth()) {
        if (showIdentityBand) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 顶栏下面露出的封面只截中间一条，高度取头像的 1.5 倍，点开再看整张；
                // 往上再多出顶栏那一截垫在玻璃底下；下拉多少就往下拉长多少，
                // 封面是居中裁切，拉长就是把上下被裁掉的部分逐渐露出来。高度在布局阶段读，不逐帧重组。
                .layout { measurable, constraints ->
                    val height = (topInset + ProfileAvatarRing * 1.5f).roundToPx() +
                        pullState.dragOffset.roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
        ) {
            // 封面下沿是一条直线，再压一道分隔线把边界收实：封面很浅或白底时不至于像没铺满。
            // 上沿在顶栏玻璃底下，由顶栏的渐变接住。
            val dividerColor = scheme.dividerLine
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val stroke = CoverDivider.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height - stroke / 2),
                            end = Offset(size.width, size.height - stroke / 2),
                            strokeWidth = stroke,
                        )
                    }
                    .background(
                        Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceContainer))
                    ),
            ) {
            if (hasCover) {
                val origins = LocalImageOrigins.current
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().testTag("profile_cover"),
                    userScrollEnabled = covers.size > 1,
                ) { page ->
                    AsyncImage(
                        model = covers[page],
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            origins.putCacheKey(covers[page], state.result.memoryCacheKey)
                            val size = state.painter.intrinsicSize
                            if (size.width > 0f && size.height > 0f) {
                                coverAspects[page] = size.width / size.height
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .imageOrigin(covers[page], 0.dp, aspect = coverAspects[page], model = covers[page])
                            .clickable {
                                onCover(covers.map { Pic(thumb = it, large = it) }, page)
                            },
                    )
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }
            }
            if (hasCover) {
                if (covers.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        repeat(covers.size) { i ->
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(
                                            alpha = if (i == pagerState.currentPage) 0.95f else 0.45f
                                        )
                                    )
                            )
                        }
                    }
                }
            }
            val hint = when (pullState.refreshState) {
                RefreshState.Pulling -> ProfileRefreshTexts[0]
                RefreshState.ThresholdReached -> ProfileRefreshTexts[1]
                RefreshState.Refreshing -> ProfileRefreshTexts[2]
                RefreshState.RefreshComplete -> ProfileRefreshTexts[3]
                else -> ""
            }
            if (hint.isNotEmpty()) {
                Text(
                    hint,
                    fontSize = 12.sp,
                    color = onCoverColor.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset + 12.dp)
                        .graphicsLayer { alpha = pullState.pullProgress },
                )
            }
            // 头像一行贴着封面底部：不拉时上下各留 17dp 正好居中，拉长时停在原位，上方露出封面。
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = (ProfileAvatarRing * 0.5f) / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(ProfileAvatarRing)) {
                    val avatarUrl = user?.avatar.orEmpty()
                    val canOpenAvatar = avatarUrl.isNotBlank() && onAvatar != null
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(2.dp)
                            .then(
                                if (canOpenAvatar) {
                                    Modifier
                                        .imageOrigin(avatarUrl, 32.dp, aspect = 1f, model = avatarUrl)
                                        .clickable(role = Role.Image) { onAvatar.invoke(avatarUrl) }
                                        .testTag("profile_avatar")
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Avatar(user?.avatar, 64.dp)
                    }
                    if (user != null) {
                        VerifiedBadge(
                            kind = user.verifiedKind,
                            size = 20.dp,
                            modifier = Modifier.align(Alignment.BottomEnd),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                val renameScope = rememberCoroutineScope()
                val keyboardController = LocalSoftwareKeyboardController.current
                var editingName by remember(user?.id) { mutableStateOf(false) }
                var originalName by remember(user?.id) { mutableStateOf("") }
                var nameDraft by remember(user?.id) { mutableStateOf(TextFieldValue("")) }
                var savingName by remember(user?.id) { mutableStateOf(false) }
                var pendingRenameConfirm by remember(user?.id) { mutableStateOf<String?>(null) }
                val nameFocus = remember { FocusRequester() }
                val editProgress by animateFloatAsState(
                    targetValue = if (editingName) 1f else 0f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    label = "profileNameEdit",
                )
                val nameFontSize = (20f - 1.5f * editProgress).sp
                LaunchedEffect(editingName) {
                    if (editingName) {
                        nameFocus.requestFocus()
                        runCatching { App.repo.syncServerRenameHint() }
                    }
                }
                BackHandler(enabled = editingName && !savingName) {
                    nameDraft = TextFieldValue(originalName, TextRange(originalName.length))
                    editingName = false
                }
                fun commitRename() {
                    val trimmed = nameDraft.text.trim()
                    if (trimmed.isEmpty()) {
                        App.toast("昵称不能为空")
                        return
                    }
                    if (trimmed == originalName) {
                        editingName = false
                        return
                    }
                    if (onRename == null || savingName) return
                    keyboardController?.hide()
                    pendingRenameConfirm = trimmed
                }
                pendingRenameConfirm?.let { targetName ->
                    ConfirmDialog(
                        title = "确认修改昵称",
                        message = "确定将微博昵称修改为「$targetName」吗？",
                        confirmText = "确认修改",
                        onDismiss = { pendingRenameConfirm = null },
                        onConfirm = {
                            pendingRenameConfirm = null
                            val handler = onRename ?: return@ConfirmDialog
                            if (savingName) return@ConfirmDialog
                            savingName = true
                            renameScope.launch {
                                handler(targetName)
                                    .onSuccess {
                                        editingName = false
                                        App.toast("昵称已更新")
                                    }
                                    .onFailure {
                                        App.toast(it.message ?: "修改昵称失败")
                                    }
                                savingName = false
                            }
                        },
                    )
                }
                Column(Modifier.weight(1f)) {
                    val membership = user?.membership ?: Membership.None
                    if (tag != null || membership != Membership.None) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (tag != null) {
                                FollowTagBadge(tag)
                            }
                            if (membership != Membership.None) {
                                MemberBadge(membership, rank = user?.mbrank ?: 0)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (editProgress > 0.01f) {
                                    val stroke = 1.5.dp.toPx()
                                    drawLine(
                                        color = onCoverColor.copy(alpha = 0.85f * editProgress),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width * editProgress, size.height),
                                        strokeWidth = stroke,
                                    )
                                }
                            }
                            .padding(bottom = (3f * editProgress).dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (editingName) {
                            BasicTextField(
                                value = nameDraft,
                                onValueChange = { next ->
                                    val clean = next.text.replace("\n", "").take(30)
                                    nameDraft = if (clean == next.text) next else next.copy(text = clean)
                                },
                                singleLine = true,
                                enabled = !savingName,
                                textStyle = TextStyle(
                                    color = onCoverColor,
                                    fontSize = nameFontSize,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                cursorBrush = SolidColor(onCoverColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitRename() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(nameFocus)
                                    .testTag("profile_my_id_input"),
                            )
                        } else {
                            val canInlineRename = editableProfile && user != null && onRename != null
                            Text(
                                text = if (editProgress > 0.01f) nameDraft.text else (user?.name ?: "加载中…"),
                                color = onCoverColor,
                                fontSize = nameFontSize,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .then(
                                        if (canInlineRename) {
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val current = user.name
                                                    originalName = current
                                                    nameDraft = TextFieldValue(current, TextRange(current.length))
                                                    editingName = true
                                                }
                                                .testTag("profile_my_id")
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                        AnimatedVisibility(
                            visible = editingName,
                            enter = fadeIn(tween(220)) +
                                scaleIn(
                                    initialScale = 0.72f,
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                ) +
                                expandHorizontally(
                                    expandFrom = Alignment.End,
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                ),
                            exit = fadeOut(tween(180)) +
                                scaleOut(
                                    targetScale = 0.72f,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                ) +
                                shrinkHorizontally(
                                    shrinkTowards = Alignment.End,
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                ),
                        ) {
                            val trimmedDraft = nameDraft.text.trim()
                            val canSave = !savingName && trimmedDraft.isNotEmpty() && trimmedDraft != originalName
                            val saveAlpha by animateFloatAsState(
                                targetValue = if (canSave) 0.95f else 0.35f,
                                animationSpec = tween(durationMillis = 160),
                                label = "profileNameSaveAlpha",
                            )
                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !savingName, role = Role.Button) {
                                            nameDraft = TextFieldValue(originalName, TextRange(originalName.length))
                                            editingName = false
                                        }
                                        .testTag("profile_name_restore"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Undo,
                                        contentDescription = "撤回",
                                        modifier = Modifier.size(16.dp),
                                        tint = onCoverColor.copy(alpha = 0.9f),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = canSave, role = Role.Button) {
                                            commitRename()
                                        }
                                        .testTag("profile_name_save"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Ok,
                                        contentDescription = if (savingName) "保存中" else "保存",
                                        modifier = Modifier.size(16.dp),
                                        tint = onCoverColor.copy(alpha = saveAlpha),
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = editingName,
                        enter = fadeIn(tween(200)) + expandVertically(animationSpec = tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(animationSpec = tween(150)),
                    ) {
                        val renameVersion = App.profileRenameVersion
                        val renameUid = user?.id.orEmpty().ifBlank { runCatching { App.repo.session?.uid.orEmpty() }.getOrDefault("") }
                        val renameRecord = remember(renameUid, renameVersion) {
                            runCatching { App.store.profileRenameRecord(renameUid) }.getOrDefault(ProfileRenameRecord())
                        }
                        val renameStatus = remember(renameRecord, membership) {
                            computeProfileRenameStatus(renameRecord, membership)
                        }
                        Text(
                            text = renameStatus.summary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = if (renameStatus.limited) {
                                if (hasCover) Color(0xFFFFD180) else scheme.error
                            } else {
                                onCoverColor.copy(alpha = 0.78f)
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("profile_name_hint"),
                        )
                    }
                    if (user?.verified == true) {
                        Text(
                            "已认证",
                            fontSize = 11.sp,
                            color = if (hasCover) Color.White.copy(alpha = 0.85f) else scheme.primary,
                        )
                    }
                }
                if (onFollow != null) {
                    Spacer(Modifier.width(8.dp))
                    FollowCapsule(
                        text = followLabel,
                        onClick = onFollow,
                        modifier = Modifier.testTag("follow_button"),
                    )
                }
            }
        }
        } else {
            Spacer(Modifier.height(topInset))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!user?.description.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    user.description,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            // 统计卡与下方功能分组同款：同样的卡片、同样的 12dp 横向边距，边缘对齐。
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 10.dp),
                cornerRadius = CardDefaults.CornerRadius,
                insideMargin = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Stat("微博", user?.statuses ?: 0, modifier = Modifier.weight(1f), testTag = "profile_statuses", onClick = onStatusesClick)
                    Stat("关注", user?.friends ?: 0, modifier = Modifier.weight(1f), testTag = "profile_following") {
                        onRelations(RelationKind.Following)
                    }
                    Stat("粉丝", user?.followers ?: 0, modifier = Modifier.weight(1f), testTag = "profile_followers") {
                        onRelations(RelationKind.Followers)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 4.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fmtCount(value), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
    }
}
