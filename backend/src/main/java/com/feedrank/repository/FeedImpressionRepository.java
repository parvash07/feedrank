package com.feedrank.repository;

import com.feedrank.entity.FeedImpression;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImpressionRepository extends JpaRepository<FeedImpression, Long> {
  List<FeedImpression> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
