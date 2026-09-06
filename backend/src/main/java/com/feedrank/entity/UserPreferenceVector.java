package com.feedrank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_preference_vectors")
public class UserPreferenceVector {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "vector_dim", nullable = false)
  private int vectorDim = 768;

  @Column(name = "vector_text", nullable = false, columnDefinition = "TEXT")
  private String vectorText = "[]";

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public int getVectorDim() { return vectorDim; }
  public void setVectorDim(int vectorDim) { this.vectorDim = vectorDim; }
  public String getVectorText() { return vectorText; }
  public void setVectorText(String vectorText) { this.vectorText = vectorText; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
