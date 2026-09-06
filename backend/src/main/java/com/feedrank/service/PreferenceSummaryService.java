package com.feedrank.service;

import com.feedrank.dto.response.PreferenceWeightResponse;
import com.feedrank.dto.response.PreferencesResponse;
import com.feedrank.repository.InteractionRepository;
import com.feedrank.repository.UserPreferenceVectorRepository;
import com.feedrank.service.ranking.ExplorationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the "why am I seeing this" debug view.
 * Keeps PreferencesController to a single dependency.
 */
@Service
public class PreferenceSummaryService {

  private final PreferenceService prefs;
  private final UserPreferenceVectorRepository vectors;
  private final InteractionRepository interactions;
  private final EmbeddingService embeddings;
  private final ExplorationService exploration;

  public PreferenceSummaryService(PreferenceService prefs,
      UserPreferenceVectorRepository vectors,
      InteractionRepository interactions,
      EmbeddingService embeddings,
      ExplorationService exploration) {
    this.prefs = prefs;
    this.vectors = vectors;
    this.interactions = interactions;
    this.embeddings = embeddings;
    this.exploration = exploration;
  }

  @Transactional(readOnly = true)
  public PreferencesResponse summary(Long userId) {
    var weights = prefs.weightsFor(userId).stream()
        .map(p -> new PreferenceWeightResponse(p.getTag(),
            Math.round(p.getWeight() * 1000.0) / 1000.0))
        .toList();
    var vec = vectors.findById(userId);
    return new PreferencesResponse(weights, embeddings.getProvider(),
        vec.isPresent(), embeddings.getDimension(), interactions.countByUserId(userId));
  }

  @Transactional(readOnly = true)
  public PreferenceVectorView vector(Long userId) {
    var vec = vectors.findById(userId);
    if (vec.isEmpty()) return new PreferenceVectorView(false, 0, null, null);
    String t = vec.get().getVectorText();
    return new PreferenceVectorView(true, vec.get().getVectorDim(),
        t.substring(0, Math.min(200, t.length())),
        vec.get().getUpdatedAt().toString());
  }

  public double explorationEpsilon() {
    return exploration.getEpsilon();
  }

  public record PreferenceVectorView(boolean present, int dim, String preview, String updatedAt) {}
}
