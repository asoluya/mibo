package com.mibo.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.positionInWindow
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
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
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
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

/** 去掉外框后补在列表 12dp 内边距上的横向留白，合计 16dp，与顶栏按钮外框对齐。 */

@Composable
fun StatusCard(
    status: Status,
    onOpen: () -> Unit,
    onUser: (User) -> Unit,
    onImage: (List<Pic>, Int) -> Unit,
    onVideo: (VideoInfo) -> Unit,
    onLike: (Rect) -> Unit,
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
    /** 信息流按官方 140 字规则折叠长文；详情页传 false，始终显示全文。 */
    foldLongText: Boolean = true,
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
    // 屏蔽博主：本地名单制，这个博主的微博从此不在信息流出现（设置「应用相关」可取消）。
    val selfUid = if (App.isRepoReady()) App.repo.session?.uid else null
    val blockTarget = status.user?.takeIf { u ->
        u.id.isNotBlank() && u.id != selfUid && App.blockedAuthors.none { it.uid == u.id }
    }
    val handleBlockAuthor: (() -> Unit)? = blockTarget?.let { u ->
        {
            App.blockAuthor(u.id, u.name)
            App.toast("已屏蔽 @${u.name}")
        }
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
            onBlockAuthor = handleBlockAuthor,
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
            StatusVoteCard(status = status, actionsEnabled = actionsEnabled, onCard = false)
        }
        StatusMedia(status, compact, onImage, onVideo, onCard = false)
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
                onCard = false,
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
                var likeAnchor by remember { mutableStateOf(Rect.Zero) }
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
                    Box(Modifier.onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInWindow()
                        likeAnchor = Rect(pos.x, pos.y, pos.x + coordinates.size.width.toFloat(), pos.y + coordinates.size.height.toFloat())
                    }) {
                        ActionIcon(
                            icon = if (status.liked) MiboIcons.HeartFill else MiboIcons.Heart,
                            text = fmtCount(status.attitudes),
                            onClick = { onLike(likeAnchor) },
                            tint = if (status.liked) scheme.primary else scheme.onSurfaceSecondary,
                            enabled = actionsEnabled,
                        )
                    }
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
    // 全应用统一平铺：无卡片底色与圆角，内容全宽，条间一条与正文同缩进的细分隔线。
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = scheme.dividerLine.copy(alpha = 0.6f),
            thickness = 0.6.dp,
        )
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
    onBlockAuthor: (() -> Unit)? = null,
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
                if (showMore && (onToggleFavorite != null || onToggleCloudFavorite != null || onDelete != null || onCopyLinks != null || onCopyWeiboLink != null || onBlockAuthor != null)) {
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
                        onBlockAuthor = onBlockAuthor,
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
    onBlockAuthor: (() -> Unit)?,
) {
    val scheme = MiuixTheme.colorScheme
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier = Modifier
            .height(20.dp)
            .widthIn(min = 32.dp)
            .squircleClip(6.dp)
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                anchorBounds = Rect(
                    pos.x,
                    pos.y,
                    pos.x + coordinates.size.width.toFloat(),
                    pos.y + coordinates.size.height.toFloat(),
                )
            }
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
                        if (onBlockAuthor != null) {
                            add(App.GlassMenuItem(text = "屏蔽博主", icon = MiboIcons.Block, onClick = onBlockAuthor))
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

@Composable
fun StatusMedia(
    status: Status,
    compact: Boolean,
    onImage: (List<Pic>, Int) -> Unit,
    onVideo: (VideoInfo) -> Unit,
    onCard: Boolean = true,
) {
    val video = status.video
    if (video != null && video.url.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        VideoCard(video, compact, onVideo)
    }
    val resolvableInlineLinks = remember(status.id, status.textHtml, status.pics) {
        if (status.pics.isEmpty()) extractResolvablePhotoLinksFromHtml(status.textHtml) else emptyList()
    }
    var resolvedInlinePics by remember(status.id, resolvableInlineLinks) {
        mutableStateOf(
            if (resolvableInlineLinks.isEmpty()) emptyList()
            else App.repo.cachedInlineImageLinks(resolvableInlineLinks).orEmpty(),
        )
    }
    LaunchedEffect(status.id, resolvableInlineLinks) {
        if (resolvableInlineLinks.isNotEmpty() && resolvedInlinePics.isEmpty()) {
            resolvedInlinePics = App.repo.resolveInlineImageLinks(resolvableInlineLinks)
        }
    }
    val effectivePics = if (status.pics.isNotEmpty()) status.pics else resolvedInlinePics
    if (effectivePics.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        ImageGrid(effectivePics, compact, onImage, onVideo)
    }
    val bigPic = status.bigPic
    if (bigPic != null && bigPic.url.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        // 大图卡里分两类：商品/文章等链接卡改走小图横排（整幅大图太占屏）；
        // 图片页 / wbox / 超话这类本质是图片的仍走大图，点开看图。
        val openImageFirst = isWeiboWboxLink(bigPic.scheme) ||
            superTopicId(bigPic.scheme) != null ||
            isImageLink(bigPic.scheme) ||
            isWeiboPhotoPageLink(bigPic.scheme)
        if (bigPic.isLarge && (openImageFirst || bigPic.scheme.isBlank())) {
            BigPicCard(bigPic, onImage)
        } else {
            PageLinkCard(bigPic, compact = compact, onCard = onCard)
        }
    }
}

/** 普通网页 / 微博正文外链卡片：左侧小缩略图 + 右侧标题与摘要，不把缩略图放大成整行大图。 */
@Composable
private fun PageLinkCard(
    info: BigPicInfo,
    compact: Boolean,
    onCard: Boolean,
) {
    val scheme = MiuixTheme.colorScheme
    val linkHandler = LocalLinkHandler.current
    val hasTarget = info.scheme.isNotBlank()
    val thumbSize = if (compact) 52.dp else 60.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(nestedBlockTone(onCard))
            .then(if (hasTarget) Modifier.clickable { linkHandler(info.scheme) } else Modifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = info.url,
            contentDescription = info.title.ifBlank { null },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceContainerHigh),
        )
        if (info.title.isNotBlank() || info.subtitle.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (info.title.isNotBlank()) {
                    Text(
                        text = info.title,
                        fontSize = if (compact) 13.sp else 14.sp,
                        lineHeight = if (compact) 18.sp else 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (info.subtitle.isNotBlank()) {
                    Text(
                        text = info.subtitle,
                        fontSize = if (compact) 11.sp else 12.sp,
                        lineHeight = if (compact) 15.sp else 17.sp,
                        color = scheme.onSurfaceSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BigPicCard(
    bigPic: BigPicInfo,
    onImage: (List<Pic>, Int) -> Unit,
) {
    val linkHandler = LocalLinkHandler.current
    val saver = LocalImageSaver.current
    var ratio by remember(bigPic.url) { mutableFloatStateOf(16f / 9f) }
    val isWbox = remember(bigPic.scheme) { isWeiboWboxLink(bigPic.scheme) }
    val isSuperTopicScheme = remember(bigPic.scheme) { superTopicId(bigPic.scheme) != null }
    val targetUrl = remember(bigPic.scheme, isWbox) {
        if (isWbox) null else resolveWeiboLink(bigPic.scheme)
    }
    val openImageFirst = isWbox || isSuperTopicScheme ||
        isImageLink(bigPic.scheme) || isWeiboPhotoPageLink(bigPic.scheme)
    val singlePic = remember(bigPic.url) {
        listOf(Pic(thumb = bigPic.url, large = bigPic.url, preserveUrl = true))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .imageOrigin(bigPic.url, 10.dp, aspect = ratio, model = bigPic.url)
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .imagePressGestures(
                onClick = {
                    if (!openImageFirst && targetUrl != null) linkHandler(bigPic.scheme) else onImage(singlePic, 0)
                },
                onLongPress = { pos -> saver?.show(listOf(bigPic.url), 0, pos, preserveUrls = true) },
            ),
    ) {
        val origins = LocalImageOrigins.current
        AsyncImage(
            model = bigPic.url,
            contentDescription = bigPic.title.ifBlank { null },
            contentScale = ContentScale.Crop,
            onSuccess = { state ->
                origins.putCacheKey(bigPic.url, state.result.memoryCacheKey)
                val size = state.painter.intrinsicSize
                if (size.width > 0f && size.height > 0f) {
                    ratio = (size.width / size.height).coerceIn(0.62f, 2.2f)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (isWbox || isSuperTopicScheme || bigPic.title.isNotBlank()) {
            val label = buildString {
                if (bigPic.title.isNotBlank()) append(bigPic.title)
                when {
                    isWbox -> {
                        if (isNotEmpty()) append(" · ")
                        append("小程序预览图")
                    }
                    isSuperTopicScheme && targetUrl != null -> {
                        if (isNotEmpty()) append(" · ")
                        append("进入超话")
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .then(
                        if (!isWbox && targetUrl != null) {
                            Modifier.clickable { linkHandler(bigPic.scheme) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ImageGrid(
    pics: List<Pic>,
    compact: Boolean,
    onImage: (List<Pic>, Int) -> Unit,
    onVideo: (VideoInfo) -> Unit = {},
) {
    val urls = pics.map { it.large }
    val imageItems = remember(pics) {
        pics.filterNot { it.isVideo }.map {
            SaveImageItem(url = it.large, liveUrl = it.live, preserveUrl = it.preserveUrl)
        }
    }
    // 长按任意一张图片缩略图就能弹出保存菜单（实况图按实况保存），视频条目不走图片保存。
    val saver = LocalImageSaver.current
    val origins = LocalImageOrigins.current
    if (pics.size == 1) {
        val pic = pics.first()
        // 单图限高：格子比例不低于 1:1（竖图也只显示到正方形，顶部对齐裁切），
        // 宽度也收窄，避免一张图占大半屏；点开看图仍是原图原比例。
        val ratio = if (pic.width > 0 && pic.height > 0) {
            (pic.width.toFloat() / pic.height.toFloat()).coerceIn(1.0f, 1.9f)
        } else {
            1.45f
        }
        // 真实的宽高比（不是裁切后的格子比例）交给看图页，它才能把第一帧摆得和这张缩略图一模一样。
        val declaredAspect = if (pic.width > 0 && pic.height > 0) pic.width.toFloat() / pic.height else null
        var measuredAspect by remember(urls[0]) { mutableStateOf<Float?>(null) }
        val aspect = declaredAspect ?: measuredAspect
        val isLong = !pic.isVideo && isLongImageAspect(aspect)
            Box(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.65f else 0.75f)
                .aspectRatio(ratio)
                .imageOrigin(urls[0], 10.dp, aspect = aspect, model = pic.large)
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .imagePressGestures(
                    onClick = {
                        if (pic.isVideo) {
                            onVideo(VideoInfo(pic.videoTitle, pic.large.ifBlank { pic.thumb }, pic.videoUrl))
                        } else {
                            onImage(pics, 0)
                        }
                    },
                    onLongPress = if (!pic.isVideo && imageItems.isNotEmpty()) {
                        { pos -> saver?.showItems(imageItems, 0, pos) }
                    } else {
                        null
                    },
                ),
        ) {
            AsyncImage(
                model = pic.large,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = if (isLong) Alignment.TopCenter else Alignment.Center,
                onSuccess = { state ->
                    origins.putCacheKey(urls[0], state.result.memoryCacheKey)
                    if (declaredAspect == null) {
                        val s = state.painter.intrinsicSize
                        if (s.width > 0f && s.height > 0f) measuredAspect = s.width / s.height
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            when {
                pic.isVideo -> {
                    GridPlayBadge(46.dp, 26.dp, Modifier.align(Alignment.Center))
                    if (pic.durationSec > 0) {
                        VideoDurationBadge(
                            seconds = pic.durationSec,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(SingleBadgeInset),
                        )
                    }
                }
                else -> {
                    if (pic.isLive) {
                        LiveBadge(urls[0], Modifier.align(Alignment.BottomStart).padding(SingleBadgeInset))
                    }
                    if (isLong) {
                        LongImageBadge(urls[0], Modifier.align(Alignment.BottomEnd).padding(SingleBadgeInset))
                    }
                }
            }
        }
        return
    }

    // 多图统一三列小方格：2 张图也只占一列高，不再用两列大方格撑高整条微博。
    val columns = 3
    val rows = (pics.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < pics.size) {
                                            val pic = pics[index]
                        val shown = pic.thumb.ifBlank { pic.large }
                        val declaredAspect = if (pic.width > 0 && pic.height > 0) {
                            pic.width.toFloat() / pic.height
                        } else {
                            null
                        }
                        var measuredAspect by remember(urls[index]) { mutableStateOf<Float?>(null) }
                        val aspect = declaredAspect ?: measuredAspect
                        val isLong = !pic.isVideo && isLongImageAspect(aspect)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .imageOrigin(urls[index], 8.dp, aspect = aspect, model = shown)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .imagePressGestures(
                                    onClick = {
                                        if (pic.isVideo) {
                                            onVideo(VideoInfo(pic.videoTitle, pic.large.ifBlank { pic.thumb }, pic.videoUrl))
                                        } else {
                                            onImage(pics, index)
                                        }
                                    },
                                    onLongPress = if (!pic.isVideo && imageItems.isNotEmpty()) {
                                        { pos ->
                                            val saveIdx = pics.take(index).count { !it.isVideo }
                                            saver?.showItems(imageItems, saveIdx, pos)
                                        }
                                    } else {
                                        null
                                    },
                                ),
                        ) {
                            AsyncImage(
                                model = shown,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = if (isLong) Alignment.TopCenter else Alignment.Center,
                                onSuccess = { state ->
                                    origins.putCacheKey(urls[index], state.result.memoryCacheKey)
                                    if (declaredAspect == null) {
                                        val s = state.painter.intrinsicSize
                                        if (s.width > 0f && s.height > 0f) measuredAspect = s.width / s.height
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            when {
                                pic.isVideo -> {
                                    GridPlayBadge(34.dp, 18.dp, Modifier.align(Alignment.Center))
                                    if (pic.durationSec > 0) {
                                        VideoDurationBadge(
                                            seconds = pic.durationSec,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                        )
                                    }
                                }
                                else -> {
                                    if (pic.isLive) {
                                        LiveBadge(urls[index], Modifier.align(Alignment.BottomStart).padding(GridBadgeInset))
                                    }
                                    if (isLong) {
                                        LongImageBadge(urls[index], Modifier.align(Alignment.BottomEnd).padding(4.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridPlayBadge(
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(MiuixIcons.Play, "播放视频", Modifier.size(iconSize), tint = Color.White)
    }
}

internal fun formatVideoDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, sec)
    } else {
        "%d:%02d".format(m, sec)
    }
}

@Composable
private fun VideoDurationBadge(seconds: Int, modifier: Modifier = Modifier) {
    Text(
        text = formatVideoDuration(seconds),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** 缩略图右下角的「长图」角标，看图页覆盖当前图时先隐去，退场落位后再淡回。 */
@Composable
internal fun LongImageBadge(url: String, modifier: Modifier = Modifier) {
    val covered = LocalImageOrigins.current.viewing == url
    val fade = animateFloatAsState(
        targetValue = if (covered) 0f else 1f,
        animationSpec = tween(LiveBadgeFadeMillis),
        label = "longImageBadge",
    )
    Text(
        text = "长图",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .graphicsLayer { alpha = fade.value }
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** 多图格子里、单张大图上，实况标记各自离左下角的距离。 */
private val GridBadgeInset = 2.dp
private val SingleBadgeInset = 4.dp

/**
 * 缩略图左下角的实况标记，点进看图页后按住才会播那段短视频。
 *
 * 看图页是整块照片盖在这一格上的，退场到最后一帧才整层撤掉；标记若一直在，就会在那一帧整块
 * 跳出来。所以被打开的那一张在页面开着时先让位，等页面走了再淡回来。别的缩略图照旧。
 */
@Composable
private fun LiveBadge(url: String, modifier: Modifier = Modifier) {
    val covered = LocalImageOrigins.current.viewing == url
    val fade = animateFloatAsState(
        targetValue = if (covered) 0f else 1f,
        animationSpec = tween(LiveBadgeFadeMillis),
        label = "liveBadge",
    )
    Box(
        modifier
            .graphicsLayer { alpha = fade.value }
            .size(LiveBadgeSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(com.mibo.app.R.drawable.ic_live_photo),
            contentDescription = "实况",
            modifier = Modifier.size(13.dp),
            tint = Color.White,
        )
    }
}

/** 看图页撤走之后标记淡回来的时长。 */
private const val LiveBadgeFadeMillis = 220

@Composable
fun VideoCard(video: VideoInfo, compact: Boolean, onVideo: (VideoInfo) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 2.1f else 1.78f)
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable { onVideo(video) },
    ) {
        if (video.cover.isNotBlank()) {
            AsyncImage(
                model = video.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f),
                    )
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MiuixIcons.Play, "播放", Modifier.size(26.dp), tint = Color.White)
        }
        if (video.isLive) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30)),
                )
                Text(
                    text = "直播",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (video.title.isNotBlank()) {
            Text(
                text = video.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * 卡片正文。长微博在信息流里按官方规则折叠，点「展开全文」就地展开；详情页传 fold = false。
 * 展开状态只跟着这一条微博的 id 走，滑出屏幕再回来重新折叠。
 */
@Composable
private fun FoldableWeiboText(
    status: Status,
    fold: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
) {
    val folded = status.foldedTextHtml
    val hasFolded = !folded.isNullOrBlank() && folded != status.textHtml
    var expanded by remember(status.id) { mutableStateOf(false) }
    var fullHtml by remember(status.id) { mutableStateOf<String?>(null) }
    var loading by remember(status.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 全文没随列表到手的长文（多见于转发链里的原博）：textHtml 只是截断版，
    // 展开时按需补拉全文，拉到才算展开；拉不到保持原样并提示。
    val pending = status.longTextPending && fullHtml == null
    val canFold = fold && (hasFolded || pending)
    val folding = canFold && !expanded
    // 详情页不折叠，但待补的全文也要自动拉齐，不能停在截断版。
    LaunchedEffect(status.id, fold, pending) {
        if (!fold && pending && !loading) {
            loading = true
            fullHtml = App.repo.loadLongTextHtml(status)
            loading = false
        }
    }
    WeiboText(
        html = fullHtml ?: if (folding) (if (hasFolded) folded!! else status.textHtml) else status.textHtml,
        fontSize = fontSize,
        lineHeight = lineHeight,
        expandLabel = when {
            !folding -> null
            loading -> "加载中…"
            else -> "展开全文"
        },
        onExpand = if (folding && !loading) ({
            if (pending) {
                loading = true
                scope.launch {
                    fullHtml = App.repo.loadLongTextHtml(status)
                    loading = false
                    if (fullHtml != null) expanded = true else App.toast("全文加载失败，请重试")
                }
            } else {
                expanded = true
            }
        }) else null,
    )
}

@Composable
fun RetweetBlock(    status: Status,
    compact: Boolean,
    onOpen: () -> Unit,
    onUser: (User) -> Unit,
    onImage: (List<Pic>, Int) -> Unit,
    onVideo: (VideoInfo) -> Unit,
    onOpenQuoted: ((Status, ExpandOrigin) -> Unit)? = null,
    actionsEnabled: Boolean = true,
    /** 当前块是否直接落在卡片面上：决定嵌套块的底色，卡片去掉后要反过来才看得见。 */
    onCard: Boolean = true,
    /** 被转发的原博在信息流里是否折叠长文。 */
    foldLongText: Boolean = true,
) {
    val scheme = MiuixTheme.colorScheme
    remember(status) { StatusMemory.remember(status) }
    // The quoted post is a page of its own: it opens from the block it is drawn in and comes back
    // into it, so the block carries the same expand origin a whole card does.
    val quotedOrigin = rememberExpandOrigin(cornerRadius = 10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .expandOrigin(quotedOrigin)
            .clip(RoundedCornerShape(10.dp))
            .background(nestedBlockTone(onCard))
            .clickable {
                if (onOpenQuoted != null) onOpenQuoted(status, quotedOrigin) else onOpen()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "@${status.user?.name.orEmpty()}：",
                color = scheme.primary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            val retweetVisibility = status.visibility
            if (retweetVisibility != null && !retweetVisibility.isPublic) {
                Spacer(Modifier.width(5.dp))
                StatusVisibilityIcon(visibility = retweetVisibility, compact = true)
            }
        }
        if (status.textHtml.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            FoldableWeiboText(
                status = status,
                fold = foldLongText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        if (status.isVote) {
            Spacer(Modifier.height(6.dp))
            StatusVoteCard(status = status, actionsEnabled = actionsEnabled, onCard = !onCard)
        }
        StatusMedia(status, true, onImage, onVideo, onCard = !onCard)
    }
}

@Composable
fun StatusVoteCard(
    status: Status,
    modifier: Modifier = Modifier,
    actionsEnabled: Boolean = true,
    /** 是否直接落在卡片面上，决定投票块的底色；卡片去掉后要反过来。 */
    onCard: Boolean = true,
) {
    if (!actionsEnabled) {
        Text(
            text = "投票内容请通过网页查看",
            modifier = modifier,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontSize = 13.sp,
        )
        return
    }
    val linkHandler = LocalLinkHandler.current
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    var voteId by remember(status.id) { mutableStateOf(status.voteId) }
    var resolving by remember(status.id) { mutableStateOf(false) }
    var vote by remember(status.id, voteId) { mutableStateOf<VoteInfo?>(null) }
    var loading by remember(status.id, voteId) { mutableStateOf(false) }
    var error by remember(status.id, voteId) { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var showRevoteConfirm by remember { mutableStateOf(false) }
    val selectedIds = remember(status.id, vote?.id) { mutableStateListOf<String>() }

    // 1. 如果没有直接拿到 voteId，异步尝试从正文短链或接口解析
    LaunchedEffect(status.id, status.textHtml) {
        if (voteId == null && !resolving) {
            resolving = true
            val resolved = App.repo.resolveStatusVoteId(status.id, status.textHtml)
            if (resolved != null) {
                voteId = resolved
            }
            resolving = false
        }
    }

    // 2. 拿到 voteId 后，拉取详细投票数据并填充选项
    fun loadVote(id: String) {
        loading = true
        error = null
        scope.launch {
            runCatching { App.repo.fetchVote(id) }
                .onSuccess { info ->
                    vote = info
                    selectedIds.clear()
                    info.options.filter { it.selected }.forEach { selectedIds.add(it.id) }
                    loading = false
                }
                .onFailure { err ->
                    error = err.message ?: "加载投票失败"
                    loading = false
                }
        }
    }

    LaunchedEffect(voteId) {
        val id = voteId
        if (id != null) {
            loadVote(id)
        }
    }

    // 重新投票确认弹窗
    if (showRevoteConfirm && vote != null) {
        val currentVote = vote!!
        ConfirmDialog(
            title = "重新投票",
            message = "确定要撤回之前的投票并重新选择吗？",
            confirmText = "重新投票",
            onConfirm = {
                showRevoteConfirm = false
                submitting = true
                scope.launch {
                    runCatching {
                        App.repo.uncastVote(currentVote.id, currentVote.sk)
                    }.onSuccess { updated ->
                        vote = updated
                        selectedIds.clear()
                        App.toast("已撤回投票，请重新选择")
                    }.onFailure { err ->
                        App.toast(err.message ?: "撤回失败")
                    }
                    submitting = false
                }
            },
            onDismiss = { showRevoteConfirm = false },
        )
    }

    val currentVote = vote
    val currentId = voteId
    val buttonText = MiuixTheme.textStyles.button.copy(fontSize = 13.sp)

    // 底色与引用微博同层；不能对主题色再 copy(alpha)：暗色的层次色本身就是半透明白，
    // copy 会覆盖而不是叠乘原透明度，卡片会被提亮成一整块浅灰。
    Column(
        modifier = modifier
            .fillMaxWidth()
            .squircleBackground(nestedBlockTone(onCard), 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { /* 消费点击事件，防止冒泡到外部卡片的详情跳转 */ }
            .padding(12.dp),
    ) {
        // 头部：类型与参与人数、投票标题，点击进入全屏投票页
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .squircleClip(8.dp)
                .clickable {
                    val vid = currentId ?: currentVote?.id
                    if (vid != null) {
                        linkHandler("https://vote.weibo.com/h5/index/index?vote_id=$vid")
                    }
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = MiboIcons.Poll,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = listOfNotNull(
                        "投票",
                        currentVote?.let {
                            when {
                                it.isExpired -> "已截止"
                                it.isMultiSelect -> "多选（最多${it.maxSelect}项）"
                                else -> "单选"
                            }
                        },
                        currentVote?.partNum?.takeIf { it > 0 }?.let { "${fmtCount(it)}人参与" },
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (currentVote != null && currentVote.title.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentVote.title,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                )
            }
        }

        // 内容区域
        when {
            // 正在解析短链或拉取数据
            resolving || (loading && currentVote == null) -> {
                Text(
                    text = if (resolving) "正在识别投票…" else "正在加载投票选项…",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // 加载失败或未找到投票 ID
            currentVote == null -> {
                VoteFooter(
                    text = error ?: "暂未获取到投票详情",
                    color = if (error != null) scheme.error else scheme.onSurfaceSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    TextButton(
                        text = if (currentId != null) "重试" else "网页查看",
                        onClick = {
                            val vid = currentId
                            if (vid != null) {
                                loadVote(vid)
                            } else {
                                linkHandler("https://m.weibo.cn/status/${status.id}")
                            }
                        },
                        textStyle = buttonText,
                        // Miuix TextButton 没有 size 档位，用最小尺寸与内边距近似原来的 Small。
                        minWidth = 48.dp,
                        minHeight = 32.dp,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // 模式 1：尚未参与且进行中 -> 原生可交互投票选项
            !currentVote.parted && !currentVote.isExpired -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    currentVote.options.forEach { option ->
                        val isSelected = selectedIds.contains(option.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .squircleBackground(
                                    if (isSelected) scheme.primary.copy(alpha = 0.12f)
                                    else scheme.surfaceContainer,
                                    10.dp,
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (currentVote.isMultiSelect) {
                                        if (isSelected) {
                                            selectedIds.remove(option.id)
                                        } else if (selectedIds.size < currentVote.maxSelect) {
                                            selectedIds.add(option.id)
                                        } else {
                                            App.toast("最多选择 ${currentVote.maxSelect} 项")
                                        }
                                    } else {
                                        selectedIds.clear()
                                        selectedIds.add(option.id)
                                    }
                                }
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (option.pic.isNotBlank()) {
                                AsyncImage(
                                    model = option.pic,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .squircleClip(6.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                text = option.title,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            if (currentVote.isMultiSelect) {
                                Checkbox(
                                    state = ToggleableState(isSelected),
                                    onClick = null,
                                )
                            } else {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                )
                            }
                        }
                    }

                    VoteFooter(
                        text = currentVote.deadlineLabel,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        TextButton(
                            text = if (submitting) "提交中…" else "投票",
                            onClick = {
                                submitting = true
                                scope.launch {
                                    runCatching {
                                        App.repo.castVote(currentVote.id, selectedIds.toList(), currentVote.sk)
                                    }.onSuccess { updated ->
                                        vote = updated
                                        App.toast("投票成功")
                                    }.onFailure { err ->
                                        App.toast(err.message ?: "投票失败")
                                    }
                                    submitting = false
                                }
                            },
                            enabled = selectedIds.isNotEmpty() && !submitting,
                            // COUI 的禁用蓝叠在嵌套层上文字几乎不可读，未选中时退回中性灰
                            colors = ButtonDefaults.textButtonColorsPrimary(
                                disabledColor = scheme.surfaceContainer,
                                disabledTextColor = scheme.onSurfaceSecondary,
                            ),
                            textStyle = buttonText,
                            // Miuix TextButton 没有 size 档位，用最小尺寸与内边距近似原来的 Small。
                            minWidth = 48.dp,
                            minHeight = 32.dp,
                            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // 模式 2：已完成投票或已截止 -> 原生连续曲率百分比胶囊进度条与统计
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    currentVote.options.forEach { option ->
                        val progress = (option.partRatio / 100f).coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(600, easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)),
                            label = "inline_vote_progress_${option.id}",
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .squircleBackground(scheme.surfaceContainer, 10.dp),
                        ) {
                            // 连续曲率填充进度条
                            Box(Modifier.matchParentSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .background(
                                            if (option.selected) scheme.primary.copy(alpha = 0.2f)
                                            else scheme.onSurface.copy(alpha = 0.08f),
                                        ),
                                )
                            }
                            // 文本与比例，长选项换行而不是被截断，行高与未投票时一致
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (option.pic.isNotBlank()) {
                                    AsyncImage(
                                        model = option.pic,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .squircleClip(6.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = option.title,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = if (option.selected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (option.selected) scheme.primary else scheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (option.selected) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = MiboIcons.Check,
                                            contentDescription = "已投",
                                            tint = scheme.primary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                if (option.partNum.isNotBlank()) {
                                    Text(
                                        text = option.partNum,
                                        fontSize = 11.sp,
                                        color = scheme.onSurfaceSecondary,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = option.ratioLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (option.selected) scheme.primary else scheme.onSurface,
                                )
                            }
                        }
                    }

                    VoteFooter(
                        text = listOf(
                            if (currentVote.parted) "已参与" else "",
                            if (currentVote.isExpired) "已截止" else currentVote.deadlineLabel,
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (currentVote.parted && !currentVote.isExpired) {
                            TextButton(
                                text = if (submitting) "撤回中…" else "重新投票",
                                onClick = { showRevoteConfirm = true },
                                enabled = !submitting,
                                textStyle = buttonText,
                                // Miuix TextButton 没有 size 档位，用最小尺寸与内边距近似原来的 Small。
                                minWidth = 48.dp,
                                minHeight = 32.dp,
                                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 投票卡片底部一行：左侧说明文字可截断，右侧操作按钮始终完整，两种投票状态下行高一致。 */
@Composable
private fun VoteFooter(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.onSurfaceSecondary,
    action: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        action()
    }
}

/**
 * The tone of a block nested inside a status card. COUI's light surfaceContainer is the same white
 * as the card, so nested blocks vanished into it. Light takes the page grey on a card and the card
 * white on the page; dark keeps the translucent white that already reads as a layer.
 */
@Composable
private fun nestedBlockTone(onCard: Boolean): Color {
    val scheme = MiuixTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f || !onCard) {
        scheme.surfaceContainer
    } else {
        scheme.background
    }
}
