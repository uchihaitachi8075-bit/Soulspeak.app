package com.example.soulspeak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun CreateConfessionPreview() {
    SoulspeakTheme {
        CreateConfessionScreen(onDismiss = {}, onPost = { _, _, _ -> })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConfessionScreen(
    onDismiss: () -> Unit,
    onPost: (String, String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Love") }
    var selectedMood by remember { mutableStateOf("Happy") }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = { Text("New Confession", color = SoftWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SoftWhite)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onPost(text, selectedCategory, selectedMood)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What's on your mind? Stay anonymous...", color = SoftWhite.copy(alpha = 0.3f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = SoftWhite.copy(alpha = 0.1f),
                    cursorColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Category", color = SoftWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            val categories = listOf("Love", "Career", "Family", "Health", "Dreams", "Funny")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.take(3).forEach { category ->
                    CategoryChip(category, selectedCategory == category) { selectedCategory = category }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.drop(3).forEach { category ->
                    CategoryChip(category, selectedCategory == category) { selectedCategory = category }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("How are you feeling?", color = SoftWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            val moods = listOf("Happy", "Sad", "Angry", "Anxious", "Excited", "Lonely")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.take(3).forEach { mood ->
                    MoodChip(mood, selectedMood == mood) { selectedMood = mood }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.drop(3).forEach { mood ->
                    MoodChip(mood, selectedMood == mood) { selectedMood = mood }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) PrimaryPurple else CardDark,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SoftWhite.copy(alpha = 0.1f)),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = name, color = if (isSelected) SoftWhite else SoftWhite.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun MoodChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) SoftPink else CardDark,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SoftWhite.copy(alpha = 0.1f)),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = name, color = if (isSelected) SoftWhite else SoftWhite.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}
