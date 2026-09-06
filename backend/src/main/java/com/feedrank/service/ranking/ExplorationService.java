package com.feedrank.service.ranking;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase 5: epsilon-greedy explore/exploit. With prob epsilon, inject random /
 * under-explored-topic items into top-N instead of pure ranking-by-score.
 * Also exposes per-topic Thompson-sampling helpers (Beta sampling) for demo.
 */
@Service
public class ExplorationService {
  private final double epsilon;
  private final Random random = new Random();

  public ExplorationService(@Value("${app.ranking.exploration-epsilon:0.1}") double epsilon) {
    this.epsilon = epsilon;
  }

  public double getEpsilon() { return epsilon; }

  public List<ScoredItem> applyEpsilonGreedy(List<ScoredItem> ranked, int topN) {
    if (ranked.size() <= topN || epsilon <= 0) return ranked.stream().limit(topN).toList();
    List<ScoredItem> top = new ArrayList<>(ranked.stream().limit(topN).toList());
    List<ScoredItem> rest = ranked.stream().skip(topN).toList();
    if (rest.isEmpty()) return top;
    int exploreSlots = Math.max(1, (int) Math.round(topN * epsilon));
    for (int i = 0; i < exploreSlots && !rest.isEmpty(); i++) {
      ScoredItem pick = rest.get(random.nextInt(rest.size()));
      int slot = random.nextInt(top.size());
      top.set(slot, new ScoredItem(pick.item(), pick.score(),
          "exploration: " + pick.reason(), true));
    }
    return top;
  }

  /** Thompson sampling: sample engagement-rate estimate per topic from Beta(a,b). */
  public static double sampleBeta(int alpha, int beta, Random r) {
    return betaSample(alpha, r) / (betaSample(alpha, r) + betaSample(beta, r) + 1e-9);
  }

  private static double betaSample(int shape, Random r) {
    // Marsaglia-Tsang gamma sampler for integer-ish shapes
    if (shape <= 0) return 1e-9;
    double d = shape - 1.0 / 3, c = 1.0 / Math.sqrt(9 * d);
    while (true) {
      double x = r.nextGaussian(), v = 1 + c * x;
      if (v <= 0) continue;
      v = v * v * v;
      double u = r.nextDouble();
      if (u < 1 - 0.0331 * x * x * x * x) return d * v;
      if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) return d * v;
    }
  }

  /** Visible for tests: deterministic variant with seed. */
  public List<ScoredItem> applyEpsilonGreedy(List<ScoredItem> ranked, int topN, long seed) {
    Random r = new Random(seed);
    if (ranked.size() <= topN || epsilon <= 0) return ranked.stream().limit(topN).toList();
    List<ScoredItem> top = new ArrayList<>(ranked.stream().limit(topN).toList());
    List<ScoredItem> rest = ranked.stream().skip(topN).toList();
    if (rest.isEmpty()) return top;
    int exploreSlots = Math.max(1, (int) Math.round(topN * epsilon));
    for (int i = 0; i < exploreSlots && !rest.isEmpty(); i++) {
      ScoredItem pick = rest.get(r.nextInt(rest.size()));
      top.set(r.nextInt(top.size()),
          new ScoredItem(pick.item(), pick.score(), "exploration: " + pick.reason(), true));
    }
    return top;
  }
}
