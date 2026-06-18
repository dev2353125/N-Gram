package com.ngram;

import java.util.ArrayList;
import java.util.List;

public class TextCleaner {

    // Cleans the raw input text and returns a list of individual words (tokens)
	public static List<String> tokenize(String text, boolean lowercase, boolean removeSpecialChars) {

        // Only convert to lowercase if the user chose that option
        if (lowercase) {
            text = text.toLowerCase();
        }

        // Only strip special characters if the user chose that option
        if (removeSpecialChars) {
            // If lowercase was applied, only a-z stays; otherwise keep A-Z too
            if (lowercase) {
                text = text.replaceAll("[^a-z0-9 ]", "");
            } else {
                text = text.replaceAll("[^a-zA-Z0-9 ]", "");
            }
        }

        // Split the text into individual words wherever there's a space
        String[] words = text.trim().split("\\s+");

        // Create an empty list to hold cleaned words
        List<String> tokens = new ArrayList<>();

        // Loop through each word and add it to the list
        for (String word : words) {
            // Only add the word if it's not empty
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        // Return the finished list of clean tokens
        return tokens;
    }

    // Takes a list of tokens and generates N-Grams of sizes between minN and maxN
    public static List<String> generateNGrams(List<String> tokens, int minN, int maxN) {

        // Create an empty list to store all generated N-Grams
        List<String> ngrams = new ArrayList<>();

        // Loop through each N size we want (e.g. 1, 2, 3)
        for (int n = minN; n <= maxN; n++) {

            // Loop through the tokens and only stopping early enough to grab n words at a time
            for (int i = 0; i <= tokens.size() - n; i++) {

                // Create a mini list of n words starting at position i
                List<String> group = tokens.subList(i, i + n);

                // Join those words with a space to form the N-Gram string
                String ngram = String.join(" ", group);

                // Add the finished N-Gram to our list
                ngrams.add(ngram);
            }
        }

        // Return the full list of N-Grams
        return ngrams;
    }
}