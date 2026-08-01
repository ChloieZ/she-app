package com.herspace.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    generallyFriendlyCount: Int = 0,
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
    val bgColor = color.copy(alpha = 0.08f)
    val textOnColor = Color.White
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
        Column {
            // ── 顶部色条 ——
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color)
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // 地点名称 + 距离 + 友好度标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(summary.placeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(friendliness.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
                            if (distText != null) {
                                Text(" · $distText", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── Tab 式统计 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatTab("🟢", "${summary.friendlyCount}", "友好", FriendlyGreen)
                    StatTab("🟡", "${generallyFriendlyCount}", "一般", NeutralYellow)
                    StatTab("🔴", "${notFriendlyCount + veryUnfriendlyCount}", "不友好", UnfriendlyRed)
                }
                Spacer(Modifier.height(4.dp))
                Text("总投票: ${summary.totalVotes}", fontSize = 11.sp, color = TextSecondary)

                // 友好度进度条
                Spacer(Modifier.height(8.dp))
                val ratio = if (summary.totalVotes > 0) summary.friendlyCount.toFloat() / summary.totalVotes else 0f
                LinearProgressIndicator(
                    progress = ratio,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = FriendlyGreen,
                    trackColor = Color(0xFFE0E0E0)
                )

                // 已投票信息
                if (existingVoteType != null) {
                    Spacer(Modifier.height(8.dp))
                    val voteLabel = when (existingVoteType) {
                        "friendly" -> "🥰 友好"
                        "generally_friendly" -> "🙂 一般"
                        "not_friendly" -> "😐 不友好"
                        else -> existingVoteType
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FriendlyGreen.copy(alpha = 0.12f)
                    ) {
                        Text(" 我的评价: $voteLabel", fontSize = 13.sp, color = FriendlyGreen, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // ── 投票选项（可滑动） ──
                AnimatedVisibility(visible = showVoteOptions) {
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(8.dp))
                        Text("你觉得这里怎么样？", fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))

                        VoteOption("🥰 友好", "安心、舒适、设施完善", FriendlyGreen) {
                            showVoteOptions = false; onSubmitVote?.invoke("friendly")
                        }
                        Spacer(Modifier.height(6.dp))
                        VoteOption("🙂 一般", "还可以，整体体验不错", NeutralYellow) {
                            showVoteOptions = false; onSubmitVote?.invoke("generally_friendly")
                        }
                        Spacer(Modifier.height(6.dp))
                        VoteOption("😐 不友好", "有改进空间", Color(0xFFFF9800)) {
                            showVoteOptions = false; onSubmitVote?.invoke("not_friendly")
                        }
                    }
                }

                // ── 底部按钮 ──
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (onSubmitVote != null) {
                        FilledTonalButton(
                            onClick = { showVoteOptions = !showVoteOptions },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (showVoteOptions) Color(0xFFEEEEEE) else color.copy(alpha = 0.15f),
                                contentColor = if (showVoteOptions) TextSecondary else color
                            )
                        ) {
                            Icon(
                                if (showVoteOptions) Icons.Default.Close else Icons.Default.ThumbUp,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showVoteOptions) "收起" else if (existingVoteType != null) "修改投票" else "投票",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEEEEEE),
                            contentColor = TextSecondary
                        )
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("关闭", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTab(emoji: String, count: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$emoji $count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
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
