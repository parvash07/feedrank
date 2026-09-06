package com.feedrank.mapper;

import com.feedrank.dto.response.ImpressionResponse;
import com.feedrank.dto.response.InteractionResponse;
import com.feedrank.entity.FeedImpression;
import com.feedrank.entity.Interaction;
import org.springframework.stereotype.Component;

@Component
public class InteractionMapper {

  public InteractionResponse toResponse(Interaction in) {
    return new InteractionResponse(
        in.getId(), in.getItemId(), in.getInteractionType(),
        in.getDwellTimeMs(), in.getCreatedAt().toString());
  }

  public ImpressionResponse toResponse(FeedImpression imp) {
    return new ImpressionResponse(
        imp.getItemId(), imp.isExploration(),
        imp.getReason(), imp.getCreatedAt().toString());
  }
}
