package com.example.soulspeak

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    roomName: String,
    currentUserId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val theme = LocalSoulspeakColors.current
    
    // Unique Session ID for this room (User #1, User #2, etc.)
    var myUniqueId by remember { mutableStateOf("User ...") }
    var roomSoulId by remember { mutableStateOf("") }

    // Dynamic Wallpaper Gradient based on room name
    val wallpaperBrush = remember(roomName) {
        when {
            roomName.contains("Love", true) -> Brush.verticalGradient(listOf(Color(0xFFFFB7C5), Color(0xFF9D174D)))
            roomName.contains("Gaming", true) -> Brush.verticalGradient(listOf(Color(0xFF5B21B6), Color(0xFF1E1B4B)))
            roomName.contains("Money", true) -> Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF064E3B)))
            roomName.contains("Gossip", true) -> Brush.verticalGradient(listOf(Color(0xFFFFDEE9), Color(0xFFB5FFFC)))
            roomName.contains("Fitness", true) -> Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B)))
            else -> Brush.verticalGradient(listOf(theme.background, theme.background.copy(alpha = 0.8f)))
        }
    }

    LaunchedEffect(roomId) {
        db.collection("chat_rooms").document(roomId).get().addOnSuccessListener { doc ->
            val count = doc.getLong("memberCount") ?: 1
            roomSoulId = doc.getString("soulId") ?: "Soul-Main"
            myUniqueId = "User #$count"
        }
        db.collection("users").document(currentUserId).collection("conversations").document(roomId)
            .update("unread", false)
    }

    DisposableEffect(roomId) {
        val registration = db.collection("chat_rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(ChatMessage::class.java)?.let { messages.add(it) }
                    }
                }
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$roomName Room", color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("$roomSoulId | You are $myUniqueId", color = theme.primary, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.nav)
            )
        },
        bottomBar = {
            Surface(color = theme.nav, tonalElevation = 8.dp, modifier = Modifier.imePadding()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...", color = theme.text.copy(alpha = 0.3f)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.text,
                            unfocusedTextColor = theme.text,
                            focusedBorderColor = theme.primary,
                            unfocusedBorderColor = theme.text.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val msg = hashMapOf(
                                    "senderId" to currentUserId,
                                    "senderName" to myUniqueId,
                                    "text" to messageText,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("chat_rooms").document(roomId)
                                    .collection("messages").add(msg)
                                    .addOnSuccessListener {
                                        messageText = ""
                                        if (roomId.contains("_")) {
                                            val parts = roomId.split("_")
                                            val otherId = if (parts[0] == currentUserId) parts[1] else parts[0]
                                            db.collection("users").document(otherId).collection("conversations").document(roomId).set(hashMapOf(
                                                "otherUserId" to currentUserId, "otherUsername" to myUniqueId, "lastMessage" to msg["text"], "timestamp" to System.currentTimeMillis(), "unread" to true
                                            ))
                                            db.collection("users").document(currentUserId).collection("conversations").document(roomId).set(hashMapOf(
                                                "otherUserId" to otherId, "otherUsername" to roomName, "lastMessage" to msg["text"], "timestamp" to System.currentTimeMillis(), "unread" to false
                                            ))
                                        }
                                    }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = theme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(wallpaperBrush)) {
            // Subtle Overlay to make text readable
            Box(modifier = Modifier.fillMaxSize().background(theme.background.copy(alpha = 0.3f)))
            
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message, message.senderId == currentUserId, onUserClick)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean, onUserClick: (String) -> Unit) {
    val theme = LocalSoulspeakColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                color = theme.primary,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 2.dp)
                    .clickable { onUserClick(message.senderId) }
            )
        }
        Surface(
            color = if (isCurrentUser) theme.primary else theme.card.copy(alpha = 0.9f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 16.dp
            ),
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                color = if (isCurrentUser) Color.White else theme.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
    }
}
