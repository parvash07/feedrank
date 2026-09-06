package com.feedrank.service.ranking;

import com.feedrank.entity.FeedItem;
import com.feedrank.repository.UserPreferenceVectorRepository;
import com.feedrank.service.CosineSimilarity;
import com.feedrank.service.EmbeddingService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase 4: cosine similarity between item embedding and user preference vector,
 * blended with the tag score + recency so cold-start still behaves.
 */
@Service
public class EmbeddingRankingService implements RankingService {
  private final UserPreferenceVectorRepository vectors;
  private final EmbeddingService embeddings;
  private final TagBasedRankingService tagFallback;
  private final double halfLifeHours;

  public EmbeddingRankingService(UserPreferenceVectorRepository vectors,
      EmbeddingService embeddings, TagBasedRankingService tagFallback,
      @Value("${app.ranking.recency-half-life-hours:48}") double halfLifeHours) {
    this.vectors = vectors;
    this.embeddings = embeddings;
    this.tagFallback = tagFallback;
    this.halfLifeHours = halfLifeHours;
  }

  @Override public String name() { return "embedding"; }

  @Override
  public List<ScoredItem> rank(Long userId, List<FeedItem> candidates, int limit) {
    var upv = vectors.findById(userId);
    if (upv.isEmpty()) return tagFallback.rank(userId, candidates, limit);
    double[] userVec = CosineSimilarity.parse(upv.get().getVectorText(), embeddings.getDimension());
    boolean empty = Arrays.stream(userVec).allMatch(d -> d == 0.0);
    if (empty) return tagFallback.rank(userId, candidates, limit);

    var now = java.time.Instant.now();
    return candidates.stream().map(it -> {
      double[] itemVec = embeddings.getOrCreate(it);
      double sim = CosineSimilarity.cosine(itemVec, userVec);
      double rec = TagBasedRankingService.recency(it.getCreatedAt(), now, halfLifeHours);
      double finalScore = sim * 5.0 * (0.4 + 0.6 * rec) + rec * 0.3;
      return new ScoredItem(it, finalScore, "cosine=" + String.format("%.3f", sim), false);
    }).sorted(Comparator.comparingDouble(ScoredItem::score).reversed()).limit(limit).toList();
  }
}
