package com.ngram;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class NGramTest {

    // Tests that clean() correctly lowercases and strips special characters
    @Test
    public void testClean() {
        String cleaned = TextCleaner.clean("The Quick Brown Fox", true, true);

        // "the quick brown fox" should be 19 characters including spaces
        assertEquals(19, cleaned.length());

        // Checks that the first character is lowercase 't'
        assertEquals('t', cleaned.charAt(0));
    }

    // Tests that character bigrams are generated and counted correctly
    @Test
    public void testBigrams() {
        String cleaned = TextCleaner.clean("hello", true, true);
        Map<String, Integer> freqMap = TextCleaner.countNGrams(cleaned, 2, 2);

        // "hello" produces 4 unique bigrams: he, el, ll, lo
        assertEquals(4, freqMap.size());
        assertEquals(1, (int) freqMap.get("he"));
        assertEquals(1, (int) freqMap.get("lo"));
    }

    // Tests that character unigram frequency counts are correct
    @Test
    public void testFrequencyCounts() {
        String cleaned = TextCleaner.clean("aab", true, true);
        Map<String, Integer> freqMap = TextCleaner.countNGrams(cleaned, 1, 1);

        // 'a' appears twice and 'b' appears once
        assertEquals(2, (int) freqMap.get("a"));
        assertEquals(1, (int) freqMap.get("b"));
    }

    // Tests that empty input doesn't crash the program (resolved)
    @Test
    public void testEmptyInput() {
        String cleaned = TextCleaner.clean("", true, true);
        assertTrue(cleaned.isEmpty());
    }
}
