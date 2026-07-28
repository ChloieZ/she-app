package com.herspace.app.ui.vote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herspace.app.ui.theme.*
import com.herspace.app.util.SearchResult

private fun mapVoteLabel(type: String): String = when (type) {
    "friendly" -> "🥰 女性友好"
    "not_friendly" -> "😐 不够友好"
    "very_unfriendly" -> "😡 很不友好"
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteScreen(
    viewModel: VoteViewModel,
    onBack: () -> Unit,
    onVoteComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("投票", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            when (state.step) {
                VoteStep.LOCATING -> LocatingContent()
                VoteStep.SELECTING -> SelectingContent(
                    places = state.nearbyPlaces,
                    isManualInput = state.isManualInput,
                    manualPlaceName = state.manualPlaceName,
                    todayVotedIds = state.todayVotedIds,
                    onPlaceSelected = { viewModel.selectPlace(it) },
                    onSwitchToManual = { viewModel.switchToManualInput() },
                    onManualNameChanged = { viewModel.updateManualPlaceName(it) },
                    onConfirmManual = { viewModel.confirmManualPlace() }
                )
                VoteStep.VOTING -> VotingContent(
                    placeName = state.selectedPlace?.name ?: "",
                    existingVoteType = state.existingVoteType,
                    onSubmitVote = { viewModel.submitVote(it) }
                )
                VoteStep.DONE -> DoneContent(
                    errorMessage = state.errorMessage,
                    onContinue = {
                        viewModel.reset()
                        onVoteComplete()
                    },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun LocatingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PinkPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在获取你的位置…",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SelectingContent(
    places: List<SearchResult>,
    isManualInput: Boolean,
    manualPlaceName: String,
    todayVotedIds: Set<String>,
    onPlaceSelected: (SearchResult) -> Unit,
    onSwitchToManual: () -> Unit,
    onManualNameChanged: (String) -> Unit,
    onConfirmManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "你目前在哪个店铺？",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "选择系统检测到的店铺，或手动输入",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 附近店铺列表
        if (places.isNotEmpty() && !isManualInput) {
            Text(
                text = "定位到附近店铺",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PinkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(places) { place ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PinkPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = place.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = PinkPrimary
                                )
                                // 已投票：右上角绿色小圆点
                                if (place.placeId in todayVotedIds) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(FriendlyGreen)
                                            .border(2.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = place.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            val voted = place.placeId in todayVotedIds
                            if (voted) {
                                TextButton(onClick = { onPlaceSelected(place) }) {
                                    Text("修改", color = PinkPrimary, fontSize = 14.sp)
                                }
                            } else {
                                TextButton(onClick = { onPlaceSelected(place) }) {
                                    Text("投票", color = PinkPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onSwitchToManual,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("搜不到？手动输入店名")
            }
        }

        // 手动输入模式
        if (isManualInput) {
            OutlinedTextField(
                value = manualPlaceName,
                onValueChange = onManualNameChanged,
                placeholder = { Text("请输入店铺名称") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfirmManual,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                ),
                enabled = manualPlaceName.isNotBlank()
            ) {
                Text("确认", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun VotingContent(
    placeName: String,
    existingVoteType: String?,
    onSubmitVote: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "为「$placeName」投票",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (existingVoteType != null) {
            Text(
                text = "当前投票: ${mapVoteLabel(existingVoteType)}，点击可修改",
                fontSize = 13.sp,
                color = FriendlyGreen
            )
        } else {
            Text(
                text = "这个空间对女性友好吗？",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 三个大按钮
        VoteButton(
            icon = Icons.Default.SentimentSatisfied,
            label = "女性友好",
            description = "安心、舒适、设施完善",
            color = FriendlyGreen,
            onClick = { onSubmitVote("friendly") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VoteButton(
            icon = Icons.Default.SentimentDissatisfied,
            label = "不够友好",
            description = "有改进空间，体验一般",
            color = NeutralYellow,
            onClick = { onSubmitVote("not_friendly") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VoteButton(
            icon = Icons.Default.SentimentVeryDissatisfied,
            label = "很不友好",
            description = "感到不适、不安全、有歧视",
            color = UnfriendlyRed,
            onClick = { onSubmitVote("very_unfriendly") }
        )
    }
}

@Composable
private fun VoteButton(
    icon: ImageVector,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DoneContent(
    errorMessage: String?,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = FriendlyGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "感谢你的投票！",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "你的每一次投票都在帮助更多女性了解身边的友好空间",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, fontSize = 12.sp, color = UnfriendlyRed, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("继续投票", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PinkPrimary
                    )
                ) {
                    Text("返回地图", fontSize = 16.sp)
                }
            }
        }
    }
}
