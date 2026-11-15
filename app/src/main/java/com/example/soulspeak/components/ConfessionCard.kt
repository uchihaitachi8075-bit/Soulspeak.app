package com.example.soulspeak.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

@Composable
fun ConfessionCard(
    confessionId: String,
    username: String,
    avatar: String,
    category: String,
    mood: String,
    text: String,
    likes: Int = 0,
    userId: String = "",
    onDelete: (() -> Unit)? = null,
    onCommentClick: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(likes) }
    var showMenu by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    
    val theme = LocalSoulspeakColors.current

    // iOS Style Glass Card
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(theme.text.copy(alpha = 0.2f), theme.text.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        color = theme.text.copy(alpha = 0.05f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(theme.text.copy(alpha = 0.1f), Color.Transparent)))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(theme.primary, theme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = username,
                    color = theme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = theme.text.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(theme.card)
                    ) {
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = { onDelete(); showMenu = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report", color = theme.text) },
                            onClick = { showMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = text,
                color = theme.text.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 12,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = theme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, theme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = category,
                        color = theme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { 
                            isLiked = !isLiked
                            val inc = if (isLiked) 1L else -1L
                            likeCount += inc.toInt()
                            db.collection("confessions").document(confessionId).update("likes", FieldValue.increment(inc))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            tint = if (isLiked) SoftPink else theme.text.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text("$likeCount", color = theme.text.copy(alpha = 0.5f), fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = onCommentClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = theme.primary.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
