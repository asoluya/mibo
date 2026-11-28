package com.mibo.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

enum class FeedTabItem(val key: String, val label: String, val tag: String) {
    None("none", "隐藏", "feed_tab_none"),
    Follow("follow", "关注", "feed_tab_follow"),
    Circle("circle", "好友", "feed_tab_circle"),
    Recommend("recommend", "推荐", "feed_tab_recommend"),
    Local("local", "本地关注", "feed_tab_local"),
    Original("original", "原创微博", "feed_tab_original"),
    Special("special", "特别关注", "feed_tab_special"),
    Secret("secret", "悄悄关注", "feed_tab_secret"),
    Favorites("favorites", "收藏", "feed_tab_favorites"),
    Nearby("nearby", "附近", "feed_tab_nearby"),

    /**
     * 自建的关注分组。分组本身是账号里的数据，页面上按分组逐个列出，选中哪个由
     * [FeedTabSelection.group] 决定；单独留下这一项只用来表示「分组」这一类。
     */
    Group("group", "关注分组", "feed_tab_group"),
    ;

    companion object {
        fun fromKey(key: String?, default: FeedTabItem): FeedTabItem {
            return entries.firstOrNull { it.key == key } ?: default
        }
    }
}

/** 视频画质档位：播放页选一次后固定，之后所有视频都按所选档位取流。 */
enum class VideoQuality(val key: String, val label: String) {
    NoWatermark("nowm", "无水印"),
    Original("orig", "原画"),
    ;

    companion object {
        fun fromKey(key: String?): VideoQuality = entries.firstOrNull { it.key == key } ?: Original
    }
}

/**
 * 首页某一栏看什么。内置流直接用 [item]；关注分组是 [item] 为 [FeedTabItem.Group]
 * 再加上具体那一组的 gid 与名字 —— 分组是账号里的数据，不能写进枚举。
 */
data class FeedTabSelection(
    val item: FeedTabItem,
    val group: FollowGroup? = null,
) {
    /** 分组跟着 gid 走：同一栏里换个分组就是换了一条流。 */
    val key: String
        get() = when {
            item != FeedTabItem.Group -> item.key
            group != null -> "group_${group.gid}"
            else -> FeedTabItem.Group.key
        }

    val label: String
        get() = when {
            item != FeedTabItem.Group -> item.label
            group != null -> group.title.ifBlank { FeedTabItem.Group.label }
            else -> FeedTabItem.Group.label
        }

    val isGroup: Boolean get() = item == FeedTabItem.Group && group != null
}

/**
 * 设置与顶栏「更多」菜单共用的一份选项：内置流在前，关注分组跟在后。
 *
 * 当前选中的分组即使这次没拉到（分组被删了、或者还没加载完）也留在列表里，
 * 否则那一栏会突然没有对应项、而且没法改回去。
 */
fun feedTabOptions(
    groups: List<FollowGroup>,
    allowNone: Boolean,
    current: FeedTabSelection? = null,
): List<FeedTabSelection> = buildList {
    if (allowNone) add(FeedTabSelection(FeedTabItem.None))
    FeedTabItem.entries
        .filter { it != FeedTabItem.None && it != FeedTabItem.Group }
        .forEach { add(FeedTabSelection(it)) }
    groups.forEach { add(FeedTabSelection(FeedTabItem.Group, it)) }
    current?.takeIf { it.isGroup && groups.none { group -> group.gid == it.group?.gid } }
        ?.let { add(it) }
}

class Settings(context: Context) {
    private val sp = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    private companion object {
        const val SearchHistoryLimit = 10

        /** 续看位置在 SharedPreferences 里的前缀，后面接流的 key（旧版不分账号）。 */
        const val ScrollKeyPrefix = "scroll_"

        /** 按账号区分的续看位置前缀，格式 `scroll_v2_<uid>_<key>`。 */
        const val ScopedScrollKeyPrefix = "scroll_v2_"

        /** 首页停在某个关注分组时 [feedTabKey] 里的标记，见 [FeedTabSelection.key]。 */
        const val GroupKeyMarker = "group_"
    }

    /** Recent queries, newest first. Backed by `search_history` as a JSON array. */
    val searchHistory = mutableStateListOf<String>().also { list ->
        list.addAll(parseSearchHistoryJson(sp.getString("search_history", null)))
    }

    // 0 = follow system, 1 = light, 2 = dark
    var darkMode by mutableIntStateOf(
        if (sp.contains("dark_mode")) sp.getInt("dark_mode", 0)
        else if (sp.contains("dark")) if (sp.getBoolean("dark", false)) 2 else 1 else 0
    )
        private set

    var fontScale by mutableFloatStateOf(sp.getFloat("font_scale", 1f).coerceIn(0.8f, 1.2f))
        private set

    /** 「我」页入口及一级设置选项图标配色样式。 */

    var mainTab by mutableIntStateOf(sp.getInt("main_tab", 0))
        private set

    // 0 = 好友圈, 1 = 关注, 2 = 推荐. New installs open on 关注; the v2 key stored 0 = 关注 and
    // 1 = 推荐, so its value shifts up by one when no v3 value exists yet.
    var feedTab by mutableIntStateOf(
        sp.getInt("feed_tab_v3", (sp.getInt("feed_tab_v2", 0) + 1).coerceIn(0, 2))
    )
        private set

    /** 自定义微博页左侧栏浏览内容（默认好友圈，可设为隐藏、内置流或某个关注分组）。 */
    var feedLeftTab by mutableStateOf(loadFeedSelection("feed_left", FeedTabItem.Circle))
        private set

    /** 自定义微博页中间栏浏览内容（默认关注，不可设为隐藏）。 */
    var feedCenterTab by mutableStateOf(loadFeedSelection("feed_center", FeedTabItem.Follow))
        private set

    /** 自定义微博页右侧栏浏览内容（默认推荐，可设为隐藏、内置流或某个关注分组）。 */
    var feedRightTab by mutableStateOf(loadFeedSelection("feed_right", FeedTabItem.Recommend))
        private set

    /** 记录当前停留的微博页栏标识（例如 "left_circle", "follow", "right_recommend"）。 */
    var feedTabKey by mutableStateOf(sp.getString("feed_tab_key", "follow").orEmpty())
        private set

    /**
     * 「本地关注」固定用这个账号的 cookie 取数；空串表示跟随当前登录账号。
     * 标记表本身与账号无关，这条只决定拿谁的身份去拉那批人的内容。
     */
    var localFollowUid by mutableStateOf(sp.getString("local_follow_uid", "").orEmpty())
        private set

    /** 用户在登录页选了「不登录，纯本地浏览」；有登录账号时以账号为准，这条不起作用。 */
    var localMode by mutableStateOf(sp.getBoolean("local_mode", false))
        private set

    /**
     * 附近看哪座城：默认跟着设备定位走；[nearbyCustom] 打开后固定看 [nearbyPlaceName]，
     * 坐标存在下面两项里。微博的同城流只认坐标，选城市也就是换一组坐标。
     */
    var nearbyCustom by mutableStateOf(sp.getBoolean("nearby_custom", false))
        private set

    var nearbyPlaceName by mutableStateOf(sp.getString("nearby_place_name", "").orEmpty())
        private set

    var nearbyPlaceLat by mutableFloatStateOf(sp.getFloat("nearby_place_lat", 0f))
        private set

    var nearbyPlaceLon by mutableFloatStateOf(sp.getFloat("nearby_place_lon", 0f))
        private set

    /** 启动时自动查更新，默认开。 */
    var autoCheckUpdate by mutableStateOf(sp.getBoolean("auto_check_update", true))
        private set

    /** 启动时检测更新（启动静默检查的触发开关，与自动检查更新同时打开才生效），默认开。 */
    var checkUpdateOnStartup by mutableStateOf(sp.getBoolean("check_update_on_startup", true))
        private set

    /** 接收预发布版本；关闭后更新检查只认正式版。默认开，和以前的更新行为一致。 */
    var acceptPrerelease by mutableStateOf(sp.getBoolean("accept_prerelease", true))
        private set

    /**
     * 顶栏与悬浮底栏的玻璃混色不透明度（0.3–1.0）：模糊开启时顶栏 textureBlur 的混色浓度
     * 与底栏底色都按它走。默认 0.78，即转换前 COUI 玻璃的既有观感。
     */
    var barGlassAlpha by mutableFloatStateOf(sp.getFloat("bar_glass_alpha", 0.78f))
        private set

    /** 悬浮底栏整体偏移（dp）：x 正值右移、y 正值下移，范围 -32..32，默认 0（原位置）。 */
    var barOffsetX by mutableFloatStateOf(sp.getFloat("bar_offset_x", 0f))
        private set

    var barOffsetY by mutableFloatStateOf(sp.getFloat("bar_offset_y", 0f))
        private set

    /** 悬浮底栏大小比例：1 为原大小，范围 0.85..1.15，经缩放密度作用于底栏与展开三钮。 */
    var barScale by mutableFloatStateOf(sp.getFloat("bar_scale", 1f))
        private set

    /** 展开按钮自动关闭：开启后底栏三钮展开时，点任意其他地方就收起。默认关（展开后常驻）。 */
    var expandAutoClose by mutableStateOf(sp.getBoolean("expand_auto_close", false))
        private set

    /** 滑动时隐藏顶栏和底栏：向下滑动浏览时自动收起两栏，向上滑动恢复。默认关。 */
    var hideBarsOnScroll by mutableStateOf(sp.getBoolean("hide_bars_on_scroll", true))
        private set


    /** 使用内置浏览器打开外部网页：开启后在应用内 WebView 打开，关闭后直接跳转系统浏览器。默认开。 */
    var inAppBrowser by mutableStateOf(sp.getBoolean("in_app_browser", true))
        private set

    /** 实况图滑动自动播放：开启后进入或滑动到实况图时自动播放一次，结束后仅支持长按重播。默认开。 */
    var livePhotoAutoPlay by mutableStateOf(sp.getBoolean("live_photo_auto_play", true))
        private set

    /** 实况图声音播放：开启后播放实况图附带的视频声音，关闭时静音播放。默认开。 */
    var livePhotoSound by mutableStateOf(sp.getBoolean("live_photo_sound", true))
        private set

    /** 视频画质：播放页顶栏选一次后固定，解析与播放都按此档位取流。默认原画。 */
    var videoQuality by mutableStateOf(VideoQuality.fromKey(sp.getString("video_quality", null)))
        private set

    /** 前台监测新帖：应用在前台时每 20 秒检测一次关注流，有新微博时刷新按钮发光提醒。默认关。 */
    var monitorNewPosts by mutableStateOf(sp.getBoolean("monitor_new_posts", false))

    var likeConfirm by mutableStateOf(sp.getBoolean("like_confirm", true))
        private set

    /** 后台通知总开关：开启后前台服务定时轮询新的赞 / 评论 / @，有新的就发系统通知。默认关。 */
    var notifyEnabled by mutableStateOf(sp.getBoolean("notify_enabled", false))
        private set

    /** 后台通知轮询间隔（秒）：越短越及时也越耗电。默认 60 秒，范围 10..3600。 */
    var notifyIntervalSeconds by mutableStateOf(sp.getInt("notify_interval_seconds", 60))
        private set

    /** 上次检查更新的时间戳。 */
    var updateCheckedAt by mutableLongStateOf(sp.getLong("update_checked_at", 0L))
        private set

    /** 点过「跳过此版本」的版本号：自动检查时不再拿它打扰。 */
    var skippedUpdate by mutableStateOf(sp.getString("skipped_update", "").orEmpty())
        private set

    fun applyMainTab(value: Int) {
        mainTab = value
        sp.edit().putInt("main_tab", value).apply()
    }

    fun applyVideoQuality(value: VideoQuality) {
        videoQuality = value
        sp.edit().putString("video_quality", value.key).apply()
    }

    /** Records a submitted query, newest first, keeping at most [SearchHistoryLimit] entries. */
    fun pushSearchHistory(keyword: String) {
        if (!pushSearchHistoryItem(searchHistory, keyword, SearchHistoryLimit)) return
        saveSearchHistory()
    }

    /** Removes a single entry from [searchHistory] and persists the change. */
    fun removeSearchHistory(keyword: String) {
        if (!removeSearchHistoryItem(searchHistory, keyword)) return
        saveSearchHistory()
    }

    fun clearSearchHistory() {
        searchHistory.clear()
        saveSearchHistory()
    }

    private fun saveSearchHistory() {
        sp.edit().putString("search_history", encodeSearchHistoryJson(searchHistory)).apply()
    }

    fun applyFeedTab(value: Int) {
        feedTab = value.coerceIn(0, 2)
        sp.edit().putInt("feed_tab_v3", feedTab).apply()
    }

    fun applyFeedLeftTab(selection: FeedTabSelection) {
        feedLeftTab = selection
        saveFeedSelection("feed_left", selection)
    }

    fun applyFeedCenterTab(selection: FeedTabSelection) {
        if (selection.item == FeedTabItem.None) return
        feedCenterTab = selection
        saveFeedSelection("feed_center", selection)
    }

    fun applyFeedRightTab(selection: FeedTabSelection) {
        feedRightTab = selection
        saveFeedSelection("feed_right", selection)
    }

    /**
     * 某个账号的首页栏上次停在哪儿：卡片下标与偏了几像素。按账号和流分开记，
     * 切换账号后各保持各自的阅读位置，不会互相带跑。
     */
    fun feedScroll(owner: String, key: String): Pair<Int, Int>? {
        val raw = sp.getString(scopedFeedScrollPrefKey(owner, key), null)
            ?: sp.getString(ScrollKeyPrefix + key, null)
            ?: return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val index = parts[0].toIntOrNull() ?: return null
        val offset = parts[1].toIntOrNull() ?: return null
        return index.coerceAtLeast(0) to offset.coerceAtLeast(0)
    }

    fun applyFeedScroll(owner: String, key: String, index: Int, offset: Int) {
        val editor = sp.edit()
            .putString(
                scopedFeedScrollPrefKey(owner, key),
                "${index.coerceAtLeast(0)},${offset.coerceAtLeast(0)}",
            )
        val legacy = ScrollKeyPrefix + key
        if (sp.contains(legacy)) editor.remove(legacy)
        editor.apply()
    }

    /** 切号时只清旧版未按账号区分的滚动位置，避免串到新账号，保留各账号自己的位置。 */
    fun clearLegacyFeedScrolls() {
        val editor = sp.edit()
        sp.all.keys
            .filter { it.startsWith(ScrollKeyPrefix) && !it.startsWith(ScopedScrollKeyPrefix) }
            .forEach(editor::remove)
        editor.apply()
    }

    /** 退出登录时清掉所有账号的续看位置。 */
    fun clearFeedScrolls() {
        val editor = sp.edit()
        sp.all.keys.filter { it.startsWith(ScrollKeyPrefix) }.forEach(editor::remove)
        editor.apply()
    }

    /**
     * 换账号 / 退出登录时把三栏内容复位。关注分组是账号里的数据，gid 换了账号就不存在，
     * 留着只会让首页出现一条打不开的空栏（中间栏不可隐藏，还会顶掉「关注」）。
     */
    fun clearGroupFeedTabs() {
        if (feedLeftTab.isGroup) applyFeedLeftTab(FeedTabSelection(FeedTabItem.Circle))
        if (feedCenterTab.isGroup) applyFeedCenterTab(FeedTabSelection(FeedTabItem.Follow))
        if (feedRightTab.isGroup) applyFeedRightTab(FeedTabSelection(FeedTabItem.Recommend))
        // 栏标识形如 "center_group_5012345"：带上位置前缀，用 contains 才兜得住。
        if (feedTabKey.isEmpty() || feedTabKey.contains(GroupKeyMarker)) {
            applyFeedTabKey(FeedTabItem.Follow.key)
        }
    }

    private fun loadFeedSelection(prefix: String, default: FeedTabItem): FeedTabSelection {
        val item = FeedTabItem.fromKey(sp.getString("${prefix}_tab", null), default)
        if (item != FeedTabItem.Group) return FeedTabSelection(item)
        val gid = sp.getString("${prefix}_group_gid", "").orEmpty()
        val title = sp.getString("${prefix}_group_title", "").orEmpty()
        // 只存到「分组」这一档但没有具体组时退回内置的默认流，免得首页出现一条空栏。
        return if (gid.isBlank()) FeedTabSelection(default) else FeedTabSelection(item, FollowGroup(gid, title))
    }

    private fun saveFeedSelection(prefix: String, selection: FeedTabSelection) {
        sp.edit()
            .putString("${prefix}_tab", selection.item.key)
            .putString("${prefix}_group_gid", selection.group?.gid.orEmpty())
            .putString("${prefix}_group_title", selection.group?.title.orEmpty())
            .apply()
    }

    /** 附近改回跟设备定位走。 */
    fun applyNearbyDevice() {
        nearbyCustom = false
        sp.edit().putBoolean("nearby_custom", false).apply()
    }

    /** 附近固定看某座城：名字只用来在界面上说明现在看的是哪儿。 */
    fun applyNearbyPlace(name: String, lat: Double, lon: Double) {
        nearbyCustom = true
        nearbyPlaceName = name
        nearbyPlaceLat = lat.toFloat()
        nearbyPlaceLon = lon.toFloat()
        sp.edit()
            .putBoolean("nearby_custom", true)
            .putString("nearby_place_name", name)
            .putFloat("nearby_place_lat", lat.toFloat())
            .putFloat("nearby_place_lon", lon.toFloat())
            .apply()
    }

    fun applyFeedTabKey(key: String) {
        feedTabKey = key
        sp.edit().putString("feed_tab_key", key).apply()
    }

    fun applyAutoCheckUpdate(value: Boolean) {
        autoCheckUpdate = value
        sp.edit().putBoolean("auto_check_update", value).apply()
    }

    fun applyCheckUpdateOnStartup(value: Boolean) {
        checkUpdateOnStartup = value
        sp.edit().putBoolean("check_update_on_startup", value).apply()
    }

    fun applyAcceptPrerelease(value: Boolean) {
        acceptPrerelease = value
        sp.edit().putBoolean("accept_prerelease", value).apply()
    }

    fun applyBarGlassAlpha(value: Float) {
        val clamped = value.coerceIn(0.3f, 1f)
        barGlassAlpha = clamped
        sp.edit().putFloat("bar_glass_alpha", clamped).apply()
    }

    fun applyBarOffsetX(value: Float) {
        val clamped = value.coerceIn(-32f, 32f)
        barOffsetX = clamped
        sp.edit().putFloat("bar_offset_x", clamped).apply()
    }

    fun applyBarOffsetY(value: Float) {
        val clamped = value.coerceIn(-32f, 32f)
        barOffsetY = clamped
        sp.edit().putFloat("bar_offset_y", clamped).apply()
    }

    fun applyBarScale(value: Float) {
        val clamped = value.coerceIn(0.85f, 1.15f)
        barScale = clamped
        sp.edit().putFloat("bar_scale", clamped).apply()
    }

    /** 首页「收藏」流看哪一份：false 本地收藏（默认），true 账号云端收藏。 */
    var favoritesCloud by mutableStateOf(sp.getBoolean("favorites_cloud", false))
        private set

    fun applyFavoritesCloud(value: Boolean) {
        favoritesCloud = value
        sp.edit().putBoolean("favorites_cloud", value).apply()
    }


    fun applyExpandAutoClose(value: Boolean) {
        expandAutoClose = value
        sp.edit().putBoolean("expand_auto_close", value).apply()
    }

    fun applyHideBarsOnScroll(value: Boolean) {
        hideBarsOnScroll = value
        sp.edit().putBoolean("hide_bars_on_scroll", value).apply()
    }


    fun applyInAppBrowser(value: Boolean) {
        inAppBrowser = value
        sp.edit().putBoolean("in_app_browser", value).apply()
    }






    fun applyLivePhotoAutoPlay(value: Boolean) {
        livePhotoAutoPlay = value
        sp.edit().putBoolean("live_photo_auto_play", value).apply()
    }

    fun applyLivePhotoSound(value: Boolean) {
        livePhotoSound = value
        sp.edit().putBoolean("live_photo_sound", value).apply()
    }

    fun applyMonitorNewPosts(value: Boolean) {
        monitorNewPosts = value
        sp.edit().putBoolean("monitor_new_posts", value).apply()
    }

    /** 监测新帖的检测间隔（秒）：前台时每隔该秒数轻查一次关注流，默认 20。 */
    var monitorIntervalSeconds by mutableStateOf(sp.getInt("monitor_interval_seconds", 20))
        private set

    fun applyMonitorIntervalSeconds(value: Int) {
        val coerced = value.coerceIn(5, 3600)
        monitorIntervalSeconds = coerced
        sp.edit().putInt("monitor_interval_seconds", coerced).apply()
    }

    fun applyLikeConfirm(value: Boolean) {
        likeConfirm = value
        sp.edit().putBoolean("like_confirm", value).apply()
    }

    /** 后台通知是否也推关注新帖：默认关；开了之后每轮轮询顺带查关注流，有新帖发通知。 */
    var notifyNewPosts by mutableStateOf(sp.getBoolean("notify_new_posts", false))
        private set

    fun applyNotifyNewPosts(value: Boolean) {
        notifyNewPosts = value
        sp.edit().putBoolean("notify_new_posts", value).apply()
    }

    /** 只推送特别关注：开后互动通知与新帖推送只报特别关注的人，名单拉不到时不拦截。 */
    var notifySpecialOnly by mutableStateOf(sp.getBoolean("notify_special_only", false))
        private set

    fun applyNotifySpecialOnly(value: Boolean) {
        notifySpecialOnly = value
        sp.edit().putBoolean("notify_special_only", value).apply()
    }

    fun applyNotifyEnabled(value: Boolean) {
        notifyEnabled = value
        sp.edit().putBoolean("notify_enabled", value).apply()
    }

    fun applyNotifyIntervalSeconds(value: Int) {
        val coerced = value.coerceIn(10, 3600)
        notifyIntervalSeconds = coerced
        sp.edit().putInt("notify_interval_seconds", coerced).apply()
    }


    fun applyLocalFollowUid(uid: String) {
        localFollowUid = uid
        sp.edit().putString("local_follow_uid", uid).apply()
    }

    fun applyLocalMode(value: Boolean) {
        localMode = value
        sp.edit().putBoolean("local_mode", value).apply()
    }

    fun applyUpdateCheckedAt(value: Long) {
        updateCheckedAt = value
        sp.edit().putLong("update_checked_at", value).apply()
    }

    fun applySkippedUpdate(value: String) {
        skippedUpdate = value
        sp.edit().putString("skipped_update", value).apply()
    }

    /** 超话签到没有状态查询接口，签成功/已签后把当地日期记下来，当天再进超话页直接显示已签到。 */
    fun superTopicCheckinDay(containerId: String): String =
        sp.getString("st_checkin_$containerId", "").orEmpty()

    fun markSuperTopicCheckin(containerId: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        sp.edit().putString("st_checkin_$containerId", today).apply()
    }

    fun isSuperTopicCheckedInToday(containerId: String): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        return superTopicCheckinDay(containerId) == today
    }

    /** 浏览过的超话：打开超话页时记录（id/名称/头像），最近在前，最多 24 条。 */
    fun recentSuperTopics(): List<RecentSuperTopic> {
        val raw = sp.getString("recent_super_topics", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").trim()
                val title = o.optString("title").trim()
                if (id.isBlank() || title.isBlank()) {
                    null
                } else {
                    RecentSuperTopic(id, title, o.optString("avatar"))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordSuperTopicVisit(containerId: String, title: String, avatar: String) {
        if (containerId.isBlank() || title.isBlank()) return
        val rest = recentSuperTopics().filter { !it.containerId.equals(containerId, ignoreCase = true) }
        val merged = (listOf(RecentSuperTopic(containerId, title, avatar)) + rest).take(24)
        val arr = org.json.JSONArray()
        merged.forEach { t ->
            arr.put(
                org.json.JSONObject().apply {
                    put("id", t.containerId)
                    put("title", t.title)
                    put("avatar", t.avatar)
                },
            )
        }
        sp.edit().putString("recent_super_topics", arr.toString()).apply()
    }

    fun clearRecentSuperTopics() {
        sp.edit().remove("recent_super_topics").apply()
    }

    fun applyDarkMode(value: Int) {
        darkMode = value
        sp.edit().putInt("dark_mode", value).apply()
    }

    fun applyFontScale(value: Float) {
        val clamped = value.coerceIn(0.8f, 1.2f)
        fontScale = clamped
        sp.edit().putFloat("font_scale", clamped).apply()
    }

    fun cycleDarkMode(): Int {
        val next = (darkMode + 1) % 3
        applyDarkMode(next)
        return next
    }
}


internal fun parseSearchHistoryJson(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty() && item !in this) add(item)
            }
        }
    }.getOrDefault(emptyList())
}

internal fun encodeSearchHistoryJson(history: List<String>): String {
    val arr = JSONArray()
    history.forEach(arr::put)
    return arr.toString()
}

internal fun pushSearchHistoryItem(
    history: MutableList<String>,
    keyword: String,
    limit: Int = 10,
): Boolean {
    val query = keyword.trim()
    if (query.isEmpty()) return false
    history.remove(query)
    history.add(0, query)
    while (history.size > limit) {
        history.removeAt(history.lastIndex)
    }
    return true
}

internal fun removeSearchHistoryItem(
    history: MutableList<String>,
    keyword: String,
): Boolean = history.remove(keyword)

internal fun scopedFeedScrollPrefKey(owner: String, key: String): String =
    "scroll_v2_${snapshotOwnerKey(owner)}_$key"
