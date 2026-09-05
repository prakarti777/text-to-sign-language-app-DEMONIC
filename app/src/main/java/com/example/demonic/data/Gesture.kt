package com.example.demonic.data

data class Gesture(
    val id: String,
    val videoPath: String?,
    val aliases: List<String>,
    val rule: GestureRule? = null,
    val type: String? = "single"
)

sealed interface GestureRule {
    data class Compound(val components: List<String>) : GestureRule
    data class Initialized(val letter: String, val base: String) : GestureRule
    data class Fingerspell(val word: String) : GestureRule
}
