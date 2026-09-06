package com.feedrank.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "feed_items")
public class FeedItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "external_id", nullable = false, unique = true, length = 64)
  private String externalId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String url;

  @Column(nullable = false, length = 32)
  private String source = "hackernews";

  @Column(length = 128)
  private String author;

  @Column(nullable = false)
  private int score = 0;

  /** Comma-separated tags, e.g. "ai,python,startups" */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String tags = "";

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(name = "embedding_text", columnDefinition = "TEXT")
  private String embeddingText;

  @Column(name = "hn_time")
  private Long hnTime;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public List<String> tagList() {
    if (tags == null || tags.isBlank()) return List.of();
    return Arrays.stream(tags.split(","))
        .map(String::trim).map(String::toLowerCase)
        .filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
  }

  public void setTagList(List<String> t) {
    this.tags = String.join(",", t);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getExternalId() { return externalId; }
  public void setExternalId(String externalId) { this.externalId = externalId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
  public int getScore() { return score; }
  public void setScore(int score) { this.score = score; }
  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  public String getEmbeddingText() { return embeddingText; }
  public void setEmbeddingText(String embeddingText) { this.embeddingText = embeddingText; }
  public Long getHnTime() { return hnTime; }
  public void setHnTime(Long hnTime) { this.hnTime = hnTime; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
