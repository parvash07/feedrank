package com.feedrank.service;

import com.feedrank.entity.FeedItem;
import com.feedrank.entity.Interaction;
import com.feedrank.entity.PreferenceWeight;
import com.feedrank.entity.UserPreferenceVector;
import com.feedrank.exception.ResourceNotFoundException;
import com.feedrank.repository.FeedItemRepository;
import com.feedrank.repository.InteractionRepository;
import com.feedrank.repository.PreferenceWeightRepository;
import com.feedrank.repository.UserPreferenceVectorRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 2+3: logs interactions and maintains tag weights + preference vector.
 * Weight deltas: click +1.0, upvote +2.0, dwell>=30s +1.5, dwell 10-30s +0.5, skip -0.4.
 */
@Service
public class PreferenceService {

  private final InteractionRepository interactions;
  private final PreferenceWeightRepository weights;
  private final FeedItemRepository items;
  private final UserPreferenceVectorRepository vectors;
  private final EmbeddingService embeddings;
  private final JdbcTemplate jdbc;
  private final int vectorDim;

  public PreferenceService(InteractionRepository interactions,
      PreferenceWeightRepository weights, FeedItemRepository items,
      UserPreferenceVectorRepository vectors, EmbeddingService embeddings,
      JdbcTemplate jdbc, @Value("${app.embeddings.dimension:768}") int vectorDim) {
    this.interactions = interactions;
    this.weights = weights;
    this.items = items;
    this.vectors = vectors;
    this.embeddings = embeddings;
    this.jdbc = jdbc;
    this.vectorDim = vectorDim;
  }

  public static double deltaFor(String type, Long dwellMs) {
    return switch (type) {
      case "click" -> 1.0;
      case "upvote" -> 2.0;
      case "skip" -> -0.4;
      case "dwell" -> {
        if (dwellMs == null) yield 0.0;
        if (dwellMs >= 30_000) yield 1.5;
        if (dwellMs >= 10_000) yield 0.5;
        yield 0.0;
      }
      default -> 0.0;
    };
  }

  @Transactional
  public Interaction logInteraction(Long userId, Long itemId, String type, Long dwellMs) {
    FeedItem item = items.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("unknown item " + itemId));
    Interaction in = new Interaction();
    in.setUserId(userId);
    in.setItemId(itemId);
    in.setInteractionType(type);
    in.setDwellTimeMs(dwellMs);
    in.setCreatedAt(Instant.now());
    interactions.save(in);

    double delta = deltaFor(type, dwellMs);
    if (delta != 0) {
      for (String tag : item.tagList()) {
        PreferenceWeight pw = weights.findById(new PreferenceWeight.PreferenceWeightId(userId, tag))
            .orElseGet(() -> {
              PreferenceWeight n = new PreferenceWeight();
              n.setUserId(userId);
              n.setTag(tag);
              n.setWeight(0.0);
              return n;
            });
        pw.setWeight(round3(pw.getWeight() + delta));
        pw.setUpdatedAt(Instant.now());
        weights.save(pw);
      }
    }
    // Phase 4: fold positive engagements into the user's preference vector (EMA)
    if (delta > 0) {
      double[] itemVec = embeddings.getOrCreate(item);
      if (itemVec != null) {
        updateUserVector(userId, itemVec, 0.15);
      }
    }
    return in;
  }

  @Transactional
  public void updateUserVector(Long userId, double[] itemVec, double alpha) {
    UserPreferenceVector upv = vectors.findById(userId).orElseGet(() -> {
      UserPreferenceVector n = new UserPreferenceVector();
      n.setUserId(userId);
      n.setVectorDim(itemVec.length);
      n.setVectorText(CosineSimilarity.stringify(new double[itemVec.length]));
      return n;
    });
    double[] cur = CosineSimilarity.parse(upv.getVectorText(), itemVec.length);
    boolean allZero = Arrays.stream(cur).allMatch(d -> d == 0.0);
    double[] next = new double[itemVec.length];
    for (int i = 0; i < next.length; i++) {
      next[i] = allZero ? itemVec[i] : (1 - alpha) * cur[i] + alpha * itemVec[i];
    }
    upv.setVectorDim(itemVec.length);
    upv.setVectorText(CosineSimilarity.stringify(next));
    upv.setUpdatedAt(Instant.now());
    vectors.save(upv);
  }

  /** Time decay: multiply weights by 0.98 daily (called by scheduler). */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void scheduledDecay() {
    decayAll(0.98);
  }

  @Transactional
  public void decayAll(double factor) {
    jdbc.update("UPDATE preference_weights SET weight = weight * ?, updated_at = now()", factor);
  }

  @Transactional
  public void decayUser(Long userId, double factor) {
    weights.decayAll(userId, factor);
  }

  public List<PreferenceWeight> weightsFor(Long userId) {
    return weights.findByUserId(userId).stream()
        .sorted(Comparator.comparingDouble(PreferenceWeight::getWeight).reversed())
        .toList();
  }

  private static double round3(double d) { return Math.round(d * 1000.0) / 1000.0; }
}
