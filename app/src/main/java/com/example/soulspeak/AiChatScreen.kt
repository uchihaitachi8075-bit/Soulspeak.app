package com.example.soulspeak

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(onBack: () -> Unit) {
    // Sahi API Key jo aapke screenshot mein hai:
    val apiKey = "AQ.Ab8RN6JdMxfGg3vePHbZVxCkGwLwjvOnkclBr6jc7ZftpfBpQ"
    val model = remember { GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey) }
    val chat = remember { model.startChat() }
    
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AiMessage>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Guardian", color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Anonymous AI Companion", color = PrimaryPurple, fontSize = 12.sp)
                    }
                },
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
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Talk to AI...", color = SoftWhite.copy(alpha = 0.3f)) },
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
                            if (messageText.isNotBlank()) {
                                val userMsg = messageText
                                messages.add(AiMessage(userMsg, true))
                                messageText = ""
                                
                                scope.launch {
                                    try {
                                        val response = chat.sendMessage(userMsg)
                                        response.text?.let { 
                                            messages.add(AiMessage(it, false))
                                        }
                                    } catch (e: Exception) {
                                        val errorMsg = e.message ?: "Unknown error"
                                        messages.add(AiMessage("Error: $errorMsg. Please check your internet connection.", false))
                                    }
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AI Guardian is here to listen. You are safe and anonymous.",
                        color = SoftWhite.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(messages) { message ->
                AiMessageBubble(message)
            }
        }
    }
}

@Composable
fun AiMessageBubble(message: AiMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) PrimaryPurple else CardDark,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message.text,
                color = SoftWhite,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
    }
}
