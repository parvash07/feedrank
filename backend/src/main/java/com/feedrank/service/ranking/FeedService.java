package com.feedrank.service.ranking;

import com.feedrank.entity.FeedImpression;
import com.feedrank.entity.FeedItem;
import com.feedrank.repository.FeedImpressionRepository;
import com.feedrank.repository.FeedItemRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Orchestrates candidate fetch -> ranking -> exploration -> impression logging. */
@Service
public class FeedService {
  private final FeedItemRepository items;
  private final TagBasedRankingService tagRanking;
  private final EmbeddingRankingService embeddingRanking;
  private final ExplorationService exploration;
  private final FeedImpressionRepository impressions;
  private final String mode;

  public FeedService(FeedItemRepository items, TagBasedRankingService tagRanking,
      EmbeddingRankingService embeddingRanking, ExplorationService exploration,
      FeedImpressionRepository impressions,
      @Value("${app.ranking.mode:tag}") String mode) {
    this.items = items;
    this.tagRanking = tagRanking;
    this.embeddingRanking = embeddingRanking;
    this.exploration = exploration;
    this.impressions = impressions;
    this.mode = mode;
  }

  public List<ScoredItem> feedFor(Long userId, int limit, String modeOverride, boolean explore) {
    int pool = Math.max(limit * 5, 100);
    List<FeedItem> candidates = items.findAll(PageRequest.of(0, pool,
        org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
    RankingService rs = "embedding".equalsIgnoreCase(Objects.toString(modeOverride, mode))
        ? embeddingRanking : tagRanking;
    List<ScoredItem> ranked = rs.rank(userId, candidates, pool);
    List<ScoredItem> finalList = explore ? exploration.applyEpsilonGreedy(ranked, limit)
        : ranked.stream().limit(limit).toList();
    // log impressions (best-effort)
    try {
      for (ScoredItem s : finalList) {
        FeedImpression imp = new FeedImpression();
        imp.setUserId(userId);
        imp.setItemId(s.item().getId());
        imp.setExploration(s.exploration());
        imp.setReason((rs.name() + (s.exploration() ? "+explore" : "")) + ": " + s.reason());
        imp.setCreatedAt(Instant.now());
        impressions.save(imp);
      }
    } catch (Exception ignored) {}
    return finalList;
  }

  public List<ScoredItem> latestChronological(int limit) {
    return items.findAll(PageRequest.of(0, limit,
        org.springframework.data.domain.Sort.by("createdAt").descending()))
        .getContent().stream()
        .map(it -> new ScoredItem(it, 0, "chronological", false)).toList();
  }
}
