package com.herspace.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationHelper(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 使用 Android 原生 LocationManager 获取位置
     * 不依赖 Google Play Services，国产手机兼容性更好
     */
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        // 先尝试 GPS 定位
        var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) return@withContext location

        // 再尝试网络定位
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null) return@withContext location

        // GPS 和网络都无缓存，返回 null（用户稍后手动移动地图或由 onMyLocationChange 补充）
        null
    }

    /** 模拟附近店铺数据（MVP 阶段使用，后续接入真实 POI 数据） */
    fun getMockNearbyPlaces(lat: Double, lng: Double): List<MockPlace> {
        return listOf(
            MockPlace("place_001", "星巴克（中心店）", lat + 0.001, lng + 0.001),
            MockPlace("place_002", "海底捞（万象城店）", lat - 0.001, lng + 0.002),
            MockPlace("place_003", "ZARA（国贸店）", lat + 0.002, lng - 0.001),
            MockPlace("place_004", "喜茶（步行街店）", lat - 0.002, lng - 0.002),
            MockPlace("place_005", "URBAN REVIVO", lat + 0.0015, lng - 0.0015),
            MockPlace("place_006", "Nike（品牌体验店）", lat - 0.001, lng + 0.001),
        )
    }

    data class MockPlace(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double
    )
}
