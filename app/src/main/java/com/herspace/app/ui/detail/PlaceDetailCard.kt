package com.herspace.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herspace.app.data.db.PlaceSummary
import com.herspace.app.data.model.FriendlinessLevel
import com.herspace.app.ui.theme.*

@Composable
fun PlaceDetailCard(
    summary: PlaceSummary,
    friendliness: FriendlinessLevel,
    existingVoteType: String? = null,
    distanceMeters: Double? = null,
    userLat: Double? = null,
    userLng: Double? = null,
    notFriendlyCount: Int = 0,
    veryUnfriendlyCount: Int = 0,
    onDismiss: () -> Unit,
    onSubmitVote: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = when (friendliness) {
        FriendlinessLevel.FRIENDLY -> FriendlyGreen
        FriendlinessLevel.NEUTRAL -> NeutralYellow
        FriendlinessLevel.UNFRIENDLY -> UnfriendlyRed
        FriendlinessLevel.UNKNOWN -> Color(0xFF9E9E9E)
    }
    var showVoteOptions by remember { mutableStateOf(false) }
    val distText = if (distanceMeters != null) {
        if (distanceMeters < 1000) "${distanceMeters.toInt()}m"
        else "${"%.1f".format(distanceMeters / 1000)}km"
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 地点名称 + 距离
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.placeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                if (distText != null) {
                    Text(distText, fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 友好度
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("友好度: ", fontSize = 14.sp, color = TextSecondary)
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                Text(friendliness.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text("总投票: ${summary.totalVotes}", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row {
                Text("🟢 ${summary.friendlyCount} 友好  ", fontSize = 13.sp, color = FriendlyGreen, fontWeight = FontWeight.Medium)
                Text("🟡 ${notFriendlyCount} 一般  ", fontSize = 13.sp, color = NeutralYellow, fontWeight = FontWeight.Medium)
                Text("🔴 ${veryUnfriendlyCount} 不友好", fontSize = 13.sp, color = UnfriendlyRed, fontWeight = FontWeight.Medium)
            }

            // 友好度进度条
            val ratio = if (summary.totalVotes > 0) summary.friendlyCount.toFloat() / summary.totalVotes else 0f
            LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = FriendlyGreen, trackColor = Color(0xFFE0E0E0))
            Spacer(Modifier.height(8.dp))

            // 已投票信息
            if (existingVoteType != null) {
                val voteLabel = when (existingVoteType) {
                    "friendly" -> "🥰 女性友好"
                    "not_friendly" -> "😐 不够友好"
                    "very_unfriendly" -> "😡 很不友好"
                    else -> existingVoteType
                }
                Text("我的评价: $voteLabel", fontSize = 13.sp, color = FriendlyGreen, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
            }

            // 投票选项
            AnimatedVisibility(visible = showVoteOptions) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text("你觉得这里怎么样？", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    VoteOption("🥰 女性友好", "安心、舒适、设施完善", FriendlyGreen) {
                        showVoteOptions = false
                        onSubmitVote?.invoke("friendly")
                    }
                    Spacer(Modifier.height(6.dp))
                    VoteOption("😐 不够友好", "有改进空间，体验一般", NeutralYellow) {
                        showVoteOptions = false
                        onSubmitVote?.invoke("not_friendly")
                    }
                    Spacer(Modifier.height(6.dp))
                    VoteOption("😡 很不友好", "感到不适、不安全", UnfriendlyRed) {
                        showVoteOptions = false
                        onSubmitVote?.invoke("very_unfriendly")
                    }
                }
            }

            // 底部分组按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onSubmitVote != null) {
                    Button(
                        onClick = { showVoteOptions = !showVoteOptions },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (existingVoteType != null) NeutralYellow else PinkPrimary),
                        enabled = true
                    ) {
                        Text(
                            if (showVoteOptions) "取消" else if (existingVoteType != null) "修改投票" else "投票",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("关闭", color = PinkPrimary) }
            }
        }
    }
}

@Composable
private fun VoteOption(label: String, desc: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f), contentColor = color)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
