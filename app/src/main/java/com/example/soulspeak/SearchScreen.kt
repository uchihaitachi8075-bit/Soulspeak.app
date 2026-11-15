package com.example.soulspeak

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

data class UserSearchResult(
    val id: String = "",
    val username: String = "",
    val avatar: String = "👤",
    val publicId: String = ""
)

@Composable
fun SearchScreen(onUserClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val searchResults = remember { mutableStateListOf<UserSearchResult>() }
    val suggestedSouls = remember { mutableStateListOf<UserSearchResult>() }
    var isSearching by remember { mutableStateOf(false) }
    var myPublicId by remember { mutableStateOf("Syncing...") }
    val context = LocalContext.current
    val theme = LocalSoulspeakColors.current

    // Fetch My ID and Suggested Souls
    LaunchedEffect(currentUserId) {
        db.collection("users").document(currentUserId).get().addOnSuccessListener { 
            myPublicId = it.getString("publicId") ?: "Open Profile to Sync"
        }
        
        // Fetch suggested souls
        db.collection("users")
            .whereNotEqualTo("publicId", null)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                suggestedSouls.clear()
                for (doc in snapshot.documents) {
                    if (doc.id != currentUserId) {
                        suggestedSouls.add(UserSearchResult(
                            id = doc.id,
                            username = doc.getString("username") ?: "Soul",
                            avatar = doc.getString("avatar") ?: "👤",
                            publicId = doc.getString("publicId") ?: ""
                        ))
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Discover Souls", style = MaterialTheme.typography.headlineMedium, color = theme.text, fontWeight = FontWeight.Bold)
                Text("Your ID: $myPublicId", color = theme.primary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    val newId = "SOUL-${currentUserId.take(4).uppercase()}"
                    db.collection("users").document(currentUserId).update("publicId", newId).addOnSuccessListener {
                        myPublicId = newId
                        Toast.makeText(context, "ID Synced!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sync ID", color = theme.text, fontSize = 10.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                if (it.length >= 2) {
                    isSearching = true
                    val queryText = it.trim().uppercase()
                    val searchId = if (queryText.startsWith("SOUL-")) queryText else "SOUL-$queryText"
                    
                    db.collection("users")
                        .orderBy("publicId")
                        .startAt(searchId)
                        .endAt(searchId + "\uf8ff")
                        .limit(15)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            searchResults.clear()
                            for (doc in snapshot.documents) {
                                searchResults.add(UserSearchResult(
                                    id = doc.id,
                                    username = doc.getString("username") ?: "Anonymous Soul",
                                    avatar = doc.getString("avatar") ?: "👤",
                                    publicId = doc.getString("publicId") ?: ""
                                ))
                            }
                            isSearching = false
                        }
                        .addOnFailureListener { isSearching = false }
                } else {
                    searchResults.clear()
                }
            },
            placeholder = { Text("Search Soul ID (e.g. PQRM)", color = theme.text.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.primary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.text, unfocusedTextColor = theme.text,
                focusedBorderColor = theme.primary, unfocusedBorderColor = theme.text.copy(alpha = 0.1f),
                focusedContainerColor = theme.text.copy(alpha = 0.03f), unfocusedContainerColor = theme.text.copy(alpha = 0.03f)
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
        } else {
            val listToShow = if (searchQuery.isEmpty()) suggestedSouls else searchResults
            
            if (searchQuery.isEmpty() && suggestedSouls.isNotEmpty()) {
                Text("Suggested Souls", color = theme.text.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No soul found with that ID.", color = theme.text.copy(alpha = 0.4f))
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listToShow) { user ->
                    SearchUserItem(user) { onUserClick(user.id) }
                }
            }
        }
    }
}

@Composable
fun SearchUserItem(user: UserSearchResult, onClick: () -> Unit) {
    val theme = LocalSoulspeakColors.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        color = theme.text.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.text.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp).clip(CircleShape), color = theme.primary.copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) { Text(user.avatar, fontSize = 20.sp) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(user.username, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(user.publicId, color = theme.primary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("View Profile", color = theme.text.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
