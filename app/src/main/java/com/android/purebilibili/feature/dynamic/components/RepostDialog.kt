// 文件路径: feature/dynamic/components/RepostDialog.kt
package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 *  动态转发对话框
 */
@Composable
fun RepostDialog(
    onDismiss: () -> Unit,
    onRepost: (content: String, onComplete: (Boolean) -> Unit) -> Unit
) {
    var repostText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = { if (!isPosting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = AppShapes.container(ContainerLevel.Dialog),
            color = AppSurfaceTokens.cardContainer()
        ) {
            Column(
                modifier = Modifier.padding(AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.ArrowTurnUpRight,
                        contentDescription = null,
                        modifier = Modifier.size(AppSpacingTokens.ExtraLarge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
                    Text(
                        "转发动态",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(AppSpacingTokens.Large))
                
                // 输入框
                OutlinedTextField(
                    value = repostText,
                    onValueChange = { repostText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppSpacingTokens.TripleExtraLarge * 2 + AppSpacingTokens.ExtraLarge),
                    placeholder = { 
                        Text(
                            "说点什么吧...(可选)",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        ) 
                    },
                    shape = AppShapes.container(ContainerLevel.Card),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(AppSpacingTokens.Large + AppSpacingTokens.ExtraSmall))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isPosting
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(AppSpacingTokens.Medium))
                    
                    Button(
                        onClick = {
                            isPosting = true
                            onRepost(repostText) { success ->
                                if (!success) {
                                    isPosting = false
                                }
                            }
                        },
                        enabled = !isPosting,
                        shape = AppShapes.container(ContainerLevel.Sheet)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppSpacingTokens.Large),
                                strokeWidth = AppSpacingTokens.Micro,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("转发")
                        }
                    }
                }
            }
        }
    }
}
