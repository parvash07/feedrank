package com.feedrank.mapper;

import com.feedrank.dto.response.FeedItemResponse;
import com.feedrank.service.ranking.ScoredItem;
import org.springframework.stereotype.Component;

/** Entity/ranking-result -> API response. Keeps controllers free of mapping code. */
@Component
public class FeedItemMapper {

  public FeedItemResponse toResponse(ScoredItem s) {
    var it = s.item();
    return new FeedItemResponse(
        it.getId(), it.getExternalId(), it.getTitle(), it.getUrl(),
        it.getSource(), it.getAuthor(), it.getScore(), it.tagList(),
        it.getCreatedAt().toString(),
        Math.round(s.score() * 1000.0) / 1000.0,
        s.reason(), s.exploration());
  }
}
