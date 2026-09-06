package com.feedrank.dto.response;

import java.util.List;

public record FeedItemResponse(
    Long id,
    String externalId,
    String title,
    String url,
    String source,
    String author,
    int score,
    List<String> tags,
    String createdAt,
    double rankScore,
    String reason,
    boolean exploration) {}
