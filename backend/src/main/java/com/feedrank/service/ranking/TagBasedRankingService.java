package com.feedrank.service.ranking;

import com.feedrank.entity.FeedItem;
import com.feedrank.entity.PreferenceWeight;
import com.feedrank.repository.PreferenceWeightRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase 3 (MVP): score = sum(tag weights) blended with recency.
 * recency = exp(-ageHours / halfLifeHours). final = tagScore * (0.35 + 0.65*recency) + hnScore*0.002
 * Pure scoring logic is in static methods for testability.
 */
@Service
public class TagBasedRankingService implements RankingService {
  private final PreferenceWeightRepository weights;
  private final double halfLifeHours;

  public TagBasedRankingService(PreferenceWeightRepository weights,
      @Value("${app.ranking.recency-half-life-hours:48}") double halfLifeHours) {
    this.weights = weights;
    this.halfLifeHours = halfLifeHours;
  }

  @Override public String name() { return "tag"; }

  @Override
  public List<ScoredItem> rank(Long userId, List<FeedItem> candidates, int limit) {
    Map<String, Double> w = new HashMap<>();
    for (PreferenceWeight pw : weights.findByUserId(userId)) w.put(pw.getTag(), pw.getWeight());
    Instant now = Instant.now();
    return candidates.stream()
        .map(it -> {
          double tagScore = scoreTags(it, w);
          double rec = recency(it.getCreatedAt(), now, halfLifeHours);
          double hnBoost = Math.min(it.getScore(), 1000) * 0.002;
          double finalScore = tagScore * (0.35 + 0.65 * rec) + hnBoost + rec * 0.3;
          String reason = w.isEmpty() ? "new (no preferences yet)"
              : "tags[" + String.join(",", it.tagList()) + "]=" + String.format("%.2f", tagScore);
          return new ScoredItem(it, finalScore, reason, false);
        })
        .sorted(Comparator.comparingDouble(ScoredItem::score).reversed())
        .limit(limit)
        .toList();
  }

  public static double scoreTags(FeedItem item, Map<String, Double> w) {
    double s = 0;
    for (String t : item.tagList()) s += w.getOrDefault(t, 0.0);
    return s;
  }

  public static double recency(Instant created, Instant now, double halfLifeHours) {
    if (created == null) return 0.5;
    double ageH = Math.max(0, Duration.between(created, now).toMinutes() / 60.0);
    return Math.exp(-ageH / Math.max(halfLifeHours, 1.0));
  }
}
