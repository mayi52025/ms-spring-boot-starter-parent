package com.ms.middleware.console.agent.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向中文运维文档的分块器：先按句/段切开，再装进固定窗口，并带重叠。
 *
 * <p>目的不是「文学分句完美」，而是：
 * <ul>
 *   <li>索引时切成可独立命中的小块 → 检索只带回相关几句，而不是整篇手册</li>
 *   <li>重叠避免关键句落在块边界被切断</li>
 * </ul>
 * STABLE run 摘要本身很短，一般不走本分块器。
 */
public final class TextChunker {

    private TextChunker() {
    }

    /**
     * @param chunkSize 单块目标上限（字符）
     * @param overlap   相邻块重叠字符数；0 表示不重叠
     */
    public static List<String> chunk(String text, int chunkSize, int overlap) {
        // 仅防止无意义的过小窗口；测试可用更小值验证切分逻辑
        int size = Math.max(16, chunkSize);
        int ov = Math.max(0, Math.min(overlap, size / 2));
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").trim();
        if (normalized.length() <= size) {
            chunks.add(normalized);
            return chunks;
        }

        List<String> sentences = splitSentences(normalized);
        if (sentences.size() <= 1) {
            return fixedWindowWithOverlap(normalized, size, ov);
        }

        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence.length() > size) {
                // 单句超长：先把当前块落盘，再对超长句做窗口切
                flush(current, chunks);
                chunks.addAll(fixedWindowWithOverlap(sentence, size, ov));
                continue;
            }
            if (current.length() > 0 && current.length() + sentence.length() > size) {
                String finished = current.toString().trim();
                chunks.add(finished);
                current.setLength(0);
                if (ov > 0 && finished.length() > 0) {
                    String tail = finished.substring(Math.max(0, finished.length() - ov)).trim();
                    if (!tail.isEmpty()) {
                        current.append(tail);
                        if (!tail.endsWith("\n")) {
                            current.append('\n');
                        }
                    }
                }
                // 重叠尾 + 本句仍超窗：丢掉重叠，本句单独起块，避免一块无限膨胀
                if (current.length() + sentence.length() > size) {
                    current.setLength(0);
                }
            }
            current.append(sentence);
        }
        flush(current, chunks);
        return chunks;
    }

    /** 兼容旧签名：无重叠 */
    public static List<String> chunk(String text, int chunkSize) {
        return chunk(text, chunkSize, 0);
    }

    /**
     * 按中文句号/叹问/分号/换行切开；保留分隔符在句末。
     */
    static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            cur.append(c);
            if (isBoundary(c)) {
                String s = cur.toString().trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) {
            String s = cur.toString().trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean isBoundary(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；' || c == '\n'
                || c == '!' || c == '?' || c == ';';
    }

    private static List<String> fixedWindowWithOverlap(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int from = 0;
        while (from < text.length()) {
            int to = Math.min(text.length(), from + size);
            String piece = text.substring(from, to).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (to >= text.length()) {
                break;
            }
            int step = Math.max(1, size - overlap);
            from += step;
        }
        return chunks;
    }

    private static void flush(StringBuilder current, List<String> chunks) {
        if (current.length() == 0) {
            return;
        }
        String s = current.toString().trim();
        if (!s.isEmpty()) {
            chunks.add(s);
        }
        current.setLength(0);
    }
}
