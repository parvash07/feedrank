package com.feedrank.dto.response;

public record ImpressionResponse(
    Long itemId,
    boolean exploration,
    String reason,
    String createdAt) {}
