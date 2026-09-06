package com.feedrank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interactions")
public class Interaction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "interaction_type", nullable = false, length = 16)
  private String interactionType;

  @Column(name = "dwell_time_ms")
  private Long dwellTimeMs;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getItemId() { return itemId; }
  public void setItemId(Long itemId) { this.itemId = itemId; }
  public String getInteractionType() { return interactionType; }
  public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
  public Long getDwellTimeMs() { return dwellTimeMs; }
  public void setDwellTimeMs(Long dwellTimeMs) { this.dwellTimeMs = dwellTimeMs; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
