package com.example.soulspeak

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class CommentData(
    val id: String = "",
    val userId: String = "",
    val username: String = "Anonymous",
    val text: String = "",
    val timestamp: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    confessionId: String,
    currentUserId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var commentText by remember { mutableStateOf("") }
    val comments = remember { mutableStateListOf<CommentData>() }
    val context = LocalContext.current

    // Listen for comments
    DisposableEffect(confessionId) {
        val registration = db.collection("confessions")
            .document(confessionId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    comments.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(CommentData::class.java)?.let { 
                            comments.add(it.copy(id = doc.id)) 
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
                title = { Text("Comments", color = SoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavDark)
            )
        },
        bottomBar = {
            Surface(
                color = NavDark,
                tonalElevation = 8.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = SoftWhite.copy(alpha = 0.3f)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = SoftWhite.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                val newComment = hashMapOf(
                                    "userId" to currentUserId,
                                    "username" to "Anonymous",
                                    "text" to commentText,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("confessions")
                                    .document(confessionId)
                                    .collection("comments")
                                    .add(newComment)
                                    .addOnSuccessListener {
                                        commentText = ""
                                        // Update comment count on post
                                        db.collection("confessions").document(confessionId)
                                            .update("comments", com.google.firebase.firestore.FieldValue.increment(1))
                                    }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = SoftWhite)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (comments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No comments yet.", color = SoftWhite.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(comment)
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: CommentData) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = PrimaryPurple.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("👤", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = comment.username,
                color = PrimaryPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topEnd = 12.dp)
            ) {
                Text(
                    text = comment.text,
                    color = SoftWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}
