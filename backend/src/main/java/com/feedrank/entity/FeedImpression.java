package com.feedrank.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feed_impressions")
public class FeedImpression {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "user_id", nullable = false)
  private Long userId;
  @Column(name = "item_id", nullable = false)
  private Long itemId;
  @Column(name = "is_exploration", nullable = false)
  private boolean exploration = false;
  @Column(columnDefinition = "TEXT")
  private String reason;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getItemId() { return itemId; }
  public void setItemId(Long itemId) { this.itemId = itemId; }
  public boolean isExploration() { return exploration; }
  public void setExploration(boolean exploration) { this.exploration = exploration; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
