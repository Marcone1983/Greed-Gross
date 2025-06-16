package com.greedandgross.app.models

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isImage: Boolean = false,
    val imageUrl: String? = null,
    val strainInfo: StrainInfo? = null
)

data class StrainInfo(
    val name: String,
    val parent1: String,
    val parent2: String,
    val thcContent: String,
    val cbdContent: String,
    val phenotype: String,
    val floweringTime: String,
    val yield: String,
    val terpenes: List<String>,
    val effects: List<String>
)