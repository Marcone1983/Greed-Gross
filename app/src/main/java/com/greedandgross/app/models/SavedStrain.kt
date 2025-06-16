package com.greedandgross.app.models

data class SavedStrain(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val parentStrain1: String = "",
    val parentStrain2: String = "",
    val characteristics: String = "",
    val thcContent: String = "",
    val cbdContent: String = "",
    val phenotype: String = "", // Indica/Sativa/Hybrid
    val floweringTime: String = "",
    val yield: String = "",
    val effects: String = "",
    val terpenes: String = "",
    val medicalUse: String = "", // Malattie e condizioni mediche
    val createdBy: String = "", // userId
    val createdAt: Long = 0,
    val isFavorite: Boolean = false
) {
    // Costruttore vuoto richiesto da Firebase
    constructor() : this("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", 0, false)
}