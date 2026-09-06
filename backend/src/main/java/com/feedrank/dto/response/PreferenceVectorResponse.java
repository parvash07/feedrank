package com.feedrank.dto.response;

public record PreferenceVectorResponse(
    boolean present,
    int dim,
    String preview,
    String updatedAt,
    double explorationEpsilon) {}
