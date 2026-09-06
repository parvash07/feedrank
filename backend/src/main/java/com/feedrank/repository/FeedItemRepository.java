package com.feedrank.repository;

import com.feedrank.entity.FeedItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
  Optional<FeedItem> findByExternalId(String externalId);
  boolean existsByExternalId(String externalId);

  @Query(value = "SELECT * FROM feed_items ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
  List<FeedItem> findLatest(int limit, int offset);

  @Query(value = "SELECT * FROM feed_items WHERE embedding_text IS NULL LIMIT :limit", nativeQuery = true)
  List<FeedItem> findWithoutEmbedding(int limit);
}
