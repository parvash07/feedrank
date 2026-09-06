package com.feedrank.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "preference_weights")
@IdClass(PreferenceWeight.PreferenceWeightId.class)
public class PreferenceWeight {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Id
  @Column(length = 64)
  private String tag;

  @Column(nullable = false)
  private double weight = 0.0;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static class PreferenceWeightId implements Serializable {
    private Long userId;
    private String tag;
    public PreferenceWeightId() {}
    public PreferenceWeightId(Long userId, String tag) { this.userId = userId; this.tag = tag; }
    @Override public boolean equals(Object o) {
      if (!(o instanceof PreferenceWeightId other)) return false;
      return Objects.equals(userId, other.userId) && Objects.equals(tag, other.tag);
    }
    @Override public int hashCode() { return Objects.hash(userId, tag); }
  }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getTag() { return tag; }
  public void setTag(String tag) { this.tag = tag; }
  public double getWeight() { return weight; }
  public void setWeight(double weight) { this.weight = weight; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
