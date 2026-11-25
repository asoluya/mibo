package com.mibo.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Answer
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Create
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp

object MiboIcons {

    val Repost: ImageVector get() = MiuixIcons.Forward

    val Comment: ImageVector get() = MiuixIcons.Answer

    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 20f)
                curveTo(10.4f, 18.6f, 3.5f, 14f, 3.5f, 9.3f)
                curveTo(3.5f, 6.6f, 5.6f, 4.5f, 8.2f, 4.5f)
                curveTo(9.7f, 4.5f, 11.1f, 5.3f, 12f, 6.6f)
                curveTo(12.9f, 5.3f, 14.3f, 4.5f, 15.8f, 4.5f)
                curveTo(18.4f, 4.5f, 20.5f, 6.6f, 20.5f, 9.3f)
                curveTo(20.5f, 14f, 13.6f, 18.6f, 12f, 20f)
                close()
            }
        }.build()
    }

    /** 发布羽毛：推特写推按钮那支羽毛笔，描边风格与 Heart 一致。 */
    val Feather: ImageVector by lazy {
        ImageVector.Builder(
            name = "Feather",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.24f, 12.24f)
                arcTo(6f, 6f, 0f, false, false, 11.75f, 3.75f)
                lineTo(5f, 10.5f)
                lineTo(5f, 19f)
                lineTo(13.5f, 19f)
                close()
                moveTo(16f, 8f)
                lineTo(2f, 22f)
                moveTo(17.5f, 15f)
                lineTo(9f, 15f)
            }
        }.build()
    }

    /** 管理员：盾牌里一个对勾，安全与账号管理语义，给「账号相关」入口用。 */
    val AdminShield: ImageVector by lazy {
        ImageVector.Builder(
            name = "AdminShield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                lineTo(19f, 5.2f)
                lineTo(19f, 11f)
                curveTo(19f, 15.6f, 16.1f, 19.2f, 12f, 21f)
                curveTo(7.9f, 19.2f, 5f, 15.6f, 5f, 11f)
                lineTo(5f, 5.2f)
                close()
                moveTo(8.8f, 11.6f)
                lineTo(11f, 13.8f)
                lineTo(15.4f, 9.4f)
            }
        }.build()
    }

    val HeartFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "HeartFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 20.4f)
                curveTo(10.2f, 18.8f, 3f, 14f, 3f, 9.2f)
                curveTo(3f, 6.3f, 5.3f, 4f, 8.1f, 4f)
                curveTo(9.7f, 4f, 11.1f, 4.8f, 12f, 6.2f)
                curveTo(12.9f, 4.8f, 14.3f, 4f, 15.9f, 4f)
                curveTo(18.7f, 4f, 21f, 6.3f, 21f, 9.2f)
                curveTo(21f, 14f, 13.8f, 18.8f, 12f, 20.4f)
                close()
            }
        }.build()
    }

    /** 收藏星（描边五角星）。 */
    val Star: ImageVector by lazy {
        outline("Star") {
            moveTo(12f, 2f)
            lineTo(14.5f, 8.6f)
            lineTo(21.5f, 8.9f)
            lineTo(16f, 13.3f)
            lineTo(17.9f, 20.1f)
            lineTo(12f, 16.2f)
            lineTo(6.1f, 20.1f)
            lineTo(8f, 13.3f)
            lineTo(2.5f, 8.9f)
            lineTo(9.5f, 8.6f)
            close()
        }
    }

    /** 收藏星（实心五角星）。 */
    val StarFill: ImageVector by lazy {
        ImageVector.Builder(
            name = "StarFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            lineTo(14.5f, 8.6f)
            lineTo(21.5f, 8.9f)
            lineTo(16f, 13.3f)
            lineTo(17.9f, 20.1f)
            lineTo(12f, 16.2f)
            lineTo(6.1f, 20.1f)
            lineTo(8f, 13.3f)
            lineTo(2.5f, 8.9f)
            lineTo(9.5f, 8.6f)
            close()
            }
        }.build()
    }

    val Emoji: ImageVector by lazy {
        ImageVector.Builder(
            name = "Emoji",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(9f, 9.5f)
                lineTo(9f, 9.6f)
                moveTo(15f, 9.5f)
                lineTo(15f, 9.6f)
                moveTo(8f, 14f)
                curveTo(9f, 15.5f, 10.4f, 16.3f, 12f, 16.3f)
                curveTo(13.6f, 16.3f, 15f, 15.5f, 16f, 14f)
            }
        }.build()
    }

    val At: ImageVector by lazy {
        ImageVector.Builder(
            name = "At",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(19.5f, 12f)
                curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
                curveTo(7.86f, 19.5f, 4.5f, 16.14f, 4.5f, 12f)
                curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
                curveTo(16.14f, 4.5f, 19.5f, 7.86f, 19.5f, 12f)
                verticalLineTo(13.2f)
                curveTo(19.5f, 14.9f, 20.3f, 15.8f, 21.4f, 15.8f)
                curveTo(22.5f, 15.8f, 23f, 14.8f, 23f, 13.2f)
                verticalLineTo(12f)
                moveTo(15.8f, 12f)
                curveTo(15.8f, 14.1f, 14.1f, 15.8f, 12f, 15.8f)
                curveTo(9.9f, 15.8f, 8.2f, 14.1f, 8.2f, 12f)
                curveTo(8.2f, 9.9f, 9.9f, 8.2f, 12f, 8.2f)
                curveTo(14.1f, 8.2f, 15.8f, 9.9f, 15.8f, 12f)
                verticalLineTo(12f)
            }
        }.build()
    }


    // ---- 图标统一走 Miuix 官方图标：有官方对应的一律委托 MiuixIcons（见下方各 getter）；
    // 只有 Miuix 没有对应的（爱心、表情、@、话题、超话、点赞拇指、投票、摄像机、地球、微博标）
    // 才保留自绘，且维持与 Miuix 一致的线性风格。----

    private fun outline(name: String, shape: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = shape,
            )
        }.build()

    val FeedOutline: ImageVector get() = MiuixIcons.Home
    val FeedFilled: ImageVector get() = MiuixIcons.Home
    val MessagesOutline: ImageVector get() = MiuixIcons.Messages
    val MessagesFilled: ImageVector get() = MiuixIcons.Messages
    val ProfileOutline: ImageVector get() = MiuixIcons.ContactsCircle
    val ProfileFilled: ImageVector get() = MiuixIcons.ContactsCircle

    /** 超话前缀：微博官方双环螺旋钻石轮廓。 */
    val SuperTopic: ImageVector by lazy {
        ImageVector.Builder(
            name = "SuperTopic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12.0f, 15.1f)
                lineTo(6.7f, 9.8f)
                lineTo(8.3f, 7.5f)
                lineTo(15.7f, 7.5f)
                lineTo(17.3f, 9.8f)
                lineTo(9.8f, 17.3f)
                lineTo(2.5f, 9.8f)
                lineTo(6.4f, 4.3f)
                lineTo(17.6f, 4.3f)
                lineTo(21.5f, 9.8f)
                lineTo(12.0f, 19.7f)
            }
        }.build()
    }

    /** 视频前缀：线性摄影机图标（圆角机身 + 右侧镜头锥）。 */
    val VideoCamera: ImageVector by lazy {
        ImageVector.Builder(
            name = "VideoCamera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // 机身圆角矩形 (2.5..15.0, 6.2..17.8, r=2.3)
                moveTo(4.8f, 6.2f)
                horizontalLineTo(12.7f)
                curveTo(13.97f, 6.2f, 15.0f, 7.23f, 15.0f, 8.5f)
                verticalLineTo(15.5f)
                curveTo(15.0f, 16.77f, 13.97f, 17.8f, 12.7f, 17.8f)
                horizontalLineTo(4.8f)
                curveTo(3.53f, 17.8f, 2.5f, 16.77f, 2.5f, 15.5f)
                verticalLineTo(8.5f)
                curveTo(2.5f, 7.23f, 3.53f, 6.2f, 4.8f, 6.2f)
                close()
                // 右侧镜头梯形锥（接机身右边，不重复画竖线）
                moveTo(15.0f, 10.2f)
                lineTo(20.4f, 7.1f)
                curveTo(21.05f, 6.72f, 21.5f, 7.12f, 21.5f, 7.9f)
                verticalLineTo(16.1f)
                curveTo(21.5f, 16.88f, 21.05f, 17.28f, 20.4f, 16.9f)
                lineTo(15.0f, 13.8f)
            }
        }.build()
    }

    val Poll: ImageVector by lazy {
        ImageVector.Builder(
            name = "Poll",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 19f)
                lineTo(5f, 11f)
                moveTo(12f, 19f)
                lineTo(12f, 5f)
                moveTo(19f, 19f)
                lineTo(19f, 14f)
            }
        }.build()
    }

    val Check: ImageVector get() = MiuixIcons.Ok

    /** 网页查看：地球经纬线轮廓，外径与多线视觉字重对齐 COUI Regular 顶栏图标。 */
    val Globe: ImageVector by lazy {
        ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.45f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.8f, 12f)
                curveTo(20.8f, 16.86f, 16.86f, 20.8f, 12f, 20.8f)
                curveTo(7.14f, 20.8f, 3.2f, 16.86f, 3.2f, 12f)
                curveTo(3.2f, 7.14f, 7.14f, 3.2f, 12f, 3.2f)
                curveTo(16.86f, 3.2f, 20.8f, 7.14f, 20.8f, 12f)
                close()
                moveTo(15.6f, 12f)
                curveTo(15.6f, 16.86f, 13.99f, 20.8f, 12f, 20.8f)
                curveTo(10.01f, 20.8f, 8.4f, 16.86f, 8.4f, 12f)
                curveTo(8.4f, 7.14f, 10.01f, 3.2f, 12f, 3.2f)
                curveTo(13.99f, 3.2f, 15.6f, 7.14f, 15.6f, 12f)
                close()
                moveTo(3.73f, 9f)
                horizontalLineTo(20.27f)
                moveTo(3.73f, 15f)
                horizontalLineTo(20.27f)
            }
        }.build()
    }




    /** 赞赏码专用：Miuix 风格实心大拇指（官方图标库没有点赞图标，按其填充字形风格自绘）。 */
    val PraiseThumb: ImageVector by lazy {
        ImageVector.Builder(
            name = "PraiseThumb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // 袖口
                moveTo(2.8f, 10.6f)
                horizontalLineTo(6.6f)
                verticalLineTo(20.2f)
                horizontalLineTo(2.8f)
                close()
                // 手掌与翘起的大拇指
                moveTo(7.6f, 20.2f)
                verticalLineTo(11.4f)
                lineTo(12.6f, 3.9f)
                curveTo(13.0f, 3.3f, 13.8f, 3.1f, 14.4f, 3.5f)
                curveTo(15.6f, 4.2f, 16.1f, 5.7f, 15.7f, 7.2f)
                lineTo(15.1f, 9.6f)
                horizontalLineTo(20.3f)
                curveTo(21.3f, 9.6f, 22.0f, 10.5f, 21.8f, 11.5f)
                lineTo(20.4f, 18.4f)
                curveTo(20.2f, 19.4f, 19.4f, 20.2f, 18.4f, 20.2f)
                close()
            }
        }.build()
    }

    /** 收到的赞：线性大拇指轮廓。 */
    val ThumbUp: ImageVector by lazy {
        outline("ThumbUp") {
            // 左侧袖口
            moveTo(4.0f, 11.2f)
            curveTo(4.0f, 10.4f, 4.6f, 9.8f, 5.4f, 9.8f)
            horizontalLineTo(7.6f)
            verticalLineTo(20.0f)
            horizontalLineTo(5.4f)
            curveTo(4.6f, 20.0f, 4.0f, 19.4f, 4.0f, 18.6f)
            close()
            // 右侧大拇指与手掌外框
            moveTo(7.6f, 10.6f)
            lineTo(11.3f, 4.4f)
            curveTo(11.7f, 3.7f, 12.6f, 3.5f, 13.3f, 3.9f)
            curveTo(13.9f, 4.3f, 14.2f, 5.0f, 14.0f, 5.7f)
            lineTo(13.1f, 9.2f)
            horizontalLineTo(18.1f)
            curveTo(19.5f, 9.2f, 20.5f, 10.4f, 20.2f, 11.8f)
            lineTo(18.9f, 18.0f)
            curveTo(18.6f, 19.2f, 17.6f, 20.0f, 16.4f, 20.0f)
            horizontalLineTo(7.6f)
        }
    }

    /** 发出的评论：纸张与正在书写的笔（纯线性）。 */
    val WriteComment: ImageVector get() = MiuixIcons.Create

    /** 浏览历史：线性时钟。 */
    val History: ImageVector get() = MiuixIcons.Recent




    /** 链接前缀：线性图片框图标（圆角边框 + 山峰 + 太阳），用于「查看图片」。 */
    val Image: ImageVector get() = MiuixIcons.Image

    /** 链接前缀：线性微博眼睛与双波纹图标，用于「微博正文」。 */
    val Weibo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Weibo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.5f, 19.6f)
                curveTo(6.1f, 19.6f, 2.8f, 17.5f, 2.8f, 14.3f)
                curveTo(2.8f, 12.1f, 4.7f, 9.4f, 7.6f, 7.7f)
                curveTo(9.8f, 6.4f, 11.7f, 6.9f, 11.1f, 8.8f)
                curveTo(10.8f, 9.6f, 11.4f, 10.0f, 12.4f, 9.6f)
                curveTo(14.6f, 8.7f, 16.5f, 9.3f, 16.0f, 11.0f)
                curveTo(15.8f, 11.7f, 16.2f, 12.1f, 17.0f, 12.5f)
                curveTo(18.0f, 13.1f, 18.4f, 14.0f, 18.4f, 14.9f)
                curveTo(18.4f, 17.7f, 14.9f, 19.6f, 10.5f, 19.6f)
                close()
                moveTo(12.4f, 14.7f)
                curveTo(12.4f, 15.95f, 11.4f, 16.95f, 10.2f, 16.95f)
                curveTo(9.0f, 16.95f, 8.0f, 15.95f, 8.0f, 14.7f)
                curveTo(8.0f, 13.45f, 9.0f, 12.45f, 10.2f, 12.45f)
                curveTo(11.4f, 12.45f, 12.4f, 13.45f, 12.4f, 14.7f)
                close()
                moveTo(15.2f, 6.7f)
                curveTo(17.1f, 6.7f, 18.7f, 8.3f, 18.7f, 10.2f)
                moveTo(15.4f, 3.8f)
                curveTo(18.8f, 3.8f, 21.5f, 6.5f, 21.5f, 9.9f)
            }
        }.build()
    }

    /** 实况图声音开启：线性扬声器与双声波。 */
    val VolumeOn: ImageVector get() = MiuixIcons.VolumeUp

    /** 实况图静音：线性扬声器与叉号。 */
    val VolumeOff: ImageVector get() = MiuixIcons.VolumeOff
}
