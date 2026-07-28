package com.herspace.app.ui.map

import android.content.Context
import android.location.Location
import com.herspace.app.data.api.ApiClient
import com.herspace.app.data.api.TokenManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herspace.app.data.db.PlaceSummary
import com.herspace.app.data.model.FriendlinessLevel
import com.herspace.app.data.repository.PlaceRepository
import com.herspace.app.util.LocationHelper
import com.herspace.app.util.SearchHistoryManager
import com.herspace.app.util.SearchResult
import com.herspace.app.util.SharedLocation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// ── 筛选条件 ──

/** 一级分类 */
enum class PlaceCategory(val label: String, val keyword: String) {
    HOTEL("酒店民宿", "酒店|民宿|宾馆"),
    SHOPPING("商场购物", "购物|商场|百货"),
    BEAUTY("丽人美容", "美容|美发|美甲|SPA"),
    ATTRACTION("景点游玩", "景点|公园|景区")
}

/** 友好度筛选 */
enum class FriendlinessFilter(val label: String) {
    ALL("全部"),
    FRIENDLY("非常友好"),
    NEUTRAL("一般友好"),
    UNFRIENDLY("不友好"),
    UNKNOWN("待探索")
}

/** 距离筛选 */
enum class DistanceFilter(val label: String, val maxKm: Float) {
    NEAR("近", 8f),
    MID("稍远", 16f),
    FAR("较远", 32f)
}

data class MapUiState(
    val currentLocation: Location? = null,
    val gotoLocation: Location? = null,
    val placeSummaries: Map<String, PlaceSummary> = emptyMap(),
    val friendlinessMap: Map<String, FriendlinessLevel> = emptyMap(),
    val selectedPlaceId: String? = null,
    val selectedPlaceSummary: PlaceSummary? = null,
    val selectedFriendliness: FriendlinessLevel? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val existingVoteType: String? = null,
    val notFriendlyCount: Int = 0,
    val veryUnfriendlyCount: Int = 0,
    val isSearchMode: Boolean = false,

    // ── 筛选状态 ──
    val selectedCategory: PlaceCategory? = null,
    val selectedFriendlinessFilter: FriendlinessFilter = FriendlinessFilter.ALL,
    val selectedDistanceFilter: DistanceFilter? = null,
    val poiResults: List<SearchResult> = emptyList(),   // 当前分类下的所有 POI
    val filteredResults: List<SearchResult> = emptyList() // 经过友好度+距离筛选后
)

class MapViewModel(
    private val repository: PlaceRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadLocation()
        loadAllPlaces()
    }

    fun loadLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                _uiState.value = _uiState.value.copy(
                    currentLocation = location,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    /** 由高德地图定位回调更新位置（持续更新） */
    fun onLocationObtained(location: Location) {
        _uiState.value = _uiState.value.copy(currentLocation = location)
        // 共享给其他页面（如投票页面）
        SharedLocation.update(location)
    }

    fun loadAllPlaces() {
        viewModelScope.launch {
            val summaries = repository.getAllPlaceSummaries()
            val friendlinessMap = mutableMapOf<String, FriendlinessLevel>()
            val summaryMap = mutableMapOf<String, PlaceSummary>()
            for (s in summaries) {
                summaryMap[s.placeId] = s
                friendlinessMap[s.placeId] = repository.calculateFriendliness(s)
            }
            _uiState.value = _uiState.value.copy(
                placeSummaries = summaryMap,
                friendlinessMap = friendlinessMap
            )
        }
    }

    // ── 分类选择 ──

    /** 选择一级分类 */
    fun selectCategory(category: PlaceCategory) {
        val current = _uiState.value.selectedCategory
        // 点同一个取消
        if (current == category) {
            _uiState.value = _uiState.value.copy(
                selectedCategory = null,
                poiResults = emptyList(),
                filteredResults = emptyList(),
                selectedFriendlinessFilter = FriendlinessFilter.ALL,
                selectedDistanceFilter = null
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedFriendlinessFilter = FriendlinessFilter.ALL,
            selectedDistanceFilter = DistanceFilter.NEAR // 默认 8 公里
        )
        // 搜索该分类下的 POI
        searchCategory(category)
    }

    /** 按分类搜索 POI */
    private fun searchCategory(category: PlaceCategory) {
        val loc = _uiState.value.currentLocation
        viewModelScope.launch {
            val items = repository.categoryPlaces(
                category.name.lowercase(),
                "杭州",
                25,
                loc?.latitude,
                loc?.longitude
            )
            // 从 API 响应中提取友好度数据（服务端根据投票计算）
            val friendMap = mutableMapOf<String, FriendlinessLevel>()
            // 未投票的标记为待探索
            for (item in items) {
                friendMap[item.placeId] = FriendlinessLevel.UNKNOWN
            }
            _uiState.value = _uiState.value.copy(
                poiResults = items,
                friendlinessMap = friendMap,
                filteredResults = applyFilters(items, friendMap)
            )
        }
    }

    /** 选择友好度筛选 */
    fun selectFriendlinessFilter(filter: FriendlinessFilter) {
        _uiState.value = _uiState.value.copy(selectedFriendlinessFilter = filter)
        applyCurrentFilters()
    }

    /** 选择距离筛选 */
    fun selectDistanceFilter(filter: DistanceFilter) {
        val current = _uiState.value.selectedDistanceFilter
        val newFilter = if (current == filter) null else filter
        _uiState.value = _uiState.value.copy(selectedDistanceFilter = newFilter)
        applyCurrentFilters()
    }

    /** 应用当前所有筛选 */
    private fun applyCurrentFilters() {
        val state = _uiState.value
        val filtered = applyFilters(state.poiResults, state.friendlinessMap)
        _uiState.value = _uiState.value.copy(filteredResults = filtered)
    }

    /** 筛选逻辑 */
    private fun applyFilters(
        items: List<SearchResult>,
        friendMap: Map<String, FriendlinessLevel>
    ): List<SearchResult> {
        val state = _uiState.value
        val userLoc = state.currentLocation

        return items.filter { item ->
            // 友好度筛选
            val level = friendMap[item.placeId]
            val passFriend = when (state.selectedFriendlinessFilter) {
                FriendlinessFilter.ALL -> true
                FriendlinessFilter.FRIENDLY -> level == FriendlinessLevel.FRIENDLY
                FriendlinessFilter.NEUTRAL -> level == FriendlinessLevel.NEUTRAL
                FriendlinessFilter.UNFRIENDLY -> level == FriendlinessLevel.UNFRIENDLY
                FriendlinessFilter.UNKNOWN -> level == FriendlinessLevel.UNKNOWN
            }
            if (!passFriend) return@filter false

            // 距离筛选
            if (state.selectedDistanceFilter != null && userLoc != null) {
                val distKm = item.distanceTo(userLoc.latitude, userLoc.longitude) / 1000.0
                distKm <= state.selectedDistanceFilter.maxKm
            } else true
        }
    }

    // ── 原有搜索逻辑 ──

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            val summary = repository.getPlaceSummary(placeId)
            if (summary != null) {
                val level = repository.calculateFriendliness(summary)
                _uiState.value = _uiState.value.copy(
                    selectedPlaceId = placeId,
                    selectedPlaceSummary = summary,
                    selectedFriendliness = level
                )
            }
        }
    }
    /** 选中搜索结果 → 移动到该位置 + 从服务端获取投票统计 */
    fun selectSearchResult(result: SearchResult) {
        // 保存搜索历史（仅当点击结果时记录）
        val q = _uiState.value.searchQuery.trim()
        if (q.isNotBlank()) SearchHistoryManager.add(q)
        viewModelScope.launch {
            val serverData = repository.getPlaceDetail(result.placeId)
            val totalVotes = serverData?.totalVotes ?: 0
            val friendlyVotes = serverData?.friendlyVotes ?: 0
            val friendliness = serverData?.friendliness ?: "neutral"
            val level = when (friendliness) {
                "friendly" -> FriendlinessLevel.FRIENDLY
                "unfriendly" -> FriendlinessLevel.UNFRIENDLY
                "neutral" -> FriendlinessLevel.NEUTRAL
                else -> FriendlinessLevel.UNKNOWN
            }

            // 查询当前用户是否已投票
            var existingType: String? = null
            try {
                val email = TokenManager.getEmail()
                if (email != null) {
                    val voteResp = ApiClient.api.myVote(result.placeId, email)
                    if (voteResp.voted) existingType = voteResp.vote?.voteType
                }
            } catch (_: Exception) {}

            _uiState.value = _uiState.value.copy(
                selectedPlaceId = result.placeId,
                selectedPlaceSummary = PlaceSummary(
                    placeId = result.placeId,
                    placeName = result.name,
                    placeLat = result.lat,
                    placeLng = result.lng,
                    totalVotes = totalVotes,
                    friendlyCount = friendlyVotes
                ),
                selectedFriendliness = level,
                existingVoteType = existingType,
                notFriendlyCount = serverData?.notFriendlyVotes ?: 0,
                veryUnfriendlyCount = serverData?.veryUnfriendlyVotes ?: 0,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                gotoLocation = Location("goto").apply {
                    latitude = result.lat
                    longitude = result.lng
                }
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPlaceId = null,
            selectedPlaceSummary = null,
            selectedFriendliness = null
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            performSearch()
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
        }
    }

    /** 进入搜索模式 */
    fun enterSearchMode() {
        _uiState.value = _uiState.value.copy(isSearchMode = true)
    }

    /** 退出搜索模式 */
    fun exitSearchMode() {
        _uiState.value = _uiState.value.copy(
            isSearchMode = false,
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            searchError = null
        )
    }

    fun performSearch() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false, searchError = null)
            return
        }
        _uiState.value = _uiState.value.copy(isSearching = true, searchError = null)
        viewModelScope.launch {
            try {
                val loc = _uiState.value.currentLocation
                val items = repository.searchPlaces(query, "杭州", 25, loc?.latitude, loc?.longitude)
                val sorted = if (loc != null) {
                    items.sortedBy { it.distanceTo(loc.latitude, loc.longitude) }
                } else items
                _uiState.value = _uiState.value.copy(
                    searchResults = sorted,
                    isSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    searchError = e.message ?: "搜索失败"
                )
            }
        }
    }

    fun refresh() {
        loadAllPlaces()
    }

    /** 从详情卡片直接投票 */
    fun submitDirectVote(place: PlaceSummary, voteType: String) {
        viewModelScope.launch {
            try {
                repository.submitVote(place.placeName, place.placeId, place.placeLat, place.placeLng, voteType)
                // 更新缓存，刷新显示
                val serverData = repository.getPlaceDetail(place.placeId)
                val total = serverData?.totalVotes ?: place.totalVotes + 1
                val friendly = serverData?.friendlyVotes ?: (place.friendlyCount + if (voteType == "friendly") 1 else 0)
                val notF = serverData?.notFriendlyVotes ?: (_uiState.value.notFriendlyCount + if (voteType == "not_friendly") 1 else 0)
                val veryU = serverData?.veryUnfriendlyVotes ?: (_uiState.value.veryUnfriendlyCount + if (voteType == "very_unfriendly") 1 else 0)
                val updatedLevel = repository.calculateFriendliness(
                    PlaceSummary(place.placeId, place.placeName, place.placeLat, place.placeLng, total, friendly)
                ).let { if (total == 0) FriendlinessLevel.UNKNOWN else it }
                _uiState.value = _uiState.value.copy(
                    existingVoteType = voteType,
                    notFriendlyCount = notF,
                    veryUnfriendlyCount = veryU,
                    selectedPlaceSummary = PlaceSummary(place.placeId, place.placeName, place.placeLat, place.placeLng, total, friendly),
                    selectedFriendliness = updatedLevel
                )
            } catch (_: Exception) {}
        }
    }
}
