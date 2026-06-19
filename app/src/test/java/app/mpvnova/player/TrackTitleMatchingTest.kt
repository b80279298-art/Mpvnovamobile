package app.mpvnova.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure track-title matching helpers in
 * [TrackTitleMatching.kt]. These guard the heuristic that lets mpvNova
 * remember "I picked the Signs & Songs sub last episode" and re-apply
 * it on the next file where the track IDs may differ between releases.
 */
class TrackTitleMatchingTest {

    // ---------- normalizeTrackTitleTokens ----------

    @Test fun normalize_emptyTitleReturnsEmptySet() {
        assertEquals(emptySet<String>(), normalizeTrackTitleTokens(""))
    }

    @Test fun normalize_lowercasesAndStripsPunctuation() {
        val tokens = normalizeTrackTitleTokens("Signs & Songs (Honorifics)")
        assertEquals(setOf("signs", "songs", "honorifics"), tokens)
    }

    @Test fun normalize_dropsStopwords() {
        // "the", "of", "for" are stopwords; "english" is kept.
        val tokens = normalizeTrackTitleTokens("The English Translation of the Episode")
        assertEquals(setOf("english", "translation", "episode"), tokens)
    }

    @Test fun normalize_dropsShortTokens() {
        // Tokens of length ≤ 2 are skipped — "is", "an" both drop.
        val tokens = normalizeTrackTitleTokens("Dialogue is an Option")
        // "is", "an", "or" stopword-like patterns gone; "dialogue", "option" remain.
        assertTrue("dialogue" in tokens)
        assertTrue("option" in tokens)
        assertFalse("is" in tokens)
        assertFalse("an" in tokens)
    }

    @Test fun normalize_collapsesDuplicates() {
        // Set semantics — repeated tokens collapse.
        val tokens = normalizeTrackTitleTokens("Signs Signs Signs")
        assertEquals(setOf("signs"), tokens)
    }

    // ---------- titleSimilarityScore ----------

    @Test fun similarity_emptyInputsAreZero() {
        assertEquals(0.0, titleSimilarityScore("", "anything"), 0.0001)
        assertEquals(0.0, titleSimilarityScore("anything", ""), 0.0001)
        assertEquals(0.0, titleSimilarityScore("", ""), 0.0001)
    }

    @Test fun similarity_identicalTitlesIsOne() {
        assertEquals(1.0, titleSimilarityScore("Signs & Songs", "Signs & Songs"), 0.0001)
    }

    @Test fun similarity_isAsymmetricByDesign() {
        // Saved title's tokens all present in the candidate → 1.0, even
        // though the candidate has extra cruft.
        val score = titleSimilarityScore(
            saved = "Signs & Songs",
            candidate = "[GroupA] Signs and Songs (English)"
        )
        assertEquals(1.0, score, 0.0001)
    }

    @Test fun similarity_partialOverlap() {
        // 1 of 2 tokens in saved appears in candidate → 0.5
        val score = titleSimilarityScore(
            saved = "Forced Subtitles",
            candidate = "Forced Captions"
        )
        assertEquals(0.5, score, 0.0001)
    }

    @Test fun similarity_completelyDifferentIsZero() {
        val score = titleSimilarityScore("Commentary", "Dialogue")
        assertEquals(0.0, score, 0.0001)
    }

    // ---------- languagePrefixMatches ----------

    @Test fun langMatch_emptyInputsDoNotMatch() {
        assertFalse(languagePrefixMatches("", "eng"))
        assertFalse(languagePrefixMatches("eng", ""))
        assertFalse(languagePrefixMatches("", ""))
    }

    @Test fun langMatch_foldsEquivalentEnglishCodes() {
        assertTrue(languagePrefixMatches("en", "eng"))
        assertTrue(languagePrefixMatches("en", "english"))
        assertTrue(languagePrefixMatches("English", "eng"))
        assertTrue(languagePrefixMatches("EN", "en"))
    }

    @Test fun langMatch_doesNotCollideDifferentLanguages() {
        assertFalse(languagePrefixMatches("en", "es"))
        assertFalse(languagePrefixMatches("ja", "jp"))   // "ja" vs "jp" really do differ
        assertFalse(languagePrefixMatches("ja", "zh"))
    }

    @Test fun langMatch_isCaseInsensitive() {
        assertTrue(languagePrefixMatches("FR", "fra"))
        assertTrue(languagePrefixMatches("De", "DEU"))
    }
}
