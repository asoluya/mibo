package com.mibo.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mibo.app.App
import com.mibo.app.data.BigPicInfo
import com.mibo.app.data.Pic
import com.mibo.app.data.FollowTags
import com.mibo.app.data.Membership
import com.mibo.app.data.Status
import com.mibo.app.data.StatusMemory
import com.mibo.app.data.StatusVisibility
import com.mibo.app.data.User
import com.mibo.app.data.VerifiedKind
import com.mibo.app.data.VideoInfo
import com.mibo.app.data.VoteInfo
import com.mibo.app.data.formatWeiboTime
import com.mibo.app.data.superTopicId
import com.mibo.app.ui.MiboIcons
import com.mibo.app.ui.ExpandOrigin
import com.mibo.app.ui.LocalLinkHandler
import com.mibo.app.ui.LiveBadgeSize
import com.mibo.app.ui.isLongImageAspect
import com.mibo.app.ui.LocalImageOrigins
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.buildCopyTextWithDirectLinks
import com.mibo.app.ui.extractResolvablePhotoLinksFromHtml
import com.mibo.app.ui.imageOrigin
import com.mibo.app.ui.isImageLink
import com.mibo.app.ui.isWeiboPhotoPageLink
import com.mibo.app.ui.isWeiboWboxLink
import com.mibo.app.ui.components.LocalImageSaver
import com.mibo.app.ui.components.SaveImageItem
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.resolveWeiboLink
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

/** 去掉外框后补在列表 12dp 内边距上的横向留白，合计 16dp，与顶栏按钮外框对齐。 */
private val FlatFrameInset = 4.dp

@Composable
fun StatusCard(
    status: Status,
    onOpen: () -> Unit,
    onUser: (User) -> Unit,
    onImage: (List<Pic>, Int) -> Unit,
    onVideo: (VideoInfo) -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showActions: Boolean = true,
    /** 只读流（本地关注、纯本地模式）：计数照常显示，但点赞 / 评论 / 转发都不响应。 */
    actionsEnabled: Boolean = !App.repo.guest,
    onRepost: () -> Unit = {},
    onComment: () -> Unit = {},
    /** Opens the quoted post when it is tapped; the frame grows from and returns into that block. */
    onOpenQuoted: ((Status, ExpandOrigin) -> Unit)? = null,
    onDeleted: (() -> Unit)? = null,
    /** 是否保留最外层卡片外框。关掉后只去掉卡片底色与圆角，内容横向与顶栏按钮对齐。 */
    keepFrame: Boolean = true,
    /** 去掉卡片外框时，是否在两条微博之间画一条与正文同缩进的底部分割线。 */
    showBottomDivider: Boolean = false,
    /** 信息流按官方 140 字规则折叠长文；详情页传 false，始终显示全文。 */
    foldLongText: Boolean = App.settings.foldLongText,
) {
    if (App.repo.isDeleted(status.id)) return
    // 列表画过就记住这条，详情页第一帧直接用它，不用再等接口。
    remember(status) { StatusMemory.remember(status) }
    val scheme = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val effectiveFavorited = App.repo.isFavorited(status.id)
    var cloudFavorited by remember(status.id) { mutableStateOf(status.favorited) }
    val canDelete = !App.repo.guest &&
        !status.user?.id.isNullOrBlank() &&
        status.user?.id == App.repo.session?.uid
    var confirmDelete by remember(status.id) { mutableStateOf(false) }
    var deleting by remember(status.id) { mutableStateOf(false) }
    val handleFavorite: () -> Unit = {
        val nowFavorited = App.repo.toggleLocalFavorite(status)
        App.toast(if (nowFavorited) "已收藏" else "已取消收藏")
    }
    // 微博收藏（云端）：已从「⋯」菜单移到操作栏的星星按钮。
    val handleCloudFavorite: () -> Unit = {
        if (App.repo.guest) {
            App.toast("请先登录微博")
        } else {
            scope.launch {
                runCatching { App.repo.favorite(status.id, cloudFavorited) }
                    .onSuccess {
                        cloudFavorited = !cloudFavorited
                        App.toast(if (cloudFavorited) "已收藏到微博" else "已取消微博收藏")
                    }
                    .onFailure { App.toast(it.message ?: "操作失败") }
            }
        }
    }
    val context = LocalContext.current
    // 复制原文：整条微博转成纯文本复制，正文里的站外链接先解析成直链再替换进去
    // （小店中转拆包、t.cn 短链跟随重定向）；@提及与 #话题# 保留文字。
    // 转发微博附带原文（//@作者:…）。正文为空（纯图/纯卡片）时回退复制微博详情链接。
    val handleCopyLinks: () -> Unit = {
        scope.launch {
            val main = buildCopyTextWithDirectLinks(status.textHtml) { App.repo.resolveShortUrl(it) }
            var hasDirectLink = main.hasDirectLink
            val fullText = buildString {
                append(main.text)
                val rt = status.retweeted
                if (rt != null) {
                    val rtCopy = buildCopyTextWithDirectLinks(rt.textHtml) { App.repo.resolveShortUrl(it) }
                    if (rtCopy.hasDirectLink) hasDirectLink = true
                    if (rtCopy.text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("//@").append(rt.user?.name.orEmpty()).append(":").append(rtCopy.text)
                    }
                }
            }.trim()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (fullText.isBlank()) {
                clipboard?.setPrimaryClip(
                    ClipData.newPlainText("mibo", "https://m.weibo.cn/detail/${status.mid.ifBlank { status.id }}"),
                )
                App.toast("已复制链接")
            } else {
                clipboard?.setPrimaryClip(ClipData.newPlainText("mibo", fullText))
                App.toast(if (hasDirectLink) "已复制原文 · 链接已转直链" else "已复制微博原文")
            }
        }
    }
    // 复制链接：这条微博的详情页地址。
    val handleCopyWeiboLink: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText("mibo", "https://m.weibo.cn/detail/${status.mid.ifBlank { status.id }}"),
        )
        App.toast("已复制链接")
    }
    val content: @Composable ColumnScope.() -> Unit = {
        StatusHeader(
            status = status,
            onUser = onUser,
            compact = compact,
            favorited = effectiveFavorited,
            onToggleFavorite = handleFavorite,
            cloudFavorited = cloudFavorited,
            onToggleCloudFavorite = null,
            onDelete = if (canDelete) { { if (!deleting) confirmDelete = true } } else null,
            onCopyLinks = handleCopyLinks,
            onCopyWeiboLink = handleCopyWeiboLink,
        )
        if (status.textHtml.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            FoldableWeiboText(
                status = status,
                fold = foldLongText,
                fontSize = if (compact) 14.sp else 15.sp,
                lineHeight = if (compact) 20.sp else 22.sp,
            )
        }
        val showOuterVote = status.isVote && (
            status.retweeted?.isVote != true ||
                (status.voteId != null && status.retweeted.voteId != null && status.voteId != status.retweeted.voteId)
            )
        if (showOuterVote) {
            Spacer(Modifier.height(8.dp))
            StatusVoteCard(status = status, actionsEnabled = actionsEnabled, onCard = keepFrame)
        }
        StatusMedia(status, compact, onImage, onVideo, onCard = keepFrame)
        if (status.retweeted != null) {
            Spacer(Modifier.height(8.dp))
            RetweetBlock(
                status = status.retweeted,
                compact = compact,
                actionsEnabled = actionsEnabled,
                onOpen = onOpen,
                onOpenQuoted = onOpenQuoted,
                onUser = onUser,
                onImage = onImage,
                onVideo = onVideo,
                onCard = keepFrame,
                foldLongText = foldLongText,
            )
        }
        val region = status.region.trim()
        if (showActions || region.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (region.isNotEmpty()) {
                    Text(
                        text = region,
                        fontSize = 11.sp,
                        color = scheme.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (showActions) {
                    Spacer(Modifier.width(8.dp))
                    ActionIcon(
                        icon = MiboIcons.Repost,
                        text = fmtCount(status.reposts),
                        onClick = onRepost,
                        enabled = actionsEnabled,
                    )
                    ActionIcon(
                        icon = MiboIcons.Comment,
                        text = fmtCount(status.comments),
                        onClick = onComment,
                        enabled = actionsEnabled,
                    )
                    ActionIcon(
                        icon = if (status.liked) MiboIcons.HeartFill else MiboIcons.Heart,
                        text = fmtCount(status.attitudes),
                        onClick = onLike,
                        tint = if (status.liked) scheme.primary else scheme.onSurfaceSecondary,
                        enabled = actionsEnabled,
                    )
                    ActionIcon(
                        icon = if (cloudFavorited) MiboIcons.StarFill else MiboIcons.Star,
                        text = "",
                        onClick = handleCloudFavorite,
                        tint = if (cloudFavorited) scheme.primary else scheme.onSurfaceSecondary,
                        enabled = actionsEnabled,
                        contentDescription = "收藏微博",
                    )
                }
            }
        }
    }
    if (keepFrame) {
        Card(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = CardDefaults.CornerRadius,
            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            onClick = onOpen,
            content = content,
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    // 列表左右各留 12dp，这里再补 4dp，内容左缘正好落在 16dp 的顶栏按钮外框上，
                    // 不再比返回/搜索按钮更靠外。
                    .padding(horizontal = FlatFrameInset, vertical = 12.dp),
                content = content,
            )
            if (showBottomDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = FlatFrameInset),
                    thickness = 0.5.dp,
                    color = scheme.dividerLine,
                )
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "删除微博",
            message = "删除后不可恢复，确定删除这条微博吗？",
            confirmText = "删除",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                if (deleting) return@ConfirmDialog
                deleting = true
                scope.launch {
                    runCatching { App.repo.deleteStatus(status.id, status.mid) }
                        .onSuccess {
                            App.toast("已删除")
                            onDeleted?.invoke()
                        }
                        .onFailure {
                            App.toast(it.message ?: "删除失败")
                        }
                    deleting = false
                }
            },
        )
    }
}

@Composable
fun StatusHeader(
    status: Status,
    onUser: (User) -> Unit,
    compact: Boolean,
    favorited: Boolean = App.repo.isFavorited(status.id),
    cloudFavorited: Boolean = status.favorited,
    showMore: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleCloudFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onCopyLinks: (() -> Unit)? = null,
    onCopyWeiboLink: (() -> Unit)? = null,
) {
    val scheme = MiuixTheme.colorScheme
    val user = status.user
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            url = user?.avatar,
            size = if (compact) 38.dp else 42.dp,
            verifiedKind = user?.verifiedKind ?: VerifiedKind.None,
            modifier = Modifier.clickable { user?.let(onUser) },
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user?.name.orEmpty(),
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // 昵称太长时先压缩昵称，别把后面的徽标和认证勾挤出去。
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { user?.let(onUser) },
                    )
                    val membership = user?.membership ?: Membership.None
                    if (membership != Membership.None) {
                        Spacer(Modifier.width(4.dp))
                        MemberBadge(membership, compact = true, rank = user?.mbrank ?: 0)
                    }
                    val tag = FollowTags[user?.id]
                    if (tag != null) {
                        Spacer(Modifier.width(4.dp))
                        FollowTagBadge(tag, compact = true)
                    }
                }
                if (showMore && (onToggleFavorite != null || onToggleCloudFavorite != null || onDelete != null || onCopyLinks != null || onCopyWeiboLink != null)) {
                    Spacer(Modifier.width(6.dp))
                    StatusMoreButton(
                        statusId = status.id,
                        favorited = favorited,
                        cloudFavorited = cloudFavorited,
                        onToggleFavorite = onToggleFavorite,
                        onToggleCloudFavorite = onToggleCloudFavorite,
                        onDelete = onDelete,
                        onCopyLinks = onCopyLinks,
                        onCopyWeiboLink = onCopyWeiboLink,
                    )
                }
            }
            val timeAndSource = buildString {
                append(formatWeiboTime(status.createdAt))
                if (status.source.isNotBlank()) {
                    append(" 来自 ")
                    append(status.source)
                }
            }
            val visibility = status.visibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (timeAndSource.isNotBlank()) {
                    Text(
                        text = timeAndSource,
                        fontSize = 11.sp,
                        color = scheme.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (visibility != null) {
                    if (timeAndSource.isNotBlank()) Spacer(Modifier.width(5.dp))
                    StatusVisibilityIcon(visibility = visibility)
                }
            }
        }
    }
}

@Composable
private fun StatusVisibilityIcon(
    visibility: StatusVisibility,
    compact: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val (icon, tint) = when (visibility.type) {
        0 -> MiuixIcons.Community to scheme.onSurfaceSecondary
        6, 2 -> MiuixIcons.Contacts to if (dark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        1 -> MiuixIcons.Lock to if (dark) Color(0xFFFBBF24) else Color(0xFFD97706)
        10 -> MiuixIcons.Community to scheme.primary
        else -> MiuixIcons.Lock to scheme.primary
    }
    Icon(
        imageVector = icon,
        contentDescription = visibility.label,
        tint = tint,
        modifier = Modifier
            .size(if (compact) 11.dp else 12.dp)
            .testTag("status_visibility_${visibility.type}"),
    )
}

@Composable
private fun StatusMoreButton(
    statusId: String,
    favorited: Boolean,
    cloudFavorited: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onToggleCloudFavorite: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onCopyLinks: (() -> Unit)?,
    onCopyWeiboLink: (() -> Unit)?,
) {
    val scheme = MiuixTheme.colorScheme
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier = Modifier
            .height(20.dp)
            .widthIn(min = 32.dp)
            .squircleClip(6.dp)
            .onGloballyPositioned { anchorBounds = it.boundsInWindow() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    // 菜单本体由根节点的全局玻璃菜单宿主渲染（整块柔光玻璃）。
                    val items = buildList {
                        if (onToggleCloudFavorite != null) {
                            add(
                                App.GlassMenuItem(
                                    text = if (cloudFavorited) "取消微博收藏" else "收藏微博",
                                    icon = if (cloudFavorited) MiboIcons.StarFill else MiboIcons.Star,
                                    onClick = onToggleCloudFavorite,
                                )
                            )
                        }
                        if (onToggleFavorite != null) {
                            add(
                                App.GlassMenuItem(
                                    text = if (favorited) "取消本地收藏" else "本地收藏",
                                    icon = if (favorited) MiboIcons.StarFill else MiboIcons.Star,
                                    onClick = onToggleFavorite,
                                )
                            )
                        }
                        if (onCopyLinks != null) {
                            add(App.GlassMenuItem(text = "复制原文", icon = MiuixIcons.Copy, onClick = onCopyLinks))
                        }
                        if (onCopyWeiboLink != null) {
                            add(App.GlassMenuItem(text = "复制链接", icon = MiuixIcons.Link, onClick = onCopyWeiboLink))
                        }
                        if (onDelete != null) {
                            add(
                                App.GlassMenuItem(
                                    text = "删除微博",
                                    icon = MiuixIcons.Delete,
                                    danger = true,
                                    onClick = onDelete,
                                )
                            )
                        }
                    }
                    App.showGlassMenu(anchorBounds, items)
                },
            )
            .padding(horizontal = 2.dp)
            .semantics { contentDescription = "更多" }
            .testTag("status_more"),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = MiuixIcons.More,
            contentDescription = null,
            tint = scheme.onSurfaceSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
