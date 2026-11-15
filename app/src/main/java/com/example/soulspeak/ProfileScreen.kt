package com.example.soulspeak

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.components.ConfessionCard
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    userConfessions: List<ConfessionData>,
    onDeletePost: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onSeeFriends: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val theme = LocalSoulspeakColors.current

    var gender by remember { mutableStateOf("Not specified") }
    var publicId by remember { mutableStateOf("Soul-XXXX") }
    var friendCount by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                gender = doc.getString("gender") ?: "Not specified"
                val existingId = doc.getString("publicId")
                if (existingId == null) {
                    publicId = "SOUL-${userId.take(4).uppercase()}"
                    db.collection("users").document(userId).update("publicId", publicId)
                } else {
                    publicId = existingId
                }
            } else {
                publicId = "SOUL-${userId.take(4).uppercase()}"
                val initialData = hashMapOf(
                    "username" to "Anonymous Fox",
                    "gender" to "Not specified",
                    "publicId" to publicId,
                    "avatar" to "🦊"
                )
                db.collection("users").document(userId).set(initialData)
            }
        }
        
        db.collection("users").document(userId).collection("friends").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                friendCount = snapshot.size()
            }
        }
    }

    if (showEditDialog) {
        var tempGender by remember { mutableStateOf(gender) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = theme.card,
            title = { Text("Edit Profile", color = theme.text) },
            text = {
                Column {
                    Text("Select Gender:", color = theme.text.copy(alpha = 0.7f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = tempGender == g,
                                onClick = { tempGender = g },
                                label = { Text(g) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = theme.primary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    db.collection("users").document(userId).update("gender", tempGender)
                    gender = tempGender
                    showEditDialog = false
                }) { Text("Save") }
            }
        )
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = theme.text)
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit", tint = theme.text)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.size(104.dp).clip(CircleShape),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "🦊", fontSize = 48.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Anonymous Fox", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(publicId, color = theme.primary, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                    color = theme.text.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.text.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat("Posts", userConfessions.size.toString())
                        ProfileStat("Gender", gender)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSeeFriends() }
                        ) {
                            Text(friendCount.toString(), color = theme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Hearts", color = theme.text.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("My Echoes", color = theme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (userConfessions.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Silence... no posts yet.", color = theme.text.copy(alpha = 0.3f))
                }
            }
        } else {
            items(userConfessions) { confession ->
                ConfessionCard(
                    confessionId = confession.id, username = confession.username, avatar = confession.avatar,
                    category = confession.category, mood = confession.mood, text = confession.text,
                    likes = confession.likes, onDelete = { onDeletePost(confession.id) },
                    onCommentClick = { onCommentClick(confession.id) }
                )
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    val theme = LocalSoulspeakColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = theme.text.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}
