package com.ngram;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Exporter {

    // 64 KB write buffer  default is 8 KB which causes many small flushes on large maps
    private static final int BUFFER_SIZE = 64 * 1024;

    public static void exportCSV(Map<String, Integer> freqMap, String filePath) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath), BUFFER_SIZE)) {
            writer.write("ngram,frequency\n");
            freqMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    try {
                        writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

        System.out.println("CSV exported to: " + filePath);
    }

    public static void exportJSON(Map<String, Integer> freqMap, String filePath) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath), BUFFER_SIZE)) {
            writer.write("{\n");

            List<Map.Entry<String, Integer>> entries = freqMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

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
