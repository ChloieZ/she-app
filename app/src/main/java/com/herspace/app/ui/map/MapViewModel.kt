package com.herspace.app.ui.map

import android.content.Context
import android.location.Location
import com.herspace.app.data.api.ApiClient
import com.herspace.app.data.api.ApiResponse
import com.herspace.app.data.api.PlaceResponse
import com.herspace.app.data.api.TokenManager
import android.widget.Toast
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herspace.app.data.db.PlaceSummary
import com.herspace.app.data.model.FriendlinessLevel
import com.herspace.app.data.repository.PlaceRepository
import com.herspace.app.data.repository.toSearchResult
import com.herspace.app.util.LocationHelper
import com.herspace.app.util.SearchHistoryManager
import com.herspace.app.util.SearchResult
import com.herspace.app.util.SharedLocation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// ── 筛选条件 ──

/** 一级分类 */
enum class PlaceCategory(val label: String, val keyword: String) {
    ALL("全部", ""),
    HOTEL("酒店民宿", "酒店|民宿|宾馆"),
    SHOPPING("商场购物", "购物|商场|百货"),
    BEAUTY("丽人美容", "美容|美发|美甲|SPA"),
    ATTRACTION("景点游玩", "景点|公园|景区"),
    FOOD("美食", "美食|餐厅|餐饮|小吃|咖啡|火锅")
}

/** 除「全部」外的实际分类 */
val PlaceCategory.realCategories: List<PlaceCategory>
    get() = PlaceCategory.entries.filter { it != PlaceCategory.ALL }

/** 友好度筛选 */
enum class FriendlinessFilter(val label: String) {
    ALL("全部"),
    FRIENDLY("非常友好"),
    NEUTRAL("一般友好"),
    UNFRIENDLY("不友好"),
    UNKNOWN("待探索")
}

/** 距离筛选（分段：近 0-8km / 稍远 8-16km / 较远 16km 以上，互不累加） */
enum class DistanceFilter(val label: String, val minKm: Float, val maxKm: Float) {
    ALL("全部", 0f, Float.MAX_VALUE),
    NEAR("近", 0f, 8f),
    MID("稍远", 8f, 16f),
    FAR("较远", 16f, Float.MAX_VALUE)
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
    val generallyFriendlyCount: Int = 0,
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
        // 预设「全部」分类，等定位就绪后再搜索（避免无坐标搜索导致数据不全/错乱）
        _uiState.value = _uiState.value.copy(selectedCategory = PlaceCategory.ALL)
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
                // 定位成功后再用精确坐标搜索当前分类
                val cat = _uiState.value.selectedCategory
                if (cat != null) searchCategory(cat)
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
        // 定位成功后：若当前选了分类则用精确坐标重新搜索（首次进入时可能没定位导致加载慢/不全）
        val cat = _uiState.value.selectedCategory
        if (cat != null) searchCategory(cat)
        if (_uiState.value.filteredResults.isEmpty()) loadUserPlaces()
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
            loadUserPlaces()
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
            selectedDistanceFilter = null // 默认不选距离，展示全部距离数据
        )
        // 搜索该分类下的 POI
        searchCategory(category)
    }

    /** 按分类搜索 POI（ALL 时并行合并所有分类） */
    private fun searchCategory(category: PlaceCategory) {
        val loc = _uiState.value.currentLocation
        val cats = if (category == PlaceCategory.ALL) PlaceCategory.ALL.realCategories else listOf(category)
        viewModelScope.launch {
            try {
                // 并行请求所有分类，加速「全部」加载
                val allResp = coroutineScope {
                    cats.map { c ->
                        async {
                            try {
                                ApiClient.api.categoryPlaces(
                                    c.name.lowercase(), "杭州", 25, loc?.latitude, loc?.longitude
                                )
                            } catch (_: Exception) {
                                ApiResponse<PlaceResponse>()
                            }
                        }
                    }.awaitAll().flatMap { it.results }
                }.distinctBy { it.id }.toMutableList()
                // 合并投票地点：全部=所有，具体分类=该分类归属的（覆盖高德搜不到的已投票地点）
                try {
                    val voted = ApiClient.api.votedPlaces()
                    val ids = allResp.map { it.id }.toMutableSet()
                    for (p in voted.places) {
                        if (p.totalVotes == 0) continue
                        if (category != PlaceCategory.ALL && p.category != category.name.lowercase()) continue
                        if (ids.add(p.id)) allResp.add(p)
                    }
                } catch (_: Exception) {}
                val items = allResp.map { it.toSearchResult() }
                val friendMap = mutableMapOf<String, FriendlinessLevel>()
                for (p in allResp) {
                    // 颜色完全以服务器汇总为准（所有用户看到一致的数据）
                    val level = when {
                        p.totalVotes == 0 -> FriendlinessLevel.UNKNOWN
                        p.friendliness == "friendly" -> FriendlinessLevel.FRIENDLY
                        p.friendliness == "neutral" -> FriendlinessLevel.NEUTRAL
                        p.friendliness == "unfriendly" -> FriendlinessLevel.UNFRIENDLY
                        else -> FriendlinessLevel.UNKNOWN
                    }
                    Log.d("HerSpaceMap", "${p.name}: serverVotes=${p.totalVotes} friendliness=${p.friendliness} matched=$level")
                    friendMap[p.id] = level
                }
                _uiState.value = _uiState.value.copy(
                    poiResults = items,
                    friendlinessMap = friendMap,
                    filteredResults = applyFilters(items, friendMap)
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    poiResults = emptyList(),
                    filteredResults = emptyList()
                )
            }
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

            // 距离筛选（分段区间，互不累加）
            if (state.selectedDistanceFilter != null && userLoc != null) {
                val distKm = item.distanceTo(userLoc.latitude, userLoc.longitude) / 1000.0
                distKm >= state.selectedDistanceFilter.minKm && distKm <= state.selectedDistanceFilter.maxKm
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
            var serverData: PlaceResponse? = null
            try {
                serverData = repository.getPlaceDetail(result.placeId)
            } catch (_: Exception) {
                // 详情请求失败：用地标已有颜色兜底，确保显示当前点击的地点（不残留旧地点）
            }
            val totalVotes = serverData?.totalVotes ?: 0
            val friendlyVotes = serverData?.friendlyVotes ?: 0
            val friendliness = serverData?.friendliness ?: "neutral"
            // 综合友好度完全以服务器汇总为准（所有用户看到一致）
            val level = when {
                totalVotes == 0 -> FriendlinessLevel.UNKNOWN
                friendliness == "friendly" -> FriendlinessLevel.FRIENDLY
                friendliness == "unfriendly" -> FriendlinessLevel.UNFRIENDLY
                friendliness == "neutral" -> FriendlinessLevel.NEUTRAL
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
                generallyFriendlyCount = serverData?.generallyFriendlyVotes ?: 0,
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
        // 重新执行当前分类搜索，保留全量分类数据
        val cat = _uiState.value.selectedCategory
        if (cat != null) searchCategory(cat)
    }

    /** 补充有投票的地点：所有用户共享（服务器全量接口，保证各账号数据一致） */
    private fun loadUserPlaces() {
        viewModelScope.launch {
            try {
                // 拉取所有有投票记录的地点（不分用户）
                val resp = ApiClient.api.votedPlaces()
                val friendMap = _uiState.value.friendlinessMap.toMutableMap()
                val summaryMap = _uiState.value.placeSummaries.toMutableMap()
                // 仅「全部」分类时把投票地点合并进地图展示；具体分类只更新颜色数据
                val isAll = _uiState.value.selectedCategory == PlaceCategory.ALL
                val poiItems = if (isAll) _uiState.value.poiResults.toMutableList() else null
                val seenIds = poiItems?.map { it.placeId }?.toMutableSet() ?: mutableSetOf()

                // 颜色完全以服务器汇总为准
                fun repoLevel(f: String, t: Int) = when {
                    t == 0 -> FriendlinessLevel.UNKNOWN
                    f == "friendly" -> FriendlinessLevel.FRIENDLY
                    f == "neutral" -> FriendlinessLevel.NEUTRAL
                    f == "unfriendly" -> FriendlinessLevel.UNFRIENDLY
                    else -> FriendlinessLevel.UNKNOWN
                }
                for (p in resp.places) {
                    if (p.totalVotes == 0) continue
                    val sid = PlaceSummary(p.id, p.name, p.lat, p.lng, p.totalVotes, p.friendlyVotes)
                    summaryMap[p.id] = sid
                    friendMap[p.id] = repoLevel(p.friendliness, p.totalVotes)
                    if (isAll && poiItems != null && seenIds.add(p.id)) poiItems.add(p.toSearchResult())
                }
                _uiState.value = _uiState.value.copy(
                    placeSummaries = summaryMap,
                    friendlinessMap = friendMap,
                    poiResults = poiItems ?: _uiState.value.poiResults,
                    filteredResults = if (isAll) applyFilters(poiItems ?: emptyList(), friendMap) else _uiState.value.filteredResults
                )
            } catch (_: Exception) {}
        }
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
                val generallyF = serverData?.generallyFriendlyVotes ?: (_uiState.value.generallyFriendlyCount + if (voteType == "generally_friendly") 1 else 0)
                val notF = serverData?.notFriendlyVotes ?: (_uiState.value.notFriendlyCount + if (voteType == "not_friendly") 1 else 0)
                val veryU = serverData?.veryUnfriendlyVotes ?: (_uiState.value.veryUnfriendlyCount + if (voteType == "very_unfriendly") 1 else 0)
                // 颜色以服务器汇总为准（避免本地 ratio 把「一般」误判为不友好）
                val updatedLevel = when {
                    total == 0 -> FriendlinessLevel.UNKNOWN
                    serverData?.friendliness == "friendly" -> FriendlinessLevel.FRIENDLY
                    serverData?.friendliness == "neutral" -> FriendlinessLevel.NEUTRAL
                    serverData?.friendliness == "unfriendly" -> FriendlinessLevel.UNFRIENDLY
                    else -> FriendlinessLevel.UNKNOWN
                }
                _uiState.value = _uiState.value.copy(
                    existingVoteType = voteType,
                    generallyFriendlyCount = generallyF,
                    notFriendlyCount = notF,
                    veryUnfriendlyCount = veryU,
                    selectedPlaceSummary = PlaceSummary(place.placeId, place.placeName, place.placeLat, place.placeLng, total, friendly),
                    selectedFriendliness = updatedLevel
                )
                // 投票后刷新地图上的汇总数据
                loadAllPlaces()
            } catch (_: Exception) {}
        }
    }
}
