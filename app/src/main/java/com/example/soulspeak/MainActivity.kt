package com.example.soulspeak

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soulspeak.components.ConfessionCard
import com.example.soulspeak.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        
        // Automatically get version code from the app's build config
        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode
            }
        } catch (e: Exception) { 1 }

        setContent {
            var isGirlsTheme by remember { mutableStateOf(false) }
            
            SoulspeakTheme(isGirlsTheme = isGirlsTheme) {
                val context = LocalContext.current
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateUrl by remember { mutableStateOf("") }
                
                val currentThemeColors = LocalSoulspeakColors.current

                LaunchedEffect(Unit) {
                    val db = FirebaseFirestore.getInstance()
                    // Updated to match your screenshot paths
                    db.collection("app_config.").document("version_settings.").get().addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val minVersion = doc.getLong("min_version") ?: 0
                            val url = doc.getString("url") ?: ""
                            if (currentVersionCode < minVersion) {
                                updateUrl = url
                                showUpdateDialog = true
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(currentThemeColors.background)) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(currentThemeColors.primary.copy(alpha = 0.08f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(200f, 200f), radius = 1200f)))
                    
                    if (showUpdateDialog) {
                        UpdateDialog(url = updateUrl)
                    } else if (currentUser == null) {
                        LoginScreen(
                            isGirlsTheme = isGirlsTheme,
                            onThemeChange = { isGirlsTheme = it },
                            onLoginSuccess = { currentUser = auth.currentUser }
                        )
                    } else {
                        MainScreen(userId = currentUser?.uid ?: "", onLogout = { auth.signOut(); currentUser = null })
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(url: String) {
    val context = LocalContext.current
    val theme = LocalSoulspeakColors.current
    AlertDialog(
        onDismissRequest = { },
        containerColor = theme.card,
        title = { Text("Update Available! 🚀", color = theme.text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("A newer, better version of Soulspeak is here. Update now to continue your journey.", color = theme.text.copy(alpha = 0.7f), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = { 
                    if (url.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Update Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun MainScreen(userId: String, onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var selectedItem by remember { mutableIntStateOf(0) }
    var showCreateScreen by remember { mutableStateOf(false) }
    var activeChatRoom by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedConfessionForComments by remember { mutableStateOf<String?>(null) }
    var selectedUserForProfile by remember { mutableStateOf<String?>(null) }
    var showFriendsList by remember { mutableStateOf(false) }
    var showRecentChats by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val confessions = remember { mutableStateListOf<ConfessionData>() }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                db.collection("users").document(userId).set(hashMapOf("username" to "Soul User", "gender" to "Not specified", "publicId" to "SOUL-${userId.take(4).uppercase()}", "avatar" to "👤"))
            }
        }
        // Listen for unread messages
        db.collection("users").document(userId).collection("conversations")
            .whereEqualTo("unread", true)
            .addSnapshotListener { snapshot, _ ->
                NotificationState.setChats(snapshot?.size() ?: 0)
            }
        db.collection("users").document(userId).collection("invites").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { diff ->
                if (diff.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    val data = diff.document.data
                    val roomId = data["roomId"] as String
                    val cat = data["category"] as String
                    Toast.makeText(context, "New Gossip Invite!", Toast.LENGTH_LONG).show()
                    activeChatRoom = roomId to cat
                    diff.document.reference.delete()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val registration = db.collection("confessions").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    confessions.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(ConfessionData::class.java)?.copy(id = doc.id)?.let { confessions.add(it) }
                    }
                }
            }
        onDispose { registration.remove() }
    }

    if (activeChatRoom != null && selectedUserForProfile == null) {
        BackHandler { activeChatRoom = null }
        if (activeChatRoom!!.first.startsWith("POSTS_")) {
            RoomPostsScreen(category = activeChatRoom!!.second, currentUserId = userId, onBack = { activeChatRoom = null }, onCommentClick = { id -> selectedConfessionForComments = id }, onUserClick = { id -> selectedUserForProfile = id })
        } else {
            ChatScreen(roomId = activeChatRoom!!.first, roomName = activeChatRoom!!.second, currentUserId = userId, onBack = { activeChatRoom = null }, onUserClick = { id -> selectedUserForProfile = id })
        }
    } else if (showRecentChats) {
        BackHandler { showRecentChats = false }
        RecentChatsScreen(currentUserId = userId, onBack = { showRecentChats = false }, onChatClick = { id, name -> activeChatRoom = id to name; showRecentChats = false })
    } else if (showFriendsList) {
        BackHandler { showFriendsList = false }
        FriendsListScreen(currentUserId = userId, onBack = { showFriendsList = false }, onChatWithFriend = { fId, fName ->
            val directId = if (userId < fId) "${userId}_${fId}" else "${fId}_${userId}"
            activeChatRoom = directId to fName
            showFriendsList = false
        })
    } else if (selectedUserForProfile != null) {
        BackHandler { selectedUserForProfile = null }
        PublicProfileScreen(targetUserId = selectedUserForProfile!!, currentUserId = userId, onBack = { selectedUserForProfile = null })
    } else if (selectedConfessionForComments != null) {
        BackHandler { selectedConfessionForComments = null }
        CommentScreen(confessionId = selectedConfessionForComments!!, currentUserId = userId, onBack = { selectedConfessionForComments = null })
    } else if (showCreateScreen) {
        CreateConfessionScreen(onDismiss = { showCreateScreen = false }, onPost = { text, category, mood ->
            val new = hashMapOf("userId" to userId, "username" to "Soul User", "avatar" to "👤", "category" to category, "mood" to mood, "timestamp" to System.currentTimeMillis(), "text" to text, "likes" to 0, "comments" to 0)
            db.collection("confessions").add(new).addOnSuccessListener { showCreateScreen = false }
        })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                if (selectedItem == 0) {
                    Header(onMessageClick = { showRecentChats = true })
                }
            },
            bottomBar = {
                val currentTheme = LocalSoulspeakColors.current
                Surface(modifier = Modifier.padding(bottom = 24.dp, start = 24.dp, end = 24.dp).clip(RoundedCornerShape(32.dp)), color = currentTheme.text.copy(alpha = 0.08f), border = androidx.compose.foundation.BorderStroke(0.5.dp, currentTheme.text.copy(alpha = 0.12f))) {
                    NavigationBar(containerColor = Color.Transparent, modifier = Modifier.height(72.dp), tonalElevation = 0.dp) {
                        val items = listOf("Home", "Search", "Rooms", "Profile")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Groups, Icons.Default.Person)
                        items.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { Icon(icons[index], item, modifier = Modifier.size(30.dp)) }, 
                                selected = selectedItem == index, 
                                onClick = { selectedItem = index }, 
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = currentTheme.primary, 
                                    unselectedIconColor = currentTheme.text.copy(alpha = 0.4f), 
                                    indicatorColor = currentTheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                val theme = LocalSoulspeakColors.current
                // Show FAB on Home (0) and Rooms (2)
                if (selectedItem == 0 || selectedItem == 2) {
                    FloatingActionButton(
                        onClick = { showCreateScreen = true },
                        containerColor = theme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 80.dp)
                    ) { 
                        Icon(Icons.Default.Add, "Post Confession")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedItem) {
                    0 -> HomeContent(confessions, onCommentClick = { id -> selectedConfessionForComments = id }, onUserClick = { id -> selectedUserForProfile = id })
                    1 -> SearchScreen(onUserClick = { id -> selectedUserForProfile = id })
                    2 -> RoomsScreen(currentUserId = userId, onJoinChat = { id, name -> activeChatRoom = id to name }, onSeePosts = { category -> activeChatRoom = "POSTS_$category" to category })
                    3 -> ProfileScreen(onLogout = onLogout, userConfessions = confessions.filter { it.userId == userId }, onDeletePost = { id -> db.collection("confessions").document(id).delete() }, onCommentClick = { id -> selectedConfessionForComments = id }, onSeeFriends = { showFriendsList = true })
                }
            }
        }
    }
}

@Composable
fun HomeContent(confessions: List<ConfessionData>, onCommentClick: (String) -> Unit, onUserClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalItemSpacing = 12.dp) {
            items(confessions) { confession ->
                ConfessionCard(confessionId = confession.id, username = confession.username, avatar = confession.avatar, category = confession.category, mood = confession.mood, text = confession.text, likes = confession.likes, userId = confession.userId, onCommentClick = { onCommentClick(confession.id) }, onUserClick = onUserClick)
            }
        }
    }
}

@Composable
fun Header(onMessageClick: () -> Unit) {
    val theme = LocalSoulspeakColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = theme.text.copy(alpha = 0.1f)) {
                Image(painter = painterResource(id = R.drawable.logo), "Logo", modifier = Modifier.padding(6.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Soulspeak", color = theme.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Speak Freely. Stay Anonymous.", color = theme.primary.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
        Row {
            IconButton(onClick = onMessageClick) {
                BadgedBox(badge = {
                    if (NotificationState.chatCount > 0) {
                        Badge(containerColor = theme.primary) {
                            Text(NotificationState.chatCount.toString(), color = Color.White)
                        }
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Chat, "Chats", tint = theme.text)
                }
            }
            IconButton(onClick = { NotificationState.clear() }) {
                BadgedBox(badge = {
                    if (NotificationState.count > 0) {
                        Badge(containerColor = Color.Red) {
                            Text(NotificationState.count.toString(), color = Color.White)
                        }
                    }
                }) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = theme.text)
                }
            }
        }
    }
}

data class ConfessionData(val id: String = "", val userId: String = "", val username: String = "", val avatar: String = "", val category: String = "", val mood: String = "", val timestamp: Long = 0, val text: String = "", val likes: Int = 0, val comments: Int = 0)
