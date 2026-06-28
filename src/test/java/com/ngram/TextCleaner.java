package com.ngram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TextCleaner {

    // Below this size, threading overhead isn't worth it
    private static final int PARALLEL_THRESHOLD = 20_000;

    // Cleans the raw input text and returns the cleaned string.
    public static String clean(String text, boolean lowercase, boolean removeSpecialChars) {
        int cores = Runtime.getRuntime().availableProcessors();
        boolean parallel = cores > 1 && text.length() >= PARALLEL_THRESHOLD;

        if (lowercase) {
            if (parallel) {
                text = parallelToLowerCase(text, cores);
            } else {
                System.out.println("[clean] Lowercasing " + text.length() + " characters using 1 thread (sequential)");
                text = text.toLowerCase();
            }
        }

        if (removeSpecialChars) {
            if (parallel) {
                return parallelFilter(text, lowercase, cores);
            } else {
                System.out.println("[clean] Removing special characters from " + text.length() + " characters using 1 thread (sequential)");
                return filterRange(text, lowercase, 0, text.length());
            }
        }

        return text;
    }

    // Lowercases the text chunk-by-chunk across threads. 
    private static String parallelToLowerCase(String text, int numThreads) {
        int len = text.length();
        int chunkSize = (int) Math.ceil((double) len / numThreads);

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> futures = new ArrayList<>();
        int threadsUsed = 0;

        try {
            int prevEnd = 0;
            for (int t = 0; t < numThreads; t++) {
                int start = prevEnd;
                int end = Math.min(len, (t + 1) * chunkSize);
                if (end < len && Character.isHighSurrogate(text.charAt(end - 1))) {
                    end++; // pull the matching low surrogate into this chunk
                }
                if (start >= end) continue;

                final int s = start, e = end;
                futures.add(pool.submit(() -> text.substring(s, e).toLowerCase(Locale.ROOT)));
                prevEnd = end;
                threadsUsed++;
            }

            System.out.println("[clean] Lowercasing " + len + " characters using " + threadsUsed + " thread(s)");

            StringBuilder result = new StringBuilder(len);
            for (Future<String> f : futures) {
                result.append(f.get());
            }
            return result.toString();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lowercasing failed during parallel execution", e);
        } finally {
            pool.shutdown();
        }
    }

    // Filters a single [start, end) range down to allowed characters.
    private static String filterRange(String text, boolean lowercase, int start, int end) {
        StringBuilder sb = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ';
            if (!lowercase) keep = keep || (c >= 'A' && c <= 'Z');
            if (keep) sb.append(c);
        }
        return sb.toString();
    }

    // Splits the filtering work by character range across threads
    private static String parallelFilter(String text, boolean lowercase, int numThreads) {
        int len = text.length();
        int chunkSize = (int) Math.ceil((double) len / numThreads);

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<String>> futures = new ArrayList<>();
        int threadsUsed = 0;

        try {
            for (int t = 0; t < numThreads; t++) {
                final int start = t * chunkSize;
                final int end = Math.min(len, start + chunkSize);
                if (start >= end) continue;

                futures.add(pool.submit(() -> filterRange(text, lowercase, start, end)));
                threadsUsed++;
            }

            System.out.println("[clean] Removing special characters from " + len + " characters using " + threadsUsed + " thread(s)");

            StringBuilder result = new StringBuilder(len);
            for (Future<String> f : futures) {
                result.append(f.get());
            }
            return result.toString();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Text cleaning failed during parallel execution", e);
        } finally {
            pool.shutdown();
        }
    }

    // Counts N-Gram frequencies, splitting the work across threads for large inputs
    public static Map<String, Integer> countNGrams(String text, int minN, int maxN) {
        int len = text.length();
        int cores = Runtime.getRuntime().availableProcessors();

        if (cores <= 1 || len < PARALLEL_THRESHOLD) {
            System.out.println("[countNGrams] Counting N-grams across " + len + " characters using 1 thread (sequential)");
            return countNGramsSequential(text, minN, maxN, 0, len);
        }

        return countNGramsParallel(text, minN, maxN, cores);
    }

    // Original single-pass logic, now also takes a [start, end) range of starting indices
    private static Map<String, Integer> countNGramsSequential(String text, int minN, int maxN, int start, int end) {
        int len = text.length();
        Map<String, Integer> freqMap = new HashMap<>();

        for (int n = minN; n <= maxN; n++) {
            // a starting index i is only valid while i + n <= len
            int rangeEnd = Math.min(end, len - n + 1);
            for (int i = start; i < rangeEnd; i++) {
                String ngram = text.substring(i, i + n);
                freqMap.merge(ngram, 1, Integer::sum);
            }
        }

        return freqMap;
    }

    // Splits the text by STARTING INDEX (not by N value) 
    private static Map<String, Integer> countNGramsParallel(String text, int minN, int maxN, int numThreads) {
        int len = text.length();
        int chunkSize = (int) Math.ceil((double) len / numThreads);

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, Integer>>> futures = new ArrayList<>();
        int threadsUsed = 0;

        try {
            for (int t = 0; t < numThreads; t++) {
                final int start = t * chunkSize;
                final int end = Math.min(len, start + chunkSize);
                if (start >= end) continue;

                Callable<Map<String, Integer>> task = () -> countNGramsSequential(text, minN, maxN, start, end);
                futures.add(pool.submit(task));
                threadsUsed++;
            }

            System.out.println("[countNGrams] Counting N-grams across " + len + " characters using " + threadsUsed + " thread(s)");

            Map<String, Integer> result = new HashMap<>();
            for (Future<Map<String, Integer>> future : futures) {
                Map<String, Integer> partial = future.get();
                for (Map.Entry<String, Integer> entry : partial.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
            return result;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("N-Gram counting failed during parallel execution", e);
        } finally {
            pool.shutdown();
        }
    }
}
