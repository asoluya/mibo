package com.mibo.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import com.mibo.app.App
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 「附近」里可选的一座城市：名字给界面看，坐标给同城流。 */
data class NearbyPlace(val name: String, val lat: Double, val lon: Double)

/**
 * 当前该看哪座城。
 *
 * 设了自定义位置就直接用那组坐标，不再碰定位（也就不需要定位权限）；
 * 否则读设备最后一次定位。拿不到返回 null，由调用方决定要不要提示。
 */
suspend fun nearbyCoords(context: Context): Pair<Double, Double>? {
    val settings = App.settings
    if (settings.nearbyCustom) {
        return settings.nearbyPlaceLat.toDouble() to settings.nearbyPlaceLon.toDouble()
    }
    return deviceCoords(context)
}

/** 设备最后一次定位：三个 provider 各取一份，留最近的那条。 */
suspend fun deviceCoords(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext null
    listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    ).mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time }?.let { it.latitude to it.longitude }
}

/** 是否已拿到定位权限（精确或大致任一即可）。 */
fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 主动读一次当前位置：向系统要一次新定位（最多等 10 秒），拿到后系统缓存的
 * 最后位置也会跟着更新，后续翻页继续用 [nearbyCoords] 即可；超时或失败退回最后已知位置。
 * 调用前需已确保有定位权限。
 */
suspend fun currentCoords(context: Context): Pair<Double, Double>? {
    val fresh = runCatching { freshFix(context) }.getOrNull()
    return fresh ?: deviceCoords(context)
}

private suspend fun freshFix(context: Context): Pair<Double, Double>? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    fun enabled(provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    val provider = when {
        fine && enabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        enabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        fine -> LocationManager.GPS_PROVIDER
        else -> LocationManager.NETWORK_PROVIDER
    }
    return withTimeoutOrNull(10_000) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            val consumer = Consumer<Location?> { loc ->
                if (cont.isActive) cont.resume(loc?.let { it.latitude to it.longitude })
            }
            runCatching {
                manager.getCurrentLocation(provider, signal, context.mainExecutor, consumer)
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }
}
