package com.mibo.app.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibo.app.App
import com.mibo.app.data.NearbyPlace
import com.mibo.app.data.deviceCoords
import com.mibo.app.ui.AppBarSurface
import com.mibo.app.ui.NavState
import com.mibo.app.ui.blurEffectsEnabled
import com.mibo.app.ui.layerBackdropIf
import com.mibo.app.ui.PinnedBarDensity
import com.mibo.app.ui.pinnedHeight
import com.mibo.app.ui.rememberListScrollFraction
import com.mibo.app.ui.glassBarContentTop
import com.mibo.app.ui.pageContainerColor
import com.mibo.app.ui.rememberExpandOrigin
import com.mibo.app.ui.Route
import com.mibo.app.ui.expandOrigin
import com.mibo.app.ui.components.BackIconButton
import com.mibo.app.ui.components.EmptyState
import com.mibo.app.ui.components.ErrorState
import com.mibo.app.ui.components.LoadingState
import com.mibo.app.ui.components.StatusCard
import com.mibo.app.ui.components.navBottomPadding
import com.mibo.app.ui.LocalBackdrop
import com.mibo.app.ui.components.DialogButtonBar
import com.mibo.app.ui.components.DialogButtonBarAction
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NearbyScreen(nav: NavState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locateError by remember { mutableStateOf<String?>(null) }
    var needPermission by remember { mutableStateOf(false) }
    var customPlace by remember { mutableStateOf(App.settings.nearbyCustom) }
    var placeName by remember { mutableStateOf(App.settings.nearbyPlaceName) }
    var showPlaceMenu by remember { mutableStateOf(false) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var permissionTick by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val topBarHeight = pinnedHeight(TopAppBarDefaults.SmallTopAppBarCenterHeight)

    // 换一次坐标换一个流：同城页的翻页游标里带着本次定位的经纬度，跨坐标续爬会串城。
    val store = remember(coords) {
        coords?.let { (lat, lon) ->
            FeedStore("nearby") { cursor, force ->
                App.repo.nearby(lat.toString(), lon.toString(), cursor, force)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            needPermission = false
            permissionTick++
        } else {
            needPermission = true
        }
    }

    // 同城流只认坐标，换了位置就把这条流整条重来；坐标没变则原地刷新。
    fun settle(pair: Pair<Double, Double>?) {
        when {
            pair == null -> locateError = "暂时拿不到位置，请到系统设置里打开定位后再试"
            pair == coords -> scope.launch { store?.refresh() }
            else -> coords = pair
        }
    }

    fun locate() {
        locateError = null
        if (App.settings.nearbyCustom) {
            settle(App.settings.nearbyPlaceLat.toDouble() to App.settings.nearbyPlaceLon.toDouble())
            return
        }
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            return
        }
        scope.launch {
            runCatching { settle(deviceCoords(context)) }
                .onFailure { locateError = it.message ?: "定位失败" }
        }
    }

    store?.let { SyncDeletedStatuses(it) }
    LaunchedEffect(Unit) { locate() }
    LaunchedEffect(permissionTick) { if (permissionTick > 0) locate() }
    LaunchedEffect(store) { store?.loadInitial() }

    LaunchedEffect(listState, store) {
        val current = store ?: return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= current.items.size - 2 && current.items.isNotEmpty()) current.loadMore()
            }
    }

    Box(Modifier.fillMaxSize().background(pageContainerColor())) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdropIf(blurEffectsEnabled(), backdrop)
                .background(pageContainerColor())
        ) {
            CompositionLocalProvider(LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null) {
                LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollEndHaptic().testTag("nearby_list"),
                // 内容从顶栏玻璃下面滚过去，顶栏才有东西可采样。
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = glassBarContentTop(topBarHeight, 8.dp),
                    end = 12.dp,
                    bottom = 8.dp + navBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val feed = store
                if (feed == null) {
                    item {
                        when {
                            locateError != null -> ErrorState(locateError!!) { locate() }
                            needPermission -> EmptyState("需要定位权限才能看附近")
                            else -> LoadingState()
                        }
                    }
                } else {
                    if (feed.items.isEmpty()) {
                        item {
                            when {
                                feed.refreshing || feed.loading -> LoadingState()
                                feed.error != null -> ErrorState(feed.error!!) {
                                    scope.launch { feed.refresh() }
                                }
                                else -> EmptyState("附近暂时没有内容")
                            }
                        }
                    }
                    itemsIndexed(feed.items, key = { _, s -> s.id }) { index, status ->
                        val origin = rememberExpandOrigin()
                        StatusCard(
                            status = status,
                            modifier = Modifier.expandOrigin(origin),
                            onOpen = { nav.push(Route.Detail(status.id, status.mid), origin) },
                            onOpenQuoted = { quoted, quotedOrigin ->
                                nav.push(Route.Detail(quoted.id, quoted.mid), quotedOrigin)
                            },
                            onUser = { nav.push(Route.Profile(it.id)) },
                            onImage = { pics, i -> nav.push(Route.Images(pics, i)) },
                            onVideo = { nav.push(Route.Video(it.url, it.title, it.isLive)) },
                            onLike = { scope.launch { feed.like(index) } },
                            onRepost = { nav.push(Route.Detail(status.id, status.mid), origin) },
                            onComment = { nav.push(Route.Detail(status.id, status.mid), origin) },
                        )
                    }
                    if (feed.items.isNotEmpty()) {
                        item { if (feed.loading) LoadingState() }
                    }
                }
            }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides if (blurEffectsEnabled()) backdrop else null,
        ) {
            PinnedBarDensity {
                AppBarSurface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    containerColor = pageContainerColor(),
                    scrollFraction = rememberListScrollFraction(listState),
                ) {
                    SmallTopAppBar(
                        title = "附近",
                        color = Color.Transparent,
                        navigationIcon = { BackIconButton { nav.pop() } },
                        actions = {
                        IconButton(
                            onClick = { locate() },
                            modifier = Modifier.testTag("nearby_relocate"),
                        ) {
                            Icon(MiuixIcons.Location, "重新定位", Modifier.size(24.dp))
                        }
                        Box {
                            IconButton(
                                onClick = { showPlaceMenu = true },
                                modifier = Modifier.testTag("nearby_more"),
                            ) {
                                Icon(MiuixIcons.More, "位置设置", Modifier.size(22.dp))
                            }
                            OverlayListPopup(
                                show = showPlaceMenu,
                                popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                onDismissRequest = { showPlaceMenu = false },
                            ) {
                                val dismiss = LocalDismissState.current
                                ListPopupColumn {
                                    DropdownImpl(
                                        item = DropdownItem(
                                            text = "位置",
                                            summary = subtitleText(customPlace, placeName),
                                        ),
                                        optionSize = 1,
                                        isSelected = false,
                                        index = 0,
                                        onSelectedIndexChange = {
                                            dismiss?.invoke()
                                            showPlaceMenu = false
                                            scope.launch {
                                                delay(180)
                                                showPlaceDialog = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                    )
                }
            }
        }
    }

    if (showPlaceDialog) {
        NearbyPlaceDialog(
            custom = customPlace,
            placeName = placeName,
            onDismiss = { showPlaceDialog = false },
            onDevice = {
                App.settings.applyNearbyDevice()
                customPlace = false
                showPlaceDialog = false
                locate()
            },
            onCustom = {
                showPlaceDialog = false
                showCityPicker = true
            },
        )
    }

    if (showCityPicker) {
        NearbyCityPickerDialog(
            selected = if (customPlace) placeName else "",
            onDismiss = { showCityPicker = false },
            onPick = { place ->
                App.settings.applyNearbyPlace(place.name, place.lat, place.lon)
                customPlace = true
                placeName = place.name
                showCityPicker = false
                coords = place.lat to place.lon
            },
        )
    }
}

private fun subtitleText(custom: Boolean, name: String): String =
    if (custom && name.isNotBlank()) name else "跟随手机定位"

private fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** 「附近的位置」：跟随定位，或者固定看自己选的那座城。 */
@Composable
private fun NearbyPlaceDialog(
    custom: Boolean,
    placeName: String,
    onDismiss: () -> Unit,
    onDevice: () -> Unit,
    onCustom: () -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { show = true }

    OverlayDialog(
        show = show,
        title = "附近的位置",
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                when (result) {
                    0 -> onDevice()
                    1 -> onCustom()
                    else -> onDismiss()
                }
            }
        },
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth(),
                propagateMinConstraints = true,
            ) {
                ListPopupColumn {
                    Box(Modifier.testTag("nearby_place_device")) {
                        DropdownImpl(
                            item = DropdownItem(
                                text = "使用当前定位",
                                summary = "按手机定位看所在城市",
                            ),
                            optionSize = 2,
                            isSelected = !custom,
                            index = 0,
                            onSelectedIndexChange = {
                                result = 0
                                show = false
                            },
                        )
                    }
                    Box(Modifier.testTag("nearby_place_custom")) {
                        DropdownImpl(
                            item = DropdownItem(
                                text = "自定义位置",
                                summary = if (custom && placeName.isNotBlank()) placeName else "选一座城市",
                            ),
                            optionSize = 2,
                            isSelected = custom,
                            index = 1,
                            onSelectedIndexChange = {
                                result = 1
                                show = false
                            },
                        )
                    }
                }
            }
            DialogButtonBar(
                negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
                positive = null,
            )
        }
    }
}

/**
 * 选城市。城市表直接用微博网页版切城市那份（名字 + 坐标），所以在国内任何地方都能换到
 * 一座有内容城；搜索只在这份表里做前缀/包含匹配，不额外发请求。
 */
@Composable
private fun NearbyCityPickerDialog(
    selected: String,
    onDismiss: () -> Unit,
    onPick: (NearbyPlace) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<NearbyPlace?>(null) }
    var places by remember { mutableStateOf<List<NearbyPlace>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        show = true
        loadError = null
        runCatching { App.repo.nearbyCities(force = reload > 0) }
            .onSuccess { places = it }
            .onFailure { loadError = it.message ?: "城市列表加载失败" }
    }

    OverlayDialog(
        show = show,
        title = "选择城市",
        outsideMargin = DpSize(16.dp, 8.dp),
        onDismissRequest = { show = false },
        onDismissFinished = {
            if (!settled) {
                settled = true
                val place = picked
                if (place != null) onPick(place) else onDismiss()
            }
        },
    ) {
        Column {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("nearby_city_query"),
                label = "城市名",
                maxLines = 1,
                minLines = 1,
            )
            val list = places
            val filtered = list?.filter { query.isBlank() || it.name.contains(query.trim()) }.orEmpty()
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(top = 8.dp),
            ) {
                when {
                    list == null && loadError != null -> ErrorState(loadError!!) {
                        places = null
                        reload++
                    }
                    list == null -> LoadingState()
                    filtered.isEmpty() -> EmptyState("没有找到这个城市")
                    else -> LazyColumn(Modifier.scrollEndHaptic()) {
                        items(filtered, key = { it.name }) { place ->
                            CityRow(
                                place = place,
                                selected = place.name == selected,
                                onClick = {
                                    picked = place
                                    show = false
                                },
                            )
                        }
                    }
                }
            }
            DialogButtonBar(
                negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
                positive = null,
            )
        }
    }
}

@Composable
private fun CityRow(place: NearbyPlace, selected: Boolean, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            place.name,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text("当前", fontSize = 12.sp, color = scheme.primary)
        }
    }
}
