package com.example.soulspeak

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.components.ConfessionCard
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomPostsScreen(
    category: String,
    currentUserId: String,
    onBack: () -> Unit,
    onCommentClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val posts = remember { mutableStateListOf<ConfessionData>() }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(category) {
        val registration = db.collection("confessions")
            .whereEqualTo("category", category)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (snapshot != null) {
                    posts.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(ConfessionData::class.java)?.copy(id = doc.id)?.let {
                            posts.add(it)
                        }
                    }
                }
            }
        onDispose { registration.remove() }
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$category Room", color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Anonymous Shared Experiences", color = PrimaryPurple, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SoftWhite)
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
        } else if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No posts in this room yet.", color = SoftWhite.copy(alpha = 0.5f))
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(posts) { post ->
                    ConfessionCard(
                        confessionId = post.id,
                        username = post.username,
                        avatar = post.avatar,
                        category = post.category,
                        mood = post.mood,
                        text = post.text,
                        likes = post.likes,
                        userId = post.userId,
                        onCommentClick = { onCommentClick(post.id) },
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }
}
