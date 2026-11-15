package com.example.soulspeak

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Shuffle
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
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomsScreen(
    currentUserId: String,
    onJoinChat: (String, String) -> Unit,
    onSeePosts: (String) -> Unit
) {
    var showMatchDialog by remember { mutableStateOf(false) }
    var showGossipDialog by remember { mutableStateOf(false) }
    var isMatching by remember { mutableStateOf(false) }
    var matchCategory by remember { mutableStateOf("") }
    var waitingRoomId by remember { mutableStateOf<String?>(null) }
    
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val theme = LocalSoulspeakColors.current

    LaunchedEffect(waitingRoomId) {
        if (waitingRoomId != null) {
            db.collection("chat_rooms").document(waitingRoomId!!).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    if ((snapshot.getLong("memberCount") ?: 1) >= 2) {
                        isMatching = false
                        onJoinChat(waitingRoomId!!, matchCategory)
                        waitingRoomId = null
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Confession Rooms", style = MaterialTheme.typography.headlineMedium, color = theme.text, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // Large Random Match Card
        Surface(
            onClick = { if (!isMatching) showMatchDialog = true },
            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(32.dp)),
            color = theme.text.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, theme.primary.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isMatching) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matching in $matchCategory...", color = theme.text, fontSize = 14.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.Default.Shuffle, null, tint = theme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Random Match & Chat", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Connect with a stranger worldwide", color = theme.text.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Special "Girls Gossip" Room (Large Card)
        Surface(
            onClick = { showGossipDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, Brush.linearGradient(listOf(SoftPink, theme.primary)), RoundedCornerShape(32.dp)),
            color = theme.text.copy(alpha = 0.05f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(SoftPink.copy(alpha = 0.1f), Color.Transparent))).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Female, null, tint = SoftPink, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Girls Gossip 🌸", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Private circles with Soul Friends", color = theme.text.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                Surface(modifier = Modifier.align(Alignment.BottomEnd), color = SoftPink, shape = RoundedCornerShape(12.dp)) {
                    Text("Enter Room", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Community Hubs", style = MaterialTheme.typography.titleLarge, color = theme.text, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val roomCategories = listOf(
            Triple("Love", "❤️", listOf(SoftPink, Color(0xFF9D174D))),
            Triple("Money", "💰", listOf(GoldenYellow, Color(0xFFB45309))),
            Triple("Fitness", "🏋️", listOf(Color(0xFF10B981), Color(0xFF047857))),
            Triple("Gaming", "🎮", listOf(theme.primary, Color(0xFF5B21B6)))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(roomCategories) { (name, icon, colors) ->
                RoomCard(name, icon, colors[0]) { onSeePosts(name) }
            }
        }
    }

    if (showMatchDialog) {
        CategorySelectionDialog("Match Topic", { showMatchDialog = false }) { cat ->
            showMatchDialog = false
            matchCategory = cat
            isMatching = true
            startSmartMatch(cat, 4, db, { id -> isMatching = false; onJoinChat(id, cat) }, { id -> waitingRoomId = id }, { isMatching = false })
        }
    }

    if (showGossipDialog) {
        GirlsGossipInviteDialog(
            currentUserId = currentUserId,
            db = db,
            onDismiss = { showGossipDialog = false },
            onInviteSent = { roomId ->
                showGossipDialog = false
                onJoinChat(roomId, "Girls Gossip")
            }
        )
    }
}

@Composable
fun GirlsGossipInviteDialog(
    currentUserId: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit,
    onInviteSent: (String) -> Unit
) {
    val theme = LocalSoulspeakColors.current
    val friends = remember { mutableStateListOf<FriendData>() }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var showFriendSelector by remember { mutableStateOf(false) }
    val selectedFriends = remember { mutableStateListOf<String>() }

    LaunchedEffect(showFriendSelector) {
        if (showFriendSelector) {
            isLoadingFriends = true
            db.collection("users").document(currentUserId).collection("friends").get().addOnSuccessListener { snapshot ->
                friends.clear()
                val friendIds = snapshot.documents.map { it.id }
                if (friendIds.isEmpty()) isLoadingFriends = false
                else {
                    friendIds.forEach { fId ->
                        db.collection("users").document(fId).get().addOnSuccessListener { doc ->
                            doc.toObject(FriendData::class.java)?.copy(id = doc.id)?.let { friends.add(it) }
                            if (friends.size == friendIds.size) isLoadingFriends = false
                        }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        title = { Text("Girls Gossip 🌸", color = SoftPink, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!showFriendSelector) {
                    Text("Start a private session and invite your friends.", color = theme.text.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showFriendSelector = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = theme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Friends to Gossip", color = theme.text)
                    }
                } else {
                    Text("Select members (Max 3):", color = theme.text.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (isLoadingFriends) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SoftPink)
                        }
                    } else if (friends.isEmpty()) {
                        Text("No friends yet.", color = theme.text.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 20.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(friends) { friend ->
                                val isSelected = selectedFriends.contains(friend.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                        if (isSelected) selectedFriends.remove(friend.id)
                                        else if (selectedFriends.size < 3) selectedFriends.add(friend.id)
                                    }
                                ) {
                                    Surface(modifier = Modifier.size(36.dp).clip(CircleShape), color = theme.primary.copy(alpha = 0.2f)) {
                                        Box(contentAlignment = Alignment.Center) { Text(friend.avatar, fontSize = 16.sp) }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(friend.username, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(friend.publicId, color = SoftPink, fontSize = 11.sp)
                                    }
                                    Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SoftPink, uncheckedColor = theme.text.copy(alpha = 0.3f)))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showFriendSelector && selectedFriends.isNotEmpty()) {
                Button(
                    onClick = {
                        val roomId = "GIRLS_${System.currentTimeMillis()}"
                        val soulId = "GOSSIP-${(1000..9999).random()}"
                        val roomData = hashMapOf(
                            "category" to "Girls Gossip", "memberCount" to 1, "maxMembers" to 4,
                            "soulId" to soulId, "status" to "WAITING", "createdBy" to currentUserId
                        )
                        db.collection("chat_rooms").document(roomId).set(roomData).addOnSuccessListener {
                            selectedFriends.forEach { fId ->
                                db.collection("users").document(fId).collection("invites").add(mapOf("roomId" to roomId, "from" to currentUserId, "category" to "Girls Gossip"))
                            }
                            onInviteSent(roomId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Send Invites & Create", color = Color.White) }
            }
        }
    )
}

@Composable
fun RoomCard(name: String, icon: String, color: Color, onClick: () -> Unit) {
    val theme = LocalSoulspeakColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(24.dp)),
        color = theme.text.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column {
                Text(icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(name, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Surface(modifier = Modifier.align(Alignment.BottomEnd), color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))) {
                Text("See Posts", color = color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategorySelectionDialog(title: String, onDismiss: () -> Unit, onSelected: (String) -> Unit) {
    val theme = LocalSoulspeakColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        title = { Text(title, color = theme.text) },
        text = {
            val categories = listOf("Love", "Money", "Fitness", "Gaming", "Dreams", "Funny")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { cat ->
                            Button(
                                onClick = { onSelected(cat) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(cat, color = theme.text) }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun startSmartMatch(category: String, limit: Int, db: FirebaseFirestore, onJoined: (String) -> Unit, onWaiting: (String) -> Unit, onFailure: (String) -> Unit) {
    db.collection("chat_rooms").whereEqualTo("category", category).whereEqualTo("memberCount", 1).whereEqualTo("maxMembers", limit).limit(1).get().addOnSuccessListener { snapshot ->
        if (snapshot.isEmpty) {
            val soulId = "Soul-${(1000..9999).random()}"
            db.collection("chat_rooms").add(hashMapOf("category" to category, "memberCount" to 1, "maxMembers" to limit, "soulId" to soulId, "status" to "WAITING", "createdAt" to System.currentTimeMillis())).addOnSuccessListener { onWaiting(it.id) }
        } else {
            val doc = snapshot.documents[0]
            db.collection("chat_rooms").document(doc.id).update("memberCount", FieldValue.increment(1), "status", "FULL").addOnSuccessListener { onJoined(doc.id) }
        }
    }
}
