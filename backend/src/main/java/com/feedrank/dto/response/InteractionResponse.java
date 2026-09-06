package com.feedrank.dto.response;

public record InteractionResponse(
    Long id,
    Long itemId,
    String interactionType,
    Long dwellTimeMs,
    String createdAt) {}
