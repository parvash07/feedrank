package com.feedrank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feedrank.entity.FeedItem;
import com.feedrank.repository.FeedItemRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Phase 4: item embeddings. Provider order:
 *  - provider=gemini + key: calls Gemini text-embedding-004
 *  - provider=groq  + key: Groq OpenAI-compatible embeddings endpoint
 *  - otherwise: deterministic local hash embedding (offline-friendly, demoable)
 * Results cached on feed_items.embedding_text (+ pgvector column when available).
 */
@Service
public class EmbeddingService {
  private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

  private final FeedItemRepository items;
  private final JdbcTemplate jdbc;
  private final WebClient web;
  private final String provider;
  private final String model;
  private final int dimension;
  private final String groqKey;
  private final String geminiKey;

  public EmbeddingService(FeedItemRepository items, JdbcTemplate jdbc, WebClient.Builder webBuilder,
      @Value("${app.embeddings.provider:hash}") String provider,
      @Value("${app.embeddings.model:text-embedding-004}") String model,
      @Value("${app.embeddings.dimension:768}") int dimension,
      @Value("${app.embeddings.groq-api-key:}") String groqKey,
      @Value("${app.embeddings.gemini-api-key:}") String geminiKey) {
    this.items = items;
    this.jdbc = jdbc;
    this.web = webBuilder.build();
    this.provider = provider;
    this.model = model;
    this.dimension = dimension;
    this.groqKey = groqKey == null ? "" : groqKey;
    this.geminiKey = geminiKey == null ? "" : geminiKey;
  }

  public int getDimension() { return dimension; }
  public String getProvider() { return provider; }

  public double[] getOrCreate(FeedItem item) {
    if (item.getEmbeddingText() != null && !item.getEmbeddingText().isBlank()) {
      return CosineSimilarity.parse(item.getEmbeddingText(), dimension);
    }
    double[] vec = embed(item.getTitle() + "\n" + Objects.toString(item.getSummary(), ""));
    String text = CosineSimilarity.stringify(vec);
    item.setEmbeddingText(text);
    items.save(item);
    try {
      jdbc.update("UPDATE feed_items SET embedding_vector = ?::vector WHERE id = ?",
          "[" + text.substring(1, text.length() - 1) + "]", item.getId());
    } catch (Exception e) {
      log.debug("pgvector write skipped: {}", e.toString());
    }
    return vec;
  }

  public double[] embed(String text) {
    if ("gemini".equalsIgnoreCase(provider) && !geminiKey.isBlank()) {
      try { return geminiEmbed(text); } catch (Exception e) { log.warn("gemini failed, hash fallback: {}", e.toString()); }
    }
    if ("groq".equalsIgnoreCase(provider) && !groqKey.isBlank()) {
      try { return groqEmbed(text); } catch (Exception e) { log.warn("groq failed, hash fallback: {}", e.toString()); }
    }
    return hashEmbed(text, dimension);
  }

  /** Deterministic offline embedding: hashed bag-of-words projected to dim, L2-normalized. */
  public static double[] hashEmbed(String text, int dim) {
    double[] v = new double[dim];
    if (text == null || text.isBlank()) return v;
    String[] tokens = text.toLowerCase().split("[^a-z0-9]+");
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for (String tok : tokens) {
        if (tok.length() < 2) continue;
        byte[] h = md.digest(tok.getBytes(StandardCharsets.UTF_8));
        int idx = (Byte.toUnsignedInt(h[0]) << 8 | Byte.toUnsignedInt(h[1])) % dim;
        v[idx] += 1.0;
        int idx2 = (Byte.toUnsignedInt(h[2]) << 8 | Byte.toUnsignedInt(h[3])) % dim;
        v[idx2] += 0.5;
      }
    } catch (Exception e) {
      for (String tok : tokens) v[Math.abs(tok.hashCode()) % dim] += 1.0;
    }
    double n = 0;
    for (double d : v) n += d * d;
    n = Math.sqrt(n);
    if (n > 0) for (int i = 0; i < v.length; i++) v[i] /= n;
    return v;
  }

  private double[] geminiEmbed(String text) {
    JsonNode res = web.post()
        .uri("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":embedContent?key=" + geminiKey)
        .bodyValue(Map.of("content", Map.of("parts", List.of(Map.of("text", text)))))
        .retrieve().bodyToMono(JsonNode.class).timeout(Duration.ofSeconds(20)).block();
    JsonNode vals = res.path("embedding").path("values");
    double[] v = new double[dimension];
    for (int i = 0; i < Math.min(vals.size(), dimension); i++) v[i] = vals.get(i).asDouble();
    return v;
  }

  private double[] groqEmbed(String text) {
    JsonNode res = web.post()
        .uri("https://api.groq.com/openai/v1/embeddings")
        .header("Authorization", "Bearer " + groqKey)
        .bodyValue(Map.of("model", "nomic-embed-text-v1.5", "input", text))
        .retrieve().bodyToMono(JsonNode.class).timeout(Duration.ofSeconds(20)).block();
    JsonNode vals = res.path("data").path(0).path("embedding");
    double[] v = new double[dimension];
    for (int i = 0; i < Math.min(vals.size(), dimension); i++) v[i] = vals.get(i).asDouble();
    return v;
  }
}
