package com.feedrank.repository;

import com.feedrank.entity.Interaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
  List<Interaction> findTop200ByUserIdOrderByCreatedAtDesc(Long userId);
  long countByUserId(Long userId);
}
