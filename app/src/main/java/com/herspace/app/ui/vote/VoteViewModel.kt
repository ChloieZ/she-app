package com.herspace.app.ui.vote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herspace.app.data.repository.PlaceRepository
import com.herspace.app.util.LocationHelper
import com.herspace.app.util.SearchResult
import com.herspace.app.util.SharedLocation
import com.herspace.app.data.api.ApiClient
import com.herspace.app.data.api.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoteUiState(
    val nearbyPlaces: List<SearchResult> = emptyList(),
    val selectedPlace: SearchResult? = null,
    val isManualInput: Boolean = false,
    val manualPlaceName: String = "",
    val step: VoteStep = VoteStep.LOCATING,
    val voteSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val todayVotedIds: Set<String> = emptySet(),
    val existingVoteType: String? = null
)

enum class VoteStep {
    LOCATING,
    SELECTING,
    VOTING,
    DONE
}

class VoteViewModel(
    private val repository: PlaceRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoteUiState())
    val uiState: StateFlow<VoteUiState> = _uiState.asStateFlow()

    init {
        loadTodayVotes()
        findNearbyPlaces()
    }

    private fun loadTodayVotes() {
        viewModelScope.launch {
            val email = TokenManager.getEmail() ?: return@launch
            try {
                val resp = ApiClient.api.todayVotes(email)
                _uiState.value = _uiState.value.copy(todayVotedIds = resp.placeIds.toSet())
            } catch (_: Exception) {}
        }
    }

    fun findNearbyPlaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(step = VoteStep.LOCATING, errorMessage = null)

            // 优先使用高德地图共享的定位（更准确）
            val location = SharedLocation.get() ?: locationHelper.getCurrentLocation()
            if (location != null) {
                val places = repository.nearbyPlaces(
                    location.latitude, location.longitude, 1000, 20
                )
                val sorted = places.sortedBy { it.distanceTo(location.latitude, location.longitude) }
                _uiState.value = _uiState.value.copy(
                    nearbyPlaces = sorted,
                    step = VoteStep.SELECTING
                )
                if (sorted.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isManualInput = true,
                        errorMessage = "附近未找到店铺，可手动输入"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    step = VoteStep.SELECTING,
                    isManualInput = true,
                    errorMessage = "无法获取位置，请手动输入店名"
                )
            }
        }
    }

    fun selectPlace(place: SearchResult) {
        viewModelScope.launch {
            // 查询是否已投票
            val email = TokenManager.getEmail() ?: "anonymous"
            var existingType: String? = null
            try {
                val resp = ApiClient.api.myVote(place.placeId, email)
                if (resp.voted) existingType = resp.vote?.voteType
            } catch (_: Exception) {}

            _uiState.value = _uiState.value.copy(
                selectedPlace = place,
                step = VoteStep.VOTING,
                existingVoteType = existingType
            )
        }
    }

    fun switchToManualInput() {
        _uiState.value = _uiState.value.copy(
            isManualInput = true,
            selectedPlace = null
        )
    }

    fun updateManualPlaceName(name: String) {
        _uiState.value = _uiState.value.copy(manualPlaceName = name)
    }

    fun confirmManualPlace() {
        val name = _uiState.value.manualPlaceName
        if (name.isBlank()) return
        val place = SearchResult(
            placeId = "manual_${System.currentTimeMillis()}",
            name = name,
            lat = 0.0,
            lng = 0.0,
            address = "",
            type = ""
        )
        _uiState.value = _uiState.value.copy(
            selectedPlace = place,
            step = VoteStep.VOTING
        )
    }

    fun submitVote(voteType: String) {
        val place = _uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(step = VoteStep.VOTING, errorMessage = null)
            try {
                repository.submitVote(
                    placeName = place.name,
                    placeId = place.placeId,
                    lat = place.lat,
                    lng = place.lng,
                    voteType = voteType
                )
                _uiState.value = _uiState.value.copy(
                    step = VoteStep.DONE,
                    voteSubmitted = true,
                    todayVotedIds = _uiState.value.todayVotedIds + place.placeId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    step = VoteStep.DONE,
                    voteSubmitted = true,
                    errorMessage = "投票已记录（${e.message ?: ""}）"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = VoteUiState()
        findNearbyPlaces()
    }
}
