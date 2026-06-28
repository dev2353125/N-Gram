package com.ngram;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class TextCleaner {

    // Threshold: below this N, use long-encoded keys (zero String allocation)
    private static final int MAX_ENCODED_N = 8;

    // Minimum chunk size (in bytes) before splitting into parallel subtasks
    private static final int PARALLEL_THRESHOLD = 1 << 20; // 1 MB

    //  Clean the text into a compact byte[] instead of a String
    public static byte[] cleanToBytes(String text, boolean lowercase, boolean removeSpecialChars) {
        // Pre-allocate worst-case length — we'll trim at the end
        byte[] buf = new byte[text.length()];
        int pos = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (lowercase && c >= 'A' && c <= 'Z') {
                c = (char) (c + 32); // fast toLowerCase for ASCII
            }

            if (removeSpecialChars) {
                boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ' ';
                if (!lowercase) keep = keep || (c >= 'A' && c <= 'Z');
                if (!keep) continue;
            }

            buf[pos++] = (byte) c;
        }

        // Trim to actual size without copying if already exact
        if (pos == buf.length) return buf;
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }
    // Legacy String-based clean()
    public static String clean(String text, boolean lowercase, boolean removeSpecialChars) {
        byte[] bytes = cleanToBytes(text, lowercase, removeSpecialChars);
        return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    // Counts engrams in byte
    public static Map<String, Integer> countNGrams(byte[] text, int minN, int maxN) {
        Map<String, Integer> result = new HashMap<>(estimateCapacity(text.length, minN, maxN));

        for (int n = minN; n <= maxN; n++) {
            if (n <= MAX_ENCODED_N) {
                mergeEncodedCounts(result, countEncoded(text, n), n);
            } else {
                countSubstring(text, n, result);
            }
        }
        return result;
    }

    // Overload for backward compatibility with existing Main.java and tests
    public static Map<String, Integer> countNGrams(String text, int minN, int maxN) {
        return countNGrams(
            text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
            minN, maxN
        );
    }

    // Packs n-gram bytes into a single long
    private static HashMap<Long, int[]> countEncoded(byte[] text, int n) {
        int len = text.length;
        // Capacity sized to expected unique n-grams to minimize rehashing
        HashMap<Long, int[]> map = new HashMap<>(Math.min(len - n + 1, 1 << 20));

        if (len < n) return map;

        // Build initial window key for position 0
        long key = 0;
        for (int i = 0; i < n; i++) {
            key = (key << 7) | (text[i] & 0x7F);
        }

        // Mask to remove the oldest character when sliding
        long removeMask = ~(0x7FL << (7 * (n - 1)));
        // Shift amount to slide out leftmost char
        int slideShift = 7 * (n - 1);

        // First position
        map.computeIfAbsent(key, k -> new int[1])[0]++;

        // Slide the window — O(len) with zero String allocations
        for (int i = 1; i <= len - n; i++) {
            // Remove leftmost char, shift left, add new rightmost char
            key = ((key << 7) & removeMask) | (text[i + n - 1] & 0x7FL);
            map.computeIfAbsent(key, k -> new int[1])[0]++;
        }

        return map;
    }

    // Decode a long key back into a String for the output map
    private static void mergeEncodedCounts(Map<String, Integer> result,
                                            HashMap<Long, int[]> encoded,
                                            int n) {
        byte[] buf = new byte[n];
        for (Map.Entry<Long, int[]> e : encoded.entrySet()) {
            long key = e.getKey();
            // Extract bytes from the packed long (MSB first)
            for (int i = n - 1; i >= 0; i--) {
                buf[i] = (byte) (key & 0x7F);
                key >>= 7;
            }
            String ngram = new String(buf, java.nio.charset.StandardCharsets.ISO_8859_1);
            result.merge(ngram, e.getValue()[0], Integer::sum);
        }
    }

    //   This is basically a fallback for large N counts
    private static void countSubstring(byte[] text, int n, Map<String, Integer> result) {
        String str = new String(text, java.nio.charset.StandardCharsets.ISO_8859_1);
        int len = str.length();
        for (int i = 0; i <= len - n; i++) {
            result.merge(str.substring(i, i + n), 1, Integer::sum);
        }
    }

    // This wraps countNGrams for large files
    // It Splits the byte array into different CPU-count chunks and counts independently then merges Overlap of (maxN - 1) bytes is added at each boundary to capture n-grams that would otherwise be split across chunks
    public static Map<String, Integer> countNGramsParallel(byte[] text, int minN, int maxN) {
        if (text.length < PARALLEL_THRESHOLD) {
            return countNGrams(text, minN, maxN);
        }

        int cores = Runtime.getRuntime().availableProcessors();
        int overlap = maxN - 1; // bytes shared between adjacent chunks
        int chunkSize = text.length / cores;

        @SuppressWarnings("unchecked")
        RecursiveTask<Map<String, Integer>>[] tasks = new RecursiveTask[cores];

        for (int t = 0; t < cores; t++) {
            final int start = t * chunkSize;
            final int end = (t == cores - 1)
                ? text.length
                : Math.min(start + chunkSize + overlap, text.length);

            // Copy the slice so each thread works on independent memory
            final byte[] slice = new byte[end - start];
            System.arraycopy(text, start, slice, 0, end - start);
            final int minNf = minN, maxNf = maxN;

            tasks[t] = new RecursiveTask<Map<String, Integer>>() {
                @Override
                protected Map<String, Integer> compute() {
                    return countNGrams(slice, minNf, maxNf);
                }
            };
        }

        ForkJoinPool pool = ForkJoinPool.commonPool();
        for (RecursiveTask<?> task : tasks) pool.execute(task);

        // Merge all partial maps
        Map<String, Integer> merged = new HashMap<>();
        for (RecursiveTask<Map<String, Integer>> task : tasks) {
            task.join().forEach((k, v) -> merged.merge(k, v, Integer::sum));
        }
        return merged;
    }

    // Helper: estimate initial HashMap capacity to avoid rehashing
    private static int estimateCapacity(int len, int minN, int maxN) {
        // Unique n-grams are at most min(len - n + 1, alphabet^n)
        // For character-level ngrams, alphabet = 38 (a-z + 0-9 + space)
        long cap = 0;
        for (int n = minN; n <= maxN; n++) {
            cap += Math.min(len, (long) Math.pow(38, n));
        }
        // HashMap needs load factor headroom (default 0.75)
        return (int) Math.min(cap * 4 / 3, Integer.MAX_VALUE / 2);
    }
}
