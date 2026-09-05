package com.example.demonic.domain

import com.example.demonic.data.Gesture
import com.example.demonic.data.GestureRepository
import com.example.demonic.data.GestureRule

data class TranslationResult(
    val resolved: List<Gesture>,
    val unresolved: List<String>
)

class GestureResolver(private val repository: GestureRepository) {

    fun resolveGlosses(glosses: List<String>): TranslationResult {
        val gestures = repository.getGestures()
        val resolvedList = mutableListOf<Gesture>()
        val unresolvedList = mutableListOf<String>()

        var i = 0
        while (i < glosses.size) {
            val currentWord = glosses[i].trim()

            if (currentWord.isEmpty() || isPunctuation(currentWord)) {
                i += 1
                continue
            }

            var matchedGesture: Gesture? = null
            var matchedLength = 0

            val maxRemainingLength = glosses.size - i
            for (len in maxRemainingLength downTo 1) {
                val subSequence = glosses.subList(i, i + len)
                val joinedSpace = subSequence.joinToString(" ")
                val joinedUnderscore = subSequence.joinToString("_")

                val match = gestures.find { gesture ->
                    gesture.id.equals(joinedSpace, ignoreCase = true) ||
                    gesture.id.equals(joinedUnderscore, ignoreCase = true) ||
                    gesture.aliases.any { alias -> alias.equals(joinedSpace, ignoreCase = true) }
                }

                if (match != null) {
                    matchedGesture = match
                    matchedLength = len
                    break
                }
            }

            if (matchedGesture != null) {
                val resolved = resolveConcept(matchedGesture)
                if (resolved.isNotEmpty()) {
                    resolvedList.addAll(resolved)
                    i += matchedLength
                } else {
                    unresolvedList.add(currentWord)
                    i += 1
                }
            } else {
                unresolvedList.add(currentWord)
                i += 1
            }
        }

        return TranslationResult(resolvedList, unresolvedList)
    }

    private fun isPunctuation(word: String): Boolean {
        return word.matches(Regex("[\\p{Punct}]+"))
    }

    private fun resolveConcept(gesture: Gesture): List<Gesture> {
        val gestures = repository.getGestures()

        if (gesture.videoPath != null) {
            return listOf(gesture)
        }

        val rule = gesture.rule ?: return emptyList()

        return when (rule) {
            is GestureRule.Compound -> {
                rule.components.flatMap { componentId ->
                    val component = gestures.find { it.id.equals(componentId, ignoreCase = true) }
                    if (component != null) resolveConcept(component) else emptyList()
                }
            }
            is GestureRule.Initialized -> {
                val letterId = "letter_${rule.letter.lowercase()}"
                val letterGesture = gestures.find { it.id.equals(letterId, ignoreCase = true) }
                val baseGesture = gestures.find { it.id.equals(rule.base, ignoreCase = true) }

                val result = mutableListOf<Gesture>()
                if (letterGesture != null) result.addAll(resolveConcept(letterGesture))
                if (baseGesture != null) result.addAll(resolveConcept(baseGesture))
                result
            }
            is GestureRule.Fingerspell -> {
                rule.word.lowercase().mapNotNull { char ->
                    val letterId = "letter_$char"
                    val letterGesture = gestures.find { it.id.equals(letterId, ignoreCase = true) }
                    if (letterGesture != null) {
                        val resolved = resolveConcept(letterGesture)
                        if (resolved.isNotEmpty()) resolved[0] else null
                    } else null
                }
            }
        }
    }
}
