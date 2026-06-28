package com.ngram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TextCleaner {

    private static final int MULTITHREADING_THRESHOLD = 10_000;

    // N <= this value uses the int[] fast path and above it falls back to HashMap
    // N=4 -> 37^4 = 1.8M slots = 7 MB (I believe this is generally safe)
    // N=5 -> 37^5 = 69M slots = 264 MB threshold for fallback since its unstable after 5
    private static final int MAX_N_FOR_ARRAY = 4;

    // 37 valid characters: a-z (0-25), 0-9 (26-35), space (36)
    private static final int BASE = 37;

    // Maps a character to its base-37 index or -1 if not in the alphabet
    // A flat 128-entry lookup table avoids branching in the hot loop
    private static final int[] CHAR_INDEX = new int[128];

    static {
        java.util.Arrays.fill(CHAR_INDEX, -1);
        for (int i = 0; i < 26; i++) CHAR_INDEX['a' + i] = i;          // a-z -> 0-25
        for (int i = 0; i < 10; i++) CHAR_INDEX['0' + i] = 26 + i;     // 0-9 -> 26-35
        CHAR_INDEX[' '] = 36;                                            // space -> 36
    }

    //  Clean 

    public static String clean(String text, boolean lowercase, boolean removeSpecialChars) {

        if (!removeSpecialChars && !lowercase) return text;

        StringBuilder sb = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (lowercase && c >= 'A' && c <= 'Z') c = (char) (c + 32);

            if (removeSpecialChars) {
                boolean keep = (c >= 'a' && c <= 'z')
                        || (c >= '0' && c <= '9')
                        || c == ' '
                        || (!lowercase && c >= 'A' && c <= 'Z');
                if (keep) sb.append(c);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    // Public entry point

    public static Map<String, Integer> countNGrams(String text, int minN, int maxN) {
        if (maxN <= MAX_N_FOR_ARRAY) {
            // Fast path (int[] array per N value, no String allocation in hot loop)
            return text.length() < MULTITHREADING_THRESHOLD
                    ? countNGramsArray(text, minN, maxN)
                    : countNGramsArrayMultiThreaded(text, minN, maxN);
        } else {
            // Safe fallback for large N where arrays would be too big
            return text.length() < MULTITHREADING_THRESHOLD
                    ? countNGramsHashMap(text, minN, maxN)
                    : countNGramsHashMapMultiThreaded(text, minN, maxN);
        }
    }

    //  Array fast path (single-threaded) 

    private static Map<String, Integer> countNGramsArray(String text, int minN, int maxN) {
        int len = text.length();

        // One int[] per N value. Indexed by base-37 encoding of the N-gram
        int[][] arrays = new int[maxN - minN + 1][];
        int[] sizes = new int[maxN - minN + 1];
        for (int n = minN; n <= maxN; n++) {
            sizes[n - minN] = pow37(n);
            arrays[n - minN] = new int[sizes[n - minN]];
        }

        // Pre-convert the text to an int[] of char indices once
        int[] indices = toIndexArray(text);
        if (indices == null) {
            // if Text contains characters outside our alphabet then fall back to HashMap
            return countNGramsHashMap(text, minN, maxN);
        }

        for (int n = minN; n <= maxN; n++) {
            int[] arr = arrays[n - minN];
            for (int i = 0; i <= len - n; i++) {
                int key = 0;
                for (int j = 0; j < n; j++) key = key * BASE + indices[i + j];
                arr[key]++;
            }
        }

        return decodeArrays(arrays, sizes, minN, maxN);
    }

    //  Array fast path (multi-threaded) 

    private static Map<String, Integer> countNGramsArrayMultiThreaded(String text, int minN, int maxN) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int len = text.length();
        int chunkSize = len / numThreads;
        int overlap = maxN - 1;

        int[] indices = toIndexArray(text);
        if (indices == null) return countNGramsHashMapMultiThreaded(text, minN, maxN);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<int[][]>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int start = t * chunkSize;
            int end = (t == numThreads - 1) ? len : start + chunkSize;
            int extendedEnd = Math.min(end + overlap, len);

            // Pass the index slice, not a String — avoids substring allocation
            final int[] slice = java.util.Arrays.copyOfRange(indices, start, extendedEnd);
            futures.add(executor.submit(new ArrayNGramCounter(slice, minN, maxN)));
        }

        // Merge  (just add element-wise across the int[] arrays)
        int numN = maxN - minN + 1;
        int[][] merged = new int[numN][];
        int[] sizes = new int[numN];
        for (int n = minN; n <= maxN; n++) {
            sizes[n - minN] = pow37(n);
            merged[n - minN] = new int[sizes[n - minN]];
        }

        for (Future<int[][]> future : futures) {
            try {
                int[][] partial = future.get();
                for (int ni = 0; ni < numN; ni++) {
                    for (int k = 0; k < sizes[ni]; k++) {
                        merged[ni][k] += partial[ni][k];
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("N-gram thread failed: " + e.getMessage(), e);
            }
        }

        executor.shutdown();
        return decodeArrays(merged, sizes, minN, maxN);
    }

    private static class ArrayNGramCounter implements Callable<int[][]> {
        private final int[] indices;
        private final int minN, maxN;

        ArrayNGramCounter(int[] indices, int minN, int maxN) {
            this.indices = indices;
            this.minN = minN;
            this.maxN = maxN;
        }

        @Override
        public int[][] call() {
            int len = indices.length;
            int numN = maxN - minN + 1;
            int[][] arrays = new int[numN][];
            for (int n = minN; n <= maxN; n++) {
                arrays[n - minN] = new int[pow37(n)];
            }
            for (int n = minN; n <= maxN; n++) {
                int[] arr = arrays[n - minN];
                for (int i = 0; i <= len - n; i++) {
                    int key = 0;
                    for (int j = 0; j < n; j++) key = key * BASE + indices[i + j];
                    arr[key]++;
                }
            }
            return arrays;
        }
    }

    // HashMap fallback (single-threaded) 

    private static Map<String, Integer> countNGramsHashMap(String text, int minN, int maxN) {
        int len = text.length();
        Map<String, Integer> freqMap = new HashMap<>();
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i <= len - n; i++) {
                freqMap.merge(text.substring(i, i + n), 1, Integer::sum);
            }
        }
        return freqMap;
    }

    // HashMap fallback (multi-threaded) 

    private static Map<String, Integer> countNGramsHashMapMultiThreaded(String text, int minN, int maxN) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int len = text.length();
        int chunkSize = len / numThreads;
        int overlap = maxN - 1;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Map<String, Integer>>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; t++) {
            int start = t * chunkSize;
            int end = (t == numThreads - 1) ? len : start + chunkSize;
            int extendedEnd = Math.min(end + overlap, len);
            final String chunk = text.substring(start, extendedEnd);
            futures.add(executor.submit(new HashMapNGramCounter(chunk, minN, maxN)));
        }

        Map<String, Integer> merged = new HashMap<>();
        for (Future<Map<String, Integer>> future : futures) {
            try {
                for (Map.Entry<String, Integer> entry : future.get().entrySet()) {
                    merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("N-gram thread failed: " + e.getMessage(), e);
            }
        }

        executor.shutdown();
        return merged;
    }

    private static class HashMapNGramCounter implements Callable<Map<String, Integer>> {
        private final String chunk;
        private final int minN, maxN;

        HashMapNGramCounter(String chunk, int minN, int maxN) {
            this.chunk = chunk;
            this.minN = minN;
            this.maxN = maxN;
        }

        @Override
        public Map<String, Integer> call() {
            int len = chunk.length();
            Map<String, Integer> freqMap = new HashMap<>();
            for (int n = minN; n <= maxN; n++) {
                for (int i = 0; i <= len - n; i++) {
                    freqMap.merge(chunk.substring(i, i + n), 1, Integer::sum);
                }
            }
            return freqMap;
        }
    }

    // Helpers

    // Converts text to array of base-37 indices and Returns null if any char is outside alphabet
    private static int[] toIndexArray(String text) {
        int len = text.length();
        int[] indices = new int[len];
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            int idx = (c < 128) ? CHAR_INDEX[c] : -1;
            if (idx == -1) return null; // if unexpected char then caller falls back to HashMap
            indices[i] = idx;
        }
        return indices;
    }

    // Decodes int[] arrays back to a String->Integer map for output
    private static Map<String, Integer> decodeArrays(int[][] arrays, int[] sizes, int minN, int maxN) {
        Map<String, Integer> result = new HashMap<>();
        char[] buf = new char[maxN];

        for (int ni = 0; ni < arrays.length; ni++) {
            int n = minN + ni;
            int[] arr = arrays[ni];
            for (int key = 0; key < sizes[ni]; key++) {
                if (arr[key] == 0) continue; // skip unused slots
                // Decode base-37 integer back to characters
                int tmp = key;
                for (int j = n - 1; j >= 0; j--) {
                    buf[j] = indexToChar(tmp % BASE);
                    tmp /= BASE;
                }
                result.put(new String(buf, 0, n), arr[key]);
            }
        }

        return result;
    }

    private static int pow37(int exp) {
        int result = 1;
        for (int i = 0; i < exp; i++) result *= BASE;
        return result;
    }

    private static char indexToChar(int idx) {
        if (idx < 26) return (char) ('a' + idx);
        if (idx < 36) return (char) ('0' + idx - 26);
        return ' ';
    }
}
