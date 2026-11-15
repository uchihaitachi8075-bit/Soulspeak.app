package com.example.soulspeak

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object NotificationState {
    var count by mutableIntStateOf(0)
    var chatCount by mutableIntStateOf(0)
    
    fun increment() {
        count++
    }
    
    fun clear() {
        count = 0
    }

    fun setChats(newCount: Int) {
        chatCount = newCount
    }
}
