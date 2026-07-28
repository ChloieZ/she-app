package com.herspace.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.herspace.app.data.model.FriendlinessLevel
import com.herspace.app.ui.detail.PlaceDetailCard
import com.herspace.app.ui.theme.*
import com.herspace.app.util.SearchHistoryManager
import com.herspace.app.util.SearchResult
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToVote: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 运行时定位权限
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.loadLocation()
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            viewModel.loadLocation()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("她空间", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkPrimary,
                    titleContentColor = Color.White
                ),
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "用户", tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("👤 ${com.herspace.app.data.api.TokenManager.getEmail() ?: "未登录"}") },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("🚪 退出登录") },
                            onClick = { showMenu = false; onLogout() }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = onNavigateToVote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("立即投票", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── 高德地图 ──
            val lifecycleOwner = LocalLifecycleOwner.current
            var mapView by remember { mutableStateOf<MapView?>(null) }
            var aMap by remember { mutableStateOf<AMap?>(null) }
            var zoomLevel by remember { mutableFloatStateOf(15f) }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        mapView = this
                        val am = map
                        aMap = am

                        am.myLocationStyle = MyLocationStyle().apply {
                            myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
                            interval(2000)
                            strokeWidth(1f)
                        }
                        am.isMyLocationEnabled = true
                        am.uiSettings.isMyLocationButtonEnabled = true

                        am.setOnMyLocationChangeListener { location ->
                            if (location != null) {
                                val latLng = LatLng(location.latitude, location.longitude)
                                am.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                viewModel.onLocationObtained(location)
                            }
                        }

                        // 监听缩放级别变化
                        am.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                            override fun onCameraChange(cameraPosition: com.amap.api.maps.model.CameraPosition?) {}
                            override fun onCameraChangeFinish(cameraPosition: com.amap.api.maps.model.CameraPosition?) {
                                zoomLevel = cameraPosition?.zoom ?: 15f
                            }
                        })
                    }
                },
                update = { mv ->
                    val am = aMap ?: return@AndroidView
                    am.clear()
                    val items = state.filteredResults
                    if (items.isEmpty()) return@AndroidView

                    val isZoomedIn = zoomLevel >= 15f

                    for (item in items) {
                        val level = state.friendlinessMap[item.placeId] ?: FriendlinessLevel.UNKNOWN
                        val color = when (level) {
                            FriendlinessLevel.FRIENDLY -> Color(0xFF4CAF50)
                            FriendlinessLevel.NEUTRAL -> Color(0xFFFFC107)
                            FriendlinessLevel.UNFRIENDLY -> Color(0xFFF44336)
                            FriendlinessLevel.UNKNOWN -> Color(0xFF9E9E9E)
                        }
                        val latLng = LatLng(item.lat, item.lng)

                        if (isZoomedIn) {
                            // ≥ 15 级 → 带店名的彩色标签
                            val marker = am.addMarker(MarkerOptions()
                                .position(latLng)
                                .title(item.name)
                                .snippet(item.address)
                                .icon(BitmapDescriptorFactory.fromBitmap(createLabelBitmap(item.name, color)))
                                .anchor(0.5f, 0.5f)
                            )
                            marker?.apply { }
                        } else {
                            // < 15 级 → 彩色圆点
                            am.addMarker(MarkerOptions()
                                .position(latLng)
                                .title(item.name)
                                .snippet(item.address)
                                .icon(BitmapDescriptorFactory.fromBitmap(createDotBitmap(color)))
                                .anchor(0.5f, 0.5f)
                            )
                        }
                    }

                    // 点击标记 → 查看详情
                    am.setOnMarkerClickListener { marker ->
                        val item = items.find { it.name == marker.title }
                        if (item != null) {
                            keyboardController?.hide()
                            viewModel.selectSearchResult(item)
                        }
                        true
                    }
                    am.setOnMapClickListener { viewModel.clearSelection() }

                    // 📍 定位标记
                    state.currentLocation?.let { loc ->
                        am.addMarker(MarkerOptions()
                            .position(LatLng(loc.latitude, loc.longitude))
                            .icon(BitmapDescriptorFactory.fromBitmap(createEmojiBitmap("\uD83D\uDCCD", 120f)))
                            .anchor(0.5f, 0.85f)
                            .zIndex(999f)
                        )
                    }
                }
            )

            // 生命周期
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                        Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // 跳转
            LaunchedEffect(aMap, state.gotoLocation) {
                val am = aMap
                val loc = state.gotoLocation
                if (am != null && loc != null) {
                    am.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f))
                }
            }

            // ── 顶部筛选栏 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // 搜索框
                SearchBar(state, viewModel, keyboardController)

                Spacer(modifier = Modifier.height(8.dp))

                // 一级 Tab：分类
                CategoryTabs(
                    selected = state.selectedCategory,
                    onSelect = { viewModel.selectCategory(it) }
                )

                // 二级 Tab：友好度（选中分类后显示）
                Spacer(modifier = Modifier.height(2.dp))
                if (state.selectedCategory != null) {
                    FriendlinessTabs(
                        selected = state.selectedFriendlinessFilter,
                        onSelect = { viewModel.selectFriendlinessFilter(it) }
                    )
                }
            }  // end top filter column

            // ── 搜索模式浮层 ──
            if (state.isSearchMode) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        // 顶部搜索栏 + 返回按钮
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.exitSearchMode() }) {
                                Icon(Icons.Default.ArrowBack, "返回")
                            }
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("搜索酒店/商场/餐厅…") },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F5F5),
                                    unfocusedContainerColor = Color(0xFFF5F5F5)
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() })
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { viewModel.performSearch() },
                                shape = RoundedCornerShape(28.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PinkPrimary, contentColor = Color.White),
                                modifier = Modifier.size(52.dp)
                            ) { Icon(Icons.Default.Search, "搜索") }
                        }

                        // 搜索历史（无输入时） / 搜索结果
                        if (state.searchQuery.isBlank() && state.searchResults.isEmpty()) {
                            val history = SearchHistoryManager.getHistory()
                            if (history.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column {
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("搜索历史", fontSize = 13.sp, color = Color(0xFF888888))
                                            TextButton(onClick = { SearchHistoryManager.clear(); viewModel.onSearchQueryChanged(" ") }) { Text("清除", fontSize = 12.sp, color = PinkPrimary) }
                                        }
                                        history.take(10).forEachIndexed { idx, q ->
                                            Row(Modifier.fillMaxWidth().clickable { viewModel.onSearchQueryChanged(q); viewModel.performSearch() }.padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = Color(0xFFBBBBBB))
                                                Spacer(Modifier.width(12.dp))
                                                Text(q, fontSize = 15.sp, color = TextPrimary)
                                            }
                                            if (idx < history.size - 1) Divider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            } else {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无搜索历史", fontSize = 14.sp, color = Color(0xFFBBBBBB))
                                }
                            }
                        } else if (!state.isSearching && state.searchResults.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                LazyColumn(Modifier.heightIn(max = 500.dp)) {
                                    items(state.searchResults) { place ->
                                        Row(Modifier.fillMaxWidth().clickable { viewModel.selectSearchResult(place) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(PinkPrimaryLight), contentAlignment = Alignment.Center) {
                                                Text(place.name.take(1), fontWeight = FontWeight.Bold, color = PinkPrimary)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(place.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                Text(place.address, fontSize = 12.sp, color = Color(0xFF999999), maxLines = 1)
                                            }
                                        }
                                        if (place != state.searchResults.last()) Divider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        } else if (state.isSearching) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(32.dp), color = PinkPrimary)
                            }
                        }
                        Box(Modifier.weight(1f)) { } // spacer to allow scroll
                    }
                }
            }

            // ── 左侧竖向距离 Tab ──
            AnimatedVisibility(
                visible = state.selectedCategory != null,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                DistanceTabs(
                    selected = state.selectedDistanceFilter,
                    count = state.filteredResults.size,
                    onSelect = { viewModel.selectDistanceFilter(it) }
                )
            }

            // ── 详情卡片 ——
            AnimatedVisibility(
                visible = state.selectedPlaceSummary != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                state.selectedPlaceSummary?.let { summary ->
                    state.selectedFriendliness?.let { level ->
                        val userLoc = state.currentLocation
                        val dist = if (userLoc != null) {
                            summary.let { s ->
                                val r = 6371000.0
                                val dLat = Math.toRadians(s.placeLat - userLoc.latitude)
                                val dLng = Math.toRadians(s.placeLng - userLoc.longitude)
                                val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(userLoc.latitude)) * cos(Math.toRadians(s.placeLat)) * sin(dLng / 2).pow(2)
                                r * 2 * atan2(sqrt(a), sqrt(1 - a))
                            }
                        } else null
                        PlaceDetailCard(
                            summary = summary,
                            friendliness = level,
                            existingVoteType = state.existingVoteType,
                            distanceMeters = dist,
                            userLat = userLoc?.latitude,
                            userLng = userLoc?.longitude,
                            notFriendlyCount = state.notFriendlyCount,
                            veryUnfriendlyCount = state.veryUnfriendlyCount,
                            onDismiss = { viewModel.clearSelection() },
                            onSubmitVote = { voteType ->
                                viewModel.submitDirectVote(summary, voteType)
                            }
                        )
                    }
                }
            }

            // 搜索反馈
            if (state.isSearching) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp),
                    color = PinkPrimary
                )
            }
            LaunchedEffect(state.isSearching, state.searchResults, state.searchError, state.selectedDistanceFilter) {
                if (!state.isSearching) {
                    if (state.searchError != null) {
                        snackbarHostState.showSnackbar("搜索失败: ${state.searchError}", duration = SnackbarDuration.Short)
                    } else if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                        snackbarHostState.showSnackbar("未搜到「${state.searchQuery}」相关结果", duration = SnackbarDuration.Short)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
//  搜索栏
// ═══════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchBar(state: MapUiState, viewModel: MapViewModel, keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = {},
            placeholder = { Text("搜索酒店/商场/餐厅…") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF757575)) },
            modifier = Modifier.weight(1f).clickable { viewModel.enterSearchMode() }.focusable(false),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
            ),
            enabled = false,
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = { viewModel.enterSearchMode() },
            shape = RoundedCornerShape(28.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PinkPrimary, contentColor = Color.White),
            modifier = Modifier.size(52.dp)
        ) { Icon(Icons.Default.Search, "搜索") }
    }
}

// ═══════════════════════════════════════
//  一级分类 Tab
// ═══════════════════════════════════════

@Composable
private fun CategoryTabs(selected: PlaceCategory?, onSelect: (PlaceCategory) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlaceCategory.entries.forEach { cat ->
            val isSelected = cat == selected
            Surface(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PinkPrimary else Color(0xFFF5F5F5),
                contentColor = if (isSelected) Color.White else Color(0xFF616161)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (cat) {
                            PlaceCategory.HOTEL -> Icons.Default.Hotel
                            PlaceCategory.SHOPPING -> Icons.Default.ShoppingCart
                            PlaceCategory.BEAUTY -> Icons.Default.Favorite
                            PlaceCategory.ATTRACTION -> Icons.Default.Tour
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(cat.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
//  二级友好度 Tab
// ═══════════════════════════════════════

@Composable
private fun FriendlinessTabs(selected: FriendlinessFilter, onSelect: (FriendlinessFilter) -> Unit) {
    val styleMap = mapOf(
        FriendlinessFilter.ALL to Pair(Color(0xFFE0E0E0), Color(0xFF616161)),
        FriendlinessFilter.FRIENDLY to Pair(Color(0xFF4CAF50), Color.White),
        FriendlinessFilter.NEUTRAL to Pair(Color(0xFFFFC107), Color.White),
        FriendlinessFilter.UNFRIENDLY to Pair(Color(0xFFF44336), Color.White),
        FriendlinessFilter.UNKNOWN to Pair(Color(0xFF9E9E9E), Color.White)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterHorizontally)
    ) {
        FriendlinessFilter.entries.forEach { filter ->
            val (bg, fg) = styleMap[filter]!!
            val isSelected = filter == selected
            Surface(
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(14.dp),
                color = bg,
                border = if (isSelected) BorderStroke(2.dp, Color.White) else null
            ) {
                Box(
                    modifier = if (filter == FriendlinessFilter.ALL)
                        Modifier.padding(horizontal = 10.dp, vertical = 10.dp).padding(end = 10.dp)
                    else
                        Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        filter.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) fg else if (filter == FriendlinessFilter.ALL) Color(0xFF616161) else fg.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════
//  左侧竖向距离 Tab
// ═══════════════════════════════════════

@Composable
private fun DistanceTabs(
    selected: DistanceFilter?,
    count: Int,
    onSelect: (DistanceFilter) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(start = 8.dp)
            .width(80.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DistanceFilter.entries.forEach { filter ->
                val isSelected = filter == selected
                val icon = when (filter) {
                    DistanceFilter.NEAR -> Icons.Default.NearMe
                    DistanceFilter.MID -> Icons.Default.MyLocation
                    DistanceFilter.FAR -> Icons.Default.Public
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(filter) }
                        .background(
                            if (isSelected) PinkPrimary.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) PinkPrimary else Color(0xFF757575)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        filter.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PinkPrimary else Color(0xFF757575),
                        maxLines = 1
                    )
                }
                if (filter != DistanceFilter.FAR) {
                Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
//  图标工具函数
// ═══════════════════════════════════════

/** 彩色圆点（缩小用） */
private fun createDotBitmap(color: Color): Bitmap {
    val size = 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(cx, cx, cx, paint)
    val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(cx, cx, cx - 2f, stroke)
    return bitmap
}

/** 店名标签（放大用）*/
private fun createLabelBitmap(name: String, color: Color): Bitmap {
    val textSize = 36f
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.BLACK
        this.textSize = textSize
    }
    val nameWidth = textPaint.measureText(name).toInt() + 40 // 左右 padding
    val h = 56 // 标签高度

    val bitmap = Bitmap.createBitmap(nameWidth, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 圆角矩形背景（白色底）
    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    val rect = android.graphics.RectF(0f, 0f, nameWidth.toFloat(), h.toFloat())
    canvas.drawRoundRect(rect, 12f, 12f, bgPaint)

    // 左侧色条
    val barPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRoundRect(
        android.graphics.RectF(0f, 0f, 6f, h.toFloat()),
        3f, 3f, barPaint
    )

    // 店名
    textPaint.setColor(AndroidColor.parseColor("#333333"))
    canvas.drawText(name, 28f, h / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f), textPaint)

    // 描边
    val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.parseColor("#E0E0E0")
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    canvas.drawRoundRect(rect, 12f, 12f, stroke)

    return bitmap
}

private fun createEmojiBitmap(emoji: String, sizePx: Float): Bitmap {
    val s = sizePx.toInt()
    val bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx * 0.9f
        textAlign = android.graphics.Paint.Align.CENTER
        color = AndroidColor.BLACK
    }
    canvas.drawText(emoji, s / 2f, s / 2f - ((paint.descent() + paint.ascent()) / 2f), paint)
    return bitmap
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "${meters.toInt()}m" else "${"%.1f".format(meters / 1000)}km"
}
