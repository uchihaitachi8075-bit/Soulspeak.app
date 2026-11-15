package com.example.soulspeak

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    targetUserId: String,
    currentUserId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var username by remember { mutableStateOf("Loading...") }
    var gender by remember { mutableStateOf("Not specified") }
    var publicId by remember { mutableStateOf("Soul-XXXX") }
    var postCount by remember { mutableIntStateOf(0) }
    var isFriend by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val theme = LocalSoulspeakColors.current

    LaunchedEffect(targetUserId) {
        db.collection("users").document(targetUserId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                username = doc.getString("username") ?: "Anonymous Soul"
                gender = doc.getString("gender") ?: "Not specified"
                publicId = doc.getString("publicId") ?: "SOUL-${targetUserId.take(4).uppercase()}"
            }
        }
        db.collection("confessions").whereEqualTo("userId", targetUserId).get().addOnSuccessListener { 
            postCount = it.size()
        }
        db.collection("users").document(currentUserId).collection("friends").document(targetUserId).get().addOnSuccessListener { 
            isFriend = it.exists()
        }
    }

    Scaffold(
        containerColor = theme.background,
        topBar = {
            TopAppBar(
                title = { Text("Soul Profile", color = theme.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.nav)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp).clip(CircleShape),
                color = theme.text.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Brush.linearGradient(listOf(theme.primary, theme.secondary)))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "👤", fontSize = 60.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = username, color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = publicId, color = theme.primary, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                color = theme.text.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.text.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PublicProfileStat("Gender", gender)
                    PublicProfileStat("Posts", postCount.toString())
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!isFriend) {
                        db.collection("users").document(currentUserId).collection("friends").document(targetUserId)
                            .set(mapOf("timestamp" to System.currentTimeMillis()))
                            .addOnSuccessListener {
                                isFriend = true
                                Toast.makeText(context, "Added to Soul Friends!", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFriend) Color.Gray.copy(alpha = 0.3f) else theme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !isFriend
            ) {
                Icon(if (isFriend) Icons.Default.VerifiedUser else Icons.Default.PersonAdd, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFriend) "Soul Friend" else "Add as Friend", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Posts are hidden for privacy. Only Soul Friends can see more.",
                color = theme.text.copy(alpha = 0.4f),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun PublicProfileStat(label: String, value: String) {
    val theme = LocalSoulspeakColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = theme.text.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}
