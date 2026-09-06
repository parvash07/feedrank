package com.feedrank.service.ranking;

import com.feedrank.entity.FeedItem;

public record ScoredItem(FeedItem item, double score, String reason, boolean exploration) {}
