package com.herspace.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herspace.app.ui.theme.*

/** 三个输入框统一文本样式（含字体内边距 + 足够行高，避免掩码圆点被裁剪） */
private val AuthFieldTextStyle = TextStyle(
    color = Color.Black,
    fontSize = 16.sp,
    lineHeight = 28.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)

/** 自定义密码掩码：使用 ●（U+25CF），部分系统字体对默认 • 渲染异常 */
private class BulletVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val masked = buildAnnotatedString {
            repeat(text.length) { append('\u25CF') }
        }
        return TransformedText(masked, OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoggedIn()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            // 键盘弹出时内容上移，避免遮挡输入框（targetSdk34 需显式处理 IME inset）
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Favorite, null, modifier = Modifier.size(64.dp), tint = PinkPrimary)
            Spacer(Modifier.height(4.dp))
            Text("她空间", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PinkPrimary)
            Text("女性友好空间地图", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { if (!state.isLoginMode) viewModel.toggleMode() }) {
                    Text("登录", fontWeight = if (state.isLoginMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.isLoginMode) PinkPrimary else TextSecondary, fontSize = 16.sp)
                }
                Spacer(Modifier.width(24.dp))
                TextButton(onClick = { if (state.isLoginMode) viewModel.toggleMode() }) {
                    Text("注册", fontWeight = if (!state.isLoginMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!state.isLoginMode) PinkPrimary else TextSecondary, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    AnimatedVisibility(visible = !state.isLoginMode) {
                        Column {
                            OutlinedTextField(
                                value = state.nickname,
                                onValueChange = { viewModel.updateNickname(it) },
                                label = { Text("昵称（选填）") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = AuthFieldTextStyle
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = { Text("邮箱") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        textStyle = AuthFieldTextStyle
                    )
                    Spacer(Modifier.height(12.dp))

                    var showPwd by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPwd = !showPwd }) {
                                Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (showPwd) VisualTransformation.None else BulletVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                        textStyle = AuthFieldTextStyle
                    )
                    Spacer(Modifier.height(8.dp))

                    AnimatedVisibility(visible = state.error != null) {
                        Text(state.error ?: "", color = UnfriendlyRed, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.submit() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(if (state.isLoginMode) "登录" else "注册", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (state.isLoginMode) "还没有账号？点击上方「注册」" else "已有账号？点击上方「登录」",
                fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center
            )
        }
    }
}
