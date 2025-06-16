package com.greedandgross.app.models

data class GlobalChatMessage(
    val id: String = "",
    val username: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val isSystem: Boolean = false
) {
    // Costruttore vuoto richiesto da Firebase
    constructor() : this("", "", "", 0, "", false)
}