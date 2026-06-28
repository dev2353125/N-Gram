package com.ngram;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("    N-Gram Generator    ");
            System.out.println("1 - Type text manually");
            System.out.println("2 - Load a .txt file");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            String inputText = "";

            if (choice.equals("1")) {
                System.out.print("Enter your text: ");
                inputText = scanner.nextLine();

            } else if (choice.equals("2")) {
                System.out.print("Enter full path to your .txt file: ");
                String filePath = scanner.nextLine().trim().replace("\"", "");

                // Memory-mapped file read: OS maps the file into virtual memory
                // so the JVM never copies the full 100MB into the heap at once.
                // Much faster than Files.readString for large inputs.
                try (FileChannel channel = FileChannel.open(
                        Paths.get(filePath), StandardOpenOption.READ)) {

                    long size = channel.size();
                    MappedByteBuffer buffer = channel.map(
                        FileChannel.MapMode.READ_ONLY, 0, size);

                    byte[] bytes = new byte[(int) size];
                    buffer.get(bytes);
                    inputText = new String(bytes, StandardCharsets.UTF_8);
                }

            } else {
                System.out.println("Invalid choice, exiting.");
                return;
            }

            System.out.print("Convert to lowercase? (y/n): ");
            boolean lowercase = scanner.nextLine().trim().equalsIgnoreCase("y");

            System.out.print("Remove special characters? (y/n): ");
            boolean removeSpecialChars = scanner.nextLine().trim().equalsIgnoreCase("y");

            System.out.print("Enter minimum N: ");
            int minN = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter maximum N: ");
            int maxN = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter output folder path (type full address): ");
            String outputFolder = scanner.nextLine().trim();

            // --- Start timer AFTER reading the file, to measure processing only ---
            long startTime = System.currentTimeMillis();

            // Clean directly into bytes (no intermediate String allocation)
            byte[] cleanedBytes = TextCleaner.cleanToBytes(inputText, lowercase, removeSpecialChars);

            // Use parallel counting for large inputs (>1MB), sequential otherwise
            Map<String, Integer> freqMap = TextCleaner.countNGramsParallel(
                cleanedBytes, minN, maxN);

            long endTime = System.currentTimeMillis();

            // Use File.separator for cross-platform path building
            String sep = java.io.File.separator;
            Exporter.exportCSV(freqMap, outputFolder + sep + "ngrams.csv");
            Exporter.exportJSON(freqMap, outputFolder + sep + "ngrams.json");

            // Compute total N-grams from cleaned length (no need to store them)
            long totalNgrams = 0;
            long len = cleanedBytes.length;
            for (int n = minN; n <= maxN; n++) {
                if (len >= n) totalNgrams += (len - n + 1);
            }

            System.out.println("\n=== Results ===");
            System.out.println("Input size:        " + inputText.length() + " characters");
            System.out.println("Characters found:  " + cleanedBytes.length);
            System.out.println("N-Grams generated: " + totalNgrams);
            System.out.println("Unique N-Grams:    " + freqMap.size());
            System.out.println("Processing time:   " + (endTime - startTime) + " ms");
            System.out.println("Threads used:      " +
                Runtime.getRuntime().availableProcessors());
        }
    }
}
