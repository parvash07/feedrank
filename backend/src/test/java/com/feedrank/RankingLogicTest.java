package com.feedrank;

import com.feedrank.entity.FeedItem;
import com.feedrank.service.CosineSimilarity;
import com.feedrank.service.ranking.TagBasedRankingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankingLogicTest {

  private FeedItem item(String title, String tags, Instant created) {
    FeedItem it = new FeedItem();
    it.setTitle(title);
    it.setTags(tags);
    it.setCreatedAt(created);
    return it;
  }

  @Test void tagScoreSumsWeights() {
    FeedItem it = item("AI stuff", "ai,python", Instant.now());
    assertEquals(3.0, TagBasedRankingService.scoreTags(it, Map.of("ai", 2.0, "python", 1.0)), 1e-9);
    assertEquals(0.0, TagBasedRankingService.scoreTags(it, Map.of()), 1e-9);
  }

  @Test void recencyDecays() {
    Instant now = Instant.now();
    double fresh = TagBasedRankingService.recency(now, now, 48);
    double old = TagBasedRankingService.recency(now.minusSeconds(48 * 3600), now, 48);
    assertEquals(1.0, fresh, 1e-9);
    assertEquals(1 / Math.E, old, 1e-6);
  }

  @Test void cosineSimilarityBasics() {
    assertEquals(1.0, CosineSimilarity.cosine(new double[]{1, 0}, new double[]{1, 0}), 1e-9);
    assertEquals(0.0, CosineSimilarity.cosine(new double[]{1, 0}, new double[]{0, 1}), 1e-9);
    assertEquals(0.0, CosineSimilarity.cosine(new double[]{0, 0}, new double[]{1, 1}), 1e-9);
    double[] v = CosineSimilarity.parse("[1,2,3]", 5);
    assertEquals(5, v.length);
    assertEquals(2.0, v[1], 1e-9);
    assertEquals(0.0, v[4], 1e-9);
  }

  @Test void preferenceDeltas() {
    assertEquals(1.0, com.feedrank.service.PreferenceService.deltaFor("click", null));
    assertEquals(2.0, com.feedrank.service.PreferenceService.deltaFor("upvote", null));
    assertEquals(-0.4, com.feedrank.service.PreferenceService.deltaFor("skip", null));
    assertEquals(1.5, com.feedrank.service.PreferenceService.deltaFor("dwell", 45_000L));
    assertEquals(0.5, com.feedrank.service.PreferenceService.deltaFor("dwell", 15_000L));
    assertEquals(0.0, com.feedrank.service.PreferenceService.deltaFor("dwell", 2_000L));
  }
}
