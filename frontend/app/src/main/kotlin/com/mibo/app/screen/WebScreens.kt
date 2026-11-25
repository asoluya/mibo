package com.mibo.app.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.Api
import com.mibo.app.data.CookieVault
import com.mibo.app.ui.MiboIcons
import com.mibo.app.ui.NavState
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.Route
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.WeiboWebView
import com.mibo.app.ui.components.cleanMobileChromeUserAgent
import com.mibo.app.ui.openExternalUrl
import com.mibo.app.ui.openAppScheme
import com.mibo.app.ui.isExternalShopLink
import com.mibo.app.ui.openShopApp
import com.mibo.app.ui.unwrapShopRedirect
import com.mibo.app.ui.SHOP_GOODS_AUTO_JUMP_JS
import com.mibo.app.ui.isShopGoodsPage
import com.mibo.app.ui.openExternalUrl
import com.mibo.app.ui.pageContainerColor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

@Composable
fun WebScreen(nav: NavState, url: String, title: String) {
    val context = LocalContext.current
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val attemptedShopLandings = remember { mutableSetOf<String>() }
    // 外链已递给淘宝 / 美团等 App 后，内置网页没有留下的意义，自动退回上一页，
    // 免得用户从外部 App 回来还停在空白中转页上。
    var handedOff by remember { mutableStateOf(false) }
    LaunchedEffect(handedOff) {
        if (handedOff) nav.pop()
    }

    fun currentPageUrl(): String =
        webView?.url
            ?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
            ?: url

    BackHandler {
        if (canGoBack) webView?.goBack() else nav.pop()
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            PinnedBarDensity {
                SmallTopAppBar(
                    color = pageContainerColor(),
                    title = title,
                    navigationIcon = {
                        BackIconButton {
                            if (canGoBack) webView?.goBack() else nav.pop()
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("url", currentPageUrl()))
                                App.toast("已复制链接")
                            },
                            modifier = Modifier.testTag("web_copy_url"),
                        ) {
                            Icon(MiuixIcons.Copy, "复制链接", Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = { openExternalUrl(context, currentPageUrl()) },
                            modifier = Modifier.testTag("web_open_external"),
                        ) {
                            Icon(MiboIcons.Globe, "在浏览器中打开", Modifier.size(24.dp))
                        }
                    },
                )
            }
        },
    ) { padding ->
        val webUserAgent = remember(url, App.repo.session?.loginType) {
            val host = android.net.Uri.parse(url).host?.lowercase().orEmpty()
            val isDesktopWeibo = host == "weibo.com" || host == "www.weibo.com"
            if (App.repo.session?.loginType == com.mibo.app.data.LOGIN_TYPE_CLIENT && isDesktopWeibo) {
                com.mibo.app.data.Api.DESKTOP_UA
            } else {
                com.mibo.app.data.Api.WEB_UA
            }
        }
        WeiboWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding),
            userAgent = webUserAgent,
            onCreated = { webView = it },
            onBackAvailable = { canGoBack = it },
            onCustomScheme = { target ->
                val opened = openAppScheme(context, target)
                if (opened) handedOff = true
                opened
            },
            onUrlLoading = { _, rawTarget ->
                val target = unwrapShopRedirect(rawTarget)
                if (isExternalShopLink(target)) {
                    // 定向投递成功才拦下；失败（App 没装、或短链域名如 dpurl.cn 没有 App 认领）时
                    // 放行让 WebView 继续加载，不要甩给系统浏览器——落地页（如 click.meituan.com）
                    // 自身的跳端 scheme 会再被 onCustomScheme 接住打开 App。
                    val delivered = openShopApp(context, target)
                    if (delivered) handedOff = true
                    delivered
                } else {
                    false
                }
            },
            onPageFinished = { finishedUrl ->
                if (isShopGoodsPage(finishedUrl)) {
                    webView?.evaluateJavascript(SHOP_GOODS_AUTO_JUMP_JS, null)
                } else if (isExternalShopLink(finishedUrl) && attemptedShopLandings.add(finishedUrl)) {
                    // 302 一跳直达的落地页不会再过 onUrlLoading（如 dpurl.cn → click.meituan.com），
                    // 加载完成后补一次定向投递，成功就直接进 App。
                    if (openShopApp(context, finishedUrl)) handedOff = true
                }
            },
        )
    }
}

@Composable
fun LoginScreen(nav: NavState, addAccount: Boolean, fresh: Boolean = false, relogin: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var hint by remember {
        mutableStateOf(
            if (relogin != null) "请重新登录「$relogin」"
            else "",
        )
    }
    val loginUserAgent = remember(context) {
        val raw = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault(Api.WEB_UA)
        cleanMobileChromeUserAgent(raw)
    }

    fun tryFinishCookieLogin() {
        if (saving) return
        val cookies = CookieVault.readDomains()
        val header = CookieVault.authHeader(cookies, "m.weibo.cn")
        if (header.isBlank() || !header.contains("SUB=")) return
        saving = true
        hint = "登录成功，正在保存…"
        scope.launch {
            val account = runCatching { App.repo.loginWithCookie(header, cookies) }.getOrNull()
            if (account != null) {
                hint = "已登录：${account.name.ifBlank { account.uid }}"
                if (addAccount) {
                    nav.reset(Route.Main)
                    nav.push(Route.Accounts)
                } else {
                    nav.reset(Route.Main)
                }
            } else {
                saving = false
                hint = "读取登录信息失败，请重试"
            }
        }
    }

    BackHandler(enabled = canGoBack || nav.stack.size > 1) {
        if (canGoBack) webView?.goBack()
        else if (nav.stack.size > 1) nav.pop()
    }

    Scaffold(
        containerColor = pageContainerColor(),
        topBar = {
            PinnedBarDensity {
                SmallTopAppBar(
                    color = pageContainerColor(),
                    title = if (relogin != null) "重新登录" else if (addAccount) "添加账号" else "登录",
                    navigationIcon = {
                        BackIconButton {
                            if (canGoBack) webView?.goBack()
                            else if (nav.stack.size > 1) nav.pop()
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                hint = if (relogin != null) "请重新登录「$relogin」" else ""
                                webView?.reload()
                            },
                            modifier = Modifier.testTag("login_web_reload"),
                        ) {
                            Icon(MiuixIcons.Refresh, "刷新页面", Modifier.size(22.dp))
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            WeiboWebView(
                url = "https://passport.weibo.cn/signin/login?entry=wapsso&r=https%3A%2F%2Fm.weibo.cn%2F",
                modifier = Modifier.fillMaxSize(),
                // 纯本地模式手上只有访客 cookie，塞进登录页只会让下面误以为已经登录。
                useSessionCookie = !fresh && App.repo.hasSession(),
                clearCookies = fresh,
                userAgent = loginUserAgent,
                onCreated = { webView = it },
                onBackAvailable = { canGoBack = it },
                onProgressChanged = { _, p -> progress = p },
                onPageCommitVisible = { _, _ -> tryFinishCookieLogin() },
                onPageFinished = { tryFinishCookieLogin() },
                onReceivedError = { _, code, desc ->
                    if (!saving) {
                        hint = if (desc.isNotBlank()) "页面加载异常（$code）：$desc，可点右上角刷新" else "页面加载异常（$code），可点右上角刷新"
                    }
                },
            )
            if (progress in 1..99) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(progress / 100f)
                        .height(2.dp)
                        .background(MiuixTheme.colorScheme.primary),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(MiuixTheme.colorScheme.background.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hint.isNotBlank()) {
                    Text(
                        text = hint,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
                TextButton(
                    text = "验证码 / 密码登录",
                    onClick = {
                        val prev = nav.stack.dropLast(1).lastOrNull()?.route
                        if (prev is Route.SmsLogin) nav.pop()
                        else nav.push(Route.SmsLogin(addAccount, relogin))
                    },
                    modifier = Modifier.testTag("login_sms"),
                )
                // 只在启动就落到登录页时给：从纯本地模式点进来登录的，返回键就能回去。
                if (!addAccount && nav.stack.size == 1) {
                    TextButton(
                        text = "不登录，纯本地浏览",
                        onClick = {
                            App.repo.enterLocalMode()
                            nav.reset(Route.Main)
                        },
                        modifier = Modifier.testTag("login_local_mode"),
                    )
                }
            }
        }
    }
}
