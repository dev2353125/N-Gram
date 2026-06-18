package com.ngram;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {

        // Create a scanner to read input from the console
    	try (Scanner scanner = new Scanner(System.in)) {

        // Ask the user how they want to provide text
        System.out.println("    N-Gram Generator    ");
        System.out.println("1 - Type text manually");
        System.out.println("2 - Load a .txt file");
        System.out.print("Choose an option: ");

        // Read the user's choice
        String choice = scanner.nextLine().trim();

        // This will hold the raw input text
        String inputText = "";

        // If the user chooses option 1 then let them type text directly
        if (choice.equals("1")) {
            System.out.print("Enter your text: ");
            inputText = scanner.nextLine();

        // If user chose option 2, ask for a file path and read the file
        } else if (choice.equals("2")) {
            System.out.print("Enter full path to your .txt file: ");
            String filePath = scanner.nextLine().trim();

            // If the user pasted the path with quotation marks then remove them
            filePath = filePath.replace("\"", "");

            //inputText = new String(Files.readAllBytes(Paths.get(filePath))); <- UTF-8 unsupported (old)
            //This supports and reads UTF-8 compatible files
            inputText = Files.readString(Paths.get(filePath), java.nio.charset.StandardCharsets.UTF_8);
        // If they entered something else, exit the program
        } else {
            System.out.println("Invalid choice, exiting.");
            return;
        }

        // Ask if the user wants to convert text to lowercase
        System.out.print("Convert to lowercase? (y/n): ");
        boolean lowercase = scanner.nextLine().trim().equalsIgnoreCase("y");

        // Ask if the user wants to remove special characters
        System.out.print("Remove special characters? (y/n): ");
        boolean removeSpecialChars = scanner.nextLine().trim().equalsIgnoreCase("y");

        // Ask the user for the minimum N value
        System.out.print("Enter minimum N: ");
        int minN = Integer.parseInt(scanner.nextLine().trim());

        // Ask the user for the maximum N value
        System.out.print("Enter maximum N: ");
        int maxN = Integer.parseInt(scanner.nextLine().trim());

        // Ask where to save the output files
        System.out.print("Enter output folder path (type full address): ");
        String outputFolder = scanner.nextLine().trim();

        // Start tracking how long the processing takes
        long startTime = System.currentTimeMillis();

        // Clean the text and split it into tokens (individual words)
        List<String> tokens = TextCleaner.tokenize(inputText, lowercase, removeSpecialChars);

        // Generate all N-Grams from the tokens using the min and max N values
        List<String> ngrams = TextCleaner.generateNGrams(tokens, minN, maxN);

        // Count how many times each N-Gram appears
        Map<String, Integer> freqMap = Exporter.countFrequencies(ngrams);

        // Stop the timer
        long endTime = System.currentTimeMillis();

        // Export results to CSV and JSON in the output folder
        Exporter.exportCSV(freqMap, outputFolder + "\\ngrams.csv");
        Exporter.exportJSON(freqMap, outputFolder + "\\ngrams.json");

        // Print a summary of everything that was processed
        System.out.println("\n=== Results ===");
        System.out.println("Input size:       " + inputText.length() + " characters");
        System.out.println("Tokens found:     " + tokens.size());
        System.out.println("N-Grams generated:" + ngrams.size());
        System.out.println("Unique N-Grams:   " + freqMap.size());
        System.out.println("Processing time:  " + (endTime - startTime) + " ms");

        // Close the scanner to free up resources
    	}
    }
}