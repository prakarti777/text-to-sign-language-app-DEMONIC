package com.example.demonic.domain

import com.example.demonic.data.Gesture
import com.example.demonic.data.GestureRepository

class PhraseMatcher(private val repository: GestureRepository) {

    private val grammarEngine = IslGrammarEngine()
    private val gestureResolver = GestureResolver(repository)

    fun matchPhrases(inputText: String): TranslationResult {
        val knownPhrases = repository.getGestures()
            .flatMap { it.aliases + it.id }
            .filter { it.contains(" ") || it.contains("-") || it.contains("_") }
            .map { it.lowercase().replace("_", " ").replace("-", " ") }
            .distinct()
            .sortedByDescending { it.length }

        val glosses = grammarEngine.transform(inputText, knownPhrases)
        return gestureResolver.resolveGlosses(glosses)
    }

    fun getGrammarEngine(): IslGrammarEngine = grammarEngine
    fun getGestureResolver(): GestureResolver = gestureResolver
}
