package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.ChallengeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Long> {
    List<ChallengeSubmission> findByChallengeIdOrderByGradeDesc(Long challengeId);
    Optional<ChallengeSubmission> findByChallengeIdAndUserId(Long challengeId, Long userId);
    List<ChallengeSubmission> findByChallengeIdAndWinnerTrue(Long challengeId);
}
