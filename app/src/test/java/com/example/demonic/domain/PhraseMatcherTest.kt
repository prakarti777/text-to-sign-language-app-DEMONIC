package com.example.demonic.domain

import com.example.demonic.data.Gesture
import com.example.demonic.data.GestureRepository
import com.example.demonic.data.GestureRule
import org.junit.Assert.assertEquals
import org.junit.Test

class PhraseMatcherTest {

    private val mockRepository = object : GestureRepository {
        override fun getGestures(): List<Gesture> = listOf(
            Gesture("no", "gestures/no.mp4", listOf("no", "nope")),
            Gesture("i_am_fine", "gestures/i_am_fine.mp4", listOf("I am fine", "I'm fine", "im fine", "I am good", "I'm good")),
            Gesture("thank_you", "gestures/thank_you.mp4", listOf("thank you", "thanks", "thankyou")),
            Gesture("how_are_you", "gestures/how_are_you.mp4", listOf("how are you", "how are you doing")),
            Gesture("good_morning", "gestures/good_morning.mp4", listOf("good morning")),
            Gesture("good_evening", "gestures/good_evening.mp4", listOf("good evening")),
            Gesture("thank_you_very_much", "gestures/thank_you_very_much.mp4", listOf("thank you very much", "many thanks")),
            Gesture("i_don_t_understand", "gestures/i_don_t_understand.mp4", listOf("I don't understand", "I do not understand", "don't understand", "do not understand")),
            Gesture("no_fear", "gestures/no_fear.mp4", listOf("no fear", "don't fear", "do not fear")),
            Gesture("sorry", "gestures/sorry.mp4", listOf("sorry", "apologize", "apologies")),
            Gesture("grandmother", "gestures/grandmother.mp4", listOf("grandmother", "grandma")),
            Gesture("who", "gestures/who.mp4", listOf("who", "whom")),
            Gesture("help_me_please", "gestures/help_me_please_anime.mp4", listOf("help me please", "help me", "help please", "please help")),
            Gesture("what_happen", "gestures/what_happen_anime.mp4", listOf("what happen", "what happened", "what is happening", "what's happening")),

            Gesture("banana", "gestures/banana.mp4", listOf("banana")),
            Gesture("female", "gestures/female.mp4", listOf("female")),
            Gesture("marriage", "gestures/marriage.mp4", listOf("marriage")),
            Gesture("drink", "gestures/drink.mp4", listOf("drink")),
            Gesture("friend", "gestures/friend.mp4", listOf("friend")),

            Gesture("letter_c", "gestures/letter_c.mp4", listOf()),
            Gesture("letter_j", "gestures/letter_j.mp4", listOf()),
            Gesture("letter_o", "gestures/letter_o.mp4", listOf()),
            Gesture("letter_b", "gestures/letter_b.mp4", listOf()),

            Gesture("wife", null, listOf("wife"), GestureRule.Compound(listOf("female", "marriage"))),
            Gesture("coffee", null, listOf("coffee"), GestureRule.Initialized("c", "drink")),
            Gesture("job", null, listOf("job"), GestureRule.Fingerspell("job"))
        )
    }

    private val phraseMatcher = PhraseMatcher(mockRepository)

    @Test
    fun testBaseGestures() {
        val result = phraseMatcher.matchPhrases("No.")
        assertEquals(1, result.resolved.size)
        assertEquals("no", result.resolved[0].id)
    }

    @Test
    fun testPhraseMatching() {
        val result = phraseMatcher.matchPhrases("I am fine.")
        assertEquals(1, result.resolved.size)
        assertEquals("i_am_fine", result.resolved[0].id)
    }

    @Test
    fun testCompoundRule() {
        val result = phraseMatcher.matchPhrases("Wife.")
        assertEquals(2, result.resolved.size)
        assertEquals("female", result.resolved[0].id)
        assertEquals("marriage", result.resolved[1].id)
    }

    @Test
    fun testInitializationRule() {
        val result = phraseMatcher.matchPhrases("Coffee.")
        assertEquals(2, result.resolved.size)
        assertEquals("letter_c", result.resolved[0].id)
        assertEquals("drink", result.resolved[1].id)
    }

    @Test
    fun testFingerspellRule() {
        val result = phraseMatcher.matchPhrases("Job.")
        assertEquals(3, result.resolved.size)
        assertEquals("letter_j", result.resolved[0].id)
        assertEquals("letter_o", result.resolved[1].id)
        assertEquals("letter_b", result.resolved[2].id)
    }

    @Test
    fun testGrammarEngineSvoToSov() {
        val engine = phraseMatcher.getGrammarEngine()
        val result = engine.transform("I eat banana")
        assertEquals(3, result.size)
        assertEquals("i", result[0])
        assertEquals("banana", result[1])
        assertEquals("eat", result[2])
    }

    @Test
    fun testGrammarEngineNegationAtEnd() {
        val engine = phraseMatcher.getGrammarEngine()
        val result = engine.transform("I did not eat banana")
        assertEquals(4, result.size)
        assertEquals("i", result[0])
        assertEquals("banana", result[1])
        assertEquals("eat", result[2])
        assertEquals("no", result[3])
    }

    @Test
    fun testGrammarEngineQuestionAtEnd() {
        val engine = phraseMatcher.getGrammarEngine()
        val result = engine.transform("Who is your friend")
        assertEquals("who", result[result.size - 1])
    }

    @Test
    fun testPhrasePriorityHowAreYou() {
        val result = phraseMatcher.matchPhrases("How are you?")
        assertEquals(1, result.resolved.size)
        assertEquals("how_are_you", result.resolved[0].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testPhrasePriorityGoodMorning() {
        val result = phraseMatcher.matchPhrases("Good morning")
        assertEquals(1, result.resolved.size)
        assertEquals("good_morning", result.resolved[0].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testPhrasePriorityGoodEvening() {
        val result = phraseMatcher.matchPhrases("Good evening")
        assertEquals(1, result.resolved.size)
        assertEquals("good_evening", result.resolved[0].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testPhrasePriorityThankYouVeryMuch() {
        val result = phraseMatcher.matchPhrases("Thank you very much")
        assertEquals(1, result.resolved.size)
        assertEquals("thank_you_very_much", result.resolved[0].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testPhrasePriorityDontUnderstand() {
        val result = phraseMatcher.matchPhrases("I don't understand")
        assertEquals(1, result.resolved.size)
        assertEquals("i_don_t_understand", result.resolved[0].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testSequentialPhrases() {
        val result = phraseMatcher.matchPhrases("Thank you very much, good evening")
        assertEquals(2, result.resolved.size)
        assertEquals("thank_you_very_much", result.resolved[0].id)
        assertEquals("good_evening", result.resolved[1].id)
        assertEquals(0, result.unresolved.size)
    }

    @Test
    fun testUnresolvedHandling() {
        val result = phraseMatcher.matchPhrases("I am fine and extremely happy")
        assertEquals(1, result.resolved.size)
        assertEquals("i_am_fine", result.resolved[0].id)

        assertEquals(3, result.unresolved.size)
        assertEquals("and", result.unresolved[0])
        assertEquals("extremely", result.unresolved[1])
        assertEquals("happy", result.unresolved[2])
    }

    // New tests to verify aliases added in Phase 2 Expansion:
    @Test
    fun testAliasesSorryApologies() {
        val result = phraseMatcher.matchPhrases("Apologies")
        assertEquals(1, result.resolved.size)
        assertEquals("sorry", result.resolved[0].id)
    }

    @Test
    fun testAliasesGrandmotherGrandma() {
        val result = phraseMatcher.matchPhrases("Grandma")
        assertEquals(1, result.resolved.size)
        assertEquals("grandmother", result.resolved[0].id)
    }

    @Test
    fun testAliasesManyThanks() {
        val result = phraseMatcher.matchPhrases("Many thanks")
        assertEquals(1, result.resolved.size)
        assertEquals("thank_you_very_much", result.resolved[0].id)
    }

    @Test
    fun testAliasesDoNotUnderstand() {
        val result = phraseMatcher.matchPhrases("I do not understand")
        assertEquals(1, result.resolved.size)
        assertEquals("i_don_t_understand", result.resolved[0].id)
    }

    @Test
    fun testAliasesDontFear() {
        val result = phraseMatcher.matchPhrases("Don't fear")
        assertEquals(1, result.resolved.size)
        assertEquals("no_fear", result.resolved[0].id)
    }

    @Test
    fun testAnimationHelpMePlease() {
        val result = phraseMatcher.matchPhrases("help me please")
        assertEquals(1, result.resolved.size)
        assertEquals("help_me_please", result.resolved[0].id)
        assertEquals("gestures/help_me_please_anime.mp4", result.resolved[0].videoPath)
    }

    @Test
    fun testAnimationWhatHappen() {
        val result = phraseMatcher.matchPhrases("what happened")
        assertEquals(1, result.resolved.size)
        assertEquals("what_happen", result.resolved[0].id)
        assertEquals("gestures/what_happen_anime.mp4", result.resolved[0].videoPath)
    }

    @Test
    fun testSequentialAnimations() {
        val result = phraseMatcher.matchPhrases("help me please what happened")
        assertEquals(2, result.resolved.size)
        assertEquals("help_me_please", result.resolved[0].id)
        assertEquals("what_happen", result.resolved[1].id)
    }
}
