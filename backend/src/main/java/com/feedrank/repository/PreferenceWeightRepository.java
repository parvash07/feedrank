package com.feedrank.repository;

import com.feedrank.entity.PreferenceWeight;
import com.feedrank.entity.PreferenceWeight.PreferenceWeightId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PreferenceWeightRepository extends JpaRepository<PreferenceWeight, PreferenceWeightId> {
  List<PreferenceWeight> findByUserId(Long userId);

  @Modifying
  @Query("UPDATE PreferenceWeight p SET p.weight = p.weight * :factor, p.updatedAt = CURRENT_TIMESTAMP WHERE p.userId = :userId")
  int decayAll(Long userId, double factor);
}
