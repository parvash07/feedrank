package com.feedrank.service;

/** Pure cosine-similarity helpers (Phase 4). */
public final class CosineSimilarity {
  private CosineSimilarity() {}

  public static double cosine(double[] a, double[] b) {
    if (a == null || b == null || a.length != b.length || a.length == 0) return 0.0;
    double dot = 0, na = 0, nb = 0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      na += a[i] * a[i];
      nb += b[i] * b[i];
    }
    if (na == 0 || nb == 0) return 0.0;
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
  }

  public static double[] parse(String jsonArray, int dim) {
    double[] out = new double[dim];
    if (jsonArray == null || jsonArray.isBlank()) return out;
    String t = jsonArray.trim();
    if (t.startsWith("[")) t = t.substring(1);
    if (t.endsWith("]")) t = t.substring(0, t.length() - 1);
    if (t.isBlank()) return out;
    String[] parts = t.split(",");
    for (int i = 0; i < Math.min(parts.length, dim); i++) {
      try { out[i] = Double.parseDouble(parts[i].trim()); }
      catch (NumberFormatException ignored) {}
    }
    return out;
  }

  public static String stringify(double[] v) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < v.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(String.format("%.6f", v[i]));
    }
    return sb.append(']').toString();
  }
}
