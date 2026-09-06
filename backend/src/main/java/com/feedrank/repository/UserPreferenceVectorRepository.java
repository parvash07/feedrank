package com.feedrank.repository;

import com.feedrank.entity.UserPreferenceVector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceVectorRepository extends JpaRepository<UserPreferenceVector, Long> {
}
