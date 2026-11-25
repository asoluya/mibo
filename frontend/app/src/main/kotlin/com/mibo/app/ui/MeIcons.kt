package com.mibo.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update

/**
 * 「我」页面入口图标的功能色系（浅色 / 深色双套值，暗底略提亮保证对比度）：
 * - [Blue]：核心主干、连接、账号身份与个人记录
 * - [Orange]：个性化外观、交互体验与创作反馈
 * - [Yellow]：珍藏星标与热点发现聚光
 * - [Green]：对话通信、空间定位与应用浏览行为
 * - [Red]：强情感 / 高警示（极度克制，仅用于点赞爱心）
 * - [Neutral]：系统更新、关于与杂项兜底
 */
internal enum class MeIconTint(val light: Color, val dark: Color) {
    Green(Color(0xFF00BD13), Color(0xFF24B232)),
    Blue(Color(0xFF0080FF), Color(0xFF1A8CFF)),
    Orange(Color(0xFFFF7700), Color(0xFFF08222)),
    Yellow(Color(0xFFFFB200), Color(0xFFE5A100)),
    Red(Color(0xFFEB3B2F), Color(0xFFEB493D)),
    Neutral(Color(0xFF5C6066), Color(0xFF9EA4AD)),
    ;

    fun resolve(dark: Boolean): Color = if (dark) this.dark else light
}

/**
 * 「我」页面入口图标：统一使用 Miuix 线性图标（不足的用应用自家的 [MiboIcons] 线性图标），
 * 与全应用风格一致，不再使用 ColorOS 式双色面性图标。着色由 HubRowIcon 按
 * meIconStyle（单色 / 彩色 / 强调色）统一处理。
 */
internal object MeIcons {

    /** 我的微博：文稿。 */
    val Statuses: ImageVector get() = MiuixIcons.Notes

    /** 我的收藏 / 本地收藏：星标。 */
    val Favorites: ImageVector get() = MiuixIcons.Favorites

    /** 我赞过的：爱心。 */
    val MyLikes: ImageVector get() = MiboIcons.Heart

    /** 收到的赞：点赞拇指。 */
    val ReceivedLikes: ImageVector get() = MiboIcons.ThumbUp

    /** 收到的评论：评论气泡。 */
    val ReceivedComments: ImageVector get() = MiboIcons.Comment

    /** 发出的评论：写评论。 */
    val SentComments: ImageVector get() = MiboIcons.WriteComment

    /** 草稿箱：编辑。 */
    val Drafts: ImageVector get() = MiuixIcons.Create

    /** 浏览历史：时钟。 */
    val History: ImageVector get() = MiboIcons.History

    /** 我的超话：井号。 */
    val Topic: ImageVector get() = MiboIcons.Topic

    /** 附近：定位。 */
    val Nearby: ImageVector get() = MiuixIcons.Location

    /** 显示与外观设置：滑杆调节。 */
    val DisplaySettings: ImageVector get() = MiuixIcons.Tune

    /** 信息流设置：首页。 */
    val FeedSettings: ImageVector get() = MiuixIcons.Home

    /** 账号与内容设置：联系人。 */
    val AccountSettings: ImageVector get() = MiuixIcons.Contacts

    /** 更新设置：更新。 */
    val UpdateSettings: ImageVector get() = MiuixIcons.Update

    /** 其他设置：齿轮。 */
    val OtherSettings: ImageVector get() = MiuixIcons.Settings
}
