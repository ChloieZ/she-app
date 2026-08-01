package com.herspace.app

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.herspace.app.data.api.TokenManager
import com.herspace.app.util.SearchHistoryManager
import com.herspace.app.util.VotedPlacesStore
import com.herspace.app.data.api.ApiClient

class HerSpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 Token 管理器
        TokenManager.init(this)
        // 初始化搜索历史
        SearchHistoryManager.init(this)
        // 初始化投票地点存储
        VotedPlacesStore.init(this)
        // 初始化 API 客户端（读取上次保存的模式）
        ApiClient.init(this)

        // 高德地图 SDK 隐私合规授权（必需，否则地图不加载）
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
    }
}
