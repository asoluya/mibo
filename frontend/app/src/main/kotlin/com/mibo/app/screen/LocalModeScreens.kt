package com.mibo.app.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.mibo.app.ui.NavState
import com.mibo.app.ui.Route
import com.mibo.app.ui.components.EmptyState
import top.yukonga.miuix.kmp.basic.TextButton

/** 纯本地模式下的「消息」页：私信和通知都挂在账号上，访客身份拿不到。 */
@Composable
fun LocalMessagesPlaceholder(nav: NavState, topInset: Dp) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = topInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState("纯本地模式没有私信和通知\n登录微博账号后可用")
        TextButton(
            text = "登录微博账号",
            onClick = { nav.push(Route.SmsLogin()) },
            modifier = Modifier.testTag("local_login"),
        )
    }
}
