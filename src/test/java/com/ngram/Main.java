package com.ngram;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

public class Main {

    public static void main(String[] args) throws IOException {

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("    N-Gram Generator    ");
            System.out.println("1 - Type text manually");
            System.out.println("2 - Load a .txt file, .docx, or .pdf file");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            String inputText = "";

            if (choice.equals("1")) {
                System.out.print("Enter your text: ");
                inputText = scanner.nextLine();

            } else if (choice.equals("2")) {
                System.out.print("Enter full path to your .txt, .docx, or .pdf file: ");
                String filePath = scanner.nextLine().trim().replace("\"", "");
                String lowerPath = filePath.toLowerCase();

                if (lowerPath.endsWith(".txt")) {
                    inputText = Files.readString(Paths.get(filePath), java.nio.charset.StandardCharsets.UTF_8);

                } else if (lowerPath.endsWith(".docx")) {
                    // Reads every paragraph out of the Word document and joins them with newlines
                    try (FileInputStream fis = new FileInputStream(filePath);
                         XWPFDocument document = new XWPFDocument(fis)) {

                        StringBuilder textBuilder = new StringBuilder();
                        for (XWPFParagraph paragraph : document.getParagraphs()) {
                            textBuilder.append(paragraph.getText()).append("\n");
                        }
                        inputText = textBuilder.toString();
                    }

                } else if (lowerPath.endsWith(".pdf")) {
                    // Loads the PDF and strips out all the text across every page
                    try (PDDocument document = Loader.loadPDF(new File(filePath))) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        inputText = stripper.getText(document);
                    }

                } else {
                    System.out.println("Unsupported file type, exiting.");
                    return;
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

            // Starts timer
            long startTime = System.currentTimeMillis();

            // Cleans the text
            String cleanedText = TextCleaner.clean(inputText, lowercase, removeSpecialChars);

            // counts N-Gram frequencies in a single pass
            Map<String, Integer> freqMap = TextCleaner.countNGrams(cleanedText, minN, maxN);

            // Stops timer
            long endTime = System.currentTimeMillis();

            // Exports results
            Exporter.exportCSV(freqMap, outputFolder + "\\ngrams.csv");
            Exporter.exportJSON(freqMap, outputFolder + "\\ngrams.json");

            // this computes total N-Grams mathematically so we don't have to store them
            long totalNgrams = 0;
            for (int n = minN; n <= maxN; n++) {
                long len = cleanedText.length();
                if (len >= n) totalNgrams += (len - n + 1);
            }

            System.out.println("\n=== Results ===");
            System.out.println("Input size:       " + inputText.length() + " characters");
            System.out.println("Characters found: " + cleanedText.length());
            System.out.println("N-Grams generated:" + totalNgrams);
            System.out.println("Unique N-Grams:   " + freqMap.size());
            System.out.println("Processing time:  " + (endTime - startTime) + " ms");
        }
    }
}
