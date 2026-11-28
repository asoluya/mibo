package com.mibo.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.text.Html
import com.mibo.app.App
import com.mibo.app.MainActivity
import com.mibo.app.data.NotificationTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHANNEL_INTERACTIONS = "mibo_interactions"
private const val CHANNEL_SERVICE = "mibo_notify_service"
private const val SERVICE_NOTIFICATION_ID = 1001
private const val STATE_PREFS = "notify_poll_state"

/**
 * 后台通知轮询（方案 2）：前台服务常驻，按设置间隔拉一次赞 / 评论 / @我，
 * 发现比上次记录更新的条目就发系统通知。只报增量：首次轮询只记下最新一条，不补发历史。
 */
class NotifyPollService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            SERVICE_NOTIFICATION_ID,
            buildServiceNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (job?.isActive != true) {
            job = scope.launch { pollLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun pollLoop() {
        while (true) {
            runCatching { pollOnce() }
            val seconds = runCatching { App.settings.notifyIntervalSeconds }.getOrDefault(60).coerceIn(10, 3600)
            delay(seconds * 1_000L)
        }
    }

    private data class Check(
        val key: String,
        val label: String,
        val notificationId: Int,
        val tab: NotificationTab,
    )

    private suspend fun pollOnce() {
        if (!App.isRepoReady() || App.repo.guest) return
        // 只推送特别关注：名单走 Repo 缓存；开关关着或名单为空（拉取失败）时不拦截，避免误杀全部通知。
        val specialOnly = runCatching { App.settings.notifySpecialOnly }.getOrDefault(false)
        val specialUids: Set<String> = if (specialOnly) {
            runCatching { App.repo.specialFollowUidSet() }.getOrDefault(emptySet())
        } else {
            emptySet()
        }
        fun fromSpecial(uid: String?): Boolean =
            !specialOnly || specialUids.isEmpty() || (uid != null && uid in specialUids)
        val checks = listOf(
            Check("like", "赞", 1002, NotificationTab.Like),
            Check("comment", "评论", 1003, NotificationTab.Comment),
            Check("at_weibo", "@我", 1004, NotificationTab.AtWeibo),
            Check("at_comment", "@我", 1004, NotificationTab.AtComment),
        )
        val prefs = getSharedPreferences(STATE_PREFS, MODE_PRIVATE)
        for (check in checks) {
            val page = runCatching { App.repo.notifications(check.tab) }.getOrNull() ?: continue
            // @微博 返回的是微博列表，其余返回通知列表，统一成 (id, 一行文案, 作者 uid)。
            val entries: List<Triple<String, String, String>> = if (check.tab == NotificationTab.AtWeibo) {
                page.statuses.map { status ->
                    Triple(
                        status.id,
                        listOf(status.user?.name.orEmpty(), plainText(status.textHtml))
                            .filter { it.isNotBlank() }.joinToString("："),
                        status.user?.id.orEmpty(),
                    )
                }
            } else {
                page.notifications.map { item ->
                    val head = listOf(item.user?.name.orEmpty(), item.actionLabel)
                        .filter { it.isNotBlank() }.joinToString(" ")
                    val body = plainText(item.textHtml)
                    Triple(item.id, if (body.isBlank()) head else "$head：$body", item.user?.id.orEmpty())
                }
            }
            if (entries.isEmpty()) continue
            val topId = entries.first().first
            val stateKey = "last_${check.key}"
            val lastSeen = prefs.getString(stateKey, null)
            if (lastSeen == null) {
                prefs.edit().putString(stateKey, topId).apply()
                continue
            }
            if (topId == lastSeen) continue
            val knownIndex = entries.indexOfFirst { it.first == lastSeen }
            val newEntries = entries.take(if (knownIndex >= 0) knownIndex else entries.size)
            prefs.edit().putString(stateKey, topId).apply()
            val deliverable = newEntries.filter { fromSpecial(it.third) }
            if (deliverable.isNotEmpty()) {
                postInteractionNotification(
                    this, check.notificationId, check.label, deliverable.size, deliverable.first().second,
                )
            }
        }
        // 新帖推送（单独开关）：顺带查一次关注流，榜首变了就按同样的规则发一条。
        if (runCatching { App.settings.notifyNewPosts }.getOrDefault(false)) {
            val items = runCatching { App.repo.friends(null, force = true) }.getOrNull()?.items.orEmpty()
            if (items.isNotEmpty()) {
                val topId = items.first().id
                val stateKey = "last_friends"
                val lastSeen = prefs.getString(stateKey, null)
                if (lastSeen == null) {
                    prefs.edit().putString(stateKey, topId).apply()
                } else if (topId != lastSeen) {
                    val knownIndex = items.indexOfFirst { it.id == lastSeen }
                    val newItems = items.take(if (knownIndex >= 0) knownIndex else items.size)
                    prefs.edit().putString(stateKey, topId).apply()
                    val deliverable = newItems.filter { fromSpecial(it.user?.id) }
                    if (deliverable.isNotEmpty()) {
                        val top = deliverable.first()
                        val firstText = listOf(top.user?.name.orEmpty(), plainText(top.textHtml))
                            .filter { it.isNotBlank() }.joinToString("：")
                        postInteractionNotification(this, 1005, "微博", deliverable.size, firstText)
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, NotifyPollService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotifyPollService::class.java))
        }

        /** 开关变化后调用：开则拉起服务，关则停掉。 */
        fun sync(context: Context) {
            if (runCatching { App.settings.notifyEnabled }.getOrDefault(false)) start(context) else stop(context)
        }
    }
}

/** 开机后若通知开关是开的，把轮询服务拉起来（配合设置里的自启动引导）。 */
class NotifyBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching {
            if (App.settings.notifyEnabled) NotifyPollService.start(context)
        }
    }
}

private fun plainText(html: String): String =
    runCatching { Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString() }
        .getOrDefault(html)
        .replace('\u00A0', ' ')
        .trim()

private fun ensureChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_INTERACTIONS, "互动通知", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "新的赞、评论和 @"
        },
    )
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_SERVICE, "通知监测服务", NotificationManager.IMPORTANCE_LOW).apply {
            description = "后台通知轮询服务的常驻通知"
        },
    )
}

private fun openAppIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun buildServiceNotification(context: Context): Notification {
    val seconds = runCatching { App.settings.notifyIntervalSeconds }.getOrDefault(60)
    return Notification.Builder(context, CHANNEL_SERVICE)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Mibo 通知监测运行中")
        .setContentText("每 $seconds 秒检查一次新的赞、评论和 @")
        .setOngoing(true)
        .setContentIntent(openAppIntent(context))
        .build()
}

private fun postInteractionNotification(
    context: Context,
    notificationId: Int,
    label: String,
    newCount: Int,
    line: String,
) {
    ensureChannels(context)
    val title = if (newCount > 1) "新的$label · $newCount 条" else "新的$label"
    val notification = Notification.Builder(context, CHANNEL_INTERACTIONS)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(line)
        .setStyle(Notification.BigTextStyle().bigText(line))
        .setAutoCancel(true)
        .setContentIntent(openAppIntent(context))
        .build()
    context.getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
}
