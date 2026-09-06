package com.feedrank.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InteractionRequest(
    @NotNull Long itemId,
    @Pattern(regexp = "click|upvote|skip|dwell") String interactionType,
    Long dwellTimeMs) {}
