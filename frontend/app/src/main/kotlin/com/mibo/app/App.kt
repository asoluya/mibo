package com.mibo.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import com.mibo.app.data.CacheStore
import com.mibo.app.data.FeedSnapshotStore
import com.mibo.app.data.FollowTags
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.mibo.app.data.Repo
import com.mibo.app.data.Settings
import com.mibo.app.data.Status
import com.mibo.app.data.Store
import com.mibo.app.data.UpdateInfo
import com.mibo.app.data.Updater
import com.mibo.app.data.User
import com.mibo.app.data.WeiboEmojiBitmaps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

class MiboApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        App.init(this)
        WeiboEmojiBitmaps.attach(this)
        // 表情表先有内置的一整份，这里只是顺手把接口里的新表情补进来；失败就继续用内置表。
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
            .launch { runCatching { App.repo.emotions() } }
        // 私信附件在 attachment.api.weibo.com 上，不带 cookie 直接 403，
        // 默认加载器拿不到图，气泡就只剩一圈内边距。只给 weibo 域名挂 cookie。
        SingletonImageLoader.setSafe { ctx ->
            ImageLoader.Builder(ctx)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { App.imageClient }))
                }
                .build()
        }
    }
}

object App {
    lateinit var context: Context
        private set
    lateinit var store: Store
        private set
    lateinit var repo: Repo
        private set
    lateinit var settings: Settings
        private set
    lateinit var cache: CacheStore
        private set

    /** 首页时间线的续看快照，见 [FeedSnapshotStore]。 */
    lateinit var snapshots: FeedSnapshotStore
        private set

    fun isRepoReady(): Boolean = ::repo.isInitialized

    /** Coil 专用客户端：微博图床与附件域名补全 Referer/UA 及登录 cookie。 */
    val imageClient: OkHttpClient by lazy {
        OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            val host = req.url.host.lowercase()
            val isWeiboHost = IMAGE_REFERER_HOSTS.any { host == it || host.endsWith(".$it") }
            if (!isWeiboHost) {
                chain.proceed(req)
            } else {
                val referer = if (host == "upload.api.weibo.com" || host == "attachment.api.weibo.com") {
                    "https://api.weibo.com/chat/"
                } else {
                    "https://weibo.com/"
                }
                val builder = req.newBuilder()
                    .header("User-Agent", com.mibo.app.data.Api.WEB_UA)
                    .header("Referer", referer)
                if (host == "weibo.com" || host.endsWith(".weibo.com")) {
                    val cookie = runCatching { repo.desktopCookieOrNull() ?: repo.cookie() }.getOrNull()
                    if (!cookie.isNullOrBlank()) builder.header("Cookie", cookie)
                }
                chain.proceed(builder.build())
            }
        }.build()
    }

    private val IMAGE_REFERER_HOSTS = listOf(
        "sinaimg.cn",
        "sinaimg.com",
        "weibocdn.com",
        "weibo.com",
        "weibo.cn",
    )

    var feedVersion by mutableIntStateOf(0)
        private set

    /** `am start --es mibo.route main` 可跳过登录判断直接进主页（调试用）。 */
    var startRoute: String? = null

    /** 待处理的外部深链（流体云/系统跳转传入），由根节点消费后清空。 */
    var pendingDeepLink by mutableStateOf<String?>(null)
        private set
    var deepLinkVersion by mutableIntStateOf(0)
        private set

    fun enqueueDeepLink(rawUrl: String?) {
        val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return
        pendingDeepLink = trimmed
        deepLinkVersion++
    }

    fun consumeDeepLink(): String? {
        val target = pendingDeepLink
        pendingDeepLink = null
        return target
    }

    /** 正在检查更新：设置页那一行据此换成「正在检查…」。 */
    var checkingUpdate by mutableStateOf(false)
        private set

    /** 查到的新版本；非空时根节点弹更新提示，关掉就清空。 */
    var pendingUpdate by mutableStateOf<UpdateInfo?>(null)
        private set

    fun dismissUpdate() {
        pendingUpdate = null
    }

    /**
     * 查一次更新。[manual] 为 true 时无论有没有结果都给反馈（toast），
     * 自动检查只在真的查到新版本时挂上 [pendingUpdate]。
     */
    suspend fun checkUpdate(manual: Boolean): UpdateInfo? {
        if (checkingUpdate) {
            if (manual) toast("正在检查更新…")
            return pendingUpdate
        }
        checkingUpdate = true
        try {
            val current = Updater.currentVersion(context)
            val info = Updater.latest(current, includePrerelease = settings.acceptPrerelease)
            settings.applyUpdateCheckedAt(System.currentTimeMillis())
            if (info == null) {
                pendingUpdate = null
                if (manual) toast("已是最新版本 $current")
                return null
            }
            // 用户明确跳过这个版本就别再拿自动检查烦他，手动检查照旧提示。
            if (!manual && info.version == settings.skippedUpdate) return info
            pendingUpdate = info
            return info
        } catch (t: Throwable) {
            if (manual) toast(t.message ?: "检查更新失败")
            return null
        } finally {
            checkingUpdate = false
        }
    }

    val feedTopEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val detailRefreshEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val followEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profileUpdatedEvents = MutableSharedFlow<User>(extraBufferCapacity = 4)
    var profileRenameVersion by mutableIntStateOf(0)
        private set


    fun notifyFollowChanged() {
        followEvents.tryEmit(Unit)
    }

    fun notifyProfileUpdated(user: User) {
        profileUpdatedEvents.tryEmit(user)
    }

    fun notifyProfileRenameUpdated() {
        profileRenameVersion++
    }

    fun notifyPublished() {
        feedVersion++
    }

    fun requestFeedTop(scrollToTop: Boolean = true) {
        feedTopEvents.tryEmit(scrollToTop)
    }

    fun requestDetailRefresh(statusId: String) {
        if (statusId.isNotBlank()) detailRefreshEvents.tryEmit(statusId)
    }

    /** 应用内 Toast 事件：根节点的玻璃 Toast 宿主订阅显示，替代系统 Toast（系统样式没法做柔光玻璃）。 */
    data class ToastEvent(val message: String, val id: Long)

    var toastEvent by mutableStateOf<ToastEvent?>(null)
        private set

    fun toast(message: String) {
        if (message.isBlank()) return
        toastEvent = ToastEvent(message, System.nanoTime())
    }

    /** 全局玻璃菜单条目：根节点宿主统一渲染成整块柔光玻璃（miuix-glass 的 GlassDropdownPopup）。 */
    data class GlassMenuItem(
        val text: String,
        val summary: String? = null,
        val selected: Boolean = false,
        val icon: ImageVector? = null,
        val danger: Boolean = false,
        val children: List<GlassMenuItem>? = null,
        val onClick: (() -> Unit)? = null,
    )

    /** 一次玻璃菜单请求：锚点窗口坐标 + 条目 + 关闭回调（点外部/返回/选中叶子都会走 dismissGlassMenu）。 */
    data class GlassMenuRequest(
        val anchorBounds: Rect,
        val items: List<GlassMenuItem>,
        val onDismissed: () -> Unit = {},
        val id: Long = 0L,
    )

    var glassMenu by mutableStateOf<GlassMenuRequest?>(null)
        private set

    private var glassMenuSeq = 0L

    fun showGlassMenu(anchorBounds: Rect, items: List<GlassMenuItem>, onDismissed: () -> Unit = {}) {
        if (items.isEmpty()) return
        glassMenuSeq += 1
        glassMenu = GlassMenuRequest(anchorBounds, items, onDismissed, glassMenuSeq)
    }

    fun dismissGlassMenu() {
        val current = glassMenu ?: return
        glassMenu = null
        current.onDismissed()
    }

    /** 点赞前确认的挂起请求：根节点的柔光玻璃对话框宿主渲染它，确认/取消后唤醒等待方。 */
    class LikeConfirmRequest(
        val status: Status,
        val deferred: CompletableDeferred<Boolean>,
    )

    var likeConfirm by mutableStateOf<LikeConfirmRequest?>(null)
        private set

    fun resolveLikeConfirm(ok: Boolean) {
        val current = likeConfirm ?: return
        likeConfirm = null
        current.deferred.complete(ok)
    }

    /**
     * 统一执行微博点赞/取消赞：防重复点击、成功后同步所有列表状态，失败弹出可读提示。
     */
    suspend fun toggleStatusLike(status: Status): Status? {
        if (status.id.isBlank() || repo.isLikeInFlight(status.id)) return null
        if (repo.isLikeInFlight(status.id)) return null
        if (!status.liked && settings.likeConfirm && !repo.guest) {
            // 点赞前先弹柔光玻璃确认（取消赞不确认）；已有确认在等时忽略重复点击。
            if (likeConfirm != null) return null
            val deferred = CompletableDeferred<Boolean>()
            likeConfirm = LikeConfirmRequest(status, deferred)
            try {
                if (!deferred.await()) return null
            } catch (c: CancellationException) {
                if (likeConfirm?.deferred === deferred) likeConfirm = null
                throw c
            }
        }
        return try {
            repo.like(status.id, status.liked)
            val nextLiked = !status.liked
            val nextAttitudes = (status.attitudes + if (status.liked) -1 else 1).coerceAtLeast(0)
            repo.recordStatusLike(status.id, nextLiked, nextAttitudes)
            status.copy(liked = nextLiked, attitudes = nextAttitudes)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            toast(t.message ?: "操作失败")
            null
        }
    }

    fun init(context: Context) {
        if (this::store.isInitialized) return
        this.context = context.applicationContext
        store = Store(this.context)
        FollowTags.attach(store)
        cache = CacheStore(this.context)
        snapshots = FeedSnapshotStore(this.context)
        repo = Repo(this.context, store)
        settings = Settings(this.context)
    }
}
