package com.feedrank.service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Pure function: derive simple tags from title keywords.
 * Testable independently of the API layer.
 */
@Component
public class TagExtractor {

  private static final Set<String> STOPWORDS = Set.of(
      "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with", "is",
      "are", "was", "were", "be", "by", "at", "from", "as", "it", "its", "this",
      "that", "how", "why", "what", "when", "you", "your", "we", "i", "my",
      "after", "before", "new", "show", "ask", "hn", "vs", "via", "using",
      "way", "get", "got", "over", "under", "out", "about", "into", "than");

  private static final Pattern SPLIT = Pattern.compile("[^a-z0-9+#]+");

  /** topic alias -> canonical tag */
  private static final Map<String, String> ALIASES = Map.ofEntries(
      Map.entry("ai", "ai"), Map.entry("llm", "ai"), Map.entry("llms", "ai"),
      Map.entry("gpt", "ai"), Map.entry("ml", "ai"), Map.entry("neural", "ai"),
      Map.entry("js", "javascript"), Map.entry("ts", "typescript"),
      Map.entry("k8s", "kubernetes"), Map.entry("postgres", "postgres"),
      Map.entry("pg", "postgres"), Map.entry("db", "databases"),
      Map.entry("crypto", "crypto"), Map.entry("bitcoin", "crypto"),
      Map.entry("startup", "startups"), Map.entry("start-up", "startups"));

  public List<String> extract(String title) {
    return extract(title, 5);
  }

  public List<String> extract(String title, int maxTags) {
    if (title == null || title.isBlank()) return List.of("general");
    Map<String, Integer> freq = new LinkedHashMap<>();
    for (String raw : SPLIT.split(title.toLowerCase())) {
      String w = raw.trim();
      if (w.length() < 2 || STOPWORDS.contains(w)) continue;
      w = ALIASES.getOrDefault(w, w);
      // keep tech tokens like c++, but cap length
      if (w.length() > 24) continue;
      freq.merge(w, 1, Integer::sum);
    }
    List<String> tags = freq.entrySet().stream()
        .sorted((a, b) -> {
          int c = Integer.compare(b.getValue(), a.getValue());
          if (c != 0) return c;
          // prefer longer (more specific) tokens on ties
          return Integer.compare(b.getKey().length(), a.getKey().length());
        })
        .limit(maxTags)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    if (tags.isEmpty()) return List.of("general");
    return tags;
  }
}
