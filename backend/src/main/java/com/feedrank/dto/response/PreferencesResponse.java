package com.feedrank.dto.response;

import java.util.List;

public record PreferencesResponse(
    List<PreferenceWeightResponse> weights,
    String embeddingProvider,
    boolean hasPreferenceVector,
    int vectorDim,
    long totalInteractions) {}
