package com.ngram;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class NGramTest {

    // Tests that tokenization correctly cleans and splits text into words
    @Test
    public void testTokenize() {
    	List<String> tokens = TextCleaner.tokenize("The Quick Brown Fox", true, true);

        // Checks that we got 4 tokens
        assertEquals(4, tokens.size());

        // Checks that the first token is lowercase
        assertEquals("the", tokens.get(0));
    }

    // Tests that bigrams are generated correctly from a simple sentence
    @Test
    public void testBigrams() {
    	List<String> tokens = TextCleaner.tokenize("Full text search", true, true);

        // Generates only bigrams N = 2
        List<String> ngrams = TextCleaner.generateNGrams(tokens, 2, 2);

        // Checks that we got exactly 2 bigrams
        assertEquals(2, ngrams.size());

        // Checks the actual bigram values match the scope doc test case
        assertEquals("full text", ngrams.get(0));
        assertEquals("text search", ngrams.get(1));
    }

    // Tests that unigram frequency counts are correct
    @Test
    public void testFrequencyCounts() {
    	List<String> tokens = TextCleaner.tokenize("hello hello world", true, true);

        // Generates only unigrams (N=1)
        List<String> ngrams = TextCleaner.generateNGrams(tokens, 1, 1);

        // Counts frequencies
        Map<String, Integer> freqMap = Exporter.countFrequencies(ngrams);

        // Checks that hello appears twice and world appears once
        assertEquals(2, freqMap.get("hello"));
        assertEquals(1, freqMap.get("world"));
    }

    // Tests that empty input doesn't crash the program
    @Test
    public void testEmptyInput() {
    	List<String> tokens = TextCleaner.tokenize("", true, true);

        // Checks that we get an empty list back, not an error
        assertTrue(tokens.isEmpty());
    }
}