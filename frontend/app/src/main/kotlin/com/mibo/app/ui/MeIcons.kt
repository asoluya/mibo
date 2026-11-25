package com.mibo.app.ui

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

/**
 * 「我」页面入口图标：统一使用 Miuix 线性图标（不足的用应用自家的 [MiboIcons] 线性图标），
 * 与全应用风格一致，不再使用 ColorOS 式双色面性图标。着色由 HubRowIcon 统一用强调色。
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

    /** 其他设置：齿轮。 */
    val OtherSettings: ImageVector get() = MiuixIcons.Settings
}
