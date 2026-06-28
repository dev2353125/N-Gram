package com.ngram;

import java.util.HashMap;
import java.util.Map;

public class TextCleaner {

    // Cleans the raw input text and returns the cleaned string
    // Uses a new StringBuilder loop instead of the previous regex build
    public static String clean(String text, boolean lowercase, boolean removeSpecialChars) {

        if (lowercase) {
            text = text.toLowerCase();
        }

        if (removeSpecialChars) {
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ';
                if (!lowercase) keep = keep || (c >= 'A' && c <= 'Z');
                if (keep) sb.append(c);
            }
            return sb.toString();
        }

        return text;
    }

    // Counts N-Gram frequencies in a single pass directly into a HashMap
    public static Map<String, Integer> countNGrams(String text, int minN, int maxN) {

        int len = text.length();
        Map<String, Integer> freqMap = new HashMap<>();

        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i <= len - n; i++) {
                String ngram = text.substring(i, i + n);
                freqMap.merge(ngram, 1, Integer::sum);
            }
        }

        return freqMap;
    }
}
