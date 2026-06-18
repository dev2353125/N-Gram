package com.ngram;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Exporter {

    // Counts how many times each N-Gram appears and returns a map of ngram to their count
    public static Map<String, Integer> countFrequencies(List<String> ngrams) {

        // LinkedHashMap keeps the entries in insertion order
        Map<String, Integer> freqMap = new LinkedHashMap<>();

        // Loop through every N-Gram in the list
        for (String ngram : ngrams) {

            // If we've seen this N-Gram before then add 1 to its count otherwise start at 1
            freqMap.put(ngram, freqMap.getOrDefault(ngram, 0) + 1);
        }

        // Return the finished frequency map
        return freqMap;
    }

    // Exports the frequency map to a CSV file at the given file path
    public static void exportCSV(Map<String, Integer> freqMap, String filePath) throws IOException {

        // Open a FileWriter to create/overwrite the file at the given path
        FileWriter writer = new FileWriter(filePath);

        // Write the header row
        writer.write("ngram,frequency\n");

        // Loop through every entry in the map and write it as a CSV row
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
            writer.write(entry.getKey() + "," + entry.getValue() + "\n");
        }

        // Close the file when done to save everything
        writer.close();

        System.out.println("CSV exported to: " + filePath);
    }

    // Exports the frequency map to a JSON file at the given file path
    public static void exportJSON(Map<String, Integer> freqMap, String filePath) throws IOException {

        // Open a FileWriter to create/overwrite the file at the given path
        FileWriter writer = new FileWriter(filePath);

        // Write the opening bracket of the JSON object
        writer.write("{\n");

        // Convert the map entries to a list so we can detect the last one
        List<Map.Entry<String, Integer>> entries = freqMap.entrySet().stream().toList();

        // Loop through each entry and write it as a JSON key-value pair
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);

            // Write the ngram as the key and frequency as the value
            String line = "  \"" + entry.getKey() + "\": " + entry.getValue();

            // Add a comma after every line except the last one (JSON rule)
            if (i < entries.size() - 1) line += ",";

            writer.write(line + "\n");
        }

        // Write the closing bracket of the JSON object
        writer.write("}\n");

        // Close the file when done
        writer.close();

        System.out.println("JSON exported to: " + filePath);
    }
}