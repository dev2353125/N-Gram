package com.ngram;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Exporter {

    // Exports the frequency map to a CSV file using BufferedWriter
    public static void exportCSV(Map<String, Integer> freqMap, String filePath) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ngram,frequency\n");
            for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
        }

        System.out.println("CSV exported to: " + filePath);
    }

    // Exports the frequency map to a JSON file using BufferedWriter
    public static void exportJSON(Map<String, Integer> freqMap, String filePath) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("{\n");

            List<Map.Entry<String, Integer>> entries = freqMap.entrySet().stream().toList();

            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, Integer> entry = entries.get(i);
                String line = "  \"" + entry.getKey() + "\": " + entry.getValue();
                if (i < entries.size() - 1) line += ",";
                writer.write(line + "\n");
            }

            writer.write("}\n");
        }

        System.out.println("JSON exported to: " + filePath);
    }
}
