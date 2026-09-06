package com.feedrank.service;

import com.feedrank.dto.response.AdminStatsResponse;
import com.feedrank.dto.response.ImpressionResponse;
import com.feedrank.mapper.InteractionMapper;
import com.feedrank.repository.FeedImpressionRepository;
import com.feedrank.repository.FeedItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Debug/admin reads (exploration vs exploitation log). */
@Service
public class AdminService {

  private final FeedItemRepository items;
  private final FeedImpressionRepository impressions;
  private final InteractionMapper mapper;

  public AdminService(FeedItemRepository items, FeedImpressionRepository impressions,
      InteractionMapper mapper) {
    this.items = items;
    this.impressions = impressions;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public AdminStatsResponse stats(Long userId) {
    long explore = impressions.findTop50ByUserIdOrderByCreatedAtDesc(userId)
        .stream().filter(i -> i.isExploration()).count();
    return new AdminStatsResponse(items.count(), explore);
  }

  @Transactional(readOnly = true)
  public List<ImpressionResponse> recentImpressions(Long userId) {
    return impressions.findTop50ByUserIdOrderByCreatedAtDesc(userId)
        .stream().map(mapper::toResponse).toList();
  }
}
