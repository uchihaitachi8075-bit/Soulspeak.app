package com.example.soulspeak

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ConversationData(
    val id: String = "",
    val otherUserId: String = "",
    val otherUsername: String = "Soul",
    val lastMessage: String = "",
    val timestamp: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentChatsScreen(
    currentUserId: String,
    onBack: () -> Unit,
    onChatClick: (String, String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val theme = LocalSoulspeakColors.current
    
    var searchQuery by remember { mutableStateOf("") }
    val conversations = remember { mutableStateListOf<ConversationData>() }
    val filteredFriends = remember { mutableStateListOf<FriendData>() }
    var isLoading by remember { mutableStateOf(true) }
    var isSearchingFriends by remember { mutableStateOf(false) }

    // Listen for recent conversations
    LaunchedEffect(currentUserId) {
        db.collection("users").document(currentUserId).collection("conversations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    conversations.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(ConversationData::class.java)?.copy(id = doc.id)?.let {
                            conversations.add(it)
                        }
                    }
                }
                isLoading = false
            }
    }

    // Logic to search through friends
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearchingFriends = true
            val queryText = searchQuery.uppercase()
            val searchId = if (queryText.startsWith("SOUL-")) queryText else "SOUL-$queryText"

            db.collection("users").document(currentUserId).collection("friends").get().addOnSuccessListener { snapshot ->
                val friendIds = snapshot.documents.map { it.id }
                filteredFriends.clear()
                
                if (friendIds.isNotEmpty()) {
                    var processedCount = 0
                    friendIds.forEach { fId ->
                        db.collection("users").document(fId).get().addOnSuccessListener { doc ->
                            val pId = doc.getString("publicId") ?: ""
                            val uName = doc.getString("username") ?: ""
                            
                            if (pId.contains(searchId) || uName.contains(searchQuery, ignoreCase = true)) {
                                doc.toObject(FriendData::class.java)?.copy(id = doc.id)?.let {
                                    filteredFriends.add(it)
                                }
                            }
                            processedCount++
                            if (processedCount == friendIds.size) isSearchingFriends = false
                        }
                    }
                } else {
                    isSearchingFriends = false
                }
            }
        } else {
            filteredFriends.clear()
            isSearchingFriends = false
        }
    }

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = { Text("Recent Echoes", color = theme.text, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.nav)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Glassy Search Bar for Friends
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search friends to chat...", color = theme.text.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.primary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.text,
                    unfocusedTextColor = theme.text,
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.text.copy(alpha = 0.1f),
                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            if (isLoading || isSearchingFriends) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (searchQuery.isNotEmpty()) {
                        if (filteredFriends.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("No matching friends found.", color = theme.text.copy(alpha = 0.4f))
                                }
                            }
                        } else {
                            item { Text("Friends", color = theme.text.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                            items(filteredFriends) { friend ->
                                val directRoomId = if (currentUserId < friend.id) "${currentUserId}_${friend.id}" else "${friend.id}_${currentUserId}"
                                ConversationItemUI(
                                    name = friend.username,
                                    subText = friend.publicId,
                                    avatar = friend.avatar,
                                    onClick = { onChatClick(directRoomId, friend.username) }
                                )
                            }
                        }
                    } else {
                        if (conversations.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                    Text("No recent chats yet.", color = theme.text.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            items(conversations) { conv ->
                                ConversationItemUI(
                                    name = conv.otherUsername,
                                    subText = conv.lastMessage,
                                    avatar = "👤",
                                    onClick = { onChatClick(conv.id, conv.otherUsername) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItemUI(name: String, subText: String, avatar: String, onClick: () -> Unit) {
    val theme = LocalSoulspeakColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                color = theme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(avatar, fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = subText,
                    color = theme.text.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
