package com.example.soulspeak

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

data class FriendData(
    val id: String = "",
    val username: String = "",
    val avatar: String = "👤",
    val publicId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    currentUserId: String,
    onBack: () -> Unit,
    onChatWithFriend: (String, String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val friends = remember { mutableStateListOf<FriendData>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        db.collection("users").document(currentUserId).collection("friends")
            .get()
            .addOnSuccessListener { snapshot ->
                val friendIds = snapshot.documents.map { it.id }
                if (friendIds.isEmpty()) {
                    isLoading = false
                } else {
                    friendIds.forEach { fId ->
                        db.collection("users").document(fId).get().addOnSuccessListener { doc ->
                            doc.toObject(FriendData::class.java)?.copy(id = doc.id)?.let {
                                friends.add(it)
                            }
                            if (friends.size == friendIds.size) isLoading = false
                        }
                    }
                }
            }
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("Soul Friends", color = SoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavDark)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Soul Friends yet. Add them from chats!", color = SoftWhite.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(friends) { friend ->
                    FriendItem(friend) { onChatWithFriend(friend.id, friend.username) }
                }
            }
        }
    }
}

@Composable
fun FriendItem(friend: FriendData, onChat: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    color = PrimaryPurple.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(friend.avatar, fontSize = 24.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(friend.username, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(friend.publicId, color = PrimaryPurple, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onChat,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Message", tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Secret Message", color = SoftWhite, fontSize = 13.sp)
            }
        }
    }
}
