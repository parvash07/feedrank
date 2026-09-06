package com.feedrank.service.ranking;

import com.feedrank.entity.FeedItem;
import java.util.List;

public interface RankingService {
  String name();
  List<ScoredItem> rank(Long userId, List<FeedItem> candidates, int limit);
}
